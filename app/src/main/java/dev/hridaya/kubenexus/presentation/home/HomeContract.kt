package dev.hridaya.kubenexus.presentation.home

import dev.hridaya.kubenexus.domain.model.Cluster

data class ErrorDialogData(
    val title: String,
    val errorMessage: String,
    val rawErrorTrace: String
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val isConnecting: Boolean = false,
    val clusters: List<Cluster> = emptyList(),
    val activeCluster: Cluster? = null,
    val showAddClusterSheet: Boolean = false,
    val kubeconfigInput: String = "",
    val customClusterName: String = "",
    val kubeconfigError: String? = null,
    val errorDialogData: ErrorDialogData? = null
)

sealed interface HomeUiAction {
    data object FabClicked : HomeUiAction
    data object DismissAddClusterSheet : HomeUiAction
    data class KubeconfigInputChanged(val text: String) : HomeUiAction
    data class ClusterNameChanged(val name: String) : HomeUiAction
    data class FileImported(val content: String, val fileName: String?) : HomeUiAction
    data object ConnectAndSaveSubmitted : HomeUiAction
    data class SelectClusterClicked(val clusterId: String) : HomeUiAction
    data class TestClusterConnectionClicked(val clusterId: String) : HomeUiAction
    data class DeleteClusterClicked(val clusterId: String) : HomeUiAction
    data object DismissErrorDialog : HomeUiAction
    data class CopyErrorClicked(val text: String) : HomeUiAction
}

sealed interface HomeUiEffect {
    data class ShowToast(val message: String) : HomeUiEffect
    data class ShowSnackbar(val message: String) : HomeUiEffect
}
