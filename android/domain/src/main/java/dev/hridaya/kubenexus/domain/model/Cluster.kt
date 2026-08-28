package dev.hridaya.kubenexus.domain.model

enum class ClusterStatus {
    CONNECTED,
    DISCONNECTED,
    ERROR,
}

data class Cluster(
    val id: String,
    val name: String,
    val serverUrl: String,
    val rawKubeconfig: String,
    val contextName: String,
    val userName: String = "",
    val namespace: String = "default",
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastConnectedAt: Long? = null,
    val status: ClusterStatus = ClusterStatus.DISCONNECTED,
)

data class ParsedKubeconfig(
    val clusterName: String,
    val serverUrl: String,
    val contextName: String,
    val userName: String,
    val namespace: String,
    val rawKubeconfig: String,
    val certificateAuthorityData: String? = null,
    val insecureSkipTlsVerify: Boolean = false,
)

