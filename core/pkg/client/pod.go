package client

import (
	"bufio"
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"strings"
	"sync"
	"time"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/tools/remotecommand"
)

// Pod contains summary fields for pod listing.
type Pod struct {
	Name      string `json:"name"`
	Namespace string `json:"namespace"`
	Status    string `json:"status"`
	Ready     string `json:"ready"`
	Restarts  int32  `json:"restarts"`
	Age       string `json:"age"`
	Node      string `json:"node"`
	IP        string `json:"ip"`
}

// JSON returns the JSON string representation of the pod.
func (p *Pod) JSON() string {
	if p == nil {
		return "{}"
	}
	data, _ := json.Marshal(p)
	return string(data)
}

// PodList represents an indexed list of pods for Gomobile / Android JNI binding.
type PodList struct {
	items []Pod
}

// newPodList creates a PodList wrapper.
func newPodList(items []Pod) *PodList {
	return &PodList{items: items}
}

// Len returns the count of items in the list.
func (l *PodList) Len() int {
	if l == nil {
		return 0
	}
	return len(l.items)
}

// Get returns the pod at index, or nil if out of bounds.
func (l *PodList) Get(index int) *Pod {
	if l == nil || index < 0 || index >= len(l.items) {
		return nil
	}
	return &l.items[index]
}

// ContainerInfo holds status and runtime information for a container.
type ContainerInfo struct {
	Name         string `json:"name"`
	Image        string `json:"image"`
	Ready        bool   `json:"ready"`
	RestartCount int32  `json:"restartCount"`
	State        string `json:"state"`
}

// ContainerInfoList represents an indexed list of containers for Gomobile / Android JNI.
type ContainerInfoList struct {
	items []ContainerInfo
}

// Len returns the count of containers in the list.
func (l *ContainerInfoList) Len() int {
	if l == nil {
		return 0
	}
	return len(l.items)
}

// Get returns the container info at index, or nil if out of bounds.
func (l *ContainerInfoList) Get(index int) *ContainerInfo {
	if l == nil || index < 0 || index >= len(l.items) {
		return nil
	}
	return &l.items[index]
}

// PodCondition represents a single condition status for a pod.
type PodCondition struct {
	Type               string `json:"type"`
	Status             string `json:"status"`
	LastTransitionTime string `json:"lastTransitionTime"`
	Reason             string `json:"reason"`
	Message            string `json:"message"`
}

// PodConditionList represents an indexed list of pod conditions for Gomobile / Android JNI.
type PodConditionList struct {
	items []PodCondition
}

// Len returns the count of conditions in the list.
func (l *PodConditionList) Len() int {
	if l == nil {
		return 0
	}
	return len(l.items)
}

// Get returns the condition at index, or nil if out of bounds.
func (l *PodConditionList) Get(index int) *PodCondition {
	if l == nil || index < 0 || index >= len(l.items) {
		return nil
	}
	return &l.items[index]
}

// PodEvent represents an event associated with a pod.
type PodEvent struct {
	Type    string `json:"type"`
	Reason  string `json:"reason"`
	Message string `json:"message"`
	Age     string `json:"age"`
}

// PodEventList represents an indexed list of pod events for Gomobile / Android JNI.
type PodEventList struct {
	items []PodEvent
}

// Len returns the count of events in the list.
func (l *PodEventList) Len() int {
	if l == nil {
		return 0
	}
	return len(l.items)
}

// Get returns the event at index, or nil if out of bounds.
func (l *PodEventList) Get(index int) *PodEvent {
	if l == nil || index < 0 || index >= len(l.items) {
		return nil
	}
	return &l.items[index]
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
	LabelsJSON     string
	VolumesCSV     string
	containers     []ContainerInfo
	initContainers []ContainerInfo
	conditions     []PodCondition
	events         []PodEvent
	volumes        []string
}

// Containers returns the list of application containers.
func (d *PodDetails) Containers() *ContainerInfoList {
	if d == nil {
		return &ContainerInfoList{}
	}
	return &ContainerInfoList{items: d.containers}
}

// InitContainers returns the list of init containers.
func (d *PodDetails) InitContainers() *ContainerInfoList {
	if d == nil {
		return &ContainerInfoList{}
	}
	return &ContainerInfoList{items: d.initContainers}
}

// Conditions returns the lifecycle conditions of the pod.
func (d *PodDetails) Conditions() *PodConditionList {
	if d == nil {
		return &PodConditionList{}
	}
	return &PodConditionList{items: d.conditions}
}

// Events returns the recent events associated with the pod.
func (d *PodDetails) Events() *PodEventList {
	if d == nil {
		return &PodEventList{}
	}
	return &PodEventList{items: d.events}
}

// Volumes returns the list of volume names attached to the pod.
func (d *PodDetails) Volumes() *StringList {
	if d == nil {
		return &StringList{}
	}
	return &StringList{items: d.volumes}
}

// JSON returns full PodDetails serialized as a JSON string.
func (d *PodDetails) JSON() string {
	if d == nil {
		return "{}"
	}
	type podDetailsExport struct {
		Name           string          `json:"name"`
		Namespace      string          `json:"namespace"`
		Status         string          `json:"status"`
		Node           string          `json:"node"`
		IP             string          `json:"ip"`
		HostIP         string          `json:"hostIP"`
		RestartPolicy  string          `json:"restartPolicy"`
		StartTime      string          `json:"startTime"`
		Containers     []ContainerInfo `json:"containers"`
		InitContainers []ContainerInfo `json:"initContainers"`
		Conditions     []PodCondition  `json:"conditions"`
		Events         []PodEvent      `json:"events"`
		Volumes        []string        `json:"volumes"`
	}
	exp := podDetailsExport{
		Name:           d.Name,
		Namespace:      d.Namespace,
		Status:         d.Status,
		Node:           d.Node,
		IP:             d.IP,
		HostIP:         d.HostIP,
		RestartPolicy:  d.RestartPolicy,
		StartTime:      d.StartTime,
		Containers:     d.containers,
		InitContainers: d.initContainers,
		Conditions:     d.conditions,
		Events:         d.events,
		Volumes:        d.volumes,
	}
	data, _ := json.Marshal(exp)
	return string(data)
}

// LogCallback receives streamed container log lines and status events.
type LogCallback interface {
	OnLogLine(line string)
	OnError(err string)
	OnDone()
}

// ExecResult contains the captured stdout and stderr from a command execution.
type ExecResult struct {
	Stdout string `json:"stdout"`
	Stderr string `json:"stderr"`
}

// ExecCallback receives streamed output and lifecycle events for an interactive exec session.
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

// ListPods returns pod names in the namespace, or all namespaces if empty.
func (c *Client) ListPods(namespace string) (*StringList, error) {
	ctx, cancel := context.WithTimeout(context.Background(), c.timeout)
	defer cancel()

	pods, err := c.clientset.CoreV1().Pods(namespace).List(ctx, metav1.ListOptions{})
	if err != nil {
		return nil, fmt.Errorf("listing pods: %w", err)
	}

	names := make([]string, len(pods.Items))
	for i, pod := range pods.Items {
		names[i] = pod.Name
	}
	return newStringList(names), nil
}

// ListPodsWide returns pod summaries in the namespace, or all namespaces if empty.
func (c *Client) ListPodsWide(namespace string) (*PodList, error) {
	ctx, cancel := context.WithTimeout(context.Background(), c.timeout)
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
	return newPodList(result), nil
}

// ListPodsJSON returns pod summaries as a JSON string for Android clients.
func (c *Client) ListPodsJSON(namespace string) (string, error) {
	list, err := c.ListPodsWide(namespace)
	if err != nil {
		return "", err
	}
	data, err := json.Marshal(list.items)
	if err != nil {
		return "", fmt.Errorf("marshaling pods to json: %w", err)
	}
	return string(data), nil
}

// DescribePod returns detailed information for a specific pod.
func (c *Client) DescribePod(namespace, name string) (*PodDetails, error) {
	ctx, cancel := context.WithTimeout(context.Background(), c.timeout)
	defer cancel()

	pod, err := c.clientset.CoreV1().Pods(namespace).Get(ctx, name, metav1.GetOptions{})
	if err != nil {
		return nil, fmt.Errorf("getting pod: %w", err)
	}

	labelsJSON, _ := json.Marshal(pod.Labels)

	var volumes []string
	for _, v := range pod.Spec.Volumes {
		volumes = append(volumes, v.Name)
	}

	details := &PodDetails{
		Name:          pod.Name,
		Namespace:     pod.Namespace,
		Status:        string(pod.Status.Phase),
		Node:          pod.Spec.NodeName,
		IP:            pod.Status.PodIP,
		HostIP:        pod.Status.HostIP,
		RestartPolicy: string(pod.Spec.RestartPolicy),
		LabelsJSON:    string(labelsJSON),
		VolumesCSV:    strings.Join(volumes, ","),
		volumes:       volumes,
	}

	if pod.Status.StartTime != nil {
		details.StartTime = pod.Status.StartTime.Format(time.RFC3339)
	}

	for _, c := range pod.Spec.InitContainers {
		details.initContainers = append(details.initContainers, containerToInfo(c, pod.Status.InitContainerStatuses))
	}
	for _, c := range pod.Spec.Containers {
		details.containers = append(details.containers, containerToInfo(c, pod.Status.ContainerStatuses))
	}

	for _, cond := range pod.Status.Conditions {
		details.conditions = append(details.conditions, PodCondition{
			Type:               string(cond.Type),
			Status:             string(cond.Status),
			LastTransitionTime: cond.LastTransitionTime.Format(time.RFC3339),
			Reason:             cond.Reason,
			Message:            cond.Message,
		})
	}

	events, err := c.getPodEvents(ctx, namespace, name)
	if err != nil {
		details.events = []PodEvent{}
	} else {
		details.events = events
	}

	return details, nil
}

// DescribePodJSON returns detailed pod information serialized as a JSON string.
func (c *Client) DescribePodJSON(namespace, name string) (string, error) {
	details, err := c.DescribePod(namespace, name)
	if err != nil {
		return "", err
	}
	return details.JSON(), nil
}

// DeletePod deletes a pod by name in the specified namespace.
func (c *Client) DeletePod(namespace, name string) error {
	ctx, cancel := context.WithTimeout(context.Background(), c.timeout)
	defer cancel()

	err := c.clientset.CoreV1().Pods(namespace).Delete(ctx, name, metav1.DeleteOptions{})
	if err != nil {
		return fmt.Errorf("deleting pod %s/%s: %w", namespace, name, err)
	}
	return nil
}

// Logs returns the full log output for a container.
func (c *Client) Logs(namespace, podName, container string) (string, error) {
	ctx, cancel := context.WithTimeout(context.Background(), c.timeout)
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
	defer func() { _ = stream.Close() }()

	data, err := io.ReadAll(stream)
	if err != nil {
		return "", fmt.Errorf("reading logs: %w", err)
	}
	return string(data), nil
}

// StreamLogs follows pod logs and sends each line to callback until completed or cancelled.
func (c *Client) StreamLogs(namespace, podName, container string, callback LogCallback) {
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

// Exec executes a non-interactive command inside a pod container and returns stdout and stderr.
// The command parameter can be a single command string (e.g. "ls -la" or "uname -a").
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

// StartTerminal starts an interactive shell session (/bin/sh) with TTY for an Android terminal emulator.
func (c *Client) StartTerminal(namespace, podName, container string, callback ExecCallback) (*ExecSession, error) {
	return c.StartExecSession(namespace, podName, container, "/bin/sh", true, callback)
}

// StartExecSession starts an interactive exec session in a container with streaming callbacks and TTY support.
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
