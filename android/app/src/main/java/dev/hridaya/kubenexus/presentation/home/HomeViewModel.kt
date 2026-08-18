package dev.hridaya.kubenexus.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.network.NetworkMonitor
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.core.di.AppContainer
import dev.hridaya.kubenexus.domain.model.Cluster
import dev.hridaya.kubenexus.domain.model.ClusterConnectionStatus
import dev.hridaya.kubenexus.domain.model.Pod
import dev.hridaya.kubenexus.domain.usecase.AddClusterUseCase
import dev.hridaya.kubenexus.domain.usecase.DeleteClusterUseCase
import dev.hridaya.kubenexus.domain.usecase.GetActiveClusterUseCase
import dev.hridaya.kubenexus.domain.usecase.GetClustersUseCase
import dev.hridaya.kubenexus.domain.usecase.GetLastRefreshedUseCase
import dev.hridaya.kubenexus.domain.usecase.GetNamespacesUseCase
import dev.hridaya.kubenexus.domain.usecase.GetPodsUseCase
import dev.hridaya.kubenexus.domain.usecase.RefreshWorkloadsUseCase
import dev.hridaya.kubenexus.domain.usecase.SetActiveClusterUseCase
import dev.hridaya.kubenexus.domain.usecase.TestClusterConnectionUseCase
import dev.hridaya.kubenexus.domain.usecase.UpdateClusterNameUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val getClustersUseCase: GetClustersUseCase,
    private val getActiveClusterUseCase: GetActiveClusterUseCase,
    private val getPodsUseCase: GetPodsUseCase,
    private val getNamespacesUseCase: GetNamespacesUseCase,
    private val getLastRefreshedUseCase: GetLastRefreshedUseCase,
    private val refreshWorkloadsUseCase: RefreshWorkloadsUseCase,
    private val addClusterUseCase: AddClusterUseCase,
    private val setActiveClusterUseCase: SetActiveClusterUseCase,
    private val deleteClusterUseCase: DeleteClusterUseCase,
    private val updateClusterNameUseCase: UpdateClusterNameUseCase,
    private val testClusterConnectionUseCase: TestClusterConnectionUseCase,
    private val networkMonitor: NetworkMonitor,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _selectedNamespace = MutableStateFlow("All Namespaces")
    private var lastSyncedClusterId: String? = null

    private val _effects = Channel<HomeUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        observeNetworkConnectivity()
        observeLocalDatabase()
    }

    private fun observeNetworkConnectivity() {
        viewModelScope.launch(dispatcherProvider.main) {
            networkMonitor.isOnline.collect { online ->
                val wasOffline = !_uiState.value.isOnline
                _uiState.update { it.copy(isOnline = online) }
                if (online) {
                    val activeCluster = _uiState.value.activeCluster
                    if (activeCluster != null && wasOffline) {
                        performRefresh(
                            clusterId = activeCluster.id,
                            namespace = _selectedNamespace.value,
                            showLoading = false,
                        )
                    }
                } else {
                    _uiState.update {
                        if (it.activeCluster != null) {
                            it.copy(clusterConnectionStatus = ClusterConnectionStatus.DISCONNECTED)
                        } else {
                            it.copy(clusterConnectionStatus = ClusterConnectionStatus.OFFLINE)
                        }
                    }
                }
            }
        }
    }

    private fun observeLocalDatabase() {
        viewModelScope.launch(dispatcherProvider.main) {
            combine(
                getClustersUseCase(),
                getActiveClusterUseCase(),
                _selectedNamespace,
            ) { clusters, activeCluster, ns ->
                Triple(clusters, activeCluster, ns)
            }.flatMapLatest { (clusters, activeCluster, ns) ->
                val clusterId = activeCluster?.id
                combine(
                    getPodsUseCase(clusterId, ns),
                    getPodsUseCase(clusterId, null),
                    getNamespacesUseCase(clusterId),
                    getLastRefreshedUseCase(clusterId),
                ) { pods, allPods, namespaces, lastRefreshed ->
                    LocalWorkloadData(
                        clusters,
                        activeCluster,
                        pods,
                        allPods.size,
                        namespaces,
                        ns,
                        lastRefreshed,
                    )
                }.catch { t ->
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            clusterConnectionStatus = ClusterConnectionStatus.DISCONNECTED,
                        )
                    }
                }
            }.collect { data ->
                val status = when {
                    data.activeCluster == null -> ClusterConnectionStatus.OFFLINE
                    _uiState.value.isRefreshing || _uiState.value.isConnecting -> ClusterConnectionStatus.CONNECTING
                    _uiState.value.clusterConnectionStatus == ClusterConnectionStatus.DISCONNECTED -> ClusterConnectionStatus.DISCONNECTED
                    else -> ClusterConnectionStatus.CONNECTED
                }

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        clusters = data.clusters,
                        activeCluster = data.activeCluster,
                        pods = data.pods,
                        totalPodsCount = data.totalPodsCount,
                        availableNamespaces = if (data.namespaces.isNotEmpty()) data.namespaces else state.availableNamespaces,
                        selectedNamespace = data.selectedNamespace,
                        lastRefreshedAt = data.lastRefreshed ?: state.lastRefreshedAt,
                        clusterConnectionStatus = status,
                    )
                }

                if (data.activeCluster != null && lastSyncedClusterId != data.activeCluster.id) {
                    lastSyncedClusterId = data.activeCluster.id
                    if (data.pods.isEmpty()) {
                        performRefresh(
                            data.activeCluster.id,
                            data.selectedNamespace,
                            showLoading = true,
                        )
                    }
                }
            }
        }
    }

    private data class LocalWorkloadData(
        val clusters: List<Cluster>,
        val activeCluster: Cluster?,
        val pods: List<Pod>,
        val totalPodsCount: Int,
        val namespaces: List<String>,
        val selectedNamespace: String,
        val lastRefreshed: Long?,
    )

    fun onAction(action: HomeUiAction) {
        when (action) {
            is HomeUiAction.RefreshWorkloads -> {
                val activeClusterId = _uiState.value.activeCluster?.id
                performRefresh(
                    activeClusterId,
                    _uiState.value.selectedNamespace,
                    showLoading = true,
                )
            }

            is HomeUiAction.OpenClusterDrawer -> {
                _uiState.update { it.copy(showClusterDrawer = true) }
            }

            is HomeUiAction.DismissClusterDrawer -> {
                _uiState.update { it.copy(showClusterDrawer = false) }
            }

            is HomeUiAction.OpenFabActionSheet -> {
                _uiState.update { it.copy(showFabActionSheet = true) }
            }

            is HomeUiAction.DismissFabActionSheet -> {
                _uiState.update { it.copy(showFabActionSheet = false) }
            }

            is HomeUiAction.OpenAddClusterSheet -> {
                _uiState.update {
                    it.copy(
                        showAddClusterSheet = true,
                        showFabActionSheet = false,
                        showClusterDrawer = false,
                        kubeconfigInput = "",
                        customClusterName = "",
                        kubeconfigError = null,
                    )
                }
            }

            is HomeUiAction.DismissAddClusterSheet -> {
                if (!_uiState.value.isConnecting) {
                    _uiState.update { it.copy(showAddClusterSheet = false, kubeconfigError = null) }
                }
            }

            is HomeUiAction.KubeconfigInputChanged -> {
                _uiState.update { it.copy(kubeconfigInput = action.text, kubeconfigError = null) }
            }

            is HomeUiAction.ClusterNameChanged -> {
                _uiState.update { it.copy(customClusterName = action.name) }
            }

            is HomeUiAction.FileImported -> {
                val inferredName = action.fileName?.substringBeforeLast(".") ?: ""
                _uiState.update {
                    it.copy(
                        kubeconfigInput = action.content,
                        customClusterName = it.customClusterName.ifBlank { inferredName },
                        kubeconfigError = null,
                    )
                }
            }

            is HomeUiAction.ConnectAndSaveSubmitted -> {
                connectAndSaveCluster()
            }

            is HomeUiAction.SelectClusterClicked -> {
                selectActiveCluster(action.clusterId)
            }

            is HomeUiAction.TestClusterConnectionClicked -> {
                testClusterConnection(action.clusterId)
            }

            is HomeUiAction.RequestEditCluster -> {
                _uiState.update { it.copy(editingCluster = action.cluster) }
            }

            is HomeUiAction.DismissEditCluster -> {
                _uiState.update { it.copy(editingCluster = null) }
            }

            is HomeUiAction.SaveClusterName -> {
                saveClusterName(action.clusterId, action.newName)
            }

            is HomeUiAction.RequestDeleteCluster -> {
                _uiState.update { it.copy(clusterToDelete = action.cluster) }
            }

            is HomeUiAction.DismissDeleteCluster -> {
                _uiState.update { it.copy(clusterToDelete = null) }
            }

            is HomeUiAction.ConfirmDeleteCluster -> {
                _uiState.update { it.copy(clusterToDelete = null) }
                deleteCluster(action.clusterId)
            }

            is HomeUiAction.SelectPod -> {
                _uiState.update { it.copy(selectedPod = action.pod) }
            }

            is HomeUiAction.DismissPodDetails -> {
                _uiState.update { it.copy(selectedPod = null) }
            }

            is HomeUiAction.OpenNamespacePicker -> {
                _uiState.update { it.copy(showNamespacePicker = true) }
            }

            is HomeUiAction.DismissNamespacePicker -> {
                _uiState.update { it.copy(showNamespacePicker = false) }
            }

            is HomeUiAction.SelectNamespace -> {
                _selectedNamespace.value = action.namespace
                _uiState.update {
                    it.copy(
                        selectedNamespace = action.namespace,
                        showNamespacePicker = false,
                    )
                }
            }

            is HomeUiAction.DismissErrorDialog -> {
                _uiState.update { it.copy(errorDialogData = null) }
            }

            is HomeUiAction.CopyErrorClicked -> {
                viewModelScope.launch {
                    _effects.send(HomeUiEffect.ShowToast("Error details copied to clipboard"))
                }
            }

            is HomeUiAction.TriggerNoopAction -> {
                viewModelScope.launch {
                    _effects.send(HomeUiEffect.ShowToast(action.message))
                }
            }
        }
    }

    private fun performRefresh(clusterId: String?, namespace: String?, showLoading: Boolean) {
        if (clusterId == null) return

        _uiState.update {
            it.copy(
                isRefreshing = showLoading,
                clusterConnectionStatus = ClusterConnectionStatus.CONNECTING,
            )
        }

        viewModelScope.launch(dispatcherProvider.main) {
            when (val result = refreshWorkloadsUseCase(clusterId, namespace)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            clusterConnectionStatus = ClusterConnectionStatus.CONNECTED,
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            clusterConnectionStatus = ClusterConnectionStatus.DISCONNECTED,
                        )
                    }
                    _effects.send(HomeUiEffect.ShowSnackbar("Failed to fetch pods: ${result.error.message}"))
                }

                is Result.Loading -> Unit
            }
        }
    }

    private fun connectAndSaveCluster() {
        val input = _uiState.value.kubeconfigInput.trim()
        if (input.isBlank()) {
            _uiState.update { it.copy(kubeconfigError = "Please paste or import a valid Kubeconfig.") }
            return
        }

        _uiState.update {
            it.copy(
                isConnecting = true,
                clusterConnectionStatus = ClusterConnectionStatus.CONNECTING,
                kubeconfigError = null,
            )
        }

        viewModelScope.launch(dispatcherProvider.main) {
            val result = addClusterUseCase(
                kubeconfigRaw = input,
                customName = _uiState.value.customClusterName.ifBlank { null },
                setAsActive = true,
            )

            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            clusterConnectionStatus = ClusterConnectionStatus.CONNECTED,
                            showAddClusterSheet = false,
                            kubeconfigInput = "",
                            customClusterName = "",
                        )
                    }
                    _effects.send(HomeUiEffect.ShowToast("Connected to cluster '${result.data.name}' successfully!"))
                    _effects.send(HomeUiEffect.NavigateToHome)
                }

                is Result.Error -> {
                    val rawMsg = result.error.message
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            clusterConnectionStatus = ClusterConnectionStatus.DISCONNECTED,
                            errorDialogData = ErrorDialogData(
                                title = "Cluster Connection Failed",
                                errorMessage = "Unable to connect to Kubernetes cluster. Please verify the kubeconfig credentials and network connectivity.",
                                rawErrorTrace = rawMsg,
                            ),
                        )
                    }
                }

                is Result.Loading -> Unit
            }
        }
    }

    private fun selectActiveCluster(clusterId: String) {
        _uiState.update {
            it.copy(
                isConnecting = true,
                clusterConnectionStatus = ClusterConnectionStatus.CONNECTING,
            )
        }

        viewModelScope.launch(dispatcherProvider.main) {
            when (val result = setActiveClusterUseCase(clusterId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            clusterConnectionStatus = ClusterConnectionStatus.CONNECTED,
                        )
                    }
                    _effects.send(HomeUiEffect.ShowToast("Active cluster updated successfully!"))
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            clusterConnectionStatus = ClusterConnectionStatus.DISCONNECTED,
                            errorDialogData = ErrorDialogData(
                                title = "Failed to Activate Cluster",
                                errorMessage = "Could not establish connection to the selected cluster.",
                                rawErrorTrace = result.error.message,
                            ),
                        )
                    }
                }

                is Result.Loading -> Unit
            }
        }
    }

    private fun testClusterConnection(clusterId: String) {
        _uiState.update {
            it.copy(
                isConnecting = true,
                clusterConnectionStatus = ClusterConnectionStatus.CONNECTING,
            )
        }

        viewModelScope.launch(dispatcherProvider.main) {
            when (val result = testClusterConnectionUseCase.testCluster(clusterId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            clusterConnectionStatus = ClusterConnectionStatus.CONNECTED,
                        )
                    }
                    _effects.send(HomeUiEffect.ShowToast("Connection successful: ${result.data}"))
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            clusterConnectionStatus = ClusterConnectionStatus.DISCONNECTED,
                            errorDialogData = ErrorDialogData(
                                title = "Connection Check Failed",
                                errorMessage = "Failed to connect to cluster.",
                                rawErrorTrace = result.error.message,
                            ),
                        )
                    }
                }

                is Result.Loading -> Unit
            }
        }
    }

    private fun saveClusterName(clusterId: String, newName: String) {
        viewModelScope.launch(dispatcherProvider.main) {
            when (val result = updateClusterNameUseCase(clusterId, newName)) {
                is Result.Success -> {
                    _uiState.update { it.copy(editingCluster = null) }
                    _effects.send(HomeUiEffect.ShowToast("Cluster renamed successfully!"))
                }

                is Result.Error -> {
                    _effects.send(HomeUiEffect.ShowSnackbar("Failed to rename cluster: ${result.error.message}"))
                }

                is Result.Loading -> Unit
            }
        }
    }

    private fun deleteCluster(clusterId: String) {
        viewModelScope.launch(dispatcherProvider.main) {
            when (val result = deleteClusterUseCase(clusterId)) {
                is Result.Success -> {
                    _effects.send(HomeUiEffect.ShowSnackbar("Cluster removed."))
                }

                is Result.Error -> {
                    _effects.send(HomeUiEffect.ShowSnackbar("Failed to remove cluster: ${result.error.message}"))
                }

                is Result.Loading -> Unit
            }
        }
    }

    companion object {
        fun provideFactory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(
                    getClustersUseCase = container.getClustersUseCase,
                    getActiveClusterUseCase = container.getActiveClusterUseCase,
                    getPodsUseCase = container.getPodsUseCase,
                    getNamespacesUseCase = container.getNamespacesUseCase,
                    getLastRefreshedUseCase = container.getLastRefreshedUseCase,
                    refreshWorkloadsUseCase = container.refreshWorkloadsUseCase,
                    addClusterUseCase = container.addClusterUseCase,
                    setActiveClusterUseCase = container.setActiveClusterUseCase,
                    deleteClusterUseCase = container.deleteClusterUseCase,
                    updateClusterNameUseCase = container.updateClusterNameUseCase,
                    testClusterConnectionUseCase = container.testClusterConnectionUseCase,
                    networkMonitor = container.networkMonitor,
                    dispatcherProvider = container.dispatcherProvider,
                ) as T
            }
        }
    }
}
