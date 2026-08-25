package dev.hridaya.kubenexus.presentation.deployments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.DeploymentDraft
import dev.hridaya.kubenexus.domain.usecase.CreateDeploymentUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Guided Deployment creation (issue #5). The cluster id and starting namespace
 * are supplied by the host ([dev.hridaya.kubenexus.presentation.main.MainScreen])
 * from the Home state at creation time, mirroring the PodDetailViewModel
 * assisted pattern.
 */
@HiltViewModel(assistedFactory = CreateDeploymentViewModel.Factory::class)
class CreateDeploymentViewModel @AssistedInject constructor(
    @Assisted("clusterId") private val clusterId: String?,
    @Assisted("namespace") initialNamespace: String,
    private val createDeploymentUseCase: CreateDeploymentUseCase,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("clusterId") clusterId: String?,
            @Assisted("namespace") namespace: String,
        ): CreateDeploymentViewModel
    }

    private val _uiState = MutableStateFlow(
        validated(
            CreateDeploymentUiState(namespace = initialNamespace.ifBlank { "default" }),
        ),
    )
    val uiState: StateFlow<CreateDeploymentUiState> = _uiState.asStateFlow()

    private val _effects = Channel<CreateDeploymentUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onAction(action: CreateDeploymentUiAction) {
        when (action) {
            is CreateDeploymentUiAction.NameChanged -> reduceField { it.copy(name = action.value) }
            is CreateDeploymentUiAction.ImageChanged -> reduceField { it.copy(image = action.value) }
            is CreateDeploymentUiAction.ReplicasChanged -> reduceField { it.copy(replicas = action.value) }
            is CreateDeploymentUiAction.ContainerPortChanged -> reduceField { it.copy(containerPort = action.value) }
            is CreateDeploymentUiAction.NamespaceSelected -> reduceField { it.copy(namespace = action.namespace) }

            is CreateDeploymentUiAction.PreviewSubmitted -> previewYaml()

            is CreateDeploymentUiAction.BackToFormClicked -> _uiState.update {
                it.copy(step = CreateDeploymentStep.FORM, errorMessage = null)
            }

            is CreateDeploymentUiAction.ApplySubmitted -> applyReviewedYaml()

            is CreateDeploymentUiAction.DismissError -> _uiState.update {
                it.copy(errorMessage = null)
            }
        }
    }

    /**
     * Live validation: every edit revalidates the whole draft so errors clear as
     * the user types. Input is never rewritten or discarded on failure.
     */
    private fun reduceField(transform: (CreateDeploymentUiState) -> CreateDeploymentUiState) {
        _uiState.update { validated(transform(it)) }
    }

    private fun previewYaml() {
        val state = _uiState.value
        if (state.fieldErrors.isNotEmpty()) {
            _uiState.update { it.copy(errorMessage = "Fix the highlighted fields to continue") }
            return
        }
        val draft = buildDraft(state)
        when (val result = createDeploymentUseCase.previewYaml(draft)) {
            is Result.Success -> _uiState.update {
                it.copy(
                    step = CreateDeploymentStep.REVIEW,
                    generatedYaml = result.data,
                    errorMessage = null,
                )
            }

            is Result.Error -> _uiState.update {
                it.copy(errorMessage = result.error.message)
            }

            is Result.Loading -> Unit
        }
    }

    /**
     * Applies exactly the reviewed manifest — never regenerates from the form —
     * so the cluster can only receive what the user confirmed. On failure the
     * REVIEW step, generated YAML and all form input are kept intact.
     */
    private fun applyReviewedYaml() {
        val state = _uiState.value
        if (state.isSubmitting || state.generatedYaml == null) return
        val reviewedYaml = state.generatedYaml ?: return
        val deploymentName = state.name

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = createDeploymentUseCase(clusterId, reviewedYaml)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.send(CreateDeploymentUiEffect.Created(deploymentName))
                }

                is Result.Error -> _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = result.error.message)
                }

                is Result.Loading -> Unit
            }
        }
    }

    private fun validated(state: CreateDeploymentUiState): CreateDeploymentUiState {
        return state.copy(fieldErrors = draftWithErrors(state).second)
    }

    private fun buildDraft(state: CreateDeploymentUiState): DeploymentDraft {
        return draftWithErrors(state).first
    }

    /** Parses replicas/port safely; unparseable text becomes a field error instead of a crash. */
    private fun draftWithErrors(state: CreateDeploymentUiState): Pair<DeploymentDraft, Map<String, String>> {
        val parsedReplicas = state.replicas.trim().toIntOrNull()
        val parsedContainerPort = state.containerPort.trim().toIntOrNull()
        val draft = DeploymentDraft(
            name = state.name,
            namespace = state.namespace,
            image = state.image,
            replicas = parsedReplicas ?: DeploymentDraft.DEFAULT_REPLICAS,
            containerPort = parsedContainerPort ?: DeploymentDraft.DEFAULT_CONTAINER_PORT,
        )
        val errors = buildMap {
            if (state.replicas.isNotBlank() && parsedReplicas == null) {
                put("replicas", "Replicas must be a whole number")
            }
            if (state.containerPort.isNotBlank() && parsedContainerPort == null) {
                put("containerPort", "Port must be a whole number")
            }
            putAll(draft.validate())
        }
        return draft to errors
    }
}
