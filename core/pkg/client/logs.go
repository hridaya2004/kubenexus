package client

import (
	"bufio"
	"context"
	"fmt"
	"io"
	"strings"

	corev1 "k8s.io/api/core/v1"
)

// LogCallback receives streamed container log lines and status events.
//
// Log streaming cannot be expressed through the generic JSON resource methods:
// it is an open HTTP stream rather than a single object, so it stays as a
// purpose-built binding with a callback interface.
type LogCallback interface {
	OnLogLine(line string)
	OnError(err string)
	OnDone()
}

// Logs returns the full log output for a container.
func (c *Client) Logs(namespace, podName, container string) (string, error) {
	return c.LogsWithTail(namespace, podName, container, 0)
}

// LogsWithTail returns the log output for a container with a tail lines option.
// If tailLines is greater than zero, output is limited to that many lines from
// the end of the logs.
func (c *Client) LogsWithTail(namespace, podName, container string, tailLines int64) (string, error) {
	ctx, cancel := context.WithTimeout(context.Background(), c.timeout)
	defer cancel()

	opts := &corev1.PodLogOptions{}
	if container != "" {
		opts.Container = container
	}
	if tailLines > 0 {
		opts.TailLines = &tailLines
	}

	req := c.clientset.CoreV1().Pods(namespace).GetLogs(podName, opts)
	stream, err := req.Stream(ctx)
	if err != nil {
		return "", fmt.Errorf("opening log stream: %w", err)
	}
	defer func() { _ = stream.Close() }()

	data, err := io.ReadAll(stream)
	if err != nil {
		return "", fmt.Errorf("reading logs: %w", err)
	}
	return string(data), nil
}

// StreamLogs follows pod logs and sends each line to callback until completed or
// cancelled.
func (c *Client) StreamLogs(namespace, podName, container string, callback LogCallback) {
	c.StreamLogsWithTail(namespace, podName, container, 0, callback)
}

// StreamLogsWithTail follows pod logs with a tail lines option and sends each
// line to callback until completed or cancelled. If tailLines is greater than
// zero, streaming starts that many lines from the end of the logs.
func (c *Client) StreamLogsWithTail(namespace, podName, container string, tailLines int64, callback LogCallback) {
	if callback == nil {
		return
	}

	ctx, cancel := context.WithTimeout(context.Background(), c.timeout)
	defer cancel()

	opts := &corev1.PodLogOptions{
		Follow: true,
	}
	if container != "" {
		opts.Container = container
	}
	if tailLines > 0 {
		opts.TailLines = &tailLines
	}

	req := c.clientset.CoreV1().Pods(namespace).GetLogs(podName, opts)
	stream, err := req.Stream(ctx)
	if err != nil {
		callback.OnError(fmt.Sprintf("opening log stream: %v", err))
		callback.OnDone()
		return
	}
	defer func() { _ = stream.Close() }()

	scanner := bufio.NewScanner(stream)
	scanner.Buffer(make([]byte, 0, 64*1024), 1024*1024)
	for scanner.Scan() {
		line := scanner.Text()
		if strings.TrimSpace(line) != "" {
			callback.OnLogLine(line)
		}
	}

	if err := scanner.Err(); err != nil {
		callback.OnError(fmt.Sprintf("reading logs: %v", err))
	}
	callback.OnDone()
}
