package dev.hridaya.kubenexus.presentation.pods.detail

import dev.hridaya.kubenexus.domain.model.PodDetails

enum class PodDetailTab(val title: String) {
    DESCRIBE("Describe"),
    LOGS("Logs & Terminal")
}

data class PodDetailUiState(
    val isLoading: Boolean = true,
    val podName: String = "",
    val namespace: String = "",
    val clusterId: String? = null,
    val podDetails: PodDetails? = null,
    val selectedTab: PodDetailTab = PodDetailTab.DESCRIBE,
    val selectedContainer: String? = null,
    val logs: List<String> = emptyList(),
    val isStreamingLogs: Boolean = false,
    val isLoadingLogs: Boolean = false,
    val errorMessage: String? = null
)

sealed interface PodDetailUiAction {
    data object RefreshDescribe : PodDetailUiAction
    data class SelectTab(val tab: PodDetailTab) : PodDetailUiAction
    data class SelectContainer(val containerName: String) : PodDetailUiAction
    data object FetchLogs : PodDetailUiAction
    data object StartStreamingLogs : PodDetailUiAction
    data object StopStreamingLogs : PodDetailUiAction
    data object ClearLogs : PodDetailUiAction
}

sealed interface PodDetailUiEffect {
    data class ShowToast(val message: String) : PodDetailUiEffect
}
