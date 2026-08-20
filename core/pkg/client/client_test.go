package client

import (
	"bytes"
	"context"
	"fmt"
	"net/url"
	"strings"
	"sync"
	"testing"
	"time"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/tools/remotecommand"
)

func TestNewClient_Empty(t *testing.T) {
	_, err := NewClient("")
	if err == nil {
		t.Fatal("NewClient('') expected error, got nil")
	}
}

func TestNewClientFromBytes_Invalid(t *testing.T) {
	_, err := NewClientFromBytes([]byte("invalid-kubeconfig-yaml"))
	if err == nil {
		t.Fatal("NewClientFromBytes(invalid) expected error, got nil")
	}
}

func TestNewClientWithOptions(t *testing.T) {
	tests := []struct {
		name        string
		data        []byte
		timeoutSec  int64
		useProtobuf bool
		wantErr     bool
	}{
		{
			name:        "empty data",
			data:        nil,
			timeoutSec:  10,
			useProtobuf: true,
			wantErr:     true,
		},
		{
			name:        "invalid data",
			data:        []byte("not valid yaml"),
			timeoutSec:  10,
			useProtobuf: true,
			wantErr:     true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, err := NewClientWithOptions(tt.data, tt.timeoutSec, tt.useProtobuf)
			if (err != nil) != tt.wantErr {
				t.Errorf("NewClientWithOptions() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func TestClient_TimeoutGetSet(t *testing.T) {
	c := &Client{
		timeout: 30 * time.Second,
		config:  &rest.Config{},
	}

	if c.GetTimeout() != 30 {
		t.Errorf("GetTimeout() = %d, want 30", c.GetTimeout())
	}

	c.SetTimeout(60)
	if c.GetTimeout() != 60 {
		t.Errorf("GetTimeout() = %d, want 60", c.GetTimeout())
	}
	if c.config.Timeout != 60*time.Second {
		t.Errorf("config.Timeout = %v, want 60s", c.config.Timeout)
	}

	// Non-positive timeout should not overwrite
	c.SetTimeout(-5)
	if c.GetTimeout() != 60 {
		t.Errorf("GetTimeout() after negative = %d, want 60", c.GetTimeout())
	}
}

func TestStringList(t *testing.T) {
	list := &StringList{items: []string{"a", "b", "c"}}
	if list.Len() != 3 {
		t.Errorf("Len() = %d, want 3", list.Len())
	}
	if list.Get(0) != "a" || list.Get(1) != "b" || list.Get(2) != "c" {
		t.Errorf("Get values = [%s, %s, %s]", list.Get(0), list.Get(1), list.Get(2))
	}
	if list.Get(-1) != "" || list.Get(5) != "" {
		t.Errorf("out of bounds Get should return empty string")
	}

	var nilList *StringList
	if nilList.Len() != 0 || nilList.Get(0) != "" {
		t.Errorf("nil list operations failed")
	}
}

func TestNamespaceList(t *testing.T) {
	list := &NamespaceList{items: []Namespace{{Name: "default", Status: "Active"}}}
	if list.Len() != 1 {
		t.Errorf("Len() = %d, want 1", list.Len())
	}
	ns := list.Get(0)
	if ns == nil || ns.Name != "default" || ns.Status != "Active" {
		t.Errorf("Get(0) = %+v, want default Active", ns)
	}
	if list.Get(-1) != nil || list.Get(2) != nil {
		t.Errorf("out of bounds Get should return nil")
	}

	var nilList *NamespaceList
	if nilList.Len() != 0 || nilList.Get(0) != nil {
		t.Errorf("nil list operations failed")
	}
}

func TestPodList(t *testing.T) {
	list := &PodList{items: []Pod{{Name: "nginx", Namespace: "default"}}}
	if list.Len() != 1 {
		t.Errorf("Len() = %d, want 1", list.Len())
	}
	p := list.Get(0)
	if p == nil || p.Name != "nginx" || p.Namespace != "default" {
		t.Errorf("Get(0) = %+v, want nginx default", p)
	}
	if list.Get(-1) != nil || list.Get(1) != nil {
		t.Errorf("out of bounds Get should return nil")
	}

	var nilList *PodList
	if nilList.Len() != 0 || nilList.Get(0) != nil {
		t.Errorf("nil list operations failed")
	}
}

func TestPod_JSON(t *testing.T) {
	p := &Pod{Name: "nginx", Namespace: "default", Status: "Running"}
	podJSON := p.JSON()
	if !strings.Contains(podJSON, `"name":"nginx"`) {
		t.Errorf("JSON() = %s, want name:nginx", podJSON)
	}

	var nilPod *Pod
	if nilPod.JSON() != "{}" {
		t.Errorf("nilPod.JSON() = %s, want {}", nilPod.JSON())
	}
}

func TestContainerInfoList(t *testing.T) {
	list := &ContainerInfoList{items: []ContainerInfo{{Name: "app", Image: "nginx"}}}
	if list.Len() != 1 {
		t.Errorf("Len() = %d, want 1", list.Len())
	}
	c := list.Get(0)
	if c == nil || c.Name != "app" || c.Image != "nginx" {
		t.Errorf("Get(0) = %+v", c)
	}
	if list.Get(-1) != nil || list.Get(10) != nil {
		t.Errorf("out of bounds Get should return nil")
	}

	var nilList *ContainerInfoList
	if nilList.Len() != 0 || nilList.Get(0) != nil {
		t.Errorf("nil list operations failed")
	}
}

func TestPodConditionList(t *testing.T) {
	list := &PodConditionList{items: []PodCondition{{Type: "Ready", Status: "True"}}}
	if list.Len() != 1 {
		t.Errorf("Len() = %d, want 1", list.Len())
	}
	cond := list.Get(0)
	if cond == nil || cond.Type != "Ready" || cond.Status != "True" {
		t.Errorf("Get(0) = %+v", cond)
	}
	if list.Get(-1) != nil || list.Get(10) != nil {
		t.Errorf("out of bounds Get should return nil")
	}

	var nilList *PodConditionList
	if nilList.Len() != 0 || nilList.Get(0) != nil {
		t.Errorf("nil list operations failed")
	}
}

func TestPodEventList(t *testing.T) {
	list := &PodEventList{items: []PodEvent{{Type: "Normal", Reason: "Started"}}}
	if list.Len() != 1 {
		t.Errorf("Len() = %d, want 1", list.Len())
	}
	ev := list.Get(0)
	if ev == nil || ev.Type != "Normal" || ev.Reason != "Started" {
		t.Errorf("Get(0) = %+v", ev)
	}
	if list.Get(-1) != nil || list.Get(10) != nil {
		t.Errorf("out of bounds Get should return nil")
	}

	var nilList *PodEventList
	if nilList.Len() != 0 || nilList.Get(0) != nil {
		t.Errorf("nil list operations failed")
	}
}

func TestPodDetails(t *testing.T) {
	details := &PodDetails{
		Name:           "pod-1",
		Namespace:      "default",
		Status:         "Running",
		containers:     []ContainerInfo{{Name: "c1"}},
		initContainers: []ContainerInfo{{Name: "init-c1"}},
		conditions:     []PodCondition{{Type: "Ready"}},
		events:         []PodEvent{{Reason: "Scheduled"}},
		volumes:        []string{"vol-1"},
	}

	if details.Containers().Len() != 1 || details.Containers().Get(0).Name != "c1" {
		t.Errorf("Containers() mismatch: %+v", details.Containers())
	}
	if details.InitContainers().Len() != 1 || details.InitContainers().Get(0).Name != "init-c1" {
		t.Errorf("InitContainers() mismatch: %+v", details.InitContainers())
	}
	if details.Conditions().Len() != 1 || details.Conditions().Get(0).Type != "Ready" {
		t.Errorf("Conditions() mismatch: %+v", details.Conditions())
	}
	if details.Events().Len() != 1 || details.Events().Get(0).Reason != "Scheduled" {
		t.Errorf("Events() mismatch: %+v", details.Events())
	}
	if details.Volumes().Len() != 1 || details.Volumes().Get(0) != "vol-1" {
		t.Errorf("Volumes() mismatch: %+v", details.Volumes())
	}

	podDetailsJSON := details.JSON()
	if !strings.Contains(podDetailsJSON, `"name":"pod-1"`) || !strings.Contains(podDetailsJSON, `"status":"Running"`) {
		t.Errorf("JSON() = %s", podDetailsJSON)
	}

	var nilDetails *PodDetails
	if nilDetails.Containers().Len() != 0 || nilDetails.JSON() != "{}" {
		t.Errorf("nilDetails operations failed")
	}
}

func TestCountReadyContainers(t *testing.T) {
	tests := []struct {
		name       string
		containers []corev1.Container
		statuses   []corev1.ContainerStatus
		wantReady  int
		wantTotal  int
	}{
		{
			name:       "all ready",
			containers: []corev1.Container{{Name: "a"}, {Name: "b"}},
			statuses:   []corev1.ContainerStatus{{Name: "a", Ready: true}, {Name: "b", Ready: true}},
			wantReady:  2,
			wantTotal:  2,
		},
		{
			name:       "none ready",
			containers: []corev1.Container{{Name: "a"}, {Name: "b"}},
			statuses:   []corev1.ContainerStatus{{Name: "a", Ready: false}, {Name: "b", Ready: false}},
			wantReady:  0,
			wantTotal:  2,
		},
		{
			name:       "partial ready",
			containers: []corev1.Container{{Name: "a"}, {Name: "b"}},
			statuses:   []corev1.ContainerStatus{{Name: "a", Ready: true}, {Name: "b", Ready: false}},
			wantReady:  1,
			wantTotal:  2,
		},
		{
			name:       "no containers",
			containers: []corev1.Container{},
			statuses:   []corev1.ContainerStatus{},
			wantReady:  0,
			wantTotal:  0,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ready, total := countReadyContainers(tt.containers, tt.statuses)
			if ready != tt.wantReady {
				t.Errorf("countReadyContainers() ready = %v, want %v", ready, tt.wantReady)
			}
			if total != tt.wantTotal {
				t.Errorf("countReadyContainers() total = %v, want %v", total, tt.wantTotal)
			}
		})
	}
}

func TestCountRestarts(t *testing.T) {
	tests := []struct {
		name     string
		statuses []corev1.ContainerStatus
		want     int32
	}{
		{
			name:     "no restarts",
			statuses: []corev1.ContainerStatus{{RestartCount: 0}, {RestartCount: 0}},
			want:     0,
		},
		{
			name:     "single restart",
			statuses: []corev1.ContainerStatus{{RestartCount: 1}},
			want:     1,
		},
		{
			name:     "multiple restarts",
			statuses: []corev1.ContainerStatus{{RestartCount: 3}, {RestartCount: 5}},
			want:     8,
		},
		{
			name:     "empty",
			statuses: []corev1.ContainerStatus{},
			want:     0,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := countRestarts(tt.statuses)
			if got != tt.want {
				t.Errorf("countRestarts() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestFormatAge(t *testing.T) {
	tests := []struct {
		name     string
		duration time.Duration
		want     string
	}{
		{"4 minutes", 4 * time.Minute, "4m"},
		{"2 hours 30 minutes", 2*time.Hour + 30*time.Minute, "2h30m"},
		{"1 day 3 hours", 24*time.Hour + 3*time.Hour, "1d3h"},
		{"7 days", 7 * 24 * time.Hour, "7d0h"},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			past := time.Now().Add(-tt.duration)
			got := formatAge(past)
			if got != tt.want {
				t.Errorf("formatAge() = %q, want %q", got, tt.want)
			}
		})
	}
}

func TestContainerToInfo(t *testing.T) {
	containers := []corev1.Container{
		{Name: "app", Image: "nginx:1.21"},
		{Name: "sidecar", Image: "busybox"},
	}
	statuses := []corev1.ContainerStatus{
		{Name: "app", Ready: true, RestartCount: 2, State: corev1.ContainerState{Running: &corev1.ContainerStateRunning{}}},
		{Name: "sidecar", Ready: false, RestartCount: 0, State: corev1.ContainerState{Waiting: &corev1.ContainerStateWaiting{Reason: "PodInitializing"}}},
	}

	appInfo := containerToInfo(containers[0], statuses)
	if appInfo.Name != "app" || !appInfo.Ready || appInfo.RestartCount != 2 || appInfo.State != "Running" {
		t.Errorf("containerToInfo(app) = %+v", appInfo)
	}

	sidecarInfo := containerToInfo(containers[1], statuses)
	if sidecarInfo.Name != "sidecar" || sidecarInfo.Ready || sidecarInfo.State != "Waiting (PodInitializing)" {
		t.Errorf("containerToInfo(sidecar) = %+v", sidecarInfo)
	}
}

func TestFormatContainerState(t *testing.T) {
	tests := []struct {
		name  string
		state corev1.ContainerState
		want  string
	}{
		{
			name:  "running",
			state: corev1.ContainerState{Running: &corev1.ContainerStateRunning{}},
			want:  "Running",
		},
		{
			name:  "waiting",
			state: corev1.ContainerState{Waiting: &corev1.ContainerStateWaiting{Reason: "CrashLoopBackOff"}},
			want:  "Waiting (CrashLoopBackOff)",
		},
		{
			name:  "terminated",
			state: corev1.ContainerState{Terminated: &corev1.ContainerStateTerminated{ExitCode: 1}},
			want:  "Terminated (exit 1)",
		},
		{
			name:  "unknown",
			state: corev1.ContainerState{},
			want:  "Unknown",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := formatContainerState(tt.state)
			if got != tt.want {
				t.Errorf("formatContainerState() = %v, want %v", got, tt.want)
			}
		})
	}
}

type mockLogCallback struct {
	lines  []string
	errors []string
	done   bool
}

func (m *mockLogCallback) OnLogLine(line string) {
	m.lines = append(m.lines, line)
}

func (m *mockLogCallback) OnError(err string) {
	m.errors = append(m.errors, err)
}

func (m *mockLogCallback) OnDone() {
	m.done = true
}

func TestMockLogCallback(t *testing.T) {
	cb := &mockLogCallback{}
	cb.OnLogLine("line1")
	cb.OnLogLine("line2")
	cb.OnError("something went wrong")
	cb.OnDone()

	if len(cb.lines) != 2 || cb.lines[0] != "line1" || cb.lines[1] != "line2" {
		t.Errorf("lines = %v", cb.lines)
	}
	if len(cb.errors) != 1 || cb.errors[0] != "something went wrong" {
		t.Errorf("errors = %v", cb.errors)
	}
	if !cb.done {
		t.Error("done should be true")
	}
}

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

func TestExec_Success(t *testing.T) {
	config := &rest.Config{Host: "http://localhost:8080"}
	c := &Client{
		config:  config,
		timeout: defaultTimeout,
	}

	// Mock corev1 pod exec request doesn't need network if executor is mocked
	c.executorFactory = func(cfg *rest.Config, method string, u *url.URL) (remotecommand.Executor, error) {
		return &mockExecutor{
			streamFunc: func(ctx context.Context, options remotecommand.StreamOptions) error {
				if options.Stdin != nil {
					var inBuf bytes.Buffer
					_, _ = inBuf.ReadFrom(options.Stdin)
					if inBuf.String() != "input data" {
						return fmt.Errorf("unexpected stdin: %q", inBuf.String())
					}
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

	if c.executorFactory == nil {
		t.Fatal("executorFactory is nil")
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

func TestNamespaceStruct(t *testing.T) {
	ns := Namespace{Name: "kube-system", Status: "Active"}
	if ns.Name != "kube-system" || ns.Status != "Active" {
		t.Errorf("Namespace = %+v", ns)
	}
}

func TestExecResultStruct(t *testing.T) {
	res := ExecResult{Stdout: "out", Stderr: "err"}
	if res.Stdout != "out" || res.Stderr != "err" {
		t.Errorf("ExecResult = %+v", res)
	}
}
