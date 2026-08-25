package dev.hridaya.kubenexus.presentation.common

import dev.hridaya.kubenexus.core.common.util.K8sNames

/**
 * Single source of truth for new-namespace-name validation across the guided
 * resource-creation flows (deployment, pod, ...). Returns the UX error message
 * for an invalid name, or null when the name may be submitted.
 */
object NamespaceNameValidator {

    const val MAX_LENGTH = 63

    fun errorFor(value: String): String? {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> "Enter a namespace name"
            trimmed.length > MAX_LENGTH -> "Must be $MAX_LENGTH characters or fewer"
            !K8sNames.isValidDnsLabel(trimmed) ->
                "Use lowercase letters, numbers, and hyphens. It must start and end with a letter or number."

            else -> null
        }
    }
}
