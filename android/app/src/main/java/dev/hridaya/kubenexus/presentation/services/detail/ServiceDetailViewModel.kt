package dev.hridaya.kubenexus.presentation.services.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.network.NetworkMonitor
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.ServiceDetails
import dev.hridaya.kubenexus.domain.usecase.GetActiveClusterUseCase
import dev.hridaya.kubenexus.domain.usecase.GetServiceDetailsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Network-first detail screen for one Service (mirroring PodDetailViewModel).
 *
 * Fetches fresh describe and service details over the network on every entry and refresh.
 * Automatically re-fetches when network connectivity is restored.
 */
@HiltViewModel(assistedFactory = ServiceDetailViewModel.Factory::class)
class ServiceDetailViewModel @AssistedInject constructor(
    @Assisted("serviceName") private val serviceName: String,
    @Assisted("namespace") private val namespace: String,
    private val getServiceDetailsUseCase: GetServiceDetailsUseCase,
    private val getActiveClusterUseCase: GetActiveClusterUseCase,
    private val networkMonitor: NetworkMonitor? = null,
    private val dispatcherProvider: DispatcherProvider? = null,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("serviceName") serviceName: String,
            @Assisted("namespace") namespace: String,
        ): ServiceDetailViewModel
    }

    private val _uiState = MutableStateFlow(
        ServiceDetailUiState(
            serviceName = serviceName,
            namespace = namespace,
        ),
    )
    val uiState: StateFlow<ServiceDetailUiState> = _uiState.asStateFlow()

    private var wasOffline = false

    init {
        observeNetwork()
    }

    private fun observeNetwork() {
        val monitor = networkMonitor ?: return
        val dispatcher = dispatcherProvider?.main ?: kotlinx.coroutines.Dispatchers.Main.immediate
        viewModelScope.launch(dispatcher) {
            monitor.isOnline.collect { online ->
                if (online && wasOffline) {
                    load()
                }
                wasOffline = !online
            }
        }
    }

    fun onAction(action: ServiceDetailUiAction) {
        when (action) {
            ServiceDetailUiAction.Refresh -> load()
            is ServiceDetailUiAction.ShowPortForwardDialog -> _uiState.update {
                it.copy(showPortForwardDialog = action.show)
            }
        }
    }

    /**
     * Deliberately not called from init: [ServiceDetailRoute]'s
     * LifecycleStartEffect triggers it on every start, so an init call would
     * fire two identical fetches on first entry.
     */
    private fun load() {
        viewModelScope.launch {
            // First load takes the full spinner; a refresh of an already shown
            // service keeps the overview on screen while re-fetching.
            _uiState.update { state ->
                state.copy(isLoading = state.service == null, errorMessage = null)
            }
            val clusterId = getActiveClusterUseCase().firstOrNull()?.id
            when (val result = getServiceDetails(clusterId, namespace, serviceName)) {
                is Result.Success -> _uiState.update { state ->
                    state.copy(isLoading = false, service = result.data, errorMessage = null)
                }

                is Result.Error -> _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = LOAD_ERROR_MESSAGE,
                    )
                }

                is Result.Loading -> Unit
            }
        }
    }

    /** Single integration point if the details use case signature drifts. */
    private suspend fun getServiceDetails(
        clusterId: String?,
        namespace: String,
        name: String,
    ): Result<ServiceDetails> = getServiceDetailsUseCase(clusterId, namespace, name)

    private companion object {
        const val LOAD_ERROR_MESSAGE =
            "Couldn't load this service right now. Check that the cluster is reachable and try again."
    }
}
