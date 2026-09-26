package client

import (
	"bytes"
	"context"
	"errors"
	"fmt"
	"io"
	"strings"
	"sync"
	"unicode/utf8"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/tools/remotecommand"
	utilexec "k8s.io/client-go/util/exec"
)

// ExecResult contains the captured stdout and stderr from a command execution, and
// the command's exit code.
type ExecResult struct {
	Stdout   string `json:"stdout"`
	Stderr   string `json:"stderr"`
	ExitCode int32  `json:"exitCode"`
}

// ExecCallback receives streamed output and lifecycle events for an interactive
// exec session.
//
// Like log streaming, exec is a bidirectional stream rather than a request and
// response, so it stays a purpose-built binding rather than moving to the
// generic JSON resource methods.
type ExecCallback interface {
	OnStdout(data string)
	OnStderr(data string)
	OnError(err string)
	OnDone()
}

// errSessionClosed is returned by writes to a session that has ended.
var errSessionClosed = errors.New("session is closed")

// ExecSession represents an active interactive exec session in a container.
//
// mu guards only the closed flag and field reads. It is never held across a pipe
// write: stdin is an io.Pipe, so a write blocks until the remote side reads it,
// and holding the lock there would stop Close (and the session's own shutdown)
// from ever running.
type ExecSession struct {
	stdinWriter io.WriteCloser
	cancel      context.CancelFunc
	sizeQueue   *terminalSizeQueue // nil for sessions without a TTY
	mu          sync.Mutex
	closed      bool
}

// Resize tells the remote TTY its window size in character cells, as a terminal
// emulator does with TIOCSWINSZ. Sizes are coalesced so only the latest pending one is
// sent. It is a no-op for sessions without a TTY and fails once the session has ended.
func (s *ExecSession) Resize(cols, rows int32) error {
	if cols <= 0 || rows <= 0 {
		return fmt.Errorf("invalid terminal size %dx%d", cols, rows)
	}
	s.mu.Lock()
	queue, closed := s.sizeQueue, s.closed
	s.mu.Unlock()
	if closed {
		return errSessionClosed
	}
	if queue != nil {
		queue.push(remotecommand.TerminalSize{Width: clampUint16(cols), Height: clampUint16(rows)})
	}
	return nil
}

func clampUint16(v int32) uint16 {
	if v > 0xffff {
		return 0xffff
	}
	return uint16(v)
}

// terminalSizeQueue implements remotecommand.TerminalSizeQueue. Only the most recent
// size matters, so push replaces a size that has not been consumed yet.
type terminalSizeQueue struct {
	sizes chan remotecommand.TerminalSize
	done  <-chan struct{}
}

func newTerminalSizeQueue(done <-chan struct{}) *terminalSizeQueue {
	return &terminalSizeQueue{sizes: make(chan remotecommand.TerminalSize, 1), done: done}
}

func (q *terminalSizeQueue) push(size remotecommand.TerminalSize) {
	for {
		select {
		case q.sizes <- size:
			return
		default:
		}
		select {
		case <-q.sizes: // drop the stale, unconsumed size
		default:
		}
	}
}

// Next blocks until a size is pushed or the session ends. Returning nil tells
// remotecommand to stop watching for resizes.
func (q *terminalSizeQueue) Next() *remotecommand.TerminalSize {
	select {
	case size := <-q.sizes:
		return &size
	case <-q.done:
		return nil
	}
}

// Write writes string data to the container's standard input.
func (s *ExecSession) Write(data string) error {
	return s.WriteBytes([]byte(data))
}

// WriteBytes writes raw byte data to the container's standard input. It blocks until
// the remote side has consumed the data, so callers must not invoke it on a UI thread.
// Once the session has ended it returns "session is closed".
func (s *ExecSession) WriteBytes(data []byte) error {
	s.mu.Lock()
	writer, closed := s.stdinWriter, s.closed
	s.mu.Unlock()
	if closed || writer == nil {
		return errSessionClosed
	}
	if _, err := writer.Write(data); err != nil {
		if errors.Is(err, io.ErrClosedPipe) || errors.Is(err, errSessionClosed) {
			return errSessionClosed
		}
		return err
	}
	return nil
}

// Close terminates the exec session and releases associated resources. It unblocks
// any write in progress and is safe to call more than once.
func (s *ExecSession) Close() error {
	s.mu.Lock()
	if s.closed {
		s.mu.Unlock()
		return nil
	}
	s.closed = true
	cancel, writer := s.cancel, s.stdinWriter
	s.mu.Unlock()

	if cancel != nil {
		cancel()
	}
	if writer != nil {
		return writer.Close()
	}
	return nil
}

// callbackWriter forwards stream output to a string callback.
//
// Output arrives in arbitrary chunks, and gomobile converts each string to UTF-16,
// replacing every invalid byte with U+FFFD. A multi-byte character split across two
// chunks (box drawing, CJK, emoji) would therefore arrive as replacement characters,
// so an incomplete trailing sequence is held back until the next chunk completes it.
// A writer is used by a single stream-copy goroutine, so it needs no locking.
type callbackWriter struct {
	fn      func(string)
	pending []byte
}

func (w *callbackWriter) Write(p []byte) (int, error) {
	n := len(p)
	if n == 0 || w.fn == nil {
		return n, nil
	}
	buf := p
	if len(w.pending) > 0 {
		buf = append(w.pending, p...)
		w.pending = nil
	}
	cut := completeUTF8Prefix(buf)
	if cut < len(buf) {
		w.pending = append([]byte(nil), buf[cut:]...)
	}
	if cut > 0 {
		w.fn(string(buf[:cut]))
	}
	return n, nil
}

// Flush delivers any held-back bytes. It is called once the stream has ended.
func (w *callbackWriter) Flush() {
	if len(w.pending) > 0 && w.fn != nil {
		w.fn(string(w.pending))
	}
	w.pending = nil
}

// completeUTF8Prefix returns the length of buf without a trailing incomplete UTF-8
// sequence. Invalid bytes are not held back; only a valid-looking prefix of a
// multi-byte character at the very end is.
func completeUTF8Prefix(buf []byte) int {
	for i := len(buf) - 1; i >= 0 && i >= len(buf)-utf8.UTFMax; i-- {
		if utf8.RuneStart(buf[i]) {
			if utf8.FullRune(buf[i:]) {
				return len(buf)
			}
			return i
		}
	}
	return len(buf)
}

// Exec executes a non-interactive command inside a pod container and returns
// stdout and stderr. The command parameter can be a single command string such
// as "ls -la" or "uname -a".
func (c *Client) Exec(namespace, podName, container, command, stdin string) (*ExecResult, error) {
	ctx, cancel := context.WithTimeout(context.Background(), c.timeout)
	defer cancel()

	cmdTrimmed := strings.TrimSpace(command)
	if cmdTrimmed == "" {
		return nil, fmt.Errorf("command cannot be empty")
	}

	opts := &corev1.PodExecOptions{
		Command: commandArgv(cmdTrimmed),
		Stdout:  true,
		Stderr:  true,
		TTY:     false,
	}
	if container != "" {
		opts.Container = container
	}
	if stdin != "" {
		opts.Stdin = true
	}

	req := c.clientset.CoreV1().RESTClient().Post().
		Resource("pods").
		Namespace(namespace).
		Name(podName).
		SubResource("exec").
		VersionedParams(opts, scheme.ParameterCodec)

	execFactory := c.executorFactory
	if execFactory == nil {
		execFactory = defaultExecutorFactory
	}

	exec, err := execFactory(c.config, "POST", req.URL())
	if err != nil {
		return nil, fmt.Errorf("creating exec executor: %w", err)
	}

	var stdout, stderr bytes.Buffer
	var stdinReader io.Reader
	if stdin != "" {
		stdinReader = strings.NewReader(stdin)
	}

	err = exec.StreamWithContext(ctx, remotecommand.StreamOptions{
		Stdin:  stdinReader,
		Stdout: &stdout,
		Stderr: &stderr,
		Tty:    false,
	})

	result := &ExecResult{
		Stdout: stdout.String(),
		Stderr: stderr.String(),
	}

	// A command that ran and exited non-zero is a result, not a failure to execute.
	// gomobile turns a returned error into an exception and discards the result, so
	// returning the exit status as an error would lose the output that explains it.
	var exitErr utilexec.ExitError
	if errors.As(err, &exitErr) && exitErr.Exited() {
		result.ExitCode = int32(exitErr.ExitStatus())
		return result, nil
	}
	if err != nil {
		return result, fmt.Errorf("executing command: %w", err)
	}

	return result, nil
}

// shellMetacharacters are the characters that make a command need a shell to run.
const shellMetacharacters = " \t\n|&;<>()$`\\\"'*?[]#~=%{}!"

// commandArgv turns a command string into the exec argv. A single word with nothing a
// shell would interpret, such as "/bin/bash" or "/busybox/sh", is run directly so it
// works in images without /bin/sh (distroless :debug ships only /busybox/sh). Anything
// else is a command line and goes through /bin/sh -c.
func commandArgv(command string) []string {
	if !strings.ContainsAny(command, shellMetacharacters) {
		return []string{command}
	}
	return []string{"/bin/sh", "-c", command}
}

// StartTerminal starts an interactive shell session (/bin/sh) with a TTY for an
// Android terminal emulator.
func (c *Client) StartTerminal(namespace, podName, container string, callback ExecCallback) (*ExecSession, error) {
	return c.StartExecSession(namespace, podName, container, "/bin/sh", true, callback)
}

// StartExecSession starts an interactive exec session in a container with
// streaming callbacks and TTY support.
func (c *Client) StartExecSession(namespace, podName, container, command string, tty bool, callback ExecCallback) (*ExecSession, error) {
	if callback == nil {
		return nil, fmt.Errorf("callback cannot be nil")
	}

	cmdTrimmed := strings.TrimSpace(command)
	if cmdTrimmed == "" {
		cmdTrimmed = "/bin/sh"
	}

	opts := &corev1.PodExecOptions{
		Command: commandArgv(cmdTrimmed),
		Stdin:   true,
		Stdout:  true,
		Stderr:  !tty,
		TTY:     tty,
	}
	if container != "" {
		opts.Container = container
	}

	req := c.clientset.CoreV1().RESTClient().Post().
		Resource("pods").
		Namespace(namespace).
		Name(podName).
		SubResource("exec").
		VersionedParams(opts, scheme.ParameterCodec)

	execFactory := c.executorFactory
	if execFactory == nil {
		execFactory = defaultExecutorFactory
	}

	exec, err := execFactory(c.config, "POST", req.URL())
	if err != nil {
		return nil, fmt.Errorf("creating exec executor: %w", err)
	}

	sessionCtx, cancel := context.WithCancel(context.Background())
	stdinReader, stdinWriter := io.Pipe()

	session := &ExecSession{
		stdinWriter: stdinWriter,
		cancel:      cancel,
	}
	if tty {
		session.sizeQueue = newTerminalSizeQueue(sessionCtx.Done())
	}

	stdoutWriter := &callbackWriter{fn: callback.OnStdout}
	var stderrCallbackWriter *callbackWriter
	var stderrWriter io.Writer
	if !tty {
		stderrCallbackWriter = &callbackWriter{fn: callback.OnStderr}
		stderrWriter = stderrCallbackWriter
	}

	go func() {
		defer callback.OnDone()
		defer func() {
			// Fail pending and future writes before anything else, so a writer blocked
			// on a stream that never started reading stdin is released.
			_ = stdinReader.CloseWithError(errSessionClosed)
			_ = session.Close()
		}()

		streamOptions := remotecommand.StreamOptions{
			Stdin:  stdinReader,
			Stdout: stdoutWriter,
			Stderr: stderrWriter,
			Tty:    tty,
		}
		// Assigned only when set: a nil *terminalSizeQueue in the interface field would
		// be a non-nil interface that remotecommand calls Next on.
		if session.sizeQueue != nil {
			streamOptions.TerminalSizeQueue = session.sizeQueue
		}
		err := exec.StreamWithContext(sessionCtx, streamOptions)
		stdoutWriter.Flush()
		if stderrCallbackWriter != nil {
			stderrCallbackWriter.Flush()
		}
		if err != nil && sessionCtx.Err() == nil {
			callback.OnError(err.Error())
		}
	}()

	return session, nil
}
