package dev.hridaya.kubenexus.presentation.portforward.sessions

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
 * UI-facing snapshot of one tunnel tracked by the PortForwardSessionManager.
 * Kept in the presentation layer so the sessions surface compiles against a
 * stable shape and maps from the manager model in exactly one place (the
 * ViewModel collection seam).
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

/**
 * State of the global port-forward sessions sheet. [sessions] mirrors the
 * manager flow verbatim (minus locally dismissed rows); presentation-side
 * filtering lives in derived properties so recomposition stays cheap.
 */
data class PortForwardSessionsUiState(
    val sessions: List<ActivePortForwardSession> = emptyList(),
    val includeStopped: Boolean = false,
) {
    /** Rows rendered in the sheet honoring the include-stopped toggle. */
    val visibleSessions: List<ActivePortForwardSession>
        get() = if (includeStopped) sessions else sessions.filter { it.isActive }

    /** Non-stopped rows; drives the top-bar badge count. */
    val activeCount: Int get() = sessions.count { it.isActive }

    /** Whether any dismissed-candidate row exists; hides the toggle otherwise. */
    val hasStoppedRows: Boolean get() = sessions.any { it.isStopped }

    /** "Stop all" is offered only when more than one live row exists. */
    val canStopAll: Boolean get() = activeCount > 1
}

sealed interface PortForwardSessionsUiAction {
    data class Stop(val handleId: String) : PortForwardSessionsUiAction

    data class DismissStopped(val handleId: String) : PortForwardSessionsUiAction

    data object StopAllActive : PortForwardSessionsUiAction

    data object ToggleIncludeStopped : PortForwardSessionsUiAction
}
