package client

import (
	"fmt"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestClient_HealthChecks(t *testing.T) {
	mux := http.NewServeMux()
	mux.HandleFunc("/livez", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte("ok"))
	})
	mux.HandleFunc("/readyz", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte("ok"))
	})
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte("ok"))
	})
	mux.HandleFunc("/version", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte(`{"major":"1","minor":"30","gitVersion":"v1.30.0"}`))
	})

	server := httptest.NewServer(mux)
	defer server.Close()

	kubeconfig := fmt.Sprintf(`
apiVersion: v1
clusters:
- cluster:
    server: %s
  name: test
contexts:
- context:
    cluster: test
  name: test
current-context: test
kind: Config
`, server.URL)

	client, err := NewClient(kubeconfig)
	if err != nil {
		t.Fatalf("failed to create client: %v", err)
	}

	live, err := client.CheckLivez()
	if err != nil || !live {
		t.Errorf("CheckLivez() = %v, %v; want true, nil", live, err)
	}

	ready, err := client.CheckReadyz()
	if err != nil || !ready {
		t.Errorf("CheckReadyz() = %v, %v; want true, nil", ready, err)
	}

	healthy, err := client.CheckHealthz()
	if err != nil || !healthy {
		t.Errorf("CheckHealthz() = %v, %v; want true, nil", healthy, err)
	}

	ver, err := client.ServerVersion()
	if err != nil || ver != "v1.30.0" {
		t.Errorf("ServerVersion() = %q, %v; want v1.30.0, nil", ver, err)
	}

	ping, err := client.Ping()
	if err != nil || !strings.Contains(ping, "ready & healthy") {
		t.Errorf("Ping() = %q, %v; want ready & healthy, nil", ping, err)
	}

	healthJSON, err := client.CheckHealthJSON()
	if err != nil || !strings.Contains(healthJSON, `"livez":true`) {
		t.Errorf("CheckHealthJSON() = %q, %v; want livez:true, nil", healthJSON, err)
	}
}

func TestClient_HealthChecks_Unhealthy(t *testing.T) {
	mux := http.NewServeMux()
	mux.HandleFunc("/livez", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("internal error"))
	})
	mux.HandleFunc("/readyz", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusServiceUnavailable)
		_, _ = w.Write([]byte("not ready"))
	})
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("unhealthy"))
	})

	server := httptest.NewServer(mux)
	defer server.Close()

	kubeconfig := fmt.Sprintf(`
apiVersion: v1
clusters:
- cluster:
    server: %s
  name: test
contexts:
- context:
    cluster: test
  name: test
current-context: test
kind: Config
`, server.URL)

	client, err := NewClient(kubeconfig)
	if err != nil {
		t.Fatalf("failed to create client: %v", err)
	}

	live, err := client.CheckLivez()
	if err == nil || live {
		t.Errorf("CheckLivez() = %v, %v; want error", live, err)
	}

	ready, err := client.CheckReadyz()
	if err == nil || ready {
		t.Errorf("CheckReadyz() = %v, %v; want error", ready, err)
	}

	ping, err := client.Ping()
	if err == nil {
		t.Errorf("Ping() = %q, expected error", ping)
	}
}
