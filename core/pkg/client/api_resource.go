package client

import (
	"context"
	"encoding/json"
	"fmt"
	"sort"
	"strings"

	"gopkg.in/yaml.v3"
)

// APIResource represents a discovered Kubernetes API resource.
type APIResource struct {
	Name         string   `json:"name"`
	SingularName string   `json:"singularName"`
	Namespaced   bool     `json:"namespaced"`
	Kind         string   `json:"kind"`
	Group        string   `json:"group"`
	Version      string   `json:"version"`
	GroupVersion string   `json:"groupVersion"`
	Verbs        []string `json:"verbs"`
	ShortNames   []string `json:"shortNames"`
	Categories   []string `json:"categories"`
}

// ResourceField represents a field in the explain schema.
type ResourceField struct {
	Name        string `json:"name"`
	Type        string `json:"type"`
	Description string `json:"description"`
	Required    bool   `json:"required"`
}

// ResourceExplain contains all information provided by `kubectl explain <resource>`.
type ResourceExplain struct {
	Kind         string          `json:"kind"`
	Group        string          `json:"group"`
	Version      string          `json:"version"`
	GroupVersion string          `json:"groupVersion"`
	Description  string          `json:"description"`
	Fields       []ResourceField `json:"fields"`
}

// ListAPIResources returns all preferred API resources supported by the cluster.
func (c *Client) ListAPIResources() ([]APIResource, error) {
	if c == nil || c.clientset == nil {
		return getDefaultAPIResources(), nil
	}

	ctx, cancel := context.WithTimeout(context.Background(), c.timeout)
	defer cancel()

	_ = ctx
	resLists, err := c.clientset.Discovery().ServerPreferredResources()
	if err != nil && len(resLists) == 0 {
		return getDefaultAPIResources(), nil
	}

	var results []APIResource
	seen := make(map[string]bool)

	for _, list := range resLists {
		if list == nil {
			continue
		}
		gv := list.GroupVersion
		var group, version string
		if strings.Contains(gv, "/") {
			parts := strings.SplitN(gv, "/", 2)
			group = parts[0]
			version = parts[1]
		} else {
			group = ""
			version = gv
		}

		for _, r := range list.APIResources {
			// Filter subresources (like pods/log, pods/status, pods/exec)
			if strings.Contains(r.Name, "/") {
				continue
			}

			key := gv + "/" + r.Name
			if seen[key] {
				continue
			}
			seen[key] = true

			verbs := make([]string, len(r.Verbs))
			copy(verbs, r.Verbs)

			shortNames := make([]string, len(r.ShortNames))
			copy(shortNames, r.ShortNames)

			categories := make([]string, len(r.Categories))
			copy(categories, r.Categories)

			results = append(results, APIResource{
				Name:         r.Name,
				SingularName: r.SingularName,
				Namespaced:   r.Namespaced,
				Kind:         r.Kind,
				Group:        group,
				Version:      version,
				GroupVersion: gv,
				Verbs:        verbs,
				ShortNames:   shortNames,
				Categories:   categories,
			})
		}
	}

	sort.Slice(results, func(i, j int) bool {
		if results[i].Name == results[j].Name {
			return results[i].GroupVersion < results[j].GroupVersion
		}
		return results[i].Name < results[j].Name
	})

	return results, nil
}

// ListAPIResourcesJSON returns discovered API resources as a JSON string for Android mobile consumption.
func (c *Client) ListAPIResourcesJSON() (string, error) {
	resources, err := c.ListAPIResources()
	if err != nil {
		return "", err
	}

	data, err := json.Marshal(resources)
	if err != nil {
		return "", fmt.Errorf("marshaling api-resources to json: %w", err)
	}
	return string(data), nil
}

// ExplainResource returns detailed explanation of a resource schema similar to `kubectl explain <resource>`.
func (c *Client) ExplainResource(resourceOrKind, groupVersion string) (*ResourceExplain, error) {
	// First try to fetch from OpenAPI schema if available
	if c != nil && c.clientset != nil {
		doc, err := c.clientset.Discovery().OpenAPISchema()
		if err == nil && doc != nil && doc.Definitions != nil {
			targetKind := strings.ToLower(resourceOrKind)
			for _, namedSchema := range doc.Definitions.AdditionalProperties {
				schema := namedSchema.Value
				if schema == nil {
					continue
				}

				// Check GVK extension
				isMatch := false
				var matchedGroup, matchedVersion, matchedKind string
				for _, ext := range schema.VendorExtension {
					if ext.Name == "x-kubernetes-group-version-kind" && ext.Value != nil {
						var gvks []map[string]interface{}
						if yamlStr := ext.Value.GetYaml(); yamlStr != "" {
							_ = yaml.Unmarshal([]byte(yamlStr), &gvks)
							for _, gvk := range gvks {
								k, _ := gvk["kind"].(string)
								g, _ := gvk["group"].(string)
								v, _ := gvk["version"].(string)
								if strings.EqualFold(k, targetKind) || strings.EqualFold(k+"s", targetKind) {
									if groupVersion == "" || strings.EqualFold(v, groupVersion) || strings.EqualFold(g+"/"+v, groupVersion) {
										isMatch = true
										matchedGroup = g
										matchedVersion = v
										matchedKind = k
										break
									}
								}
							}
						}
					}
				}

				if isMatch {
					explain := &ResourceExplain{
						Kind:         matchedKind,
						Group:        matchedGroup,
						Version:      matchedVersion,
						GroupVersion: matchedVersion,
						Description:  schema.Description,
					}
					if matchedGroup != "" {
						explain.GroupVersion = matchedGroup + "/" + matchedVersion
					}

					if schema.Properties != nil {
						requiredMap := make(map[string]bool)
						for _, req := range schema.Required {
							requiredMap[req] = true
						}

						for _, prop := range schema.Properties.AdditionalProperties {
							pName := prop.Name
							pSchema := prop.Value
							pType := "object"
							pDesc := ""
							if pSchema != nil {
								pDesc = pSchema.Description
								if pSchema.Type != nil && len(pSchema.Type.Value) > 0 {
									pType = pSchema.Type.Value[0]
								} else if pSchema.XRef != "" {
									parts := strings.Split(pSchema.XRef, ".")
									pType = parts[len(parts)-1]
								}
								if pSchema.Format != "" {
									pType += " (" + pSchema.Format + ")"
								}
							}
							explain.Fields = append(explain.Fields, ResourceField{
								Name:        pName,
								Type:        pType,
								Description: pDesc,
								Required:    requiredMap[pName],
							})
						}
						sort.Slice(explain.Fields, func(i, j int) bool {
							return explain.Fields[i].Name < explain.Fields[j].Name
						})
					}
					return explain, nil
				}
			}
		}
	}

	// Fallback to built-in documentation for common resources
	if fallback := getBuiltinExplain(resourceOrKind, groupVersion); fallback != nil {
		return fallback, nil
	}

	return &ResourceExplain{
		Kind:         resourceOrKind,
		GroupVersion: groupVersion,
		Description:  fmt.Sprintf("Kubernetes resource %s (%s). Documentation schema unavailable from active cluster discovery.", resourceOrKind, groupVersion),
		Fields: []ResourceField{
			{Name: "apiVersion", Type: "string", Description: "APIVersion defines the versioned schema of this representation of an object.", Required: true},
			{Name: "kind", Type: "string", Description: "Kind is a string value representing the REST resource this object represents.", Required: true},
			{Name: "metadata", Type: "ObjectMeta", Description: "Standard object's metadata. More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#metadata", Required: false},
			{Name: "spec", Type: "object", Description: "Specification of the desired behavior of the resource.", Required: false},
			{Name: "status", Type: "object", Description: "Most recently observed status of the resource. More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#spec-and-status", Required: false},
		},
	}, nil
}

// ExplainResourceJSON returns resource explain details as a JSON string for Android.
func (c *Client) ExplainResourceJSON(resourceOrKind, groupVersion string) (string, error) {
	explain, err := c.ExplainResource(resourceOrKind, groupVersion)
	if err != nil {
		return "", err
	}

	data, err := json.Marshal(explain)
	if err != nil {
		return "", fmt.Errorf("marshaling resource explain to json: %w", err)
	}
	return string(data), nil
}

// getBuiltinExplain provides offline fallback definitions for standard Kubernetes workloads.
func getBuiltinExplain(resourceOrKind, groupVersion string) *ResourceExplain {
	target := strings.ToLower(resourceOrKind)
	switch target {
	case "pod", "pods", "po":
		return &ResourceExplain{
			Kind:         "Pod",
			Group:        "",
			Version:      "v1",
			GroupVersion: "v1",
			Description:  "Pod is a collection of containers that can run on a host. This resource is created by clients and scheduled onto hosts.",
			Fields: []ResourceField{
				{Name: "apiVersion", Type: "string", Description: "APIVersion defines the versioned schema of this representation of an object. Servers should convert recognized schemas to the latest internal value.", Required: false},
				{Name: "kind", Type: "string", Description: "Kind is a string value representing the REST resource this object represents. Servers may infer this from the endpoint the client submits requests to.", Required: false},
				{Name: "metadata", Type: "ObjectMeta", Description: "Standard object's metadata. More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#metadata", Required: false},
				{Name: "spec", Type: "PodSpec", Description: "Specification of the desired behavior of the pod. More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#spec-and-status", Required: false},
				{Name: "status", Type: "PodStatus", Description: "Most recently observed status of the pod. This data may not be up to date. Populated by the system. Read-only.", Required: false},
			},
		}

	case "deployment", "deployments", "deploy":
		return &ResourceExplain{
			Kind:         "Deployment",
			Group:        "apps",
			Version:      "v1",
			GroupVersion: "apps/v1",
			Description:  "Deployment enables declarative updates for Pods and ReplicaSets.",
			Fields: []ResourceField{
				{Name: "apiVersion", Type: "string", Description: "APIVersion defines the versioned schema of this representation of an object.", Required: false},
				{Name: "kind", Type: "string", Description: "Kind is a string value representing the REST resource this object represents.", Required: false},
				{Name: "metadata", Type: "ObjectMeta", Description: "Standard object's metadata.", Required: false},
				{Name: "spec", Type: "DeploymentSpec", Description: "Specification of the desired behavior of the Deployment.", Required: true},
				{Name: "status", Type: "DeploymentStatus", Description: "Most recently observed status of the Deployment.", Required: false},
			},
		}

	case "service", "services", "svc":
		return &ResourceExplain{
			Kind:         "Service",
			Group:        "",
			Version:      "v1",
			GroupVersion: "v1",
			Description:  "Service is an abstract way to expose an application running on a set of Pods as a network service.",
			Fields: []ResourceField{
				{Name: "apiVersion", Type: "string", Description: "APIVersion defines the versioned schema of this representation of an object.", Required: false},
				{Name: "kind", Type: "string", Description: "Kind is a string value representing the REST resource this object represents.", Required: false},
				{Name: "metadata", Type: "ObjectMeta", Description: "Standard object's metadata.", Required: false},
				{Name: "spec", Type: "ServiceSpec", Description: "Spec defines the behavior of a service. https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#spec-and-status", Required: false},
				{Name: "status", Type: "ServiceStatus", Description: "Most recently observed status of the service.", Required: false},
			},
		}

	case "namespace", "namespaces", "ns":
		return &ResourceExplain{
			Kind:         "Namespace",
			Group:        "",
			Version:      "v1",
			GroupVersion: "v1",
			Description:  "Namespace provides a scope for Names. Names of resources need to be unique within a namespace, but not across namespaces.",
			Fields: []ResourceField{
				{Name: "apiVersion", Type: "string", Description: "APIVersion defines the versioned schema of this representation of an object.", Required: false},
				{Name: "kind", Type: "string", Description: "Kind is a string value representing the REST resource this object represents.", Required: false},
				{Name: "metadata", Type: "ObjectMeta", Description: "Standard object's metadata.", Required: false},
				{Name: "spec", Type: "NamespaceSpec", Description: "Spec defines the behavior of the Namespace.", Required: false},
				{Name: "status", Type: "NamespaceStatus", Description: "Status describes the current status of a Namespace.", Required: false},
			},
		}

	case "configmap", "configmaps", "cm":
		return &ResourceExplain{
			Kind:         "ConfigMap",
			Group:        "",
			Version:      "v1",
			GroupVersion: "v1",
			Description:  "ConfigMap holds configuration data for pods to consume.",
			Fields: []ResourceField{
				{Name: "apiVersion", Type: "string", Description: "APIVersion defines the versioned schema of this representation of an object.", Required: false},
				{Name: "kind", Type: "string", Description: "Kind is a string value representing the REST resource this object represents.", Required: false},
				{Name: "metadata", Type: "ObjectMeta", Description: "Standard object's metadata.", Required: false},
				{Name: "data", Type: "map[string]string", Description: "Data contains the configuration data. Each key must consist of alphanumeric characters, '-', '_' or '.'.", Required: false},
				{Name: "binaryData", Type: "map[string]string", Description: "BinaryData contains the binary data. Each key must consist of alphanumeric characters, '-', '_' or '.'.", Required: false},
				{Name: "immutable", Type: "boolean", Description: "Immutable, if set to true, ensures that data stored in the ConfigMap cannot be updated.", Required: false},
			},
		}

	case "secret", "secrets":
		return &ResourceExplain{
			Kind:         "Secret",
			Group:        "",
			Version:      "v1",
			GroupVersion: "v1",
			Description:  "Secret holds secret data of a certain type. The total bytes of the values in the Data field must be less than MaxSecretSize bytes.",
			Fields: []ResourceField{
				{Name: "apiVersion", Type: "string", Description: "APIVersion defines the versioned schema of this representation of an object.", Required: false},
				{Name: "kind", Type: "string", Description: "Kind is a string value representing the REST resource this object represents.", Required: false},
				{Name: "metadata", Type: "ObjectMeta", Description: "Standard object's metadata.", Required: false},
				{Name: "data", Type: "map[string]string", Description: "Data contains the secret data. Each key must consist of alphanumeric characters, '-', '_' or '.'. Values are base64 encoded.", Required: false},
				{Name: "stringData", Type: "map[string]string", Description: "stringData allows specifying non-binary secret data in string form.", Required: false},
				{Name: "type", Type: "string", Description: "Used to facilitate programmatic handling of secret data.", Required: false},
				{Name: "immutable", Type: "boolean", Description: "Immutable, if set to true, ensures that data stored in the Secret cannot be updated.", Required: false},
			},
		}

	case "statefulset", "statefulsets", "sts":
		return &ResourceExplain{
			Kind:         "StatefulSet",
			Group:        "apps",
			Version:      "v1",
			GroupVersion: "apps/v1",
			Description:  "StatefulSet represents a set of pods with consistent identities and persistent storage.",
			Fields: []ResourceField{
				{Name: "apiVersion", Type: "string", Description: "APIVersion defines the versioned schema of this representation of an object.", Required: false},
				{Name: "kind", Type: "string", Description: "Kind is a string value representing the REST resource this object represents.", Required: false},
				{Name: "metadata", Type: "ObjectMeta", Description: "Standard object's metadata.", Required: false},
				{Name: "spec", Type: "StatefulSetSpec", Description: "Spec defines the desired identities of pods in this set.", Required: true},
				{Name: "status", Type: "StatefulSetStatus", Description: "Status is the current status of Pods in this StatefulSet.", Required: false},
			},
		}

	case "daemonset", "daemonsets", "ds":
		return &ResourceExplain{
			Kind:         "DaemonSet",
			Group:        "apps",
			Version:      "v1",
			GroupVersion: "apps/v1",
			Description:  "DaemonSet represents the configuration of a daemon set that ensures all (or some) Nodes run a copy of a Pod.",
			Fields: []ResourceField{
				{Name: "apiVersion", Type: "string", Description: "APIVersion defines the versioned schema of this representation of an object.", Required: false},
				{Name: "kind", Type: "string", Description: "Kind is a string value representing the REST resource this object represents.", Required: false},
				{Name: "metadata", Type: "ObjectMeta", Description: "Standard object's metadata.", Required: false},
				{Name: "spec", Type: "DaemonSetSpec", Description: "The desired behavior of this daemon set.", Required: true},
				{Name: "status", Type: "DaemonSetStatus", Description: "The current status of this daemon set.", Required: false},
			},
		}

	case "job", "jobs":
		return &ResourceExplain{
			Kind:         "Job",
			Group:        "batch",
			Version:      "v1",
			GroupVersion: "batch/v1",
			Description:  "Job represents the configuration of a single run of a job.",
			Fields: []ResourceField{
				{Name: "apiVersion", Type: "string", Description: "APIVersion defines the versioned schema of this representation of an object.", Required: false},
				{Name: "kind", Type: "string", Description: "Kind is a string value representing the REST resource this object represents.", Required: false},
				{Name: "metadata", Type: "ObjectMeta", Description: "Standard object's metadata.", Required: false},
				{Name: "spec", Type: "JobSpec", Description: "Specification of the desired behavior of a job.", Required: true},
				{Name: "status", Type: "JobStatus", Description: "Current status of a job.", Required: false},
			},
		}

	case "cronjob", "cronjobs", "cj":
		return &ResourceExplain{
			Kind:         "CronJob",
			Group:        "batch",
			Version:      "v1",
			GroupVersion: "batch/v1",
			Description:  "CronJob represents the configuration of a single cron job.",
			Fields: []ResourceField{
				{Name: "apiVersion", Type: "string", Description: "APIVersion defines the versioned schema of this representation of an object.", Required: false},
				{Name: "kind", Type: "string", Description: "Kind is a string value representing the REST resource this object represents.", Required: false},
				{Name: "metadata", Type: "ObjectMeta", Description: "Standard object's metadata.", Required: false},
				{Name: "spec", Type: "CronJobSpec", Description: "Specification of the desired behavior of a cron job, including the schedule.", Required: true},
				{Name: "status", Type: "CronJobStatus", Description: "Current status of a cron job.", Required: false},
			},
		}

	case "ingress", "ingresses", "ing":
		return &ResourceExplain{
			Kind:         "Ingress",
			Group:        "networking.k8s.io",
			Version:      "v1",
			GroupVersion: "networking.k8s.io/v1",
			Description:  "Ingress is a collection of rules that allow inbound connections to reach the endpoints defined by a backend.",
			Fields: []ResourceField{
				{Name: "apiVersion", Type: "string", Description: "APIVersion defines the versioned schema of this representation of an object.", Required: false},
				{Name: "kind", Type: "string", Description: "Kind is a string value representing the REST resource this object represents.", Required: false},
				{Name: "metadata", Type: "ObjectMeta", Description: "Standard object's metadata.", Required: false},
				{Name: "spec", Type: "IngressSpec", Description: "Spec is the desired state of the Ingress.", Required: true},
				{Name: "status", Type: "IngressStatus", Description: "Status is the current state of the Ingress.", Required: false},
			},
		}

	case "node", "nodes", "no":
		return &ResourceExplain{
			Kind:         "Node",
			Group:        "",
			Version:      "v1",
			GroupVersion: "v1",
			Description:  "Node is a worker machine in Kubernetes. Each node contains the services necessary to run Pods.",
			Fields: []ResourceField{
				{Name: "apiVersion", Type: "string", Description: "APIVersion defines the versioned schema of this representation of an object.", Required: false},
				{Name: "kind", Type: "string", Description: "Kind is a string value representing the REST resource this object represents.", Required: false},
				{Name: "metadata", Type: "ObjectMeta", Description: "Standard object's metadata.", Required: false},
				{Name: "spec", Type: "NodeSpec", Description: "Spec defines the behavior of a node.", Required: false},
				{Name: "status", Type: "NodeStatus", Description: "Most recently observed status of the node.", Required: false},
			},
		}
	}

	return nil
}

// getDefaultAPIResources provides standard Kubernetes built-in API resources as offline fallback.
func getDefaultAPIResources() []APIResource {
	return []APIResource{
		{Name: "bindings", SingularName: "binding", Namespaced: true, Kind: "Binding", GroupVersion: "v1", Version: "v1", Verbs: []string{"create"}},
		{Name: "componentstatuses", SingularName: "componentstatus", Namespaced: false, Kind: "ComponentStatus", GroupVersion: "v1", Version: "v1", ShortNames: []string{"cs"}, Verbs: []string{"get", "list"}},
		{Name: "configmaps", SingularName: "configmap", Namespaced: true, Kind: "ConfigMap", GroupVersion: "v1", Version: "v1", ShortNames: []string{"cm"}, Verbs: []string{"create", "delete", "deletecollection", "get", "list", "patch", "update", "watch"}},
		{Name: "endpoints", SingularName: "endpoints", Namespaced: true, Kind: "Endpoints", GroupVersion: "v1", Version: "v1", ShortNames: []string{"ep"}, Verbs: []string{"create", "delete", "deletecollection", "get", "list", "patch", "update", "watch"}},
		{Name: "events", SingularName: "event", Namespaced: true, Kind: "Event", GroupVersion: "v1", Version: "v1", ShortNames: []string{"ev"}, Verbs: []string{"create", "delete", "deletecollection", "get", "list", "patch", "update", "watch"}},
		{Name: "limitranges", SingularName: "limitrange", Namespaced: true, Kind: "LimitRange", GroupVersion: "v1", Version: "v1", ShortNames: []string{"limits"}, Verbs: []string{"create", "delete", "deletecollection", "get", "list", "patch", "update", "watch"}},
		{Name: "namespaces", SingularName: "namespace", Namespaced: false, Kind: "Namespace", GroupVersion: "v1", Version: "v1", ShortNames: []string{"ns"}, Verbs: []string{"create", "delete", "get", "list", "patch", "update", "watch"}},
		{Name: "nodes", SingularName: "node", Namespaced: false, Kind: "Node", GroupVersion: "v1", Version: "v1", ShortNames: []string{"no"}, Verbs: []string{"create", "delete", "deletecollection", "get", "list", "patch", "update", "watch"}},
		{Name: "persistentvolumeclaims", SingularName: "persistentvolumeclaim", Namespaced: true, Kind: "PersistentVolumeClaim", GroupVersion: "v1", Version: "v1", ShortNames: []string{"pvc"}, Verbs: []string{"create", "delete", "deletecollection", "get", "list", "patch", "update", "watch"}},
		{Name: "persistentvolumes", SingularName: "persistentvolume", Namespaced: false, Kind: "PersistentVolume", GroupVersion: "v1", Version: "v1", ShortNames: []string{"pv"}, Verbs: []string{"create", "delete", "deletecollection", "get", "list", "patch", "update", "watch"}},
		{Name: "pods", SingularName: "pod", Namespaced: true, Kind: "Pod", GroupVersion: "v1", Version: "v1", ShortNames: []string{"po"}, Categories: []string{"all"}, Verbs: []string{"create", "delete", "deletecollection", "get", "list", "patch", "update", "watch"}},
		{Name: "podtemplates", SingularName: "podtemplate", Namespaced: true, Kind: "PodTemplate", GroupVersion: "v1", Version: "v1", Verbs: []string{"create", "delete", "deletecollection", "get", "list", "patch", "update", "watch"}},
		{Name: "replicationcontrollers", SingularName: "replicationcontroller", Namespaced: true, Kind: "ReplicationController", GroupVersion: "v1", Version: "v1", ShortNames: []string{"rc"}, Categories: []string{"all"}, Verbs: []string{"create", "delete", "deletecollection", "get", "list", "patch", "update", "watch"}},
		{Name: "resourcequotas", SingularName: "resourcequota", Namespaced: true, Kind: "ResourceQuota", GroupVersion: "v1", Version: "v1", ShortNames: []string{"quota"}, Verbs: []string{"create", "delete", "deletecollection", "get", "list", "patch", "update", "watch"}},
		{Name: "secrets", SingularName: "secret", Namespaced: true, Kind: "Secret", GroupVersion: "v1", Version: "v1", Verbs: []string{"create", "delete", "deletecollection", "get", "list", "patch", "update", "watch"}},
		{Name: "serviceaccounts", SingularName: "serviceaccount", Namespaced: true, Kind: "ServiceAccount", GroupVersion: "v1", Version: "v1", ShortNames: []string{"sa"}, Verbs: []string{"create", "delete", "deletecollection", "get", "list", "patch", "update", "watch"}},
		{Name: "services", SingularName: "service", Namespaced: true, Kind: "Service", GroupVersion: "v1", Version: "v1", ShortNames: []string{"svc"}, Categories: []string{"all"}, Verbs: []string{"create", "delete", "deletecollection", "get", "list", "patch", "update", "watch"}},
		{Name: "daemonsets", SingularName: "daemonset", Namespaced: true, Kind: "DaemonSet", Group: "apps", GroupVersion: "apps/v1", Version: "v1", ShortNames: []string{"ds"}, Categories: []string{"all"}, Verbs: []string{"create", "delete", "deletecollection", "get", "list", "patch", "update", "watch"}},
		{Name: "deployments", SingularName: "deployment", Namespaced: true, Kind: "Deployment", Group: "apps", GroupVersion: "apps/v1", Version: "v1", ShortNames: []string{"deploy"}, Categories: []string{"all"}, Verbs: []string{"create", "delete", "deletecollection", "get", "list", "patch", "update", "watch"}},
		{Name: "replicasets", SingularName: "replicaset", Namespaced: true, Kind: "ReplicaSet", Group: "apps", GroupVersion: "apps/v1", Version: "v1", ShortNames: []string{"rs"}, Categories: []string{"all"}, Verbs: []string{"create", "delete", "deletecollection", "get", "list", "patch", "update", "watch"}},
		{Name: "statefulsets", SingularName: "statefulset", Namespaced: true, Kind: "StatefulSet", Group: "apps", GroupVersion: "apps/v1", Version: "v1", ShortNames: []string{"sts"}, Categories: []string{"all"}, Verbs: []string{"create", "delete", "deletecollection", "get", "list", "patch", "update", "watch"}},
		{Name: "cronjobs", SingularName: "cronjob", Namespaced: true, Kind: "CronJob", Group: "batch", GroupVersion: "batch/v1", Version: "v1", ShortNames: []string{"cj"}, Categories: []string{"all"}, Verbs: []string{"create", "delete", "deletecollection", "get", "list", "patch", "update", "watch"}},
		{Name: "jobs", SingularName: "job", Namespaced: true, Kind: "Job", Group: "batch", GroupVersion: "batch/v1", Version: "v1", Categories: []string{"all"}, Verbs: []string{"create", "delete", "deletecollection", "get", "list", "patch", "update", "watch"}},
		{Name: "ingresses", SingularName: "ingress", Namespaced: true, Kind: "Ingress", Group: "networking.k8s.io", GroupVersion: "networking.k8s.io/v1", Version: "v1", ShortNames: []string{"ing"}, Categories: []string{"all"}, Verbs: []string{"create", "delete", "deletecollection", "get", "list", "patch", "update", "watch"}},
		{Name: "networkpolicies", SingularName: "networkpolicy", Namespaced: true, Kind: "NetworkPolicy", Group: "networking.k8s.io", GroupVersion: "networking.k8s.io/v1", Version: "v1", ShortNames: []string{"netpol"}, Verbs: []string{"create", "delete", "deletecollection", "get", "list", "patch", "update", "watch"}},
	}
}
