package dev.hridaya.kubenexus.presentation.logcat

import dev.hridaya.kubenexus.domain.model.LogLevel
import dev.hridaya.kubenexus.domain.model.LogcatEntry

data class LogcatUiState(
    val logs: List<LogcatEntry> = emptyList(),
    val filteredLogs: List<LogcatEntry> = emptyList(),
    val searchQuery: String = "",
    val selectedLogLevel: LogLevel? = null,
    val autoScroll: Boolean = true,
    val isPaused: Boolean = false,
    val isLoading: Boolean = true,
    val isSearchExpanded: Boolean = false,
    val levelCounts: Map<LogLevel, Int> = emptyMap(),
)

sealed interface LogcatUiAction {
    data class UpdateSearchQuery(val query: String) : LogcatUiAction
    data class SelectLogLevel(val level: LogLevel?) : LogcatUiAction
    data object ToggleAutoScroll : LogcatUiAction
    data object TogglePause : LogcatUiAction
    data object ToggleSearch : LogcatUiAction
    data object ClearLogs : LogcatUiAction
    data object RefreshLogs : LogcatUiAction
    data object ShareLogs : LogcatUiAction
    data object CopyLogs : LogcatUiAction
    data class CopyLogEntry(val entry: LogcatEntry) : LogcatUiAction
}

sealed interface LogcatUiEvent {
    data class ShowMessage(val message: String) : LogcatUiEvent
    data class ShareText(val text: String) : LogcatUiEvent
    data class CopyToClipboard(val text: String, val label: String) : LogcatUiEvent
}
