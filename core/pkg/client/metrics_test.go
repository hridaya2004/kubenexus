package client

import (
	"encoding/json"
	"net/http"
	"testing"
)

const podMetricsPayload = `{"kind":"PodMetricsList","apiVersion":"metrics.k8s.io/v1beta1","metadata":{},"items":[` +
	`{"timestamp":"2026-08-24T10:00:00Z","window":"30s","containers":[{"name":"app","usage":{"cpu":"10555728n","memory":"98234112"}}]},` +
	`{"timestamp":"2026-08-24T10:00:00Z","window":"30s","containers":[{"name":"sidecar","usage":{"cpu":"512000n","memory":"16777216"}}]}]}`

// TestTopPodsJSON_PassesThrough pins that recorded metrics-server payloads
// reach Android untouched, and that both the new ("pods") and legacy
// ("podmetrics") resource names are understood.
func TestTopPodsJSON_PassesThrough(t *testing.T) {
	served := ""
	mux := http.NewServeMux()
	mux.HandleFunc("/apis/metrics.k8s.io/v1beta1/namespaces/default/pods", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		served = "pods"
		_, _ = w.Write([]byte(podMetricsPayload))
	})
	mux.HandleFunc("/apis/metrics.k8s.io/v1beta1/podmetrics", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		served = "podmetrics"
		_, _ = w.Write([]byte(podMetricsPayload))
	})

	c := newTestClient(t, mux)

	payload, err := c.TopPodsJSON("default")
	if err != nil {
		t.Fatalf("TopPodsJSON(default) error = %v", err)
	}
	if served != "pods" {
		t.Errorf("expected the modern pods path first, went to %q", served)
	}
	var decoded map[string]any
	if err := json.Unmarshal([]byte(payload), &decoded); err != nil {
		t.Fatalf("payload is not JSON: %v", err)
	}
	if payload != podMetricsPayload {
		t.Errorf("metrics were not passed through verbatim")
	}

	// Legacy servers only know podmetrics; the 404 on pods must fall back.
	legacyMux := http.NewServeMux()
	legacyMux.HandleFunc("/apis/metrics.k8s.io/v1beta1/namespaces/default/podmetrics", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(podMetricsPayload))
	})
	legacyClient := newTestClient(t, legacyMux)

	if _, err := legacyClient.TopPodsJSON("default"); err != nil {
		t.Fatalf("legacy podmetrics fallback error = %v", err)
	}

	served = ""
	if _, err := c.TopPodsJSON(""); err != nil {
		t.Fatalf("TopPodsJSON(all) error = %v", err)
	}
	if served == "" {
		t.Error("all-namespaces request never reached a handler")
	}
}

// TestTopPodsJSON_ErrorPropagates pins that a metrics-server failure surfaces
// as an error instead of an empty success.
func TestTopPodsJSON_ErrorPropagates(t *testing.T) {
	mux := http.NewServeMux()
	mux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		http.Error(w, "metrics-server unavailable", http.StatusInternalServerError)
	})
	c := newTestClient(t, mux)

	if _, err := c.TopPodsJSON("default"); err == nil {
		t.Fatal("expected error when metrics-server fails, got nil")
	}
}
