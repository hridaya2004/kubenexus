package dev.hridaya.kubenexus.presentation.services

import dev.hridaya.kubenexus.domain.model.ServiceSummary

/** Sentinel namespace filter that lists services across all namespaces. */
const val ALL_NAMESPACES_FILTER = "All Namespaces"

data class ServicesUiState(
    val isLoading: Boolean = true,
    val services: List<ServiceSummary> = emptyList(),
    val availableNamespaces: List<String> = listOf(
        ALL_NAMESPACES_FILTER,
        "default",
        "kube-system",
        "monitoring",
    ),
    val selectedNamespace: String = ALL_NAMESPACES_FILTER,
    val lastRefreshedAt: Long? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface ServicesUiAction {
    data object Refresh : ServicesUiAction
    data class SelectNamespace(val namespace: String) : ServicesUiAction
}
