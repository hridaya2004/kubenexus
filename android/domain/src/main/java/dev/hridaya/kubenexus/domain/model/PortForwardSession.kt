package dev.hridaya.kubenexus.domain.model

/** Cluster object a tunnel terminates on. */
enum class PortForwardTargetKind {
    Pod,
    Service,
}

/** Lifecycle of one globally-tracked port-forward tunnel. */
enum class PortForwardSessionStatus {
    /** Request accepted, waiting for the listener-ready callback. */
    STARTING,

    /** The local listener on 127.0.0.1 is accepting connections. */
    READY,

    /** The tunnel failed; [ActivePortForwardSession.message] carries the reason. */
    ERROR,

    /**
     * The tunnel was torn down (user stop or remote close). Rows linger in
     * this state until the user dismisses them.
     */
    STOPPED,
}

/**
 * Snapshot of one tunnel tracked by the PortForwardSessionManager.
 */
data class ActivePortForwardSession(
    val handleId: String,
    val kind: PortForwardTargetKind = PortForwardTargetKind.Pod,
    val namespace: String,
    val targetName: String,
    val podName: String? = null,
    val localPort: Int,
    val remotePort: Int,
    val status: PortForwardSessionStatus = PortForwardSessionStatus.STARTING,
    val message: String? = null,
) {
    val isStopped: Boolean get() = status == PortForwardSessionStatus.STOPPED
    val isActive: Boolean get() = !isStopped

    /** Row headline, e.g. "default/nginx-7d9f". */
    val title: String get() = "$namespace/$targetName"

    /** Row subtitle, e.g. "127.0.0.1:8080 -> nginx-7d9f:80". */
    val endpointLabel: String get() {
        val remoteHost = podName?.takeIf { it.isNotBlank() } ?: targetName
        return "127.0.0.1:$localPort -> $remoteHost:$remotePort"
    }
}
