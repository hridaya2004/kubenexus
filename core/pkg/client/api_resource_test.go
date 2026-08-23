package client

import (
	"encoding/json"
	"testing"
)

func TestExplainResourceJSON_Fallback(t *testing.T) {
	c := &Client{timeout: defaultTimeout}
	resourceExplainJSON, err := c.ExplainResourceJSON("pod", "")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	var explain ResourceExplain
	if err := json.Unmarshal([]byte(resourceExplainJSON), &explain); err != nil {
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
	resourceExplainJSON, err := c.ExplainResourceJSON("mycustomresource", "custom.io/v1alpha1")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	var explain ResourceExplain
	if err := json.Unmarshal([]byte(resourceExplainJSON), &explain); err != nil {
		t.Fatalf("failed to unmarshal JSON: %v", err)
	}

	if explain.Kind != "mycustomresource" {
		t.Errorf("expected Kind = mycustomresource, got %s", explain.Kind)
	}
	if explain.GroupVersion != "custom.io/v1alpha1" {
		t.Errorf("expected GroupVersion = custom.io/v1alpha1, got %s", explain.GroupVersion)
	}
}
