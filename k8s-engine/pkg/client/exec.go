package client

import (
	"bytes"
	"context"
	"errors"
	"fmt"
	"io"
	"strings"
	"sync"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/tools/remotecommand"
)

// ExecResult contains the captured stdout and stderr from a command execution.
type ExecResult struct {
	Stdout string `json:"stdout"`
	Stderr string `json:"stderr"`
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
	mu          sync.Mutex
	closed      bool
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

type callbackWriter struct {
	fn func(string)
}

func (w *callbackWriter) Write(p []byte) (int, error) {
	if len(p) > 0 && w.fn != nil {
		w.fn(string(p))
	}
	return len(p), nil
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
		Command: []string{"/bin/sh", "-c", cmdTrimmed},
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

	if err != nil {
		return result, fmt.Errorf("executing command: %w", err)
	}

	return result, nil
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
		Command: []string{"/bin/sh", "-c", cmdTrimmed},
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

	stdoutWriter := &callbackWriter{fn: callback.OnStdout}
	var stderrWriter io.Writer
	if !tty {
		stderrWriter = &callbackWriter{fn: callback.OnStderr}
	}

	go func() {
		defer callback.OnDone()
		defer func() {
			// Fail pending and future writes before anything else, so a writer blocked
			// on a stream that never started reading stdin is released.
			_ = stdinReader.CloseWithError(errSessionClosed)
			_ = session.Close()
		}()

		err := exec.StreamWithContext(sessionCtx, remotecommand.StreamOptions{
			Stdin:  stdinReader,
			Stdout: stdoutWriter,
			Stderr: stderrWriter,
			Tty:    tty,
		})
		if err != nil && sessionCtx.Err() == nil {
			callback.OnError(err.Error())
		}
	}()

	return session, nil
}
