package client

import (
	"encoding/json"
	"net/http"
	"strings"
	"testing"

	apierrors "k8s.io/apimachinery/pkg/api/errors"
)

const podMetricsListPayload = `{"kind":"PodMetricsList","apiVersion":"metrics.k8s.io/v1beta1","metadata":{},"items":[` +
	`{"metadata":{"name":"nginx","namespace":"default"},"timestamp":"2026-08-24T10:00:00Z","window":"30s","containers":[{"name":"app","usage":{"cpu":"10555728n","memory":"98234112"}}]},` +
	`{"metadata":{"name":"worker","namespace":"default"},"timestamp":"2026-08-24T10:00:00Z","window":"30s","containers":[{"name":"sidecar","usage":{"cpu":"512000n","memory":"16777216"}}]}]}`

const podMetricsPayload = `{"kind":"PodMetrics","apiVersion":"metrics.k8s.io/v1beta1",` +
	`"metadata":{"name":"nginx","namespace":"default"},"timestamp":"2026-08-24T10:00:00Z","window":"30s",` +
	`"containers":[{"name":"app","usage":{"cpu":"10555728n","memory":"98234112"}}]}`

func TestTopPodsJSON_PassesThroughVerbatim(t *testing.T) {
	var requested string
	mux := http.NewServeMux()
	mux.HandleFunc("/apis/metrics.k8s.io/v1beta1/namespaces/default/pods", func(w http.ResponseWriter, r *http.Request) {
		requested = r.URL.Path
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(podMetricsListPayload))
	})
	c := newTestClient(t, mux)

	payload, err := c.TopPodsJSON("default")
	if err != nil {
		t.Fatalf("TopPodsJSON(default) error = %v", err)
	}
	if payload != podMetricsListPayload {
		t.Error("metrics were not passed through verbatim")
	}
	if requested != "/apis/metrics.k8s.io/v1beta1/namespaces/default/pods" {
		t.Errorf("requested %q", requested)
	}
	var decoded map[string]any
	if err := json.Unmarshal([]byte(payload), &decoded); err != nil {
		t.Fatalf("payload is not JSON: %v", err)
	}
}

func TestTopPodsJSON_AllNamespacesOmitsNamespaceSegment(t *testing.T) {
	var requested string
	mux := http.NewServeMux()
	mux.HandleFunc("/apis/metrics.k8s.io/v1beta1/pods", func(w http.ResponseWriter, r *http.Request) {
		requested = r.URL.Path
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(podMetricsListPayload))
	})
	c := newTestClient(t, mux)

	if _, err := c.TopPodsJSON(""); err != nil {
		t.Fatalf("TopPodsJSON(all) error = %v", err)
	}
	if requested != "/apis/metrics.k8s.io/v1beta1/pods" {
		t.Errorf("requested %q, want the cluster-wide pods path", requested)
	}
}

// Polling a detail screen must fetch one pod rather than the whole namespace.
func TestTopPodJSON_TargetsASinglePod(t *testing.T) {
	var requested string
	mux := http.NewServeMux()
	mux.HandleFunc("/apis/metrics.k8s.io/v1beta1/namespaces/default/pods/nginx", func(w http.ResponseWriter, r *http.Request) {
		requested = r.URL.Path
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(podMetricsPayload))
	})
	c := newTestClient(t, mux)

	payload, err := c.TopPodJSON("default", "nginx")
	if err != nil {
		t.Fatalf("TopPodJSON() error = %v", err)
	}
	if requested != "/apis/metrics.k8s.io/v1beta1/namespaces/default/pods/nginx" {
		t.Errorf("requested %q", requested)
	}
	if payload != podMetricsPayload {
		t.Error("metrics were not passed through verbatim")
	}

	// A named resource returns a single object, not a list.
	var decoded struct {
		Kind  string `json:"kind"`
		Items []any  `json:"items"`
	}
	if err := json.Unmarshal([]byte(payload), &decoded); err != nil {
		t.Fatalf("payload is not JSON: %v", err)
	}
	if decoded.Kind != "PodMetrics" {
		t.Errorf("kind = %q, want PodMetrics", decoded.Kind)
	}
	if decoded.Items != nil {
		t.Error("single-pod response should not carry an items array")
	}
}

func TestTopPodJSON_RequiresNamespaceAndName(t *testing.T) {
	c := newTestClient(t, http.NewServeMux())

	if _, err := c.TopPodJSON("", "nginx"); err == nil {
		t.Error("TopPodJSON with empty namespace expected error, got nil")
	}
	if _, err := c.TopPodJSON("default", ""); err == nil {
		t.Error("TopPodJSON with empty pod name expected error, got nil")
	}
}

// Names with characters that need escaping must not corrupt the request path.
func TestTopPodJSON_EscapesPathSegments(t *testing.T) {
	var requested string
	mux := http.NewServeMux()
	mux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		requested = r.URL.EscapedPath()
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(podMetricsPayload))
	})
	c := newTestClient(t, mux)

	if _, err := c.TopPodJSON("default", "odd/name"); err != nil {
		t.Fatalf("TopPodJSON() error = %v", err)
	}
	if strings.Contains(requested, "odd/name") {
		t.Errorf("path %q contains an unescaped slash", requested)
	}
}

// A cluster without metrics-server returns 404. That must surface as an error
// rather than an empty success, and must not be retried against an alternate
// resource name: metrics.k8s.io only ever served "pods" and "nodes".
func TestTopPods_MissingMetricsServerIsASingleRequest(t *testing.T) {
	requests := 0
	mux := http.NewServeMux()
	mux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		requests++
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusNotFound)
		_, _ = w.Write([]byte(`{"kind":"Status","apiVersion":"v1","status":"Failure",` +
			`"message":"the server could not find the requested resource","reason":"NotFound","code":404}`))
	})
	c := newTestClient(t, mux)

	_, err := c.TopPodsJSON("default")
	if err == nil {
		t.Fatal("expected an error when the metrics API is absent, got nil")
	}
	if requests != 1 {
		t.Errorf("made %d requests, want 1; a missing metrics API must not be retried", requests)
	}
	if !apierrors.IsNotFound(err) {
		t.Errorf("error does not classify as NotFound, callers cannot distinguish it: %v", err)
	}
}

func TestTopPodsJSON_ServerErrorPropagates(t *testing.T) {
	mux := http.NewServeMux()
	mux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		http.Error(w, "metrics-server unavailable", http.StatusInternalServerError)
	})
	c := newTestClient(t, mux)

	if _, err := c.TopPodsJSON("default"); err == nil {
		t.Fatal("expected error when metrics-server fails, got nil")
	}
}

func TestTopPods_UnconfiguredClient(t *testing.T) {
	c := &Client{timeout: defaultTimeout}

	if _, err := c.TopPodsJSON("default"); err == nil {
		t.Error("TopPodsJSON on unconfigured client expected error, got nil")
	}
	if _, err := c.TopPodJSON("default", "nginx"); err == nil {
		t.Error("TopPodJSON on unconfigured client expected error, got nil")
	}
}
