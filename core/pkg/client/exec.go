package client

import (
	"bytes"
	"context"
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

// ExecSession represents an active interactive exec session in a container.
type ExecSession struct {
	stdinWriter io.WriteCloser
	cancel      context.CancelFunc
	mu          sync.Mutex
	closed      bool
}

// Write writes string data to the container's standard input.
func (s *ExecSession) Write(data string) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	if s.closed || s.stdinWriter == nil {
		return fmt.Errorf("session is closed")
	}
	_, err := s.stdinWriter.Write([]byte(data))
	return err
}

// WriteBytes writes raw byte data to the container's standard input.
func (s *ExecSession) WriteBytes(data []byte) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	if s.closed || s.stdinWriter == nil {
		return fmt.Errorf("session is closed")
	}
	_, err := s.stdinWriter.Write(data)
	return err
}

// Close terminates the exec session and releases associated resources.
func (s *ExecSession) Close() error {
	s.mu.Lock()
	defer s.mu.Unlock()
	if s.closed {
		return nil
	}
	s.closed = true
	if s.cancel != nil {
		s.cancel()
	}
	if s.stdinWriter != nil {
		return s.stdinWriter.Close()
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
			_ = session.Close()
			_ = stdinReader.Close()
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
