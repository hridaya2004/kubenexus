package client

import (
	"context"
	"fmt"

	apierrors "k8s.io/apimachinery/pkg/api/errors"
)

// TopPodsJSON returns the metrics.k8s.io PodMetricsList verbatim: container
// CPU and memory usage as reported by `kubectl top pods`. An empty namespace
// lists across all namespaces. Reshaping and per-pod filtering are left to
// Android.
//
// Newer metrics servers serve the list under "pods"; older ones under
// "podmetrics". Both are tried so either deployment works.
func (c *Client) TopPodsJSON(namespace string) (string, error) {
	if c == nil || c.clientset == nil {
		return "", fmt.Errorf("client is not configured")
	}

	ctx, cancel := context.WithTimeout(context.Background(), c.timeout)
	defer cancel()

	restClient := c.clientset.Discovery().RESTClient()

	fetch := func(resource string) ([]byte, error) {
		path := "/apis/metrics.k8s.io/v1beta1"
		if namespace == "" {
			path += "/" + resource
		} else {
			path += "/namespaces/" + namespace + "/" + resource
		}
		return restClient.Get().AbsPath(path).Do(ctx).Raw()
	}

	data, err := fetch("pods")
	if err != nil && apierrors.IsNotFound(err) {
		data, err = fetch("podmetrics")
	}
	if err != nil {
		return "", fmt.Errorf("fetching pod metrics: %w", err)
	}

	return string(data), nil
}
