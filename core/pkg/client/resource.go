package client

import (
	"context"
	"fmt"
	"strings"

	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/apis/meta/v1/unstructured"
	"k8s.io/apimachinery/pkg/runtime/schema"
)

// ListOptions mirrors metav1.ListOptions using only field types that Gomobile
// can bind, so Android callers get the full fidelity of a Kubernetes list call
// without the Go core having to flatten parameters per resource.
//
// Because this type is declared in the bound package and every exported field is
// a string or an int64, gobind generates a real Kotlin/Java class for it with
// getters and setters, and turns NewListOptions into a constructor. metav1.ListOptions
// itself cannot be bound: it embeds TypeMeta and carries *int64 and *bool fields.
type ListOptions struct {
	// LabelSelector restricts results by label, e.g. "app=nginx,tier!=backend".
	LabelSelector string
	// FieldSelector restricts results by field, e.g. "status.phase=Running".
	FieldSelector string
	// ResourceVersion constrains which resource versions may serve the request.
	ResourceVersion string
	// ResourceVersionMatch is one of "", "Exact" or "NotOlderThan".
	ResourceVersionMatch string
	// Continue carries the continuation token from a previous paged response.
	Continue string
	// Limit caps the number of items returned per page. Zero means no limit.
	Limit int64
	// TimeoutSeconds bounds the server-side duration of the call. Zero means the
	// client default applies.
	TimeoutSeconds int64
}

// NewListOptions returns empty list options. gobind exposes this as a
// constructor, so Kotlin can write ListOptions().apply { limit = 100 }.
func NewListOptions() *ListOptions {
	return &ListOptions{}
}

func (o *ListOptions) toK8s() metav1.ListOptions {
	if o == nil {
		return metav1.ListOptions{}
	}
	out := metav1.ListOptions{
		LabelSelector:        o.LabelSelector,
		FieldSelector:        o.FieldSelector,
		ResourceVersion:      o.ResourceVersion,
		ResourceVersionMatch: metav1.ResourceVersionMatch(o.ResourceVersionMatch),
		Continue:             o.Continue,
		Limit:                o.Limit,
	}
	if o.TimeoutSeconds > 0 {
		timeout := o.TimeoutSeconds
		out.TimeoutSeconds = &timeout
	}
	return out
}

// DeleteOptions mirrors the bindable subset of metav1.DeleteOptions.
type DeleteOptions struct {
	// GracePeriodSeconds is the duration in seconds before the object is deleted.
	// A negative value leaves the decision to the server.
	GracePeriodSeconds int64
	// PropagationPolicy is one of "", "Orphan", "Background" or "Foreground".
	PropagationPolicy string
}

// NewDeleteOptions returns delete options that defer entirely to the server.
func NewDeleteOptions() *DeleteOptions {
	return &DeleteOptions{GracePeriodSeconds: -1}
}

func (o *DeleteOptions) toK8s() metav1.DeleteOptions {
	if o == nil {
		return metav1.DeleteOptions{}
	}
	out := metav1.DeleteOptions{}
	if o.GracePeriodSeconds >= 0 {
		grace := o.GracePeriodSeconds
		out.GracePeriodSeconds = &grace
	}
	if o.PropagationPolicy != "" {
		policy := metav1.DeletionPropagation(o.PropagationPolicy)
		out.PropagationPolicy = &policy
	}
	return out
}

// GroupVersionResource identifies a resource for the generic access methods.
// Group is empty for core types such as pods and namespaces.
//
// Callers already have every field they need from ListAPIResourcesJSON, which
// reports Group, Version and Name for each resource the cluster serves, so no
// server-side discovery or name resolution is required here.
type GroupVersionResource struct {
	Group    string
	Version  string
	Resource string
}

// NewGroupVersionResource builds a resource identifier. gobind exposes this as a
// constructor taking (String, String, String).
func NewGroupVersionResource(group, version, resource string) *GroupVersionResource {
	return &GroupVersionResource{Group: group, Version: version, Resource: resource}
}

func (g *GroupVersionResource) validate() (schema.GroupVersionResource, error) {
	if g == nil {
		return schema.GroupVersionResource{}, fmt.Errorf("resource identifier is required")
	}
	if strings.TrimSpace(g.Version) == "" {
		return schema.GroupVersionResource{}, fmt.Errorf("version is required")
	}
	if strings.TrimSpace(g.Resource) == "" {
		return schema.GroupVersionResource{}, fmt.Errorf("resource is required")
	}
	return schema.GroupVersionResource{
		Group:    g.Group,
		Version:  g.Version,
		Resource: g.Resource,
	}, nil
}

// ListJSON lists any resource and returns the verbatim Kubernetes list object as
// JSON. This single generic entry point replaces the per-resource flattened
// wrappers: every field the API server returns reaches Kotlin, including labels,
// annotations and nested lists that Gomobile cannot express as native types.
//
// namespace may be empty to list across all namespaces, or for cluster-scoped
// resources. managedFields are stripped from every item because they routinely
// account for most of a response body and nothing on the client reads them.
func (c *Client) ListJSON(gvr *GroupVersionResource, namespace string, opts *ListOptions) (string, error) {
	if c == nil || c.dynamic == nil {
		return "", fmt.Errorf("client is not configured")
	}
	resource, err := gvr.validate()
	if err != nil {
		return "", err
	}

	ctx, cancel := context.WithTimeout(context.Background(), c.timeout)
	defer cancel()

	var list *unstructured.UnstructuredList
	if namespace == "" {
		list, err = c.dynamic.Resource(resource).List(ctx, opts.toK8s())
	} else {
		list, err = c.dynamic.Resource(resource).Namespace(namespace).List(ctx, opts.toK8s())
	}
	if err != nil {
		return "", fmt.Errorf("listing %s: %w", resource.String(), err)
	}

	for i := range list.Items {
		stripManagedFields(&list.Items[i])
	}

	data, err := list.MarshalJSON()
	if err != nil {
		return "", fmt.Errorf("marshaling %s list: %w", resource.String(), err)
	}
	return string(data), nil
}

// GetJSON fetches a single object by name and returns it verbatim as JSON.
func (c *Client) GetJSON(gvr *GroupVersionResource, namespace, name string) (string, error) {
	if c == nil || c.dynamic == nil {
		return "", fmt.Errorf("client is not configured")
	}
	resource, err := gvr.validate()
	if err != nil {
		return "", err
	}
	if strings.TrimSpace(name) == "" {
		return "", fmt.Errorf("name is required")
	}

	ctx, cancel := context.WithTimeout(context.Background(), c.timeout)
	defer cancel()

	var obj *unstructured.Unstructured
	if namespace == "" {
		obj, err = c.dynamic.Resource(resource).Get(ctx, name, metav1.GetOptions{})
	} else {
		obj, err = c.dynamic.Resource(resource).Namespace(namespace).Get(ctx, name, metav1.GetOptions{})
	}
	if err != nil {
		return "", fmt.Errorf("getting %s %q: %w", resource.String(), name, err)
	}

	stripManagedFields(obj)

	data, err := obj.MarshalJSON()
	if err != nil {
		return "", fmt.Errorf("marshaling %s %q: %w", resource.String(), name, err)
	}
	return string(data), nil
}

// DeleteResource deletes any object by name. Pass nil options to accept the
// server defaults.
func (c *Client) DeleteResource(gvr *GroupVersionResource, namespace, name string, opts *DeleteOptions) error {
	if c == nil || c.dynamic == nil {
		return fmt.Errorf("client is not configured")
	}
	resource, err := gvr.validate()
	if err != nil {
		return err
	}
	if strings.TrimSpace(name) == "" {
		return fmt.Errorf("name is required")
	}

	ctx, cancel := context.WithTimeout(context.Background(), c.timeout)
	defer cancel()

	if namespace == "" {
		err = c.dynamic.Resource(resource).Delete(ctx, name, opts.toK8s())
	} else {
		err = c.dynamic.Resource(resource).Namespace(namespace).Delete(ctx, name, opts.toK8s())
	}
	if err != nil {
		return fmt.Errorf("deleting %s %q: %w", resource.String(), name, err)
	}
	return nil
}

// Well-known resource identifiers, exposed so Android does not have to hardcode
// group/version strings for the handful of types it accesses directly.

// PodsResource returns the identifier for core/v1 pods.
func PodsResource() *GroupVersionResource {
	return NewGroupVersionResource("", "v1", "pods")
}

// NamespacesResource returns the identifier for core/v1 namespaces.
func NamespacesResource() *GroupVersionResource {
	return NewGroupVersionResource("", "v1", "namespaces")
}

// EventsResource returns the identifier for core/v1 events.
func EventsResource() *GroupVersionResource {
	return NewGroupVersionResource("", "v1", "events")
}

// EventsForJSON lists events referring to a specific object. This replaces the
// hand-rolled field selector that used to live inside the pod describe path.
func (c *Client) EventsForJSON(namespace, involvedKind, involvedName string) (string, error) {
	if strings.TrimSpace(involvedKind) == "" || strings.TrimSpace(involvedName) == "" {
		return "", fmt.Errorf("involvedKind and involvedName are required")
	}
	opts := &ListOptions{
		FieldSelector: fmt.Sprintf(
			"involvedObject.name=%s,involvedObject.kind=%s",
			involvedName, involvedKind,
		),
	}
	return c.ListJSON(EventsResource(), namespace, opts)
}

// stripManagedFields removes the server-side apply bookkeeping that inflates
// payload size without being read by the Android client.
func stripManagedFields(obj *unstructured.Unstructured) {
	if obj == nil || obj.Object == nil {
		return
	}
	unstructured.RemoveNestedField(obj.Object, "metadata", "managedFields")
}
