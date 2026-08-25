package dev.hridaya.kubenexus.core.common.util

import dev.hridaya.kubenexus.domain.model.PodDraft

/**
 * Renders the reviewed manifest for the guided Pod creation flow as
 * deterministic YAML.
 *
 * The generated text is exactly what gets applied: the user reviews this
 * string, and the same string is sent through the bridge, so what is shown is
 * never out of sync with what is created. The [PodDraft.containerPort] is
 * omittable because a bare container needs no port mapping at all; a zero
 * means "no ports block" rather than port zero, so the block is left out of
 * the reviewed text entirely instead of being emitted with a placeholder.
 */
object PodYamlGenerator {

    fun generate(draft: PodDraft): String = buildString {
        appendLine("apiVersion: v1")
        appendLine("kind: Pod")
        appendLine("metadata:")
        appendLine("  name: ${draft.name}")
        appendLine("  namespace: ${draft.namespace}")
        appendLine("  labels:")
        appendLine("    app: ${draft.name}")
        appendLine("spec:")
        appendLine("  containers:")
        appendLine("    - name: ${draft.name}")
        appendLine("      image: ${draft.image}")
        if (draft.containerPort > 0) {
            appendLine("      ports:")
            appendLine("        - containerPort: ${draft.containerPort}")
        }
    }
}
