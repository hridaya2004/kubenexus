package client

import (
	"context"
	"fmt"
	"net/url"
)

const metricsBasePath = "/apis/metrics.k8s.io/v1beta1"

// TopPodsJSON returns the metrics.k8s.io PodMetricsList verbatim: container CPU
// and memory usage as reported by `kubectl top pods`. An empty namespace lists
// across all namespaces. Reshaping is left to Android.
//
// Prefer TopPodJSON when only one pod is of interest; this fetches every pod in
// the namespace.
func (c *Client) TopPodsJSON(namespace string) (string, error) {
	if c == nil || c.clientset == nil {
		return "", fmt.Errorf("client is not configured")
	}

	path := metricsBasePath + "/pods"
	if namespace != "" {
		path = metricsBasePath + "/namespaces/" + url.PathEscape(namespace) + "/pods"
	}

	data, err := c.rawMetrics(path)
	if err != nil {
		return "", fmt.Errorf("fetching pod metrics: %w", err)
	}
	return data, nil
}

// TopPodJSON returns the metrics.k8s.io PodMetrics object for a single pod.
//
// Note the shape differs from TopPodsJSON: this is one PodMetrics object, not a
// PodMetricsList, matching what the API server returns for a named resource.
// Polling a single pod avoids transferring usage for every pod in the namespace.
func (c *Client) TopPodJSON(namespace, podName string) (string, error) {
	if c == nil || c.clientset == nil {
		return "", fmt.Errorf("client is not configured")
	}
	if namespace == "" || podName == "" {
		return "", fmt.Errorf("namespace and podName are required")
	}

	path := metricsBasePath + "/namespaces/" + url.PathEscape(namespace) +
		"/pods/" + url.PathEscape(podName)

	data, err := c.rawMetrics(path)
	if err != nil {
		return "", fmt.Errorf("fetching metrics for pod %q: %w", podName, err)
	}
	return data, nil
}

// rawMetrics performs a raw GET against the metrics API.
//
// metrics.k8s.io exposes exactly two resources, "pods" and "nodes", in both
// v1beta1 and the older v1alpha1, so there is no alternate resource name to
// fall back to. A 404 here means the metrics API is not served at all, which is
// the normal state of a cluster without metrics-server, and is reported as-is
// rather than retried.
func (c *Client) rawMetrics(path string) (string, error) {
	ctx, cancel := context.WithTimeout(context.Background(), c.timeout)
	defer cancel()

	data, err := c.clientset.Discovery().RESTClient().Get().AbsPath(path).Do(ctx).Raw()
	if err != nil {
		return "", err
	}
	return string(data), nil
}
