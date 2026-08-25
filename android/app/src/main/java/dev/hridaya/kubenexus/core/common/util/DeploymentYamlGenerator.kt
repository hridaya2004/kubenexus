package dev.hridaya.kubenexus.core.common.util

import dev.hridaya.kubenexus.domain.model.DeploymentDraft

/**
 * Renders the reviewed manifest for the guided Deployment creation flow as
 * deterministic YAML.
 *
 * The generated text is exactly what gets applied: the user reviews this
 * string, and the same string is sent through the bridge, so what is shown is
 * never out of sync with what is created. Labels and selector intentionally
 * match (`app: <name>`) so a Service added later can target the workload.
 */
object DeploymentYamlGenerator {

    fun generate(draft: DeploymentDraft): String = buildString {
        appendLine("apiVersion: apps/v1")
        appendLine("kind: Deployment")
        appendLine("metadata:")
        appendLine("  name: ${draft.name}")
        appendLine("  namespace: ${draft.namespace}")
        appendLine("  labels:")
        appendLine("    app: ${draft.name}")
        appendLine("spec:")
        appendLine("  replicas: ${draft.replicas}")
        appendLine("  selector:")
        appendLine("    matchLabels:")
        appendLine("      app: ${draft.name}")
        appendLine("  template:")
        appendLine("    metadata:")
        appendLine("      labels:")
        appendLine("        app: ${draft.name}")
        appendLine("    spec:")
        appendLine("      containers:")
        appendLine("        - name: ${draft.name}")
        appendLine("          image: ${draft.image}")
        if (draft.containerPort > 0) {
            appendLine("          ports:")
            appendLine("            - containerPort: ${draft.containerPort}")
        }
    }
}
