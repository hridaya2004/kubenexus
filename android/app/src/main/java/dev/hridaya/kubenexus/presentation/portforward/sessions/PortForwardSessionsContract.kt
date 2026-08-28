package dev.hridaya.kubenexus.presentation.portforward.sessions

import dev.hridaya.kubenexus.domain.model.ActivePortForwardSession
import dev.hridaya.kubenexus.domain.model.PortForwardSessionStatus
import dev.hridaya.kubenexus.domain.model.PortForwardTargetKind

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
