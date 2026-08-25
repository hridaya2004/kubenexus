package dev.hridaya.kubenexus.presentation.deployments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.usecase.GetDeploymentsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Lists Deployments for the workloads screen (issue #7). The cluster id and
 * namespace filter are supplied by the host ([dev.hridaya.kubenexus.presentation.main.MainScreen])
 * from the Home state at creation time, mirroring the CreateDeploymentViewModel
 * assisted pattern. A null or blank namespace lists across all namespaces.
 *
 * Loading is driven by [DeploymentsUiAction.Refresh]; the Route fires it on
 * lifecycle start, so returning to an already-created screen re-fetches instead
 * of showing stale data.
 */
@HiltViewModel(assistedFactory = DeploymentsViewModel.Factory::class)
class DeploymentsViewModel @AssistedInject constructor(
    @Assisted("clusterId") private val clusterId: String?,
    @Assisted("namespace") private val namespace: String?,
    private val getDeploymentsUseCase: GetDeploymentsUseCase,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("clusterId") clusterId: String?,
            @Assisted("namespace") namespace: String?,
        ): DeploymentsViewModel
    }

    private val _uiState = MutableStateFlow(DeploymentsUiState())
    val uiState: StateFlow<DeploymentsUiState> = _uiState.asStateFlow()

    fun onAction(action: DeploymentsUiAction) {
        when (action) {
            DeploymentsUiAction.Refresh -> load()
        }
    }

    /** Friendly, user-facing copy; raw errors are never surfaced to the UI. */
    private fun load() {
        viewModelScope.launch {
            // First load takes the full spinner; later loads keep the list on
            // screen under the pull-to-refresh indicator.
            _uiState.update { state ->
                if (state.deployments.isEmpty() && state.errorMessage == null) {
                    state.copy(isLoading = true, errorMessage = null)
                } else {
                    state.copy(isRefreshing = true, errorMessage = null)
                }
            }
            when (val result = getDeploymentsUseCase(clusterId, namespace)) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        deployments = result.data,
                        errorMessage = null,
                    )
                }

                is Result.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = LOAD_ERROR_MESSAGE,
                    )
                }

                is Result.Loading -> Unit
            }
        }
    }

    private companion object {
        const val LOAD_ERROR_MESSAGE =
            "Couldn't load your deployments right now. Check that the cluster is reachable and try again."
    }
}
