// Package client provides a Kubernetes client wrapper designed for Android mobile bindings.
package client

import (
	"fmt"
	"net/url"
	"time"

	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/client-go/dynamic"
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
//
// clientset is retained for endpoints that are not expressible generically:
// discovery, health probes, log streaming and exec. All resource reads and
// deletes go through dynamic, which returns unstructured objects that can be
// handed to Android as verbatim JSON.
type Client struct {
	clientset       *kubernetes.Clientset
	dynamic         dynamic.Interface
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

	return newClientFromConfig(config, timeout, contentType)
}

// newClientFromConfig builds the typed and dynamic clients from a prepared
// rest.Config.
//
// Resource reads go through the dynamic client, which copies the config and
// forces JSON content negotiation, so its response bodies are larger than the
// typed clientset's protobuf. Response compression claws most of that back and
// matters materially on a cellular connection, so it is enabled explicitly here
// rather than left to the rest.Config zero value.
func newClientFromConfig(config *rest.Config, timeout time.Duration, contentType string) (*Client, error) {
	config.DisableCompression = false

	clientset, err := kubernetes.NewForConfig(config)
	if err != nil {
		return nil, fmt.Errorf("creating clientset: %w", err)
	}

	// dynamic.NewForConfig copies the config before forcing JSON, so this does
	// not disturb the protobuf negotiation used by clientset above.
	dyn, err := dynamic.NewForConfig(config)
	if err != nil {
		return nil, fmt.Errorf("creating dynamic client: %w", err)
	}

	return &Client{
		clientset:       clientset,
		dynamic:         dyn,
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

	return newClientFromConfig(config, defaultTimeout, runtime.ContentTypeProtobuf)
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
