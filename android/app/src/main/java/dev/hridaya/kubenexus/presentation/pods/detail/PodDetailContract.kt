package dev.hridaya.kubenexus.presentation.pods.detail

import dev.hridaya.kubenexus.domain.model.ClusterConnectionStatus
import dev.hridaya.kubenexus.domain.model.PodDetails

enum class PodDetailTab(val title: String) {
    DESCRIBE("Describe"),
    LOGS("Logs"),
    TERMINAL("Terminal"),
}

enum class TerminalLineType {
    INPUT,
    STDOUT,
    STDERR,
    SYSTEM,
    ERROR,
}

data class TerminalLine(
    val text: String,
    val type: TerminalLineType = TerminalLineType.STDOUT,
    val timestamp: Long = System.currentTimeMillis(),
)

data class PodDetailUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val lastRefreshedAt: Long? = null,
    val podName: String = "",
    val namespace: String = "",
    val clusterId: String? = null,
    val podDetails: PodDetails? = null,
    val selectedTab: PodDetailTab = PodDetailTab.DESCRIBE,
    val selectedContainer: String? = null,
    val logs: List<String> = emptyList(),
    val isStreamingLogs: Boolean = false,
    val isLoadingLogs: Boolean = false,
    val errorMessage: String? = null,
    val terminalLines: List<TerminalLine> = emptyList(),
    val isTerminalActive: Boolean = false,
    val isExecutingCommand: Boolean = false,
    val execInputText: String = "",
    val activeShellCommand: String = "/bin/sh",
    val showDeleteConfirmDialog: Boolean = false,
    val isDeletingPod: Boolean = false,
    val isOnline: Boolean = true,
    val clusterConnectionStatus: ClusterConnectionStatus = ClusterConnectionStatus.CONNECTED,
) {
    val isContainerAttachable: Boolean
        get() {
            if (!isOnline || clusterConnectionStatus != ClusterConnectionStatus.CONNECTED) return false
            val currentContainer =
                (podDetails?.containers.orEmpty() + podDetails?.initContainers.orEmpty())
                    .find { it.name == selectedContainer }
            return currentContainer == null ||
                    currentContainer.state.equals(
                        "Running",
                        ignoreCase = true,
                    ) ||
                    currentContainer.ready
        }
}

sealed interface PodDetailUiAction {
    data object RefreshDescribe : PodDetailUiAction
    data class SelectTab(val tab: PodDetailTab) : PodDetailUiAction
    data class SelectContainer(val containerName: String) : PodDetailUiAction
    data object FetchLogs : PodDetailUiAction
    data object StartStreamingLogs : PodDetailUiAction
    data object StopStreamingLogs : PodDetailUiAction
    data object ClearLogs : PodDetailUiAction

    data class UpdateExecInput(val input: String) : PodDetailUiAction
    data class ExecuteCommand(val command: String) : PodDetailUiAction
    data class StartInteractiveTerminal(val shell: String? = null) : PodDetailUiAction
    data object StopInteractiveTerminal : PodDetailUiAction
    data class SendTerminalInput(val input: String) : PodDetailUiAction
    data object ClearTerminal : PodDetailUiAction

    data class ShowDeleteDialog(val show: Boolean) : PodDetailUiAction
    data object ConfirmDeletePod : PodDetailUiAction
}

sealed interface PodDetailUiEffect {
    data class ShowToast(val message: String) : PodDetailUiEffect
    data object NavigateBack : PodDetailUiEffect
}
