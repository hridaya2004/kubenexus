package dev.hridaya.kubenexus.presentation.home

import dev.hridaya.kubenexus.domain.model.Cluster
import dev.hridaya.kubenexus.domain.model.Pod

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
    val pods: List<Pod> = emptyList(),
    val availableNamespaces: List<String> = listOf("All Namespaces", "default", "kube-system", "monitoring"),
    val selectedNamespace: String = "All Namespaces",
    val showNamespacePicker: Boolean = false,
    val selectedPod: Pod? = null,
    val showClusterDrawer: Boolean = false,
    val showFabActionSheet: Boolean = false,
    val showAddClusterSheet: Boolean = false,
    val editingCluster: Cluster? = null,
    val clusterToDelete: Cluster? = null,
    val kubeconfigInput: String = "",
    val customClusterName: String = "",
    val kubeconfigError: String? = null,
    val errorDialogData: ErrorDialogData? = null
)

sealed interface HomeUiAction {
    data object OpenClusterDrawer : HomeUiAction
    data object DismissClusterDrawer : HomeUiAction
    data object OpenFabActionSheet : HomeUiAction
    data object DismissFabActionSheet : HomeUiAction
    data object OpenAddClusterSheet : HomeUiAction
    data object DismissAddClusterSheet : HomeUiAction
    data class KubeconfigInputChanged(val text: String) : HomeUiAction
    data class ClusterNameChanged(val name: String) : HomeUiAction
    data class FileImported(val content: String, val fileName: String?) : HomeUiAction
    data object ConnectAndSaveSubmitted : HomeUiAction
    data class SelectClusterClicked(val clusterId: String) : HomeUiAction
    data class TestClusterConnectionClicked(val clusterId: String) : HomeUiAction
    data class RequestEditCluster(val cluster: Cluster) : HomeUiAction
    data object DismissEditCluster : HomeUiAction
    data class SaveClusterName(val clusterId: String, val newName: String) : HomeUiAction
    data class RequestDeleteCluster(val cluster: Cluster) : HomeUiAction
    data object DismissDeleteCluster : HomeUiAction
    data class ConfirmDeleteCluster(val clusterId: String) : HomeUiAction
    data class SelectPod(val pod: Pod) : HomeUiAction
    data object DismissPodDetails : HomeUiAction
    data object OpenNamespacePicker : HomeUiAction
    data object DismissNamespacePicker : HomeUiAction
    data class SelectNamespace(val namespace: String) : HomeUiAction
    data object DismissErrorDialog : HomeUiAction
    data class CopyErrorClicked(val text: String) : HomeUiAction
    data class TriggerNoopAction(val message: String) : HomeUiAction
}

sealed interface HomeUiEffect {
    data class ShowToast(val message: String) : HomeUiEffect
    data class ShowSnackbar(val message: String) : HomeUiEffect
    data object NavigateToHome : HomeUiEffect
}
