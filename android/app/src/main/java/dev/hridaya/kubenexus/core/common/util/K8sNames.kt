package dev.hridaya.kubenexus.core.common.util

/**
 * Canonical Kubernetes name-format checks shared by form validation and bridge
 * guards, so every layer rejects the same shapes for the same reason.
 */
object K8sNames {

    const val MAX_LABEL_LENGTH = 63

    /** DNS-1123 label: lowercase alphanumerics and hyphens, no leading/trailing hyphen. */
    private val DNS_1123_LABEL = Regex("^[a-z0-9]([a-z0-9-]*[a-z0-9])$")

    fun isValidDnsLabel(value: String): Boolean =
        value.length <= MAX_LABEL_LENGTH && DNS_1123_LABEL.matches(value)
}
