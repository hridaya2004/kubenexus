package client

import (
	"context"
	"encoding/json"
	"fmt"
	"log/slog"
)

// ListAPIResourcesJSON returns the cluster's preferred API resources as the
// discovery payload, marshaled straight from what client-go reports.
// Reshaping is left to Android. Partial discovery failures keep the usable
// subset with a nil error, mirroring kubectl.
func (c *Client) ListAPIResourcesJSON() (string, error) {
	resLists, err := c.clientset.Discovery().ServerPreferredResources()
	if err != nil && len(resLists) == 0 {
		return "", err
	}
	if err != nil {
		slog.Warn("partial API discovery", "groupVersions", len(resLists), "err", err)
	}

	data, err := json.Marshal(resLists)
	if err != nil {
		return "", fmt.Errorf("marshaling discovery lists: %w", err)
	}
	return string(data), nil
}

// OpenAPISchemaJSON returns the cluster's OpenAPI v2 schema document verbatim
// from /openapi/v2. The document is large; cache it per cluster.
func (c *Client) OpenAPISchemaJSON() (string, error) {
	ctx, cancel := context.WithTimeout(context.Background(), c.timeout)
	defer cancel()

	data, err := c.clientset.Discovery().RESTClient().Get().
		AbsPath("/openapi/v2").
		SetHeader("Accept", "application/json").
		DoRaw(ctx)
	if err != nil {
		return "", fmt.Errorf("fetching OpenAPI schema: %w", err)
	}
	return string(data), nil
}
