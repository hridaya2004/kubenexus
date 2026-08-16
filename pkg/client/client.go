// Package client provides a Kubernetes client wrapper suitable for mobile bindings.
package client

import (
	"bufio"
	"context"
	"fmt"
	"io"
	"strings"
	"time"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/tools/clientcmd"
)

const defaultTimeout = 30 * time.Second

// Client wraps a Kubernetes clientset for cluster operations.
type Client struct {
	clientset *kubernetes.Clientset
	timeout   time.Duration
}

// Namespace contains name and status for a cluster namespace.
type Namespace struct {
	Name   string
	Status string
}

// Pod contains summary fields for pod listing.
type Pod struct {
	Name      string
	Namespace string
	Status    string
	Ready     string
	Restarts  int32
	Age       string
	Node      string
	IP        string
}

// ContainerInfo holds status and runtime information for a container.
type ContainerInfo struct {
	Name         string
	Image        string
	Ready        bool
	RestartCount int32
	State        string
}

// PodCondition represents a single condition status for a pod.
type PodCondition struct {
	Type               string
	Status             string
	LastTransitionTime string
	Reason             string
	Message            string
}

// PodEvent represents an event associated with a pod.
type PodEvent struct {
	Type    string
	Reason  string
	Message string
	Age     string
}

// PodDetails contains detailed pod metadata, status, containers, and events.
type PodDetails struct {
	Name           string
	Namespace      string
	Status         string
	Node           string
	IP             string
	HostIP         string
	RestartPolicy  string
	StartTime      string
	Containers     []ContainerInfo
	InitContainers []ContainerInfo
	Conditions     []PodCondition
	Events         []PodEvent
	Volumes        []string
	Labels         map[string]string
}

// LogCallback receives streamed container log lines and status events.
type LogCallback interface {
	OnLogLine(line string)
	OnError(err string)
	OnDone()
}

// Option configures a Client during construction.
type Option func(*Client) error

// WithTimeout sets the request timeout for client calls.
func WithTimeout(d time.Duration) Option {
	return func(c *Client) error {
		if d <= 0 {
			return fmt.Errorf("timeout must be positive, got %v", d)
		}
		c.timeout = d
		return nil
	}
}

// New creates a Client from a kubeconfig file path.
func New(kubeconfig string, opts ...Option) (*Client, error) {
	config, err := clientcmd.BuildConfigFromFlags("", kubeconfig)
	if err != nil {
		return nil, fmt.Errorf("building kubeconfig: %w", err)
	}

	return NewFromConfig(config, opts...)
}

// NewFromData creates a Client from raw kubeconfig YAML bytes.
func NewFromData(data []byte, opts ...Option) (*Client, error) {
	config, err := clientcmd.RESTConfigFromKubeConfig(data)
	if err != nil {
		return nil, fmt.Errorf("parsing kubeconfig: %w", err)
	}

	return NewFromConfig(config, opts...)
}

// NewFromConfig creates a Client from an existing rest.Config.
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

// ListPods returns pod names in the namespace, or all namespaces if empty.
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

// ListNamespaces returns all namespaces in the cluster.
func (c *Client) ListNamespaces(ctx context.Context) ([]Namespace, error) {
	ctx, cancel := context.WithTimeout(ctx, c.timeout)
	defer cancel()

	nsList, err := c.clientset.CoreV1().Namespaces().List(ctx, metav1.ListOptions{})
	if err != nil {
		return nil, fmt.Errorf("listing namespaces: %w", err)
	}

	namespaces := make([]Namespace, len(nsList.Items))
	for i, ns := range nsList.Items {
		namespaces[i] = Namespace{
			Name:   ns.Name,
			Status: string(ns.Status.Phase),
		}
	}
	return namespaces, nil
}

// ListPodsWide returns pod summaries in the namespace, or all namespaces if empty.
func (c *Client) ListPodsWide(ctx context.Context, namespace string) ([]Pod, error) {
	ctx, cancel := context.WithTimeout(ctx, c.timeout)
	defer cancel()

	pods, err := c.clientset.CoreV1().Pods(namespace).List(ctx, metav1.ListOptions{})
	if err != nil {
		return nil, fmt.Errorf("listing pods: %w", err)
	}

	result := make([]Pod, len(pods.Items))
	for i, pod := range pods.Items {
		ready, total := countReadyContainers(pod.Spec.Containers, pod.Status.ContainerStatuses)
		result[i] = Pod{
			Name:      pod.Name,
			Namespace: pod.Namespace,
			Status:    string(pod.Status.Phase),
			Ready:     fmt.Sprintf("%d/%d", ready, total),
			Restarts:  countRestarts(pod.Status.ContainerStatuses),
			Age:       formatAge(pod.CreationTimestamp.Time),
			Node:      pod.Spec.NodeName,
			IP:        pod.Status.PodIP,
		}
	}
	return result, nil
}

func countReadyContainers(containers []corev1.Container, statuses []corev1.ContainerStatus) (int, int) {
	total := len(containers)
	ready := 0
	for _, s := range statuses {
		if s.Ready {
			ready++
		}
	}
	return ready, total
}

func countRestarts(statuses []corev1.ContainerStatus) int32 {
	var total int32
	for _, s := range statuses {
		total += s.RestartCount
	}
	return total
}

func formatAge(t time.Time) string {
	d := time.Since(t).Truncate(time.Second)
	days := int(d.Hours()) / 24
	h := int(d.Hours()) % 24
	m := int(d.Minutes()) % 60
	if days > 0 {
		return fmt.Sprintf("%dd%dh", days, h)
	}
	if h > 0 {
		return fmt.Sprintf("%dh%dm", h, m)
	}
	return fmt.Sprintf("%dm", m)
}

// DescribePod returns detailed information for a specific pod.
func (c *Client) DescribePod(ctx context.Context, namespace, name string) (*PodDetails, error) {
	ctx, cancel := context.WithTimeout(ctx, c.timeout)
	defer cancel()

	pod, err := c.clientset.CoreV1().Pods(namespace).Get(ctx, name, metav1.GetOptions{})
	if err != nil {
		return nil, fmt.Errorf("getting pod: %w", err)
	}

	details := &PodDetails{
		Name:          pod.Name,
		Namespace:     pod.Namespace,
		Status:        string(pod.Status.Phase),
		Node:          pod.Spec.NodeName,
		IP:            pod.Status.PodIP,
		HostIP:        pod.Status.HostIP,
		RestartPolicy: string(pod.Spec.RestartPolicy),
		Labels:        cloneMap(pod.Labels),
	}

	if pod.Status.StartTime != nil {
		details.StartTime = pod.Status.StartTime.Format(time.RFC3339)
	}

	for _, c := range pod.Spec.InitContainers {
		details.InitContainers = append(details.InitContainers, containerToInfo(c, pod.Status.InitContainerStatuses))
	}
	for _, c := range pod.Spec.Containers {
		details.Containers = append(details.Containers, containerToInfo(c, pod.Status.ContainerStatuses))
	}

	for _, v := range pod.Spec.Volumes {
		details.Volumes = append(details.Volumes, v.Name)
	}

	for _, cond := range pod.Status.Conditions {
		details.Conditions = append(details.Conditions, PodCondition{
			Type:               string(cond.Type),
			Status:             string(cond.Status),
			LastTransitionTime: cond.LastTransitionTime.Format(time.RFC3339),
			Reason:             cond.Reason,
			Message:            cond.Message,
		})
	}

	events, err := c.getPodEvents(ctx, namespace, name)
	if err != nil {
		// Best effort: lack of event permissions or missing events shouldn't fail pod description.
		details.Events = []PodEvent{}
	} else {
		details.Events = events
	}

	return details, nil
}

func containerToInfo(c corev1.Container, statuses []corev1.ContainerStatus) ContainerInfo {
	info := ContainerInfo{
		Name:  c.Name,
		Image: c.Image,
	}
	for _, s := range statuses {
		if s.Name == c.Name {
			info.Ready = s.Ready
			info.RestartCount = s.RestartCount
			info.State = formatContainerState(s.State)
			break
		}
	}
	return info
}

func formatContainerState(state corev1.ContainerState) string {
	if state.Running != nil {
		return "Running"
	}
	if state.Waiting != nil {
		return fmt.Sprintf("Waiting (%s)", state.Waiting.Reason)
	}
	if state.Terminated != nil {
		return fmt.Sprintf("Terminated (exit %d)", state.Terminated.ExitCode)
	}
	return "Unknown"
}

func cloneMap(m map[string]string) map[string]string {
	if m == nil {
		return nil
	}
	clone := make(map[string]string, len(m))
	for k, v := range m {
		clone[k] = v
	}
	return clone
}

func (c *Client) getPodEvents(ctx context.Context, namespace, podName string) ([]PodEvent, error) {
	eventList, err := c.clientset.CoreV1().Events(namespace).List(ctx, metav1.ListOptions{
		FieldSelector: fmt.Sprintf("involvedObject.name=%s,involvedObject.kind=Pod", podName),
	})
	if err != nil {
		return nil, fmt.Errorf("listing pod events: %w", err)
	}

	events := make([]PodEvent, len(eventList.Items))
	for i, e := range eventList.Items {
		events[i] = PodEvent{
			Type:    e.Type,
			Reason:  e.Reason,
			Message: e.Message,
			Age:     formatAge(e.LastTimestamp.Time),
		}
	}
	return events, nil
}

// Logs returns the full log output for a container.
func (c *Client) Logs(ctx context.Context, namespace, podName, container string) (string, error) {
	ctx, cancel := context.WithTimeout(ctx, c.timeout)
	defer cancel()

	opts := &corev1.PodLogOptions{}
	if container != "" {
		opts.Container = container
	}

	req := c.clientset.CoreV1().Pods(namespace).GetLogs(podName, opts)
	stream, err := req.Stream(ctx)
	if err != nil {
		return "", fmt.Errorf("opening log stream: %w", err)
	}
	defer stream.Close()

	data, err := io.ReadAll(stream)
	if err != nil {
		return "", fmt.Errorf("reading logs: %w", err)
	}
	return string(data), nil
}

// StreamLogs follows pod logs and sends each line to callback until completed or cancelled.
func (c *Client) StreamLogs(ctx context.Context, namespace, podName, container string, callback LogCallback) {
	opts := &corev1.PodLogOptions{
		Follow: true,
	}
	if container != "" {
		opts.Container = container
	}

	req := c.clientset.CoreV1().Pods(namespace).GetLogs(podName, opts)
	stream, err := req.Stream(ctx)
	if err != nil {
		callback.OnError(fmt.Sprintf("opening log stream: %v", err))
		callback.OnDone()
		return
	}
	defer stream.Close()

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
