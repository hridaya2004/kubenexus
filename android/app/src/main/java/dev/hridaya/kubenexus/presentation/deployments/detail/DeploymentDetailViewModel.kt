package dev.hridaya.kubenexus.presentation.deployments.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.network.NetworkMonitor
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.usecase.GetActiveClusterUseCase
import dev.hridaya.kubenexus.domain.usecase.GetDeploymentDetailsUseCase
import dev.hridaya.kubenexus.domain.usecase.GetDeploymentsStreamUseCase
import dev.hridaya.kubenexus.domain.usecase.GetDeploymentsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Offline-first detail screen for one Deployment.
 *
 * Reads cached deployment summary immediately from Room, while fetching live
 * describe details over the network in the background.
 * Automatically re-fetches when network connectivity is restored.
 */
@HiltViewModel(assistedFactory = DeploymentDetailViewModel.Factory::class)
class DeploymentDetailViewModel @AssistedInject constructor(
    @Assisted("deploymentName") private val deploymentName: String,
    @Assisted("namespace") private val namespace: String,
    private val getDeploymentsUseCase: GetDeploymentsUseCase,
    private val getDeploymentDetailsUseCase: GetDeploymentDetailsUseCase,
    private val getActiveClusterUseCase: GetActiveClusterUseCase,
    private val getDeploymentsStreamUseCase: GetDeploymentsStreamUseCase? = null,
    private val networkMonitor: NetworkMonitor? = null,
    private val dispatcherProvider: DispatcherProvider? = null,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("deploymentName") deploymentName: String,
            @Assisted("namespace") namespace: String,
        ): DeploymentDetailViewModel
    }

    private val _uiState = MutableStateFlow(
        DeploymentDetailUiState(
            deploymentName = deploymentName,
            namespace = namespace,
        ),
    )
    val uiState: StateFlow<DeploymentDetailUiState> = _uiState.asStateFlow()

    private var wasOffline = false

    init {
        observeNetwork()
        load()
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

    fun onAction(action: DeploymentDetailUiAction) {
        when (action) {
            DeploymentDetailUiAction.Refresh -> load()
        }
    }

    /**
     * One active-cluster read feeds both sections; they then run concurrently
     * so a hung describe call cannot delay the summary card.
     */
    private fun load() {
        viewModelScope.launch {
            val clusterId = getActiveClusterUseCase().firstOrNull()?.id
            launch { loadSummary(clusterId) }
            launch { loadDetails(clusterId) }
        }
    }

    private suspend fun loadSummary(clusterId: String?) {
        // Read cached summary from Room stream first so it appears instantly even offline!
        val cachedSummary = getDeploymentsStreamUseCase?.invoke(clusterId, namespace)
            ?.firstOrNull()
            ?.firstOrNull { it.name == deploymentName && it.namespace == namespace }
        if (cachedSummary != null) {
            _uiState.update { state ->
                state.copy(isLoading = false, deployment = cachedSummary, errorMessage = null)
            }
        } else {
            _uiState.update { state ->
                state.copy(isLoading = state.deployment == null, errorMessage = null)
            }
        }

        when (val result = getDeploymentsUseCase(clusterId, namespace)) {
            is Result.Success -> {
                val match = result.data.firstOrNull { candidate ->
                    candidate.name == deploymentName && candidate.namespace == namespace
                }
                _uiState.update { state ->
                    if (match != null) {
                        state.copy(isLoading = false, deployment = match, errorMessage = null)
                    } else if (state.deployment == null) {
                        state.copy(
                            isLoading = false,
                            deployment = null,
                            errorMessage =
                                "Deployment \"$deploymentName\" was not found in namespace " +
                                    "\"$namespace\". It may have been deleted or renamed.",
                        )
                    } else {
                        state.copy(isLoading = false)
                    }
                }
            }

            is Result.Error -> _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    errorMessage = if (state.deployment == null) LOAD_ERROR_MESSAGE else null,
                )
            }

            is Result.Loading -> Unit
        }
    }

    private suspend fun loadDetails(clusterId: String?) {
        _uiState.update { state ->
            state.copy(isDetailsLoading = state.details == null, detailsErrorMessage = null)
        }
        when (val result = getDeploymentDetailsUseCase(clusterId, namespace, deploymentName)) {
            is Result.Success -> _uiState.update { state ->
                state.copy(
                    isDetailsLoading = false,
                    details = result.data,
                    detailsErrorMessage = null,
                )
            }

            is Result.Error -> _uiState.update { state ->
                state.copy(
                    isDetailsLoading = false,
                    detailsErrorMessage = if (state.deployment == null) DETAILS_ERROR_MESSAGE else null,
                )
            }

            is Result.Loading -> Unit
        }
    }

    private companion object {
        const val LOAD_ERROR_MESSAGE =
            "Couldn't load this deployment right now. Check that the cluster is reachable and try again."

        const val DETAILS_ERROR_MESSAGE =
            "Couldn't load the describe details for this deployment."
    }
}
