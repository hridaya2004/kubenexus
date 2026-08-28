package dev.hridaya.kubenexus.presentation.deployments.detail

import dev.hridaya.kubenexus.domain.model.DeploymentDetails
import dev.hridaya.kubenexus.domain.model.DeploymentSummary
import dev.hridaya.kubenexus.domain.model.Pod

data class DeploymentDetailUiState(
    val deploymentName: String,
    val namespace: String,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val lastRefreshedAt: Long? = null,
    val deployment: DeploymentSummary? = null,
    val errorMessage: String? = null,
    /** Describe-deployment fetch runs separately so the summary card never blocks on it. */
    val isDetailsLoading: Boolean = true,
    val details: DeploymentDetails? = null,
    val detailsErrorMessage: String? = null,
    val associatedPods: List<Pod> = emptyList(),
    val isPodsLoading: Boolean = false,
    val podsErrorMessage: String? = null,
    val showScaleDialog: Boolean = false,
    val scaleInput: Int = 1,
    val showRestartDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val isMutating: Boolean = false,
    val mutationErrorMessage: String? = null,
)

sealed interface DeploymentDetailUiAction {
    data object Refresh : DeploymentDetailUiAction
    data object OpenScaleDialog : DeploymentDetailUiAction
    data object DismissScaleDialog : DeploymentDetailUiAction
    data class ScaleInputChanged(val replicas: Int) : DeploymentDetailUiAction
    data object ConfirmScale : DeploymentDetailUiAction
    data object OpenRestartDialog : DeploymentDetailUiAction
    data object DismissRestartDialog : DeploymentDetailUiAction
    data object ConfirmRestart : DeploymentDetailUiAction
    data object OpenDeleteDialog : DeploymentDetailUiAction
    data object DismissDeleteDialog : DeploymentDetailUiAction
    data object ConfirmDelete : DeploymentDetailUiAction
    data object DismissMutationError : DeploymentDetailUiAction
}

sealed interface DeploymentDetailUiEffect {
    data object NavigateBack : DeploymentDetailUiEffect
    data class ShowSnackbar(val message: String) : DeploymentDetailUiEffect
}
