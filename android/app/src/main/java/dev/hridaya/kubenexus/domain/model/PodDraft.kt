package dev.hridaya.kubenexus.domain.model

import dev.hridaya.kubenexus.core.common.util.K8sNames

/**
 * User input for the guided Pod creation flow (issue #5 follow-up).
 *
 * Validation lives here rather than in the UI so the rules are plain-JVM
 * testable and identical between the live form check and the pre-apply gate.
 * [validate] returns one message per invalid field, keyed by field name;
 * an empty map means the draft is ready to be rendered into a manifest.
 */
data class PodDraft(
    val name: String = "",
    val namespace: String = "",
    val image: String = "",
    val containerPort: Int = DEFAULT_CONTAINER_PORT,
) {

    fun validate(): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        when {
            name.isBlank() -> errors["name"] = "Name is required"
            name.length > MAX_NAME_LENGTH ->
                errors["name"] = "Must be $MAX_NAME_LENGTH characters or fewer"
            !K8sNames.isValidDns1035Label(name) ->
                errors["name"] = "Use lowercase letters, numbers and '-' (start with a letter)"
        }

        when {
            namespace.isBlank() -> errors["namespace"] = "Namespace is required"
            namespace.length > MAX_NAME_LENGTH ->
                errors["namespace"] = "Must be $MAX_NAME_LENGTH characters or fewer"
            !K8sNames.isValidDnsLabel(namespace) ->
                errors["namespace"] = "Use lowercase letters, numbers and '-'"
        }

        when {
            image.isBlank() -> errors["image"] = "Image is required"
            image.any { it.isWhitespace() } -> errors["image"] = "Image cannot contain spaces"
        }

        if (containerPort != DEFAULT_CONTAINER_PORT && containerPort !in MIN_PORT..MAX_PORT) {
            errors["containerPort"] = "Port must be between $MIN_PORT and $MAX_PORT"
        }

        return errors
    }

    companion object {
        const val DEFAULT_CONTAINER_PORT = 0
        const val MIN_PORT = 1
        const val MAX_PORT = 65535
        const val MAX_NAME_LENGTH = 63
    }
}
