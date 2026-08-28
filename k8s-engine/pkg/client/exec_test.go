package client

import (
	"bytes"
	"context"
	"net/url"
	"strings"
	"sync"
	"testing"

	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/tools/remotecommand"
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

func TestExecResultStruct(t *testing.T) {
	res := ExecResult{Stdout: "out", Stderr: "err"}
	if res.Stdout != "out" || res.Stderr != "err" {
		t.Errorf("ExecResult = %+v", res)
	}
}
