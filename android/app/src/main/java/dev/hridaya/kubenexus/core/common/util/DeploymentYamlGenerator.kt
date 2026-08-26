package dev.hridaya.kubenexus.core.common.util

import dev.hridaya.kubenexus.domain.model.DeploymentDraft

/**
 * Renders the reviewed manifest for the guided Deployment creation flow as
 * deterministic YAML.
 *
 * The generated text is exactly what gets applied: the user reviews this
 * string, and the same string is sent through the bridge, so what is shown is
 * never out of sync with what is created. The manifest is assembled as an
 * ordered structure and serialized by yamlkt's dynamic block-style encoder,
 * which preserves key order instead of relying on hand-appended lines. Labels
 * and selector intentionally match (`app: <name>`) so a Service added later
 * can target the workload.
 */
object DeploymentYamlGenerator {

    fun generate(draft: DeploymentDraft): String {
        val containerSpec = linkedMapOf<String, Any>(
            "name" to draft.name,
            "image" to draft.image,
        )
        if (draft.containerPort > 0) {
            containerSpec["ports"] = listOf(linkedMapOf("containerPort" to draft.containerPort))
        }

        val deploymentManifest = linkedMapOf<String, Any>(
            "apiVersion" to "apps/v1",
            "kind" to "Deployment",
            "metadata" to linkedMapOf(
                "name" to draft.name,
                "namespace" to draft.namespace,
                "labels" to linkedMapOf("app" to draft.name),
            ),
            "spec" to linkedMapOf(
                "replicas" to draft.replicas,
                "selector" to linkedMapOf(
                    "matchLabels" to linkedMapOf("app" to draft.name),
                ),
                "template" to linkedMapOf(
                    "metadata" to linkedMapOf(
                        "labels" to linkedMapOf("app" to draft.name),
                    ),
                    "spec" to linkedMapOf(
                        "containers" to listOf(containerSpec),
                    ),
                ),
            ),
        )

        return renderK8sManifest(deploymentManifest)
    }
}
