package dev.hridaya.kubenexus.presentation.deployments

import dev.hridaya.kubenexus.domain.model.DeploymentSummary

/** Filter chip shown first; maps to a null (all-namespaces) argument downstream. */
const val ALL_NAMESPACES_FILTER = "All Namespaces"

data class DeploymentsUiState(
    val isLoading: Boolean = true,
    val deployments: List<DeploymentSummary> = emptyList(),
    val errorMessage: String? = null,
    val isSyncing: Boolean = false,
    val lastSyncedAt: Long? = null,
    val selectedNamespace: String = ALL_NAMESPACES_FILTER,
    val namespaces: List<String> = listOf(ALL_NAMESPACES_FILTER),
    val isOnline: Boolean = true,
)

sealed interface DeploymentsUiAction {
    data object Refresh : DeploymentsUiAction
    data class SelectNamespace(val namespace: String) : DeploymentsUiAction
}
