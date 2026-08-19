package dev.hridaya.kubenexus.presentation.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.core.di.AppContainer
import dev.hridaya.kubenexus.domain.model.APIResource
import dev.hridaya.kubenexus.domain.model.ResourceField
import dev.hridaya.kubenexus.domain.usecase.ExplainResourceUseCase
import dev.hridaya.kubenexus.domain.usecase.GetActiveClusterUseCase
import dev.hridaya.kubenexus.domain.usecase.GetAPIResourcesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExploreViewModel(
    private val getActiveClusterUseCase: GetActiveClusterUseCase,
    private val getAPIResourcesUseCase: GetAPIResourcesUseCase,
    private val explainResourceUseCase: ExplainResourceUseCase,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private val _events = Channel<ExploreUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var streamJob: Job? = null
    private var lastRefreshedJob: Job? = null
    private var explainJob: Job? = null

    init {
        observeActiveCluster()
    }

    private fun observeActiveCluster() {
        viewModelScope.launch(dispatcherProvider.main) {
            getActiveClusterUseCase().collectLatest { cluster ->
                _uiState.update { it.copy(activeCluster = cluster) }
                subscribeToResources(cluster?.id)
                subscribeToLastRefreshed(cluster?.id)
                refreshResources(cluster?.id)
            }
        }
    }

    private fun subscribeToResources(clusterId: String?) {
        streamJob?.cancel()
        streamJob = viewModelScope.launch(dispatcherProvider.main) {
            getAPIResourcesUseCase.getStream(clusterId).collectLatest { list ->
                _uiState.update { state ->
                    val filtered = applyFilter(list, state.searchQuery, state.selectedCategory)
                    state.copy(
                        resources = list,
                        filteredResources = filtered,
                    )
                }
            }
        }
    }

    private fun subscribeToLastRefreshed(clusterId: String?) {
        lastRefreshedJob?.cancel()
        lastRefreshedJob = viewModelScope.launch(dispatcherProvider.main) {
            getAPIResourcesUseCase.getLastRefreshedStream(clusterId).collectLatest { timestamp ->
                _uiState.update { it.copy(lastRefreshedAt = timestamp) }
            }
        }
    }

    fun onAction(action: ExploreUiAction) {
        when (action) {
            is ExploreUiAction.OpenSearch -> {
                _uiState.update { it.copy(isSearchActive = true) }
            }

            is ExploreUiAction.CloseSearch -> {
                _uiState.update { state ->
                    val filtered = applyFilter(state.resources, "", state.selectedCategory)
                    state.copy(
                        isSearchActive = false,
                        searchQuery = "",
                        filteredResources = filtered,
                    )
                }
            }

            is ExploreUiAction.UpdateSearchQuery -> {
                _uiState.update { state ->
                    val filtered = applyFilter(state.resources, action.query, state.selectedCategory)
                    state.copy(
                        searchQuery = action.query,
                        filteredResources = filtered,
                    )
                }
            }


            is ExploreUiAction.SelectCategory -> {
                _uiState.update { state ->
                    val filtered = applyFilter(state.resources, state.searchQuery, action.category)
                    state.copy(
                        selectedCategory = action.category,
                        filteredResources = filtered,
                    )
                }
            }

            is ExploreUiAction.Refresh -> {
                refreshResources(_uiState.value.activeCluster?.id)
            }

            is ExploreUiAction.SelectResource -> {
                loadExplain(action.resource)
            }

            is ExploreUiAction.DismissExplain -> {
                explainJob?.cancel()
                _uiState.update {
                    it.copy(
                        selectedResource = null,
                        explainDetails = null,
                        fieldSearchQuery = "",
                        filteredFields = emptyList(),
                        explainError = null,
                    )
                }
            }

            is ExploreUiAction.UpdateFieldSearchQuery -> {
                _uiState.update { state ->
                    val fields = state.explainDetails?.fields ?: emptyList()
                    val filtered = filterFields(fields, action.query)
                    state.copy(
                        fieldSearchQuery = action.query,
                        filteredFields = filtered,
                    )
                }
            }

            is ExploreUiAction.RetryExplain -> {
                loadExplain(action.resource)
            }

            is ExploreUiAction.CopyText -> {
                viewModelScope.launch {
                    _events.send(ExploreUiEvent.CopyToClipboard(action.text, action.label))
                    _events.send(ExploreUiEvent.ShowMessage("Copied ${action.label} to clipboard"))
                }
            }
        }
    }

    private fun refreshResources(clusterId: String?) {
        viewModelScope.launch(dispatcherProvider.main) {
            _uiState.update { it.copy(isRefreshing = true, isLoading = it.resources.isEmpty(), errorMessage = null) }
            val result = getAPIResourcesUseCase.refresh(clusterId)
            when (result) {
                is Result.Success -> {
                    _uiState.update { state ->
                        val filtered = applyFilter(result.data, state.searchQuery, state.selectedCategory)
                        state.copy(
                            resources = result.data,
                            filteredResources = filtered,
                            isRefreshing = false,
                            isLoading = false,
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            isRefreshing = false,
                            isLoading = false,
                            errorMessage = result.error.message,
                        )
                    }
                    _events.send(ExploreUiEvent.ShowMessage(result.error.message ?: "Failed to refresh API resources"))
                }

                is Result.Loading -> Unit
            }
        }
    }

    private fun loadExplain(resource: APIResource) {
        explainJob?.cancel()
        val clusterId = _uiState.value.activeCluster?.id

        explainJob = viewModelScope.launch(dispatcherProvider.main) {
            // Immediately display cached schema from Room database if available
            val cached = explainResourceUseCase.getCached(clusterId, resource.name, resource.groupVersion)
            if (cached != null) {
                _uiState.update { state ->
                    state.copy(
                        selectedResource = resource,
                        explainDetails = cached,
                        filteredFields = cached.fields,
                        isLoadingExplain = false,
                        explainError = null,
                        fieldSearchQuery = "",
                    )
                }
            } else {
                _uiState.update { state ->
                    state.copy(
                        selectedResource = resource,
                        explainDetails = null,
                        isLoadingExplain = true,
                        explainError = null,
                        fieldSearchQuery = "",
                        filteredFields = emptyList(),
                    )
                }
            }

            // Fetch fresh from native / cluster and sync to Room database
            val result = explainResourceUseCase(clusterId, resource.name, resource.groupVersion)
            when (result) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            selectedResource = resource,
                            explainDetails = result.data,
                            filteredFields = filterFields(result.data.fields, state.fieldSearchQuery),
                            isLoadingExplain = false,
                            explainError = null,
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update { state ->
                        if (state.explainDetails == null) {
                            state.copy(
                                isLoadingExplain = false,
                                explainError = result.error.message ?: "Failed to load explanation",
                            )
                        } else {
                            state.copy(isLoadingExplain = false)
                        }
                    }
                    _events.send(
                        ExploreUiEvent.ShowMessage(
                            result.error.message ?: "Failed to refresh explanation for ${resource.kind}",
                        ),
                    )
                }

                is Result.Loading -> Unit
            }
        }
    }


    private fun applyFilter(
        resources: List<APIResource>,
        query: String,
        category: String,
    ): List<APIResource> {
        val trimmed = query.trim().lowercase()
        return resources.filter { r ->
            val matchesCategory = when (category) {
                "All" -> true
                "Namespaced" -> r.namespaced
                "Cluster" -> !r.namespaced
                "Core (v1)" -> r.group.isEmpty() || r.groupVersion == "v1"
                "Apps" -> r.group.equals("apps", ignoreCase = true)
                "Batch" -> r.group.equals("batch", ignoreCase = true)
                "Networking" -> r.group.contains("networking", ignoreCase = true)
                else -> true
            }

            val matchesQuery = if (trimmed.isEmpty()) {
                true
            } else {
                r.name.lowercase().contains(trimmed) ||
                    r.kind.lowercase().contains(trimmed) ||
                    r.groupVersion.lowercase().contains(trimmed) ||
                    r.singularName.lowercase().contains(trimmed) ||
                    r.shortNames.any { it.lowercase().contains(trimmed) } ||
                    r.categories.any { it.lowercase().contains(trimmed) }
            }

            matchesCategory && matchesQuery
        }
    }

    private fun filterFields(fields: List<ResourceField>, query: String): List<ResourceField> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return fields
        return fields.filter { field ->
            field.name.lowercase().contains(trimmed) ||
                field.type.lowercase().contains(trimmed) ||
                field.description.lowercase().contains(trimmed)
        }
    }

    companion object {
        fun provideFactory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ExploreViewModel(
                        getActiveClusterUseCase = container.getActiveClusterUseCase,
                        getAPIResourcesUseCase = container.getAPIResourcesUseCase,
                        explainResourceUseCase = container.explainResourceUseCase,
                        dispatcherProvider = container.dispatcherProvider,
                    ) as T
                }
            }
    }
}
