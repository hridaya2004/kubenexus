package dev.hridaya.kubenexus.presentation.deployments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.DeploymentSummary
import dev.hridaya.kubenexus.domain.usecase.GetDeploymentsStreamUseCase
import dev.hridaya.kubenexus.domain.usecase.GetNamespacesUseCase
import dev.hridaya.kubenexus.domain.usecase.SyncDeploymentsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * Room is the single source of truth for the list: [GetDeploymentsStreamUseCase]
 * replays the last synced snapshot instantly (offline included), and
 * [SyncDeploymentsUseCase] refreshes the cache over the network, after which
 * the stream re-emits on its own. Namespace changes swap the stream via
 * flatMapLatest rather than refetching anything.
 *
 * Integration note: the sync/stream use cases land in parallel with this
 * change; [pushRemoteSnapshot] and [observeRoomRows] are the two seams to
 * touch if their final signatures differ slightly.
 */
@HiltViewModel(assistedFactory = DeploymentsViewModel.Factory::class)
class DeploymentsViewModel @AssistedInject constructor(
    @Assisted("clusterId") private val clusterId: String?,
    @Assisted("namespace") initialNamespace: String?,
    private val getNamespacesUseCase: GetNamespacesUseCase,
    private val syncDeploymentsUseCase: SyncDeploymentsUseCase,
    private val getDeploymentsStreamUseCase: GetDeploymentsStreamUseCase,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("clusterId") clusterId: String?,
            @Assisted("namespace") namespace: String?,
        ): DeploymentsViewModel
    }

    private val _uiState = MutableStateFlow(
        DeploymentsUiState(selectedNamespace = initialNamespace.toInitialNamespaceFilter()),
    )
    val uiState: StateFlow<DeploymentsUiState> = _uiState.asStateFlow()

    init {
        observeNamespaceOptions()
        observeCachedDeploymentRows()
    }

    fun onAction(action: DeploymentsUiAction) {
        when (action) {
            DeploymentsUiAction.Refresh -> syncRemoteSnapshot()
            is DeploymentsUiAction.SelectNamespace -> selectNamespace(action.namespace)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeCachedDeploymentRows() {
        viewModelScope.launch {
            _uiState
                .map { it.selectedNamespace }
                .distinctUntilChanged()
                .flatMapLatest { namespaceFilter ->
                    observeRoomRows(namespaceFilter.toNamespaceArgument())
                }
                .collect { cachedRows ->
                    _uiState.update { state ->
                        state.copy(deployments = cachedRows, isLoading = false, errorMessage = null)
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
            when (val result = pushRemoteSnapshot(_uiState.value.selectedNamespace.toNamespaceArgument())) {
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
