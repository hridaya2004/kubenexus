package dev.hridaya.kubenexus.presentation.logcat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.WrapText
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hridaya.kubenexus.domain.model.LogLevel
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
    val focusManager = LocalFocusManager.current
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(GhosttySurface),
    ) {
        TopAppBar(
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (uiState.isPaused) GhosttyYellow else GhosttyGreen,
                                    CircleShape,
                                ),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isPaused) "ghostty (logcat paused)" else "ghostty (logcat stream)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GhosttyText,
                        )
                    }
                    Text(
                        text = "dev.hridaya.kubenexus • ${uiState.filteredLogs.size} lines",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = GhosttyGutter,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = GhosttyText,
                    )
                }
            },
            actions = {
                IconButton(onClick = { onAction(LogcatUiAction.ToggleSearch) }) {
                    Icon(
                        imageVector = if (uiState.isSearchExpanded) Icons.Outlined.Close else Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = if (uiState.isSearchExpanded) GhosttyCyan else GhosttyText,
                    )
                }
                IconButton(onClick = onToggleWrapLines) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.WrapText,
                        contentDescription = "Wrap lines",
                        tint = if (wrapLines) GhosttyGreen else GhosttyGutter,
                    )
                }
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAction(LogcatUiAction.TogglePause)
                }) {
                    Icon(
                        imageVector = if (uiState.isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                        contentDescription = if (uiState.isPaused) "Resume" else "Pause",
                        tint = if (uiState.isPaused) GhosttyYellow else GhosttyGreen,
                    )
                }
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAction(LogcatUiAction.ClearLogs)
                }) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Clear Logs",
                        tint = GhosttyText,
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "More Options",
                            tint = GhosttyText,
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Copy All") },
                            leadingIcon = {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                            },
                            onClick = {
                                showMenu = false
                                onAction(LogcatUiAction.CopyLogs)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Share Logs") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Share, contentDescription = null)
                            },
                            onClick = {
                                showMenu = false
                                onAction(LogcatUiAction.ShareLogs)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Refresh Dump") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Refresh, contentDescription = null)
                            },
                            onClick = {
                                showMenu = false
                                onAction(LogcatUiAction.RefreshLogs)
                            },
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = GhosttySurface,
            ),
        )

        AnimatedVisibility(
            visible = uiState.isSearchExpanded,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { onAction(LogcatUiAction.UpdateSearchQuery(it)) },
                placeholder = {
                    Text(
                        "Filter by tag, message, or keyword...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = GhosttyGutter,
                    )
                },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = GhosttyCyan)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onAction(LogcatUiAction.UpdateSearchQuery("")) }) {
                            Icon(
                                Icons.Outlined.Clear,
                                contentDescription = "Clear query",
                                tint = GhosttyText
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = GhosttyText,
                    unfocusedTextColor = GhosttyText,
                    focusedBorderColor = GhosttyCyan,
                    unfocusedBorderColor = GhosttyGutter,
                    focusedContainerColor = GhosttyBg,
                    unfocusedContainerColor = GhosttyBg,
                ),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = uiState.selectedLogLevel == null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAction(LogcatUiAction.SelectLogLevel(null))
                },
                label = {
                    Text(
                        "All (${uiState.logs.size})",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GhosttyBorderHighlight,
                    selectedLabelColor = GhosttyText,
                ),
            )

            val levels = listOf(
                LogLevel.VERBOSE,
                LogLevel.DEBUG,
                LogLevel.INFO,
                LogLevel.WARN,
                LogLevel.ERROR,
                LogLevel.FATAL,
            )

            levels.forEach { level ->
                val count = uiState.levelCounts[level] ?: 0
                FilterChip(
                    selected = uiState.selectedLogLevel == level,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAction(LogcatUiAction.SelectLogLevel(if (uiState.selectedLogLevel == level) null else level))
                    },
                    label = {
                        Text(
                            "${level.code} ($count)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                        )
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(getLogLevelColor(level), CircleShape),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GhosttyBorderHighlight,
                        selectedLabelColor = GhosttyText,
                    ),
                )
            }
        }
        HorizontalDivider(color = GhosttySurface)
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
