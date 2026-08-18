package client

import (
	"encoding/json"
	"testing"
)

func TestBuiltinExplain(t *testing.T) {
	tests := []struct {
		input string
		kind  string
		gv    string
	}{
		{"pod", "Pod", "v1"},
		{"pods", "Pod", "v1"},
		{"po", "Pod", "v1"},
		{"deployment", "Deployment", "apps/v1"},
		{"deployments", "Deployment", "apps/v1"},
		{"service", "Service", "v1"},
		{"namespace", "Namespace", "v1"},
		{"configmap", "ConfigMap", "v1"},
		{"secret", "Secret", "v1"},
		{"statefulset", "StatefulSet", "apps/v1"},
		{"daemonset", "DaemonSet", "apps/v1"},
		{"job", "Job", "batch/v1"},
		{"cronjob", "CronJob", "batch/v1"},
		{"ingress", "Ingress", "networking.k8s.io/v1"},
		{"node", "Node", "v1"},
	}

	for _, tt := range tests {
		t.Run(tt.input, func(t *testing.T) {
			explain := getBuiltinExplain(tt.input, "")
			if explain == nil {
				t.Fatalf("expected explain for %s, got nil", tt.input)
			}
			if explain.Kind != tt.kind {
				t.Errorf("Kind = %s, expected %s", explain.Kind, tt.kind)
			}
			if explain.GroupVersion != tt.gv {
				t.Errorf("GroupVersion = %s, expected %s", explain.GroupVersion, tt.gv)
			}
			if len(explain.Fields) == 0 {
				t.Errorf("expected fields for %s, got 0", tt.input)
			}
			if explain.Description == "" {
				t.Errorf("expected description for %s, got empty", tt.input)
			}
		})
	}
}

func TestExplainResourceJSON_Fallback(t *testing.T) {
	c := &Client{timeout: defaultTimeout}
	jsonStr, err := c.ExplainResourceJSON("pod", "")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	var explain ResourceExplain
	if err := json.Unmarshal([]byte(jsonStr), &explain); err != nil {
		t.Fatalf("failed to unmarshal JSON: %v", err)
	}

	if explain.Kind != "Pod" {
		t.Errorf("expected Kind = Pod, got %s", explain.Kind)
	}
	if len(explain.Fields) == 0 {
		t.Error("expected non-empty fields")
	}
}

func TestExplainResourceJSON_UnknownResource(t *testing.T) {
	c := &Client{timeout: defaultTimeout}
	jsonStr, err := c.ExplainResourceJSON("mycustomresource", "custom.io/v1alpha1")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	var explain ResourceExplain
	if err := json.Unmarshal([]byte(jsonStr), &explain); err != nil {
		t.Fatalf("failed to unmarshal JSON: %v", err)
	}

	if explain.Kind != "mycustomresource" {
		t.Errorf("expected Kind = mycustomresource, got %s", explain.Kind)
	}
	if explain.GroupVersion != "custom.io/v1alpha1" {
		t.Errorf("expected GroupVersion = custom.io/v1alpha1, got %s", explain.GroupVersion)
	}
}
