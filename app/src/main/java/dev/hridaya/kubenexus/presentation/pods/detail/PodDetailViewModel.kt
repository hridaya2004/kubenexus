package dev.hridaya.kubenexus.presentation.pods.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.core.di.AppContainer
import dev.hridaya.kubenexus.domain.usecase.DescribePodUseCase
import dev.hridaya.kubenexus.domain.usecase.GetActiveClusterUseCase
import dev.hridaya.kubenexus.domain.usecase.GetPodLogsUseCase
import dev.hridaya.kubenexus.domain.usecase.StreamPodLogsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PodDetailViewModel(
    private val podName: String,
    private val namespace: String,
    private val getActiveClusterUseCase: GetActiveClusterUseCase,
    private val describePodUseCase: DescribePodUseCase,
    private val getPodLogsUseCase: GetPodLogsUseCase,
    private val streamPodLogsUseCase: StreamPodLogsUseCase,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PodDetailUiState(
            podName = podName,
            namespace = namespace
        )
    )
    val uiState: StateFlow<PodDetailUiState> = _uiState.asStateFlow()

    private val _effects = Channel<PodDetailUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var activeClusterId: String? = null
    private var streamJob: Job? = null

    init {
        loadClusterAndDescribe()
    }

    private fun loadClusterAndDescribe() {
        viewModelScope.launch(dispatcherProvider.main) {
            getActiveClusterUseCase().collect { cluster ->
                activeClusterId = cluster?.id
                _uiState.update { it.copy(clusterId = cluster?.id) }
                if (cluster != null) {
                    fetchDescribe()
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "No active Kubernetes cluster configured."
                        )
                    }
                }
            }
        }
    }

    fun onAction(action: PodDetailUiAction) {
        when (action) {
            is PodDetailUiAction.RefreshDescribe -> {
                fetchDescribe()
            }

            is PodDetailUiAction.SelectTab -> {
                _uiState.update { it.copy(selectedTab = action.tab) }
                if (action.tab == PodDetailTab.LOGS && _uiState.value.logs.isEmpty()) {
                    fetchLogs()
                }
            }

            is PodDetailUiAction.SelectContainer -> {
                _uiState.update { it.copy(selectedContainer = action.containerName) }
                if (_uiState.value.isStreamingLogs) {
                    startStreaming()
                } else {
                    fetchLogs()
                }
            }

            is PodDetailUiAction.FetchLogs -> {
                stopStreaming()
                fetchLogs()
            }

            is PodDetailUiAction.StartStreamingLogs -> {
                startStreaming()
            }

            is PodDetailUiAction.StopStreamingLogs -> {
                stopStreaming()
            }

            is PodDetailUiAction.ClearLogs -> {
                _uiState.update { it.copy(logs = emptyList()) }
            }
        }
    }

    private fun fetchDescribe() {
        val cid = activeClusterId ?: return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch(dispatcherProvider.main) {
            when (val result = describePodUseCase(cid, namespace, podName)) {
                is Result.Success -> {
                    val details = result.data
                    val defaultContainer = details.containers.firstOrNull()?.name
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            podDetails = details,
                            selectedContainer = it.selectedContainer ?: defaultContainer
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
                    _effects.send(PodDetailUiEffect.ShowToast("Failed to describe pod: ${result.error.message}"))
                }

                is Result.Loading -> Unit
            }
        }
    }

    private fun fetchLogs() {
        val cid = activeClusterId ?: return
        val container = _uiState.value.selectedContainer
        _uiState.update { it.copy(isLoadingLogs = true) }

        viewModelScope.launch(dispatcherProvider.main) {
            when (val result = getPodLogsUseCase(cid, namespace, podName, container)) {
                is Result.Success -> {
                    val lines = result.data.lines()
                    _uiState.update {
                        it.copy(
                            isLoadingLogs = false,
                            logs = lines
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingLogs = false,
                            logs = listOf("Error fetching logs: ${result.error.message}")
                        )
                    }
                }

                is Result.Loading -> Unit
            }
        }
    }

    private fun startStreaming() {
        stopStreaming()
        val cid = activeClusterId ?: return
        val container = _uiState.value.selectedContainer

        _uiState.update {
            it.copy(
                isStreamingLogs = true,
                isLoadingLogs = false
            )
        }

        streamJob = viewModelScope.launch(dispatcherProvider.main) {
            streamPodLogsUseCase(cid, namespace, podName, container)
                .onStart {
                    _uiState.update { it.copy(logs = it.logs + "[Streaming logs initiated for container '${container ?: "default"}']...") }
                }
                .catch { t ->
                    _uiState.update {
                        it.copy(
                            isStreamingLogs = false,
                            logs = it.logs + "[Log stream closed: ${t.message}]"
                        )
                    }
                }
                .collect { line ->
                    _uiState.update {
                        it.copy(logs = it.logs + line)
                    }
                }
        }
    }

    private fun stopStreaming() {
        streamJob?.cancel()
        streamJob = null
        _uiState.update { it.copy(isStreamingLogs = false) }
    }

    override fun onCleared() {
        super.onCleared()
        stopStreaming()
    }

    companion object {
        fun provideFactory(
            podName: String,
            namespace: String,
            container: AppContainer
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PodDetailViewModel(
                    podName = podName,
                    namespace = namespace,
                    getActiveClusterUseCase = container.getActiveClusterUseCase,
                    describePodUseCase = container.describePodUseCase,
                    getPodLogsUseCase = container.getPodLogsUseCase,
                    streamPodLogsUseCase = container.streamPodLogsUseCase,
                    dispatcherProvider = container.dispatcherProvider
                ) as T
            }
        }
    }
}
