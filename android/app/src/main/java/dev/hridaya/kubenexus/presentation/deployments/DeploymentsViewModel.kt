package dev.hridaya.kubenexus.presentation.deployments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.network.NetworkMonitor
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.DeploymentSummary
import dev.hridaya.kubenexus.domain.usecase.GetDeploymentsLastRefreshedUseCase
import dev.hridaya.kubenexus.domain.usecase.GetDeploymentsStreamUseCase
import dev.hridaya.kubenexus.domain.usecase.GetNamespacesUseCase
import dev.hridaya.kubenexus.domain.usecase.SyncDeploymentsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Offline-first list of Deployments for the workloads screen (issue #7).
 *
 * The cluster id arrives from the host ([dev.hridaya.kubenexus.presentation.main.MainScreen])
 * through the assisted factory, mirroring the CreateDeploymentViewModel pattern;
 * unlike Pods, the namespace is additionally switchable in-screen via chips.
 *
 * A fallback Dispatchers.Main.immediate is used when dispatcherProvider is null
 * so existing tests constructing this class with defaults continue working
 * without modification.
 */
@HiltViewModel(assistedFactory = DeploymentsViewModel.Factory::class)
class DeploymentsViewModel @AssistedInject constructor(
    @Assisted("clusterId") private val clusterId: String?,
    @Assisted("namespace") initialNamespace: String? = null,
    private val getDeploymentsStreamUseCase: GetDeploymentsStreamUseCase,
    private val getDeploymentsLastRefreshedUseCase: GetDeploymentsLastRefreshedUseCase,
    private val syncDeploymentsUseCase: SyncDeploymentsUseCase,
    private val getNamespacesUseCase: GetNamespacesUseCase,
    private val networkMonitor: NetworkMonitor? = null,
    private val dispatcherProvider: DispatcherProvider? = null,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("clusterId") clusterId: String?,
            @Assisted("namespace") namespace: String? = null,
        ): DeploymentsViewModel
    }

    private val _uiState = MutableStateFlow(
        DeploymentsUiState(
            selectedNamespace = initialNamespace?.takeIf { it.isNotBlank() } ?: ALL_NAMESPACES_FILTER,
        ),
    )
    val uiState: StateFlow<DeploymentsUiState> = _uiState.asStateFlow()

    private var wasOffline = false

    init {
        observeNetworkConnectivity()
        observeNamespaceOptions()
        observeCachedDeploymentRows()
    }

    private fun observeNetworkConnectivity() {
        val monitor = networkMonitor ?: return
        val dispatcher = dispatcherProvider?.main ?: Dispatchers.Main.immediate
        viewModelScope.launch(dispatcher) {
            monitor.isOnline.collect { online ->
                val offlineTransition = wasOffline && online
                wasOffline = !online
                _uiState.update { it.copy(isOnline = online) }
                if (offlineTransition) {
                    syncRemoteSnapshot()
                }
            }
        }
    }

    fun onAction(action: DeploymentsUiAction) {
        when (action) {
            DeploymentsUiAction.Refresh -> syncRemoteSnapshot()
            is DeploymentsUiAction.SelectNamespace -> selectNamespace(action.namespace)
        }
    }

    private val autoFetchedNamespaces = mutableSetOf<String>()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeCachedDeploymentRows() {
        val dispatcher = dispatcherProvider?.main ?: Dispatchers.Main.immediate
        viewModelScope.launch(dispatcher) {
            _uiState
                .map { it.selectedNamespace }
                .distinctUntilChanged()
                .flatMapLatest { namespaceFilter ->
                    combine(
                        observeRoomRows(namespaceFilter.toNamespaceArgument()),
                        getDeploymentsLastRefreshedUseCase(clusterId),
                    ) { cachedRows, lastRefreshed ->
                        Triple(cachedRows, lastRefreshed, namespaceFilter)
                    }
                }
                .collect { (cachedRows, lastRefreshed, namespaceFilter) ->
                    val shouldAutoFetch = cachedRows.isEmpty() &&
                        namespaceFilter !in autoFetchedNamespaces &&
                        !_uiState.value.isSyncing
                    _uiState.update { state ->
                        state.copy(
                            deployments = cachedRows,
                            lastSyncedAt = lastRefreshed ?: state.lastSyncedAt,
                            isLoading = if (shouldAutoFetch) true else false,
                            errorMessage = null,
                        )
                    }
                    if (shouldAutoFetch) {
                        autoFetchedNamespaces.add(namespaceFilter)
                        syncRemoteSnapshot()
                    }
                }
        }
    }

    private fun observeNamespaceOptions() {
        viewModelScope.launch {
            getNamespacesUseCase(clusterId).collect { clusterNamespaces ->
                _uiState.update { state ->
                    state.copy(
                        namespaces =
                            (listOf(ALL_NAMESPACES_FILTER) + clusterNamespaces.filter(String::isNotBlank))
                                .distinct(),
                    )
                }
            }
        }
    }

    /**
     * Network refresh entry point (pull-to-refresh and lifecycle start). Only
     * writes into Room; the list updates exclusively through the stream emit.
     */
    private fun syncRemoteSnapshot() {
        viewModelScope.launch {
            _uiState.update { state ->
                if (state.deployments.isEmpty() && state.errorMessage == null) {
                    state.copy(isLoading = true, errorMessage = null)
                } else {
                    state.copy(isSyncing = true, errorMessage = null)
                }
            }
            when (val result =
                pushRemoteSnapshot(_uiState.value.selectedNamespace.toNamespaceArgument())) {
                is Result.Success -> _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isSyncing = false,
                        errorMessage = null,
                        lastSyncedAt = System.currentTimeMillis(),
                    )
                }

                is Result.Error -> _uiState.update { state ->
                    // Cached rows stay visible offline; only an empty screen
                    // escalates the failure into blocking copy.
                    state.copy(
                        isLoading = false,
                        isSyncing = false,
                        errorMessage = if (state.deployments.isEmpty()) SYNC_ERROR_MESSAGE else null,
                    )
                }

                is Result.Loading -> Unit
            }
        }
    }

    private fun selectNamespace(namespace: String) {
        _uiState.update { it.copy(selectedNamespace = namespace) }
    }

    /** Integration seam for the parallel-landing sync use case signature. */
    private suspend fun pushRemoteSnapshot(namespace: String?): Result<Unit> =
        syncDeploymentsUseCase(clusterId, namespace)

    /** Integration seam for the parallel-landing Room stream use case signature. */
    private fun observeRoomRows(namespace: String?): Flow<List<DeploymentSummary>> =
        getDeploymentsStreamUseCase(clusterId, namespace)

    private companion object {
        const val SYNC_ERROR_MESSAGE =
            "Couldn't reach the cluster to sync deployments. Check your connection and try again."

        fun String?.toInitialNamespaceFilter(): String =
            takeIf { !it.isNullOrBlank() } ?: ALL_NAMESPACES_FILTER

        fun String?.toNamespaceArgument(): String? =
            takeIf { candidate -> !candidate.isNullOrBlank() && candidate != ALL_NAMESPACES_FILTER }
    }
}
