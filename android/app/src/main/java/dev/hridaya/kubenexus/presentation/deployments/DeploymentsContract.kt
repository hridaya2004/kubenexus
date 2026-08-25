package dev.hridaya.kubenexus.presentation.deployments

import dev.hridaya.kubenexus.domain.model.DeploymentSummary

data class DeploymentsUiState(
    val isLoading: Boolean = true,
    val deployments: List<DeploymentSummary> = emptyList(),
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
)

sealed interface DeploymentsUiAction {
    data object Refresh : DeploymentsUiAction
}
