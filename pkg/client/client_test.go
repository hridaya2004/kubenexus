package client

import (
	"testing"
	"time"

	corev1 "k8s.io/api/core/v1"
)

func TestWithTimeout(t *testing.T) {
	tests := []struct {
		name    string
		timeout time.Duration
		wantErr bool
	}{
		{
			name:    "valid timeout",
			timeout: 10 * time.Second,
			wantErr: false,
		},
		{
			name:    "zero timeout",
			timeout: 0,
			wantErr: true,
		},
		{
			name:    "negative timeout",
			timeout: -1 * time.Second,
			wantErr: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			c := &Client{timeout: defaultTimeout}
			err := WithTimeout(tt.timeout)(c)
			if (err != nil) != tt.wantErr {
				t.Errorf("WithTimeout(%v) error = %v, wantErr %v", tt.timeout, err, tt.wantErr)
				return
			}
			if !tt.wantErr && c.timeout != tt.timeout {
				t.Errorf("timeout = %v, want %v", c.timeout, tt.timeout)
			}
		})
	}
}

func TestDefaultTimeout(t *testing.T) {
	c := &Client{}
	if c.timeout != 0 {
		t.Errorf("zero-value Client timeout = %v, want 0", c.timeout)
	}
}

func TestNewFromData(t *testing.T) {
	tests := []struct {
		name    string
		data    []byte
		wantErr bool
	}{
		{
			name:    "invalid yaml",
			data:    []byte("not valid kubeconfig"),
			wantErr: true,
		},
		{
			name:    "empty data",
			data:    []byte{},
			wantErr: true,
		},
		{
			name:    "nil data",
			data:    nil,
			wantErr: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, err := NewFromData(tt.data)
			if (err != nil) != tt.wantErr {
				t.Errorf("NewFromData() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
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
	past := time.Now().Add(-5 * time.Minute)
	got := formatAge(past)
	if got != "5m" {
		t.Errorf("formatAge() = %v, want \"5m\"", got)
	}

	past = time.Now().Add(-2*time.Hour - 30*time.Minute)
	got = formatAge(past)
	if got != "2h30m" {
		t.Errorf("formatAge() = %v, want \"2h30m\"", got)
	}
}

func TestNamespaceStruct(t *testing.T) {
	ns := Namespace{Name: "default", Status: "Active"}
	if ns.Name != "default" || ns.Status != "Active" {
		t.Errorf("Namespace = %+v, want {Name:default Status:Active}", ns)
	}
}

func TestPodStruct(t *testing.T) {
	p := Pod{
		Name:      "nginx-abc123",
		Namespace: "default",
		Status:    "Running",
		Ready:     "1/1",
		Restarts:  0,
		Age:       "5m0s",
		Node:      "node-1",
		IP:        "10.0.0.1",
	}
	if p.Name != "nginx-abc123" || p.Ready != "1/1" || p.Node != "node-1" {
		t.Errorf("Pod = %+v", p)
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

func TestPodDetailsStruct(t *testing.T) {
	d := PodDetails{
		Name:          "nginx",
		Namespace:     "default",
		Status:        "Running",
		Node:          "node-1",
		IP:            "10.0.0.1",
		HostIP:        "192.168.1.1",
		RestartPolicy: "Always",
		Labels:        map[string]string{"app": "nginx"},
		Containers: []ContainerInfo{
			{Name: "nginx", Image: "nginx:1.21", Ready: true, State: "Running"},
		},
		Conditions: []PodCondition{
			{Type: "Ready", Status: "True"},
		},
	}
	if d.Name != "nginx" || len(d.Containers) != 1 || len(d.Conditions) != 1 {
		t.Errorf("PodDetails = %+v", d)
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
