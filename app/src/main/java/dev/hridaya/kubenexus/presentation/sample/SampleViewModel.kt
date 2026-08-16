package dev.hridaya.kubenexus.presentation.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.core.di.AppContainer
import dev.hridaya.kubenexus.domain.usecase.AddSampleItemUseCase
import dev.hridaya.kubenexus.domain.usecase.GetSampleItemsUseCase
import dev.hridaya.kubenexus.domain.usecase.RefreshSampleItemsUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SampleViewModel(
    private val getSampleItemsUseCase: GetSampleItemsUseCase,
    private val refreshSampleItemsUseCase: RefreshSampleItemsUseCase,
    private val addSampleItemUseCase: AddSampleItemUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SampleUiState())
    val uiState: StateFlow<SampleUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SampleUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        observeItems()
        refresh()
    }

    fun onAction(action: SampleUiAction) {
        when (action) {
            is SampleUiAction.RefreshTriggered -> refresh()
            is SampleUiAction.AddItemClicked -> _uiState.update { it.copy(showAddDialog = true) }
            is SampleUiAction.DismissAddDialog -> _uiState.update { it.copy(showAddDialog = false) }
            is SampleUiAction.SaveItemSubmitted -> addItem(action.title, action.description)
            is SampleUiAction.ItemClicked -> {
                viewModelScope.launch {
                    _effects.send(SampleUiEffect.NavigateToDetails(action.id))
                }
            }

            is SampleUiAction.RetryClicked -> {
                _uiState.update { it.copy(errorMessage = null, isLoading = true) }
                refresh()
            }

            is SampleUiAction.ClearError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun observeItems() {
        viewModelScope.launch {
            getSampleItemsUseCase().collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                items = result.data,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    }

                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.error.message
                            )
                        }
                    }

                    is Result.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val result = refreshSampleItemsUseCase()
            _uiState.update { it.copy(isRefreshing = false) }

            if (result is Result.Error) {
                _effects.send(SampleUiEffect.ShowSnackbar(result.error.message))
            }
        }
    }

    private fun addItem(title: String, description: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = addSampleItemUseCase(title, description)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false, showAddDialog = false) }
                    _effects.send(SampleUiEffect.ShowSnackbar("Item '${result.data.title}' added."))
                }

                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _effects.send(SampleUiEffect.ShowSnackbar(result.error.message))
                }

                is Result.Loading -> Unit
            }
        }
    }

    companion object {
        fun provideFactory(appContainer: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SampleViewModel(
                        getSampleItemsUseCase = appContainer.getSampleItemsUseCase,
                        refreshSampleItemsUseCase = appContainer.refreshSampleItemsUseCase,
                        addSampleItemUseCase = appContainer.addSampleItemUseCase
                    ) as T
                }
            }
    }
}
