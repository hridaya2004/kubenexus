package dev.hridaya.kubenexus.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.core.di.AppContainer
import dev.hridaya.kubenexus.domain.usecase.AddClusterUseCase
import dev.hridaya.kubenexus.domain.usecase.DeleteClusterUseCase
import dev.hridaya.kubenexus.domain.usecase.GetActiveClusterUseCase
import dev.hridaya.kubenexus.domain.usecase.GetClustersUseCase
import dev.hridaya.kubenexus.domain.usecase.GetNamespacesUseCase
import dev.hridaya.kubenexus.domain.usecase.GetPodsUseCase
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val getClustersUseCase: GetClustersUseCase,
    private val getActiveClusterUseCase: GetActiveClusterUseCase,
    private val getPodsUseCase: GetPodsUseCase,
    private val getNamespacesUseCase: GetNamespacesUseCase,
    private val addClusterUseCase: AddClusterUseCase,
    private val setActiveClusterUseCase: SetActiveClusterUseCase,
    private val deleteClusterUseCase: DeleteClusterUseCase,
    private val updateClusterNameUseCase: UpdateClusterNameUseCase,
    private val testClusterConnectionUseCase: TestClusterConnectionUseCase,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _selectedNamespace = MutableStateFlow("All Namespaces")
    private val _refreshTrigger = MutableStateFlow(0L)

    private val _effects = Channel<HomeUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        observeClustersAndPods()
    }

    private fun observeClustersAndPods() {
        viewModelScope.launch(dispatcherProvider.main) {
            combine(
                getClustersUseCase(),
                getActiveClusterUseCase(),
                _selectedNamespace,
                _refreshTrigger
            ) { clusters, activeCluster, ns, _ ->
                Triple(clusters, activeCluster, ns)
            }.flatMapLatest { (clusters, activeCluster, ns) ->
                combine(
                    getPodsUseCase(activeCluster?.id, ns),
                    getNamespacesUseCase(activeCluster?.id)
                ) { pods, namespaces ->
                    WorkloadData(clusters, activeCluster, pods, namespaces, ns)
                }.onStart {
                    _uiState.update { it.copy(isRefreshing = true) }
                }.catch { t ->
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            lastRefreshedAt = System.currentTimeMillis()
                        )
                    }
                    _effects.send(HomeUiEffect.ShowSnackbar("Failed to fetch pods: ${t.message ?: "Network error"}"))
                }
            }.collect { data ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        lastRefreshedAt = System.currentTimeMillis(),
                        clusters = data.clusters,
                        activeCluster = data.activeCluster,
                        pods = data.pods,
                        availableNamespaces = if (data.namespaces.isNotEmpty()) data.namespaces else state.availableNamespaces,
                        selectedNamespace = data.selectedNamespace
                    )
                }
            }
        }
    }

    private data class WorkloadData(
        val clusters: List<dev.hridaya.kubenexus.domain.model.Cluster>,
        val activeCluster: dev.hridaya.kubenexus.domain.model.Cluster?,
        val pods: List<dev.hridaya.kubenexus.domain.model.Pod>,
        val namespaces: List<String>,
        val selectedNamespace: String
    )

    fun onAction(action: HomeUiAction) {
        when (action) {
            is HomeUiAction.RefreshWorkloads -> {
                refresh()
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
                        kubeconfigError = null
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
                        kubeconfigError = null
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
                        isRefreshing = true
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

    private fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        _refreshTrigger.value = System.currentTimeMillis()
    }

    private fun connectAndSaveCluster() {
        val input = _uiState.value.kubeconfigInput.trim()
        if (input.isBlank()) {
            _uiState.update { it.copy(kubeconfigError = "Please paste or import a valid Kubeconfig.") }
            return
        }

        _uiState.update { it.copy(isConnecting = true, kubeconfigError = null) }

        viewModelScope.launch(dispatcherProvider.main) {
            val result = addClusterUseCase(
                kubeconfigRaw = input,
                customName = _uiState.value.customClusterName.ifBlank { null },
                setAsActive = true
            )

            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            showAddClusterSheet = false,
                            kubeconfigInput = "",
                            customClusterName = ""
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
                            errorDialogData = ErrorDialogData(
                                title = "Cluster Connection Failed",
                                errorMessage = "Unable to connect to Kubernetes cluster. Please verify the kubeconfig credentials and network connectivity.",
                                rawErrorTrace = rawMsg
                            )
                        )
                    }
                }

                is Result.Loading -> Unit
            }
        }
    }

    private fun selectActiveCluster(clusterId: String) {
        _uiState.update { it.copy(isConnecting = true) }

        viewModelScope.launch(dispatcherProvider.main) {
            val result = setActiveClusterUseCase(clusterId)
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isConnecting = false) }
                    _effects.send(HomeUiEffect.ShowToast("Active cluster updated successfully!"))
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            errorDialogData = ErrorDialogData(
                                title = "Failed to Activate Cluster",
                                errorMessage = "Could not establish connection to the selected cluster.",
                                rawErrorTrace = result.error.message
                            )
                        )
                    }
                }

                is Result.Loading -> Unit
            }
        }
    }

    private fun testClusterConnection(clusterId: String) {
        _uiState.update { it.copy(isConnecting = true) }

        viewModelScope.launch(dispatcherProvider.main) {
            val result = testClusterConnectionUseCase.testCluster(clusterId)
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isConnecting = false) }
                    _effects.send(HomeUiEffect.ShowToast("Connection successful: ${result.data}"))
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            errorDialogData = ErrorDialogData(
                                title = "Connection Check Failed",
                                errorMessage = "Failed to connect to cluster.",
                                rawErrorTrace = result.error.message
                            )
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
        fun provideFactory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(
                        getClustersUseCase = container.getClustersUseCase,
                        getActiveClusterUseCase = container.getActiveClusterUseCase,
                        getPodsUseCase = container.getPodsUseCase,
                        getNamespacesUseCase = container.getNamespacesUseCase,
                        addClusterUseCase = container.addClusterUseCase,
                        setActiveClusterUseCase = container.setActiveClusterUseCase,
                        deleteClusterUseCase = container.deleteClusterUseCase,
                        updateClusterNameUseCase = container.updateClusterNameUseCase,
                        testClusterConnectionUseCase = container.testClusterConnectionUseCase,
                        dispatcherProvider = container.dispatcherProvider
                    ) as T
                }
            }
    }
}
