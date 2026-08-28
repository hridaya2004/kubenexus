package client

import (
	"testing"
	"time"

	"k8s.io/client-go/rest"
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
		name       string
		data       []byte
		timeoutSec int64
		wantErr    bool
	}{
		{
			name:       "empty data",
			data:       nil,
			timeoutSec: 10,
			wantErr:    true,
		},
		{
			name:       "invalid data",
			data:       []byte("not valid yaml"),
			timeoutSec: 10,
			wantErr:    true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, err := NewClientWithOptions(tt.data, tt.timeoutSec)
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

// newClientFromConfig must produce both a typed and a dynamic client. Resource
// reads go through the dynamic one, so a nil there would surface only at runtime
// on device.
func TestNewClientFromConfig_BuildsBothClients(t *testing.T) {
	config := &rest.Config{Host: "http://localhost:8080"}

	c, err := newClientFromConfig(config, 15*time.Second)
	if err != nil {
		t.Fatalf("newClientFromConfig() error = %v", err)
	}

	if c.clientset == nil {
		t.Error("clientset is nil")
	}
	if c.dynamic == nil {
		t.Error("dynamic client is nil, generic resource methods would fail on device")
	}
	if c.timeout != 15*time.Second {
		t.Errorf("timeout = %v, want 15s", c.timeout)
	}
	if c.executorFactory == nil {
		t.Error("executorFactory is nil, exec would fail on device")
	}
}

// Response compression must stay enabled: the dynamic client forces JSON, whose
// bodies are materially larger than the protobuf they replaced, on a cellular
// connection.
func TestNewClientFromConfig_EnablesCompression(t *testing.T) {
	config := &rest.Config{Host: "http://localhost:8080", DisableCompression: true}

	if _, err := newClientFromConfig(config, defaultTimeout); err != nil {
		t.Fatalf("newClientFromConfig() error = %v", err)
	}

	if config.DisableCompression {
		t.Error("DisableCompression is still true; JSON responses would be sent uncompressed")
	}
}

// One rest.Config is shared between the typed clientset, the dynamic client and
// the exec/log factories. dynamic.NewForConfig forces JSON content negotiation,
// but it must do so on its own copy: leaking that override onto the shared
// config would silently change negotiation for every other consumer.
func TestNewClientFromConfig_DoesNotMutateSharedConfig(t *testing.T) {
	config := &rest.Config{Host: "http://localhost:8080"}
	config.ContentType = "application/test"
	config.AcceptContentTypes = "application/test"

	if _, err := newClientFromConfig(config, defaultTimeout); err != nil {
		t.Fatalf("newClientFromConfig() error = %v", err)
	}

	if config.ContentType != "application/test" {
		t.Errorf("config.ContentType = %q; dynamic client leaked its JSON override",
			config.ContentType)
	}
	if config.AcceptContentTypes != "application/test" {
		t.Errorf("config.AcceptContentTypes = %q; dynamic client leaked its JSON override",
			config.AcceptContentTypes)
	}
}
