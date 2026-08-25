package dev.hridaya.kubenexus.core.common.util

import dev.hridaya.kubenexus.domain.model.ServiceDraft

/**
 * Renders the reviewed manifest for the guided Service creation flow as
 * deterministic YAML.
 *
 * The generated text is exactly what gets applied: the user reviews this
 * string, and the same string is sent through the bridge, so what is shown is
 * never out of sync with what is created. [ServiceDraft.selectorApp]
 * intentionally targets the `app` label the Deployment generator stamps onto
 * its pods, so a Service created this way routes traffic to that workload
 * even when the Service's own name differs from it.
 */
object ServiceYamlGenerator {

    fun generate(draft: ServiceDraft): String = buildString {
        appendLine("apiVersion: v1")
        appendLine("kind: Service")
        appendLine("metadata:")
        appendLine("  name: ${draft.name}")
        appendLine("  namespace: ${draft.namespace}")
        appendLine("  labels:")
        appendLine("    app: ${draft.name}")
        appendLine("spec:")
        appendLine("  selector:")
        appendLine("    app: ${draft.selectorApp}")
        appendLine("  type: ${draft.serviceType}")
        appendLine("  ports:")
        appendLine("    - port: ${draft.port}")
        appendLine("      targetPort: ${draft.targetPort}")
    }
}
