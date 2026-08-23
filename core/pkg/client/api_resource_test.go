package client

import (
	"compress/gzip"
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"k8s.io/client-go/rest"
)

func newTestClient(t *testing.T, handler http.Handler) *Client {
	t.Helper()
	srv := httptest.NewServer(handler)
	t.Cleanup(srv.Close)

	c, err := newClientFromConfig(&rest.Config{Host: srv.URL}, 15*time.Second)
	if err != nil {
		t.Fatalf("newClientFromConfig() error = %v", err)
	}
	return c
}

func readFixture(t *testing.T, rel string) []byte {
	t.Helper()
	b, err := os.ReadFile(filepath.Join("testdata", rel))
	if err != nil {
		t.Fatalf("reading fixture %s: %v", rel, err)
	}
	return b
}

// discoveryMux replays payloads recorded from a live cluster via kubectl into
// testdata. If failGV is non-empty, that group version answers 500 instead.
func discoveryMux(t *testing.T, failGV string) http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		switch p := r.URL.Path; {
		case p == "/api":
			w.Write(readFixture(t, "discovery-api.json"))
		case p == "/apis":
			w.Write(readFixture(t, "discovery-apis.json"))
		case p == "/api/v1":
			if failGV == "v1" {
				http.Error(w, "aggregated API unavailable", http.StatusInternalServerError)
				return
			}
			w.Write(readFixture(t, filepath.Join("gv", "v1.json")))
		case strings.HasPrefix(p, "/apis/"):
			gv := strings.TrimPrefix(p, "/apis/")
			if gv == failGV {
				http.Error(w, "aggregated API unavailable", http.StatusInternalServerError)
				return
			}
			name := strings.ReplaceAll(gv, "/", "_") + ".json"
			b, err := os.ReadFile(filepath.Join("testdata", "gv", name))
			if err != nil {
				http.NotFound(w, r)
				return
			}
			w.Write(b)
		default:
			http.NotFound(w, r)
		}
	})
	return mux
}

type discoveryListing struct {
	GroupVersion string           `json:"groupVersion"`
	Resources    []map[string]any `json:"resources"`
}

func resourcesOf(t *testing.T, payload, groupVersion string) map[string]bool {
	t.Helper()
	var listings []discoveryListing
	if err := json.Unmarshal([]byte(payload), &listings); err != nil {
		t.Fatalf("payload is not a JSON array of listings: %v\npayload: %s", err, payload)
	}
	names := make(map[string]bool)
	for _, l := range listings {
		if l.GroupVersion == groupVersion {
			for _, r := range l.Resources {
				names[r["name"].(string)] = true
			}
		}
	}
	return names
}

// TestListAPIResourcesJSON_PassesThrough pins the boundary relative to what
// client-go discovery reports: every top-level resource the server listed must
// reach Android, and nothing may be added or reshaped. Note client-go itself
// excludes subresources from ServerPreferredResources output.
func TestListAPIResourcesJSON_PassesThrough(t *testing.T) {
	c := newTestClient(t, discoveryMux(t, ""))

	payload, err := c.ListAPIResourcesJSON()
	if err != nil {
		t.Fatalf("ListAPIResourcesJSON() error = %v", err)
	}

	var listings []discoveryListing
	if err := json.Unmarshal([]byte(payload), &listings); err != nil {
		t.Fatalf("decoding payload: %v", err)
	}
	if len(listings) < 2 {
		t.Fatalf("expected listings for multiple group versions, got %d", len(listings))
	}

	var want struct {
		Resources []struct {
			Name string `json:"name"`
		} `json:"resources"`
	}
	fixture := readFixture(t, filepath.Join("gv", "v1.json"))
	if err := json.Unmarshal(fixture, &want); err != nil {
		t.Fatalf("decoding v1 fixture: %v", err)
	}

	got := resourcesOf(t, payload, "v1")
	for _, r := range want.Resources {
		if strings.Contains(r.Name, "/") {
			if got[r.Name] {
				t.Errorf("subresource %q should have been excluded by client-go discovery", r.Name)
			}
			continue
		}
		if !got[r.Name] {
			t.Errorf("resource %q from server missing in payload", r.Name)
		}
	}
}

// TestListAPIResourcesJSON_PartialFailureKeepsResults mirrors kubectl: one dead
// group version must not discard the group versions that answered.
func TestListAPIResourcesJSON_PartialFailureKeepsResults(t *testing.T) {
	c := newTestClient(t, discoveryMux(t, "apps/v1"))

	payload, err := c.ListAPIResourcesJSON()
	if err != nil {
		t.Fatalf("partial discovery must not fail the call, got: %v", err)
	}

	if !resourcesOf(t, payload, "v1")["pods"] {
		t.Error("expected surviving v1 results after partial failure")
	}
	if len(resourcesOf(t, payload, "apps/v1")) != 0 {
		t.Error("failed group version should contribute no results")
	}
}

// TestOpenAPISchemaJSON_ReturnsVerbatimBody pins raw pass-through of the
// recorded /openapi/v2 document.
func TestOpenAPISchemaJSON_ReturnsVerbatimBody(t *testing.T) {
	f, err := os.Open(filepath.Join("testdata", "openapi-v2.json.gz"))
	if err != nil {
		t.Fatalf("opening schema fixture: %v", err)
	}
	defer f.Close()

	gz, err := gzip.NewReader(f)
	if err != nil {
		t.Fatalf("gzip reader: %v", err)
	}
	schema, err := io.ReadAll(gz)
	if err != nil {
		t.Fatalf("gunzipping schema fixture: %v", err)
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/openapi/v2", func(w http.ResponseWriter, r *http.Request) {
		if got := r.Header.Get("Accept"); got != "application/json" {
			t.Errorf("Accept header = %q, want application/json", got)
		}
		w.Header().Set("Content-Type", "application/json")
		w.Write(schema)
	})
	c := newTestClient(t, mux)

	got, err := c.OpenAPISchemaJSON()
	if err != nil {
		t.Fatalf("OpenAPISchemaJSON() error = %v", err)
	}
	if got != string(schema) {
		t.Errorf("schema was not passed through verbatim (%d vs %d bytes)", len(got), len(schema))
	}
}
