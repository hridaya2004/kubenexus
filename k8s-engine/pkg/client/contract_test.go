package client

import (
	"encoding/json"
	"os"
	"testing"

	"k8s.io/apimachinery/pkg/apis/meta/v1/unstructured"
)

// TestListJSONEnvelopeContract pins the JSON envelope that ListJSON hands to
// Android. The Kotlin DTOs decode this shape, and nothing else verifies the
// boundary, so a change in how unstructured lists marshal would otherwise
// surface only as silently empty lists on device.
func TestListJSONEnvelopeContract(t *testing.T) {
	list := &unstructured.UnstructuredList{Object: map[string]any{
		"apiVersion": "v1",
		"kind":       "PodList",
		"metadata": map[string]any{
			"resourceVersion": "104729",
			"continue":        "token-abc",
		},
	}}
	list.Items = []unstructured.Unstructured{{Object: map[string]any{
		"apiVersion": "v1",
		"kind":       "Pod",
		"metadata": map[string]any{
			"name":              "nginx",
			"namespace":         "default",
			"creationTimestamp": "2026-08-21T09:15:00Z",
			"labels":            map[string]any{"app": "nginx"},
			"managedFields":     []any{map[string]any{"manager": "kubectl"}},
		},
		"spec": map[string]any{
			"nodeName":   "node-1",
			"containers": []any{map[string]any{"name": "nginx", "image": "nginx:1.27.1"}},
		},
		"status": map[string]any{
			"phase": "Running",
			"podIP": "10.244.0.15",
			"containerStatuses": []any{map[string]any{
				"name": "nginx", "ready": true, "restartCount": int64(2),
				"state": map[string]any{"running": map[string]any{}},
			}},
		},
	}}}

	// Same transformation ListJSON applies.
	for i := range list.Items {
		stripManagedFields(&list.Items[i])
	}
	data, err := list.MarshalJSON()
	if err != nil {
		t.Fatalf("MarshalJSON() error = %v", err)
	}

	var envelope map[string]json.RawMessage
	if err := json.Unmarshal(data, &envelope); err != nil {
		t.Fatalf("envelope is not a JSON object: %v", err)
	}
	for _, key := range []string{"apiVersion", "kind", "metadata", "items"} {
		if _, ok := envelope[key]; !ok {
			t.Errorf("envelope is missing %q; the Kotlin DTO expects it", key)
		}
	}

	var decoded struct {
		APIVersion string `json:"apiVersion"`
		Kind       string `json:"kind"`
		Metadata   struct {
			ResourceVersion string `json:"resourceVersion"`
			Continue        string `json:"continue"`
		} `json:"metadata"`
		Items []struct {
			Metadata struct {
				Name              string            `json:"name"`
				Namespace         string            `json:"namespace"`
				CreationTimestamp string            `json:"creationTimestamp"`
				Labels            map[string]string `json:"labels"`
				ManagedFields     []any             `json:"managedFields"`
			} `json:"metadata"`
			Spec struct {
				NodeName   string `json:"nodeName"`
				Containers []struct {
					Name  string `json:"name"`
					Image string `json:"image"`
				} `json:"containers"`
			} `json:"spec"`
			Status struct {
				Phase             string `json:"phase"`
				PodIP             string `json:"podIP"`
				ContainerStatuses []struct {
					Name         string `json:"name"`
					Ready        bool   `json:"ready"`
					RestartCount int32  `json:"restartCount"`
				} `json:"containerStatuses"`
			} `json:"status"`
		} `json:"items"`
	}
	if err := json.Unmarshal(data, &decoded); err != nil {
		t.Fatalf("decoding the envelope failed: %v", err)
	}

	if decoded.Kind != "PodList" {
		t.Errorf("kind = %q, want PodList", decoded.Kind)
	}
	if decoded.Metadata.Continue != "token-abc" {
		t.Errorf("metadata.continue = %q, want token-abc", decoded.Metadata.Continue)
	}
	if len(decoded.Items) != 1 {
		t.Fatalf("items = %d, want 1", len(decoded.Items))
	}

	item := decoded.Items[0]
	if item.Metadata.ManagedFields != nil {
		t.Error("managedFields reached the client; stripping did not apply to list items")
	}
	if item.Metadata.Labels["app"] != "nginx" {
		t.Errorf("labels = %v, want app=nginx", item.Metadata.Labels)
	}
	if item.Spec.Containers[0].Image != "nginx:1.27.1" {
		t.Errorf("image = %q", item.Spec.Containers[0].Image)
	}
	if !item.Status.ContainerStatuses[0].Ready || item.Status.ContainerStatuses[0].RestartCount != 2 {
		t.Errorf("containerStatuses[0] = %+v", item.Status.ContainerStatuses[0])
	}

	// Emitting the payload lets the Android test suite decode Go's real output
	// rather than a hand-written approximation.
	if out := os.Getenv("KUBENEXUS_CONTRACT_OUT"); out != "" {
		if err := os.WriteFile(out, data, 0o600); err != nil {
			t.Fatalf("writing %s: %v", out, err)
		}
	}
}
