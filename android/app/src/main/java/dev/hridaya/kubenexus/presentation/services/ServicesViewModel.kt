package dev.hridaya.kubenexus.presentation.services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.network.NetworkMonitor
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.ServiceSummary
import dev.hridaya.kubenexus.domain.usecase.GetNamespacesUseCase
import dev.hridaya.kubenexus.domain.usecase.GetServicesLastRefreshedUseCase
import dev.hridaya.kubenexus.domain.usecase.GetServicesStreamUseCase
import dev.hridaya.kubenexus.domain.usecase.SyncServicesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Offline-first list of Services for the workloads screen, sibling of the
 * Deployments screen (issue #7 family). The cluster id and initial namespace
 * filter are supplied by the host ([dev.hridaya.kubenexus.presentation.main.MainScreen])
 * from the Home state at creation time, mirroring the DeploymentsViewModel
 * assisted pattern.
 *
 * Rendering is driven by the Room stream ([GetServicesStreamUseCase]) so cached
 * services appear instantly, airplane-mode safe. [SyncServicesUseCase] runs on
 * lifecycle start and on pull-to-refresh; a failed sync only surfaces an error
 * when there is nothing cached to show.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = ServicesViewModel.Factory::class)
class ServicesViewModel @AssistedInject constructor(
    @Assisted("clusterId") private val clusterId: String?,
    @Assisted("namespace") initialNamespace: String?,
    private val getServicesStreamUseCase: GetServicesStreamUseCase,
    private val syncServicesUseCase: SyncServicesUseCase,
    private val getNamespacesUseCase: GetNamespacesUseCase,
    private val getServicesLastRefreshedUseCase: GetServicesLastRefreshedUseCase,
    private val networkMonitor: NetworkMonitor? = null,
    private val dispatcherProvider: DispatcherProvider? = null,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("clusterId") clusterId: String?,
            @Assisted("namespace") namespace: String?,
        ): ServicesViewModel
    }

    private val _selectedNamespace = MutableStateFlow(
        initialNamespace?.takeIf { it.isNotBlank() } ?: ALL_NAMESPACES_FILTER,
    )

    private val _uiState = MutableStateFlow(
        ServicesUiState(selectedNamespace = _selectedNamespace.value),
    )
    val uiState: StateFlow<ServicesUiState> = _uiState.asStateFlow()

    private var wasOffline = false

    init {
        observeNetworkConnectivity()
        observeLocalDatabase()
    }

    private fun observeNetworkConnectivity() {
        val monitor = networkMonitor ?: return
        val dispatcher = dispatcherProvider?.main ?: kotlinx.coroutines.Dispatchers.Main.immediate
        viewModelScope.launch(dispatcher) {
            monitor.isOnline.collect { online ->
                val offlineTransition = wasOffline && online
                wasOffline = !online
                _uiState.update { it.copy(isOnline = online) }
                if (offlineTransition) {
                    sync()
                }
            }
        }
    }

    fun onAction(action: ServicesUiAction) {
        when (action) {
            ServicesUiAction.Refresh -> sync()
            is ServicesUiAction.SelectNamespace -> selectNamespace(action.namespace)
        }
    }

    /**
     * Re-collects whenever the namespace chip changes so the Room query is
     * re-issued with the new filter; namespaces stream alongside for the chips.
     */
    private fun observeLocalDatabase() {
        val dispatcher = dispatcherProvider?.main ?: kotlinx.coroutines.Dispatchers.Main.immediate
        viewModelScope.launch(dispatcher) {
            _selectedNamespace.flatMapLatest { selected ->
                combine(
                    observeServices(selected),
                    getNamespacesUseCase(clusterId),
                    getServicesLastRefreshedUseCase(clusterId),
                ) { services, namespaces, lastRefreshed ->
                    ServicesStreamData(
                        services = services,
                        namespaces = namespaces,
                        lastRefreshed = lastRefreshed,
                    )
                }
            }.collect { data ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        services = data.services,
                        lastRefreshedAt = data.lastRefreshed ?: state.lastRefreshedAt,
                        // Keep the previous chip list if the namespaces stream
                        // is momentarily empty, mirroring the Home behavior.
                        availableNamespaces = if (data.namespaces.isNotEmpty()) {
                            listOf(ALL_NAMESPACES_FILTER) +
                                    data.namespaces.filter { it != ALL_NAMESPACES_FILTER }
                        } else {
                            state.availableNamespaces
                        },
                    )
                }
            }
        }
    }

    /** Single integration point if the stream use case signature drifts. */
    private fun observeServices(namespaceFilter: String?): Flow<List<ServiceSummary>> =
        getServicesStreamUseCase(clusterId = clusterId, namespace = namespaceFilter)

    private fun selectNamespace(namespace: String) {
        _selectedNamespace.value = namespace
        _uiState.update { it.copy(selectedNamespace = namespace) }
    }

    /**
     * Friendly, user-facing copy; raw errors are never surfaced to the UI.
     * Deliberately not called from init: the Route's LifecycleStartEffect
     * triggers it on every start, so an init call would fire two identical
     * syncs on first entry.
     */
    private fun sync() {
        viewModelScope.launch {
            // First load takes the full spinner; later loads keep the list on
            // screen under the pull-to-refresh indicator.
            _uiState.update { state ->
                if (state.services.isEmpty() && state.errorMessage == null) {
                    state.copy(isLoading = true, errorMessage = null)
                } else {
                    state.copy(isRefreshing = true, errorMessage = null)
                }
            }
            when (val result = syncServices(namespaceFilter())) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = null,
                        lastRefreshedAt = System.currentTimeMillis(),
                    )
                }

                is Result.Error -> _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = if (state.services.isEmpty()) SYNC_ERROR_MESSAGE else null,
                    )
                }

                is Result.Loading -> Unit
            }
        }
    }

    /** Single integration point if the sync use case signature drifts. */
    private suspend fun syncServices(namespaceFilter: String?): Result<Unit> =
        syncServicesUseCase(clusterId = clusterId, namespace = namespaceFilter)

    private fun namespaceFilter(): String? =
        _selectedNamespace.value
            .takeIf { it.isNotBlank() && it != ALL_NAMESPACES_FILTER }

    private data class ServicesStreamData(
        val services: List<ServiceSummary>,
        val namespaces: List<String>,
        val lastRefreshed: Long?,
    )

    private companion object {
        const val SYNC_ERROR_MESSAGE =
            "Couldn't load your services right now. Check that the cluster is reachable and try again."
    }
}
