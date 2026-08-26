package dev.hridaya.kubenexus.presentation.logcat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.WrapText
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import dev.hridaya.kubenexus.presentation.logcat.LogcatUiAction
import dev.hridaya.kubenexus.presentation.logcat.LogcatUiState
import dev.hridaya.kubenexus.ui.theme.KubeNexusTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogcatTopBar(
    uiState: LogcatUiState,
    wrapLines: Boolean,
    onToggleWrapLines: () -> Unit,
    onAction: (LogcatUiAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Logcat",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            },
            actions = {
                IconButton(onClick = { onAction(LogcatUiAction.ToggleSearch) }) {
                    Icon(
                        imageVector = if (uiState.isSearchExpanded) Icons.Outlined.Close else Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = if (uiState.isSearchExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onToggleWrapLines) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.WrapText,
                        contentDescription = "Wrap lines",
                        tint = if (wrapLines) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAction(LogcatUiAction.TogglePause)
                }) {
                    Icon(
                        imageVector = if (uiState.isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                        contentDescription = if (uiState.isPaused) "Resume" else "Pause",
                        tint = if (uiState.isPaused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAction(LogcatUiAction.ClearLogs)
                }) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Clear Logs",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                LogcatOverflowMenu(
                    onCopyLogs = { onAction(LogcatUiAction.CopyLogs) },
                    onShareLogs = { onAction(LogcatUiAction.ShareLogs) },
                    onRefreshLogs = { onAction(LogcatUiAction.RefreshLogs) },
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
        )

        LogcatSearchBar(
            isExpanded = uiState.isSearchExpanded,
            searchQuery = uiState.searchQuery,
            onQueryChange = { newSearchQuery ->
                onAction(LogcatUiAction.UpdateSearchQuery(newSearchQuery))
            },
        )

        LogLevelFilterChipsRow(
            selectedLogLevel = uiState.selectedLogLevel,
            logCount = uiState.logs.size,
            levelCounts = uiState.levelCounts,
            onSelectLogLevel = { level ->
                onAction(LogcatUiAction.SelectLogLevel(level))
            },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Preview(showBackground = true)
@Composable
private fun LogcatTopBarPreview() {
    KubeNexusTheme {
        LogcatTopBar(
            uiState = LogcatUiState(),
            wrapLines = true,
            onToggleWrapLines = {},
            onAction = {},
            onNavigateBack = {},
        )
    }
}
