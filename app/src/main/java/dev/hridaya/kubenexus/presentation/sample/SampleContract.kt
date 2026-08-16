package dev.hridaya.kubenexus.presentation.sample

import androidx.compose.runtime.Immutable
import dev.hridaya.kubenexus.domain.model.SampleItem

@Immutable
data class SampleUiState(
    val items: List<SampleItem> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSaving: Boolean = false,
    val showAddDialog: Boolean = false,
    val errorMessage: String? = null
)

sealed interface SampleUiAction {
    data object RefreshTriggered : SampleUiAction
    data object AddItemClicked : SampleUiAction
    data object DismissAddDialog : SampleUiAction
    data class SaveItemSubmitted(val title: String, val description: String) : SampleUiAction
    data class ItemClicked(val id: String) : SampleUiAction
    data object RetryClicked : SampleUiAction
    data object ClearError : SampleUiAction
}

sealed interface SampleUiEffect {
    data class ShowSnackbar(val message: String) : SampleUiEffect
    data class NavigateToDetails(val id: String) : SampleUiEffect
}
