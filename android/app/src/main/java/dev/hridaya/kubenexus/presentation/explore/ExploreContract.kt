package dev.hridaya.kubenexus.presentation.explore

import dev.hridaya.kubenexus.domain.model.APIResource
import dev.hridaya.kubenexus.domain.model.Cluster
import dev.hridaya.kubenexus.domain.model.ResourceExplain
import dev.hridaya.kubenexus.domain.model.ResourceField

data class ExploreUiState(
    val resources: List<APIResource> = emptyList(),
    val filteredResources: List<APIResource> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val availableCategories: List<String> = listOf(
        "All",
        "Namespaced",
        "Cluster",
        "Core (v1)",
        "Apps",
        "Batch",
        "Networking",
    ),
    val isSearchActive: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val activeCluster: Cluster? = null,
    val selectedResource: APIResource? = null,
    val explainDetails: ResourceExplain? = null,
    val fieldSearchQuery: String = "",
    val filteredFields: List<ResourceField> = emptyList(),
    val isLoadingExplain: Boolean = false,
    val explainError: String? = null,
    val errorMessage: String? = null,
    val lastRefreshedAt: Long? = null,
)

sealed interface ExploreUiAction {
    data object OpenSearch : ExploreUiAction
    data object CloseSearch : ExploreUiAction
    data class UpdateSearchQuery(val query: String) : ExploreUiAction
    data class SelectCategory(val category: String) : ExploreUiAction
    data object Refresh : ExploreUiAction
    data class SelectResource(val resource: APIResource) : ExploreUiAction
    data object DismissExplain : ExploreUiAction
    data class UpdateFieldSearchQuery(val query: String) : ExploreUiAction
    data class RetryExplain(val resource: APIResource) : ExploreUiAction
    data class CopyText(val text: String, val label: String) : ExploreUiAction
}


sealed interface ExploreUiEvent {
    data class ShowMessage(val message: String) : ExploreUiEvent
    data class CopyToClipboard(val text: String, val label: String) : ExploreUiEvent
}
