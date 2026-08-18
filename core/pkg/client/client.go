// Package client provides a Kubernetes client wrapper designed for Android mobile bindings.
package client

import (
	"fmt"
	"net/url"
	"time"

	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/tools/clientcmd"
	"k8s.io/client-go/tools/remotecommand"
	"k8s.io/streaming/pkg/httpstream"
)

const defaultTimeout = 30 * time.Second

type executorFactoryFunc func(config *rest.Config, method string, u *url.URL) (remotecommand.Executor, error)

func defaultExecutorFactory(config *rest.Config, method string, u *url.URL) (remotecommand.Executor, error) {
	wsExec, err := remotecommand.NewWebSocketExecutor(config, "GET", u.String())
	if err != nil {
		return remotecommand.NewSPDYExecutor(config, method, u)
	}

	spdyExec, err := remotecommand.NewSPDYExecutor(config, method, u)
	if err != nil {
		return wsExec, nil //nolint:nilerr // WebSocket executor succeeded, fallback to it if SPDY fails
	}

	return remotecommand.NewFallbackExecutor(wsExec, spdyExec, httpstream.IsUpgradeFailure)
}

// Client wraps a Kubernetes clientset for mobile Android cluster operations.
type Client struct {
	clientset       *kubernetes.Clientset
	config          *rest.Config
	timeout         time.Duration
	contentType     string
	executorFactory executorFactoryFunc
}

// NewClient creates a Client from raw kubeconfig YAML string.
func NewClient(kubeconfigYAML string) (*Client, error) {
	return NewClientFromBytes([]byte(kubeconfigYAML))
}

// NewClientFromBytes creates a Client from raw kubeconfig YAML byte slice with default settings (Protobuf enabled).
func NewClientFromBytes(data []byte) (*Client, error) {
	return NewClientWithOptions(data, 30, true)
}

// NewClientWithOptions creates a Client with custom timeout and protobuf wire format settings.
func NewClientWithOptions(data []byte, timeoutSeconds int64, useProtobuf bool) (*Client, error) {
	if len(data) == 0 {
		return nil, fmt.Errorf("kubeconfig data cannot be empty")
	}

	config, err := clientcmd.RESTConfigFromKubeConfig(data)
	if err != nil {
		return nil, fmt.Errorf("parsing kubeconfig: %w", err)
	}

	timeout := defaultTimeout
	if timeoutSeconds > 0 {
		timeout = time.Duration(timeoutSeconds) * time.Second
	}
	config.Timeout = timeout

	var contentType string
	if useProtobuf {
		contentType = runtime.ContentTypeProtobuf
		config.ContentType = runtime.ContentTypeProtobuf
		config.AcceptContentTypes = runtime.ContentTypeProtobuf + "," + runtime.ContentTypeJSON
	}

	clientset, err := kubernetes.NewForConfig(config)
	if err != nil {
		return nil, fmt.Errorf("creating clientset: %w", err)
	}

	return &Client{
		clientset:       clientset,
		config:          config,
		timeout:         timeout,
		contentType:     contentType,
		executorFactory: defaultExecutorFactory,
	}, nil
}

// NewFromPath creates a Client from a local kubeconfig file path.
func NewFromPath(filePath string) (*Client, error) {
	config, err := clientcmd.BuildConfigFromFlags("", filePath)
	if err != nil {
		return nil, fmt.Errorf("building kubeconfig from %q: %w", filePath, err)
	}
	config.Timeout = defaultTimeout
	config.ContentType = runtime.ContentTypeProtobuf
	config.AcceptContentTypes = runtime.ContentTypeProtobuf + "," + runtime.ContentTypeJSON

	clientset, err := kubernetes.NewForConfig(config)
	if err != nil {
		return nil, fmt.Errorf("creating clientset: %w", err)
	}

	return &Client{
		clientset:       clientset,
		config:          config,
		timeout:         defaultTimeout,
		contentType:     runtime.ContentTypeProtobuf,
		executorFactory: defaultExecutorFactory,
	}, nil
}

// SetTimeout updates the client timeout duration in seconds.
func (c *Client) SetTimeout(timeoutSeconds int64) {
	if timeoutSeconds > 0 {
		c.timeout = time.Duration(timeoutSeconds) * time.Second
		if c.config != nil {
			c.config.Timeout = c.timeout
		}
	}
}

// GetTimeout returns the current client timeout in seconds.
func (c *Client) GetTimeout() int64 {
	return int64(c.timeout.Seconds())
}

// StringList represents an indexed list of strings for Gomobile / Android JNI binding.
type StringList struct {
	items []string
}

// newStringList creates a StringList wrapper.
func newStringList(items []string) *StringList {
	return &StringList{items: items}
}

// Len returns the count of items in the list.
func (l *StringList) Len() int {
	if l == nil {
		return 0
	}
	return len(l.items)
}

// Get returns the string at the given index, or empty string if out of bounds.
func (l *StringList) Get(index int) string {
	if l == nil || index < 0 || index >= len(l.items) {
		return ""
	}
	return l.items[index]
}
