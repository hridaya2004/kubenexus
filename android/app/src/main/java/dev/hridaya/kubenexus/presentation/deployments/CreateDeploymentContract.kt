package dev.hridaya.kubenexus.presentation.deployments

import dev.hridaya.kubenexus.domain.model.DeploymentDraft

enum class CreateDeploymentStep {
    FORM,
    REVIEW,
}

data class CreateDeploymentUiState(
    val name: String = "",
    val namespace: String = "",
    val image: String = "",
    val replicas: String = DeploymentDraft.DEFAULT_REPLICAS.toString(),
    val containerPort: String = DeploymentDraft.DEFAULT_CONTAINER_PORT.toString(),
    val fieldErrors: Map<String, String> = emptyMap(),
    val availableNamespaces: List<String> = emptyList(),
    val showCreateNamespaceDialog: Boolean = false,
    val newNamespaceName: String = "",
    val newNamespaceError: String? = null,
    val isCreatingNamespace: Boolean = false,
    val step: CreateDeploymentStep = CreateDeploymentStep.FORM,
    val generatedYaml: String? = null,

    /** Exactly what the editor shows; Apply sends this text verbatim. */
    val reviewedYaml: String = "",
    val isSubmitting: Boolean = false,
    val hasSubmitted: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface CreateDeploymentUiAction {
    data class NameChanged(val value: String) : CreateDeploymentUiAction
    data class ImageChanged(val value: String) : CreateDeploymentUiAction
    data class ReplicasChanged(val value: String) : CreateDeploymentUiAction
    data class ContainerPortChanged(val value: String) : CreateDeploymentUiAction
    data class NamespaceSelected(val namespace: String) : CreateDeploymentUiAction
    data object PreviewSubmitted : CreateDeploymentUiAction
    data object BackToFormClicked : CreateDeploymentUiAction
    data object ApplySubmitted : CreateDeploymentUiAction
    data class ReviewedYamlChanged(val value: String) : CreateDeploymentUiAction
    data object DismissError : CreateDeploymentUiAction
    data object CreateNamespaceClicked : CreateDeploymentUiAction
    data object DismissCreateNamespaceClicked : CreateDeploymentUiAction
    data class NewNamespaceNameChanged(val value: String) : CreateDeploymentUiAction
    data object CreateNamespaceSubmitted : CreateDeploymentUiAction
    data object Reset : CreateDeploymentUiAction
}

sealed interface CreateDeploymentUiEffect {
    data class Created(val deploymentName: String) : CreateDeploymentUiEffect
    data object NamespaceCreated : CreateDeploymentUiEffect
}
