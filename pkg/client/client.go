package client

import (
	"context"
	"fmt"
	"time"

	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/tools/clientcmd"
)

const defaultTimeout = 30 * time.Second

type Client struct {
	clientset *kubernetes.Clientset
	timeout   time.Duration
}

type Option func(*Client) error

func WithTimeout(d time.Duration) Option {
	return func(c *Client) error {
		if d <= 0 {
			return fmt.Errorf("timeout must be positive, got %v", d)
		}
		c.timeout = d
		return nil
	}
}

func New(kubeconfig string, opts ...Option) (*Client, error) {
	config, err := clientcmd.BuildConfigFromFlags("", kubeconfig)
	if err != nil {
		return nil, fmt.Errorf("building kubeconfig: %w", err)
	}

	return NewFromConfig(config, opts...)
}

func NewFromData(data []byte, opts ...Option) (*Client, error) {
	config, err := clientcmd.RESTConfigFromKubeConfig(data)
	if err != nil {
		return nil, fmt.Errorf("parsing kubeconfig: %w", err)
	}

	return NewFromConfig(config, opts...)
}

func NewFromConfig(config *rest.Config, opts ...Option) (*Client, error) {
	c := &Client{
		timeout: defaultTimeout,
	}

	for _, opt := range opts {
		if err := opt(c); err != nil {
			return nil, fmt.Errorf("applying option: %w", err)
		}
	}

	config.Timeout = c.timeout

	clientset, err := kubernetes.NewForConfig(config)
	if err != nil {
		return nil, fmt.Errorf("creating clientset: %w", err)
	}
	c.clientset = clientset

	return c, nil
}

func (c *Client) ListPods(ctx context.Context, namespace string) ([]string, error) {
	ctx, cancel := context.WithTimeout(ctx, c.timeout)
	defer cancel()

	pods, err := c.clientset.CoreV1().Pods(namespace).List(ctx, metav1.ListOptions{})
	if err != nil {
		return nil, fmt.Errorf("listing pods: %w", err)
	}

	names := make([]string, len(pods.Items))
	for i, pod := range pods.Items {
		names[i] = pod.Name
	}
	return names, nil
}
