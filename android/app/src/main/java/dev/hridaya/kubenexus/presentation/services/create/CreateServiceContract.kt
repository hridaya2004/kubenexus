package dev.hridaya.kubenexus.presentation.services.create

import dev.hridaya.kubenexus.domain.model.ServiceDraft

enum class CreateServiceStep {
    FORM,
    REVIEW,
}

data class CreateServiceUiState(
    val name: String = "",
    val namespace: String = "",
    val selectorApp: String = "",
    val port: String = "",
    val targetPort: String = "",
    val serviceType: String = ServiceDraft.DEFAULT_TYPE,
    val fieldErrors: Map<String, String> = emptyMap(),
    val availableNamespaces: List<String> = emptyList(),
    val showCreateNamespaceDialog: Boolean = false,
    val newNamespaceName: String = "",
    val newNamespaceError: String? = null,
    val isCreatingNamespace: Boolean = false,
    val step: CreateServiceStep = CreateServiceStep.FORM,
    val generatedYaml: String? = null,

    /** Exactly what the editor shows; Apply sends this text verbatim. */
    val reviewedYaml: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface CreateServiceUiAction {
    data class NameChanged(val value: String) : CreateServiceUiAction
    data class SelectorAppChanged(val value: String) : CreateServiceUiAction
    data class PortChanged(val value: String) : CreateServiceUiAction
    data class TargetPortChanged(val value: String) : CreateServiceUiAction
    data class ServiceTypeSelected(val value: String) : CreateServiceUiAction
    data class NamespaceSelected(val namespace: String) : CreateServiceUiAction
    data object PreviewSubmitted : CreateServiceUiAction
    data object BackToFormClicked : CreateServiceUiAction
    data object ApplySubmitted : CreateServiceUiAction
    data class ReviewedYamlChanged(val value: String) : CreateServiceUiAction
    data object DismissError : CreateServiceUiAction
    data object CreateNamespaceClicked : CreateServiceUiAction
    data object DismissCreateNamespaceClicked : CreateServiceUiAction
    data class NewNamespaceNameChanged(val value: String) : CreateServiceUiAction
    data object CreateNamespaceSubmitted : CreateServiceUiAction
}

sealed interface CreateServiceUiEffect {
    data class Created(val serviceName: String) : CreateServiceUiEffect
    data object NamespaceCreated : CreateServiceUiEffect
}
