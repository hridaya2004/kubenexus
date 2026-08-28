#!/usr/bin/env bash
# Records live API-server payloads consumed by pkg/client tests.
#
# Only Kubernetes built-in API groups are captured; CRDs installed on the
# target cluster (kyverno, cert-manager, ...) are intentionally excluded so
# fixtures stay representative of a stock cluster. Requires kubectl pointed
# at the cluster to capture, plus jq.
set -euo pipefail

for tool in kubectl jq gzip; do
    command -v "$tool" >/dev/null 2>&1 || {
        echo "error: $tool is required but not installed" >&2
        exit 1
    }
done

kubectl cluster-info >/dev/null 2>&1 || {
    echo "error: kubectl cannot reach a cluster; check your current context" >&2
    exit 1
}

cd "$(dirname "$0")/.."
out="pkg/client/testdata"
mkdir -p "$out/gv"
rm -f "$out"/gv/*.json

# Groups shipped with Kubernetes itself (core v1 is handled separately).
BUILTIN_GROUPS=(
    apiregistration.k8s.io
    apps
    events.k8s.io
    authentication.k8s.io
    authorization.k8s.io
    autoscaling
    batch
    certificates.k8s.io
    networking.k8s.io
    policy
    rbac.authorization.k8s.io
    storage.k8s.io
    admissionregistration.k8s.io
    apiextensions.k8s.io
    scheduling.k8s.io
    coordination.k8s.io
    node.k8s.io
    discovery.k8s.io
    resource.k8s.io
    flowcontrol.apiserver.k8s.io
)

# /api advertises the API server's reachable address, which is specific to the
# machine that recorded the fixture and has no bearing on the tests (they serve
# these files from a local httptest server). Replace it so re-recording does not
# commit a private network address.
kubectl get --raw /api \
    | jq -c '(.serverAddressByClientCIDRs[]?).serverAddress = "10.0.0.1:6443"' \
    > "$out/discovery-api.json"

# Record /apis with non-built-in groups stripped, so the fixture mirrors what
# the other fixtures serve and discovery sees no missing group versions.
jq --argjson allowed "$(printf '%s\n' "${BUILTIN_GROUPS[@]}" | jq -R . | jq -s .)" \
    '.groups = [.groups[] | select(.name == $allowed[])]' \
    < <(kubectl get --raw /apis) > "$out/discovery-apis.json"

kubectl get --raw /api/v1 > "$out/gv/v1.json"
for group in "${BUILTIN_GROUPS[@]}"; do
    while read -r gv; do
        [ -z "$gv" ] && continue
        echo "recording $gv"
        kubectl get --raw "/apis/$gv" > "$out/gv/$(echo "$gv" | tr '/' '_').json"
    done < <(jq -r --arg g "$group" '.groups[] | select(.name == $g) | .versions[].groupVersion' "$out/discovery-apis.json")
done

# OpenAPI: keep built-in definitions (io.k8s.*) and prune paths that only
# reference dropped (CRD) definitions.
kubectl get --raw /openapi/v2 | jq '
    .definitions = (.definitions | with_entries(select(.key | startswith("io.k8s."))))
    | .paths = (.paths | with_entries(
        select([.. | objects | .["$ref"]? // empty] |
               all(startswith("#/definitions/io.k8s.")))
      ))
' | gzip -9 > "$out/openapi-v2.json.gz"

echo "recorded $(ls "$out/gv" | wc -l) group versions into $out/gv"
