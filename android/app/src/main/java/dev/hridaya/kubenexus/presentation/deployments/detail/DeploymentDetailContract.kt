package dev.hridaya.kubenexus.presentation.deployments.detail

import dev.hridaya.kubenexus.domain.model.DeploymentDetails
import dev.hridaya.kubenexus.domain.model.DeploymentSummary

data class DeploymentDetailUiState(
    val deploymentName: String,
    val namespace: String,
    val isLoading: Boolean = true,
    val deployment: DeploymentSummary? = null,
    val errorMessage: String? = null,
    /** Describe-deployment fetch runs separately so the summary card never blocks on it. */
    val isDetailsLoading: Boolean = true,
    val details: DeploymentDetails? = null,
    val detailsErrorMessage: String? = null,
)

sealed interface DeploymentDetailUiAction {
    data object Refresh : DeploymentDetailUiAction
}
