package client

import (
	"context"
	"encoding/json"
	"fmt"

	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// Namespace contains name and status for a cluster namespace.
type Namespace struct {
	Name   string `json:"name"`
	Status string `json:"status"`
}

// NamespaceList represents an indexed list of namespaces for Gomobile / Android JNI.
type NamespaceList struct {
	items []Namespace
}

// newNamespaceList creates a NamespaceList wrapper.
func newNamespaceList(items []Namespace) *NamespaceList {
	return &NamespaceList{items: items}
}

// Len returns the number of namespaces in the list.
func (l *NamespaceList) Len() int {
	if l == nil {
		return 0
	}
	return len(l.items)
}

// Get returns the namespace at index, or nil if out of bounds.
func (l *NamespaceList) Get(index int) *Namespace {
	if l == nil || index < 0 || index >= len(l.items) {
		return nil
	}
	return &l.items[index]
}

// ListNamespaces returns all namespaces in the cluster.
func (c *Client) ListNamespaces() (*NamespaceList, error) {
	ctx, cancel := context.WithTimeout(context.Background(), c.timeout)
	defer cancel()

	nsList, err := c.clientset.CoreV1().Namespaces().List(ctx, metav1.ListOptions{})
	if err != nil {
		return nil, fmt.Errorf("listing namespaces: %w", err)
	}

	namespaces := make([]Namespace, len(nsList.Items))
	for i, ns := range nsList.Items {
		namespaces[i] = Namespace{
			Name:   ns.Name,
			Status: string(ns.Status.Phase),
		}
	}
	return &NamespaceList{items: namespaces}, nil
}

// ListNamespacesJSON returns all namespaces as a JSON string for direct Android parsing.
func (c *Client) ListNamespacesJSON() (string, error) {
	list, err := c.ListNamespaces()
	if err != nil {
		return "", err
	}
	data, err := json.Marshal(list.items)
	if err != nil {
		return "", fmt.Errorf("marshaling namespaces to json: %w", err)
	}
	return string(data), nil
}
