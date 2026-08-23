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

	ctx, cancel := context.WithTimeout(context.Background(), c.timeout)
	defer cancel()

	_ = ctx
	resLists, err := c.clientset.Discovery().ServerPreferredResources()
	if err != nil && len(resLists) == 0 {
		return err, nil
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