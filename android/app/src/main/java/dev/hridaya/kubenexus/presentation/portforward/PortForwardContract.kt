package dev.hridaya.kubenexus.presentation.portforward

/** Lifecycle of a single port-forward tunnel. */
enum class PortForwardStatus {
    /** Request accepted, waiting for the listener-ready callback. */
    STARTING,

    /** The local listener on 127.0.0.1 is accepting connections. */
    READY,

    /** The tunnel failed; [ActivePortForward.message] carries the reason. */
    ERROR,
}

/**
 * One port-forward tunnel owned by this screen. [handleId] is the opaque token
 * returned by the repository and used to stop the session.
 */
data class ActivePortForward(
    val handleId: String,
    val namespace: String,
    val podName: String,
    val localPort: Int,
    val remotePort: Int,
    val status: PortForwardStatus = PortForwardStatus.STARTING,
    val message: String? = null,
) {
    val localAddress: String get() = "127.0.0.1:$localPort"
    val targetLabel: String get() = "$podName:$remotePort"
}

/**
 * State of the port-forward UX for one pod. The decrypted kubeconfig and
 * cluster identity are deliberately absent: they live only inside the
 * ViewModel.
 */
data class PortForwardUiState(
    val activeForwards: List<ActivePortForward> = emptyList(),
    val isStarting: Boolean = false,
    val error: String? = null,
)

sealed interface PortForwardUiAction {
    data class StartForward(val localPort: Int, val remotePort: Int) : PortForwardUiAction
    data class StopForward(val handleId: String) : PortForwardUiAction
    data object DismissError : PortForwardUiAction
}
