#!/usr/bin/env bash
# Records live API-server payloads consumed by pkg/client tests.
# Requires kubectl pointed at the cluster to capture, plus jq.
set -euo pipefail

cd "$(dirname "$0")/.."
out="pkg/client/testdata"
mkdir -p "$out/gv"
rm -f "$out"/gv/*.json

kubectl get --raw /api > "$out/discovery-api.json"
kubectl get --raw /apis > "$out/discovery-apis.json"

kubectl get --raw /api/v1 > "$out/gv/v1.json"
for gv in $(jq -r '.groups[].versions[].groupVersion' "$out/discovery-apis.json"); do
    echo "recording $gv"
    kubectl get --raw "/apis/$gv" > "$out/gv/$(echo "$gv" | tr '/' '_').json"
done

kubectl get --raw /openapi/v2 | gzip -9 > "$out/openapi-v2.json.gz"

echo "recorded $(ls "$out/gv" | wc -l) group versions into $out/gv"
