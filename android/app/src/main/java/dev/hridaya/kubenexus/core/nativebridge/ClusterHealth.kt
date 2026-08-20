package dev.hridaya.kubenexus.core.nativebridge

/**
 * Represents cluster health inspection results retrieved from Kubernetes health endpoints.
 */
data class ClusterHealth(
    val livez: Boolean = false,
    val readyz: Boolean = false,
    val healthz: Boolean = false,
    val serverVersion: String = "",
    val statusMessage: String = "",
)
