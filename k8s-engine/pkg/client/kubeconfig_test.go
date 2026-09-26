package client

import (
	"encoding/base64"
	"encoding/pem"
	"fmt"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

// tlsKubeconfig returns a kubeconfig for server that trusts the test server's own
// certificate, so tests exercise real TLS verification rather than skipping it.
func tlsKubeconfig(server *httptest.Server) string {
	caPEM := pem.EncodeToMemory(&pem.Block{Type: "CERTIFICATE", Bytes: server.Certificate().Raw})
	return fmt.Sprintf(`
apiVersion: v1
clusters:
- cluster:
    server: %s
    certificate-authority-data: %s
  name: test
contexts:
- context:
    cluster: test
    user: test
  name: test
current-context: test
users:
- name: test
  user:
    token: test-token
kind: Config
`, server.URL, base64.StdEncoding.EncodeToString(caPEM))
}

func TestNewClient_AcceptsHTTPSServer(t *testing.T) {
	server := httptest.NewTLSServer(http.NotFoundHandler())
	defer server.Close()

	if _, err := NewClient(tlsKubeconfig(server)); err != nil {
		t.Fatalf("NewClient(https kubeconfig) error = %v", err)
	}
}

// Android's network security config does not apply to Go sockets, so the engine itself
// must refuse cleartext cluster connections.
func TestNewClient_RejectsPlainHTTPServer(t *testing.T) {
	kubeconfig := `
apiVersion: v1
kind: Config
clusters:
- cluster:
    server: http://127.0.0.1:8080
  name: plain
contexts:
- context:
    cluster: plain
  name: plain
current-context: plain
`
	_, err := NewClient(kubeconfig)
	if err == nil {
		t.Fatal("NewClient(http kubeconfig) error = nil, want a refusal")
	}
	if !strings.Contains(err.Error(), "does not use https") {
		t.Errorf("error = %q, want it to explain the https requirement", err)
	}
}

// client-go would run an exec credential plugin as the app's own user. Nothing
// from a kubeconfig may be executed on the device.
func TestNewClient_RejectsExecCredentialPlugin(t *testing.T) {
	kubeconfig := `
apiVersion: v1
kind: Config
clusters:
- cluster:
    server: https://127.0.0.1:6443
  name: c
contexts:
- context:
    cluster: c
    user: u
  name: c
current-context: c
users:
- name: u
  user:
    exec:
      apiVersion: client.authentication.k8s.io/v1
      command: sh
      args: ["-c", "touch /tmp/kubenexus-exec-plugin-ran"]
      interactiveMode: Never
`
	_, err := NewClient(kubeconfig)
	if err == nil {
		t.Fatal("NewClient(exec plugin kubeconfig) error = nil, want a refusal")
	}
	if !strings.Contains(err.Error(), "exec credential plugin") {
		t.Errorf("error = %q, want it to name the exec credential plugin", err)
	}
}

func TestNewClient_RejectsAuthProvider(t *testing.T) {
	kubeconfig := `
apiVersion: v1
kind: Config
clusters:
- cluster:
    server: https://127.0.0.1:6443
  name: c
contexts:
- context:
    cluster: c
    user: u
  name: c
current-context: c
users:
- name: u
  user:
    auth-provider:
      name: oidc
      config:
        idp-issuer-url: https://issuer.example
`
	_, err := NewClient(kubeconfig)
	if err == nil {
		t.Fatal("NewClient(auth-provider kubeconfig) error = nil, want a refusal")
	}
	if !strings.Contains(err.Error(), `"oidc" auth provider`) {
		t.Errorf("error = %q, want it to name the auth provider", err)
	}
}

// The checks must follow current-context: an exec user on a context that is not
// selected does not affect the connection and must not block it.
func TestNewClient_ValidatesOnlyTheCurrentContext(t *testing.T) {
	server := httptest.NewTLSServer(http.NotFoundHandler())
	defer server.Close()

	kubeconfig := tlsKubeconfig(server) + `
`
	kubeconfig = strings.Replace(kubeconfig, "users:\n", `users:
- name: unused-exec
  user:
    exec:
      apiVersion: client.authentication.k8s.io/v1
      command: sh
      interactiveMode: Never
`, 1)

	if _, err := NewClient(kubeconfig); err != nil {
		t.Fatalf("NewClient() error = %v; an exec user outside current-context must not be rejected", err)
	}
}
