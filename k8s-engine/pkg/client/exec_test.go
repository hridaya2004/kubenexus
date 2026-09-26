package client

import (
	"bytes"
	"context"
	"errors"
	"net/url"
	"strings"
	"sync"
	"testing"
	"time"
	"unicode/utf8"

	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/tools/remotecommand"
	utilexec "k8s.io/client-go/util/exec"
)

type mockExecCallback struct {
	mu     sync.Mutex
	stdout []string
	stderr []string
	errors []string
	done   bool
}

func (m *mockExecCallback) OnStdout(data string) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.stdout = append(m.stdout, data)
}

func (m *mockExecCallback) OnStderr(data string) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.stderr = append(m.stderr, data)
}

func (m *mockExecCallback) OnError(err string) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.errors = append(m.errors, err)
}

func (m *mockExecCallback) OnDone() {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.done = true
}

func (m *mockExecCallback) isDone() bool {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.done
}

func TestMockExecCallback(t *testing.T) {
	cb := &mockExecCallback{}
	cb.OnStdout("line1")
	cb.OnStderr("line2")
	cb.OnError("something went wrong")
	cb.OnDone()

	if len(cb.stdout) != 1 || cb.stdout[0] != "line1" {
		t.Errorf("stdout = %v", cb.stdout)
	}
	if len(cb.stderr) != 1 || cb.stderr[0] != "line2" {
		t.Errorf("stderr = %v", cb.stderr)
	}
	if len(cb.errors) != 1 || cb.errors[0] != "something went wrong" {
		t.Errorf("errors = %v", cb.errors)
	}
	if !cb.isDone() {
		t.Error("done should be true")
	}
}

type mockExecutor struct {
	streamFunc func(ctx context.Context, options remotecommand.StreamOptions) error
}

func (m *mockExecutor) Stream(options remotecommand.StreamOptions) error {
	return m.StreamWithContext(context.Background(), options)
}

func (m *mockExecutor) StreamWithContext(ctx context.Context, options remotecommand.StreamOptions) error {
	if m.streamFunc != nil {
		return m.streamFunc(ctx, options)
	}
	return nil
}

// newOfflineClient builds a Client whose request construction works without a
// reachable API server, so exec can be exercised end to end with a mocked
// executor.
func newOfflineClient(t *testing.T) *Client {
	t.Helper()

	config := &rest.Config{Host: "http://localhost:8080"}
	clientset, err := kubernetes.NewForConfig(config)
	if err != nil {
		t.Fatalf("kubernetes.NewForConfig() error = %v", err)
	}

	return &Client{
		clientset: clientset,
		config:    config,
		timeout:   defaultTimeout,
	}
}

func TestDefaultExecutorFactory(t *testing.T) {
	config := &rest.Config{Host: "http://localhost:8080"}
	u, err := url.Parse("http://localhost:8080/api/v1/namespaces/default/pods/pod-1/exec")
	if err != nil {
		t.Fatalf("url.Parse() error = %v", err)
	}

	exec, err := defaultExecutorFactory(config, "POST", u)
	if err != nil {
		t.Fatalf("defaultExecutorFactory() error = %v", err)
	}
	if exec == nil {
		t.Fatal("defaultExecutorFactory() returned nil executor")
	}
}

func TestExec_EmptyCommand(t *testing.T) {
	c := &Client{timeout: defaultTimeout}
	_, err := c.Exec("default", "pod-1", "container-1", "", "")
	if err == nil {
		t.Fatal("Exec with empty command expected error, got nil")
	}
}

func TestExec_CapturesStdoutAndStderr(t *testing.T) {
	c := newOfflineClient(t)

	var gotStdin string
	c.executorFactory = func(cfg *rest.Config, method string, u *url.URL) (remotecommand.Executor, error) {
		return &mockExecutor{
			streamFunc: func(ctx context.Context, options remotecommand.StreamOptions) error {
				if options.Stdin != nil {
					var inBuf bytes.Buffer
					_, _ = inBuf.ReadFrom(options.Stdin)
					gotStdin = inBuf.String()
				}
				if options.Stdout != nil {
					_, _ = options.Stdout.Write([]byte("command stdout"))
				}
				if options.Stderr != nil {
					_, _ = options.Stderr.Write([]byte("command stderr"))
				}
				return nil
			},
		}, nil
	}

	res, err := c.Exec("default", "pod-1", "container-1", "uname -a", "input data")
	if err != nil {
		t.Fatalf("Exec() error = %v", err)
	}
	if res.Stdout != "command stdout" {
		t.Errorf("Stdout = %q, want %q", res.Stdout, "command stdout")
	}
	if res.Stderr != "command stderr" {
		t.Errorf("Stderr = %q, want %q", res.Stderr, "command stderr")
	}
	if gotStdin != "input data" {
		t.Errorf("stdin forwarded = %q, want %q", gotStdin, "input data")
	}
}

// Output captured before a failure must still reach the caller, since a command
// can write useful diagnostics and then exit non-zero.
func TestExec_ReturnsOutputAlongsideError(t *testing.T) {
	c := newOfflineClient(t)

	c.executorFactory = func(cfg *rest.Config, method string, u *url.URL) (remotecommand.Executor, error) {
		return &mockExecutor{
			streamFunc: func(ctx context.Context, options remotecommand.StreamOptions) error {
				if options.Stderr != nil {
					_, _ = options.Stderr.Write([]byte("permission denied"))
				}
				return context.DeadlineExceeded
			},
		}, nil
	}

	res, err := c.Exec("default", "pod-1", "container-1", "cat /etc/shadow", "")
	if err == nil {
		t.Fatal("Exec() expected error, got nil")
	}
	if res == nil {
		t.Fatal("Exec() returned nil result alongside error, losing captured output")
	}
	if res.Stderr != "permission denied" {
		t.Errorf("Stderr = %q, want %q", res.Stderr, "permission denied")
	}
}

// A non-zero exit is a normal outcome: the output and exit code must come back
// without an error, because gomobile discards the result whenever an error is returned.
func TestExec_NonZeroExitReturnsOutputAndCode(t *testing.T) {
	c := newOfflineClient(t)

	c.executorFactory = func(cfg *rest.Config, method string, u *url.URL) (remotecommand.Executor, error) {
		return &mockExecutor{
			streamFunc: func(ctx context.Context, options remotecommand.StreamOptions) error {
				_, _ = options.Stderr.Write([]byte("ls: /nope: No such file or directory"))
				return utilexec.CodeExitError{
					Err:  errors.New("command terminated with non-zero exit code: 2"),
					Code: 2,
				}
			},
		}, nil
	}

	res, err := c.Exec("default", "pod-1", "c", "ls /nope", "")
	if err != nil {
		t.Fatalf("Exec() error = %v, want nil for a command that ran", err)
	}
	if res.ExitCode != 2 {
		t.Errorf("ExitCode = %d, want 2", res.ExitCode)
	}
	if !strings.Contains(res.Stderr, "No such file") {
		t.Errorf("Stderr = %q, want the command's message", res.Stderr)
	}
}

func TestStartTerminal_NilCallback(t *testing.T) {
	c := &Client{timeout: defaultTimeout}
	_, err := c.StartTerminal("default", "pod-1", "container-1", nil)
	if err == nil {
		t.Fatal("StartTerminal with nil callback expected error, got nil")
	}
}

func TestStartExecSession_NilCallback(t *testing.T) {
	c := &Client{timeout: defaultTimeout}
	_, err := c.StartExecSession("default", "pod-1", "container-1", "/bin/sh", true, nil)
	if err == nil {
		t.Fatal("StartExecSession with nil callback expected error, got nil")
	}
}

func TestStartExecSession_StreamsToCallback(t *testing.T) {
	c := newOfflineClient(t)

	streamDone := make(chan struct{})
	c.executorFactory = func(cfg *rest.Config, method string, u *url.URL) (remotecommand.Executor, error) {
		return &mockExecutor{
			streamFunc: func(ctx context.Context, options remotecommand.StreamOptions) error {
				defer close(streamDone)
				if options.Stdout != nil {
					_, _ = options.Stdout.Write([]byte("$ "))
				}
				return nil
			},
		}, nil
	}

	cb := &mockExecCallback{}
	session, err := c.StartExecSession("default", "pod-1", "container-1", "/bin/sh", true, cb)
	if err != nil {
		t.Fatalf("StartExecSession() error = %v", err)
	}
	if session == nil {
		t.Fatal("StartExecSession() returned nil session")
	}
	<-streamDone

	if err := session.Close(); err != nil {
		t.Errorf("Close() error = %v", err)
	}
}

// With a TTY there is no separate stderr stream, matching kubectl behaviour.
func TestStartExecSession_TTYHasNoStderrStream(t *testing.T) {
	c := newOfflineClient(t)

	gotStderr := make(chan bool, 1)
	c.executorFactory = func(cfg *rest.Config, method string, u *url.URL) (remotecommand.Executor, error) {
		return &mockExecutor{
			streamFunc: func(ctx context.Context, options remotecommand.StreamOptions) error {
				gotStderr <- options.Stderr != nil
				return nil
			},
		}, nil
	}

	session, err := c.StartExecSession("default", "pod-1", "c", "/bin/sh", true, &mockExecCallback{})
	if err != nil {
		t.Fatalf("StartExecSession() error = %v", err)
	}
	defer func() { _ = session.Close() }()

	if hasStderr := <-gotStderr; hasStderr {
		t.Error("Stderr stream is set for a TTY session, want nil")
	}
}

func TestStartExecSession_DefaultsBlankCommandToShell(t *testing.T) {
	c := newOfflineClient(t)

	gotCommand := make(chan string, 1)
	c.executorFactory = func(cfg *rest.Config, method string, u *url.URL) (remotecommand.Executor, error) {
		gotCommand <- u.RawQuery
		return &mockExecutor{}, nil
	}

	session, err := c.StartExecSession("default", "pod-1", "c", "   ", false, &mockExecCallback{})
	if err != nil {
		t.Fatalf("StartExecSession() error = %v", err)
	}
	defer func() { _ = session.Close() }()

	if query := <-gotCommand; !strings.Contains(query, "%2Fbin%2Fsh") && !strings.Contains(query, "/bin/sh") {
		t.Errorf("request query = %q, want it to carry /bin/sh", query)
	}
}

func TestExecSession_ClosedOperations(t *testing.T) {
	s := &ExecSession{}
	if err := s.Close(); err != nil {
		t.Errorf("Close() error = %v", err)
	}
	if err := s.Close(); err != nil {
		t.Errorf("second Close() error = %v", err)
	}

	if err := s.Write("data"); err == nil {
		t.Error("Write() on closed session expected error, got nil")
	}
	if err := s.WriteBytes([]byte("data")); err == nil {
		t.Error("WriteBytes() on closed session expected error, got nil")
	}
}

// A write issued while the stream is still connecting blocks, because nothing reads
// stdin yet. If the connection then fails, the write must be released and the session
// must still shut down; previously the write held the session lock that shutdown needed,
// so the write, Close and OnDone all hung forever.
func TestExecSession_WriteDuringFailedConnectDoesNotDeadlock(t *testing.T) {
	c := newOfflineClient(t)

	release := make(chan struct{})
	c.executorFactory = func(cfg *rest.Config, method string, u *url.URL) (remotecommand.Executor, error) {
		return &mockExecutor{
			streamFunc: func(ctx context.Context, options remotecommand.StreamOptions) error {
				<-release // never reads stdin, like a stream that fails to connect
				return errors.New("error dialing backend: 403 Forbidden")
			},
		}, nil
	}

	cb := &mockExecCallback{}
	session, err := c.StartExecSession("default", "pod-1", "c", "/bin/sh", true, cb)
	if err != nil {
		t.Fatalf("StartExecSession() error = %v", err)
	}

	writeErr := make(chan error, 1)
	go func() { writeErr <- session.WriteBytes([]byte("ls\r")) }()
	time.Sleep(50 * time.Millisecond) // let the write block on the pipe
	close(release)

	select {
	case err := <-writeErr:
		if !errors.Is(err, errSessionClosed) {
			t.Errorf("WriteBytes() error = %v, want %v", err, errSessionClosed)
		}
	case <-time.After(2 * time.Second):
		t.Fatal("WriteBytes() still blocked after the stream failed")
	}

	closed := make(chan error, 1)
	go func() { closed <- session.Close() }()
	select {
	case <-closed:
	case <-time.After(2 * time.Second):
		t.Fatal("Close() blocked")
	}

	deadline := time.Now().Add(2 * time.Second)
	for !cb.isDone() {
		if time.Now().After(deadline) {
			t.Fatal("OnDone was never delivered")
		}
		time.Sleep(10 * time.Millisecond)
	}
	if err := session.Write("x"); !errors.Is(err, errSessionClosed) {
		t.Errorf("Write() after end = %v, want %v", err, errSessionClosed)
	}
}

// Close must release a write that is blocked because the remote side is not reading.
func TestExecSession_CloseUnblocksPendingWrite(t *testing.T) {
	c := newOfflineClient(t)
	c.executorFactory = func(cfg *rest.Config, method string, u *url.URL) (remotecommand.Executor, error) {
		return &mockExecutor{
			streamFunc: func(ctx context.Context, options remotecommand.StreamOptions) error {
				<-ctx.Done()
				return ctx.Err()
			},
		}, nil
	}

	session, err := c.StartExecSession("default", "pod-1", "c", "/bin/sh", true, &mockExecCallback{})
	if err != nil {
		t.Fatalf("StartExecSession() error = %v", err)
	}

	writeErr := make(chan error, 1)
	go func() { writeErr <- session.Write("pending") }()
	time.Sleep(50 * time.Millisecond)

	if err := session.Close(); err != nil {
		t.Errorf("Close() error = %v", err)
	}
	select {
	case err := <-writeErr:
		if !errors.Is(err, errSessionClosed) {
			t.Errorf("Write() error = %v, want %v", err, errSessionClosed)
		}
	case <-time.After(2 * time.Second):
		t.Fatal("Write() still blocked after Close()")
	}
}

// Resize must reach remotecommand's size queue so the remote PTY lays out for the
// phone's real grid instead of a default size.
func TestExecSession_ResizeFeedsTerminalSizeQueue(t *testing.T) {
	c := newOfflineClient(t)

	gotSize := make(chan *remotecommand.TerminalSize, 1)
	c.executorFactory = func(cfg *rest.Config, method string, u *url.URL) (remotecommand.Executor, error) {
		return &mockExecutor{
			streamFunc: func(ctx context.Context, options remotecommand.StreamOptions) error {
				if options.TerminalSizeQueue == nil {
					gotSize <- nil
					return nil
				}
				gotSize <- options.TerminalSizeQueue.Next()
				<-ctx.Done()
				return nil
			},
		}, nil
	}

	session, err := c.StartExecSession("default", "pod-1", "c", "/bin/sh", true, &mockExecCallback{})
	if err != nil {
		t.Fatalf("StartExecSession() error = %v", err)
	}
	defer func() { _ = session.Close() }()

	// Only the latest of several quick resizes matters.
	if err := session.Resize(80, 24); err != nil {
		t.Fatalf("Resize() error = %v", err)
	}
	if err := session.Resize(46, 31); err != nil {
		t.Fatalf("Resize() error = %v", err)
	}

	select {
	case size := <-gotSize:
		if size == nil {
			t.Fatal("TTY session has no TerminalSizeQueue")
		}
		if size.Width != 46 || size.Height != 31 {
			t.Errorf("remote size = %dx%d, want 46x31", size.Width, size.Height)
		}
	case <-time.After(2 * time.Second):
		t.Fatal("no size reached the queue")
	}

	if err := session.Resize(0, 10); err == nil {
		t.Error("Resize(0, 10) error = nil, want an error")
	}
	_ = session.Close()
	if err := session.Resize(80, 24); !errors.Is(err, errSessionClosed) {
		t.Errorf("Resize() after Close = %v, want %v", err, errSessionClosed)
	}
}

// Without a TTY there is no window size, and the stream must not be given a queue.
func TestExecSession_NoSizeQueueWithoutTTY(t *testing.T) {
	c := newOfflineClient(t)

	hasQueue := make(chan bool, 1)
	c.executorFactory = func(cfg *rest.Config, method string, u *url.URL) (remotecommand.Executor, error) {
		return &mockExecutor{
			streamFunc: func(ctx context.Context, options remotecommand.StreamOptions) error {
				hasQueue <- options.TerminalSizeQueue != nil
				return nil
			},
		}, nil
	}

	session, err := c.StartExecSession("default", "pod-1", "c", "/bin/sh", false, &mockExecCallback{})
	if err != nil {
		t.Fatalf("StartExecSession() error = %v", err)
	}
	defer func() { _ = session.Close() }()
	if <-hasQueue {
		t.Error("non-TTY session was given a TerminalSizeQueue")
	}
	if err := session.Resize(80, 24); err != nil && !errors.Is(err, errSessionClosed) {
		t.Errorf("Resize() on non-TTY session = %v, want nil", err)
	}
}

func TestCallbackWriter(t *testing.T) {
	var written []string
	cw := &callbackWriter{
		fn: func(s string) {
			written = append(written, s)
		},
	}

	n, err := cw.Write([]byte("hello"))
	if err != nil || n != 5 {
		t.Errorf("Write() n = %d, err = %v", n, err)
	}
	if len(written) != 1 || written[0] != "hello" {
		t.Errorf("written = %v, want ['hello']", written)
	}

	n, err = cw.Write([]byte(""))
	if err != nil || n != 0 {
		t.Errorf("Write('') n = %d, err = %v", n, err)
	}
	if len(written) != 1 {
		t.Errorf("written length after empty write = %d, want 1", len(written))
	}

	cwNil := &callbackWriter{fn: nil}
	_, err = cwNil.Write([]byte("test"))
	if err != nil {
		t.Errorf("Write with nil fn error = %v", err)
	}
}

// A multi-byte character split across two stream chunks must reach the callback
// intact rather than as replacement characters.
func TestCallbackWriter_HoldsBackSplitUTF8(t *testing.T) {
	var got []string
	cw := &callbackWriter{fn: func(s string) { got = append(got, s) }}

	box := []byte("┌─┐ 界 🙂")
	for i := range box {
		if _, err := cw.Write(box[i : i+1]); err != nil {
			t.Fatalf("Write() error = %v", err)
		}
	}
	cw.Flush()

	joined := strings.Join(got, "")
	if joined != string(box) {
		t.Fatalf("callback received %q, want %q", joined, string(box))
	}
	for _, chunk := range got {
		if !utf8.ValidString(chunk) {
			t.Errorf("chunk %q is not valid UTF-8; gomobile would turn it into U+FFFD", chunk)
		}
	}
}

// Invalid bytes are passed through rather than held back forever.
func TestCallbackWriter_PassesInvalidBytesThrough(t *testing.T) {
	var got []string
	cw := &callbackWriter{fn: func(s string) { got = append(got, s) }}

	if _, err := cw.Write([]byte{'a', 0xff, 'b'}); err != nil {
		t.Fatalf("Write() error = %v", err)
	}
	if len(got) != 1 || got[0] != "a\xffb" {
		t.Fatalf("got %q, want the chunk delivered unchanged", got)
	}
}

// Bytes still held back when the stream ends are delivered by Flush.
func TestCallbackWriter_FlushDeliversTruncatedTail(t *testing.T) {
	var got []string
	cw := &callbackWriter{fn: func(s string) { got = append(got, s) }}

	_, _ = cw.Write([]byte{'o', 'k', 0xe7, 0x95}) // "ok" + first two bytes of 界
	if len(got) != 1 || got[0] != "ok" {
		t.Fatalf("before Flush got %q, want [\"ok\"]", got)
	}
	cw.Flush()
	if len(got) != 2 || got[1] != "\xe7\x95" {
		t.Fatalf("after Flush got %q, want the held-back bytes", got)
	}
}

func TestExecResultStruct(t *testing.T) {
	res := ExecResult{Stdout: "out", Stderr: "err"}
	if res.Stdout != "out" || res.Stderr != "err" {
		t.Errorf("ExecResult = %+v", res)
	}
}
