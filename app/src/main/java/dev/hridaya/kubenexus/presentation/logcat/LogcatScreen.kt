package dev.hridaya.kubenexus.presentation.logcat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hridaya.kubenexus.domain.model.LogLevel
import dev.hridaya.kubenexus.domain.model.LogcatEntry
import kotlinx.coroutines.launch

private val LogVerboseColor = Color(0xFF9E9E9E)
private val LogDebugColor = Color(0xFF42A5F5)
private val LogInfoColor = Color(0xFF66BB6A)
private val LogWarnColor = Color(0xFFFFA726)
private val LogErrorColor = Color(0xFFEF5350)
private val LogFatalColor = Color(0xFFAB47BC)
private val LogTagColor = Color(0xFF26A69A)
private val TerminalBackground = Color(0xFF0D1117)

@Composable
fun LogcatRoute(
    viewModel: LogcatViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LogcatUiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }

                is LogcatUiEvent.ShareText -> {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, event.text)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Share Logcat Logs")
                    context.startActivity(shareIntent)
                }

                is LogcatUiEvent.CopyToClipboard -> {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText(event.label, event.text)
                    clipboard.setPrimaryClip(clip)
                }
            }
        }
    }

    LogcatScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogcatScreen(
    uiState: LogcatUiState,
    onAction: (LogcatUiAction) -> Unit,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.filteredLogs.size, uiState.autoScroll) {
        if (uiState.autoScroll && uiState.filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(uiState.filteredLogs.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Logcat",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "dev.hridaya.kubenexus • ${uiState.filteredLogs.size} lines",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { onAction(LogcatUiAction.ToggleSearch) }) {
                            Icon(
                                imageVector = if (uiState.isSearchExpanded) Icons.Outlined.Close else Icons.Outlined.Search,
                                contentDescription = "Search"
                            )
                        }
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAction(LogcatUiAction.TogglePause)
                        }) {
                            Icon(
                                imageVector = if (uiState.isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                                contentDescription = if (uiState.isPaused) "Resume" else "Pause",
                                tint = if (uiState.isPaused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAction(LogcatUiAction.ClearLogs)
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Clear Logs"
                            )
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "More Options"
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Copy All") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onAction(LogcatUiAction.CopyLogs)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share Logs") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Share, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onAction(LogcatUiAction.ShareLogs)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Refresh Dump") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onAction(LogcatUiAction.RefreshLogs)
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )

                AnimatedVisibility(
                    visible = uiState.isSearchExpanded,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { onAction(LogcatUiAction.UpdateSearchQuery(it)) },
                        placeholder = { Text("Filter by tag, message, or keyword...") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Outlined.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onAction(LogcatUiAction.UpdateSearchQuery("")) }) {
                                    Icon(Icons.Outlined.Clear, contentDescription = "Clear query")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.selectedLogLevel == null,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAction(LogcatUiAction.SelectLogLevel(null))
                        },
                        label = { Text("All (${uiState.logs.size})") },
                        colors = FilterChipDefaults.filterChipColors()
                    )

                    val levels = listOf(
                        LogLevel.VERBOSE,
                        LogLevel.DEBUG,
                        LogLevel.INFO,
                        LogLevel.WARN,
                        LogLevel.ERROR,
                        LogLevel.FATAL
                    )

                    levels.forEach { level ->
                        val count = uiState.levelCounts[level] ?: 0
                        FilterChip(
                            selected = uiState.selectedLogLevel == level,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAction(LogcatUiAction.SelectLogLevel(if (uiState.selectedLogLevel == level) null else level))
                            },
                            label = { Text("${level.code} ($count)") },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(getLogLevelColor(level), CircleShape)
                                )
                            }
                        )
                    }
                }
                HorizontalDivider()
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Auto-scroll",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = uiState.autoScroll,
                            onCheckedChange = { onAction(LogcatUiAction.ToggleAutoScroll) },
                            colors = SwitchDefaults.colors(),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (uiState.filteredLogs.isNotEmpty()) listState.animateScrollToItem(
                                        0
                                    )
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowUpward,
                                contentDescription = "Scroll to top",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (uiState.filteredLogs.isNotEmpty()) {
                                        listState.animateScrollToItem(uiState.filteredLogs.size - 1)
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowDownward,
                                contentDescription = "Scroll to bottom",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(TerminalBackground)
        ) {
            if (uiState.isLoading && uiState.logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.searchQuery.isNotEmpty() || uiState.selectedLogLevel != null) {
                            "No logs match your filter"
                        } else {
                            "No logcat entries available"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(
                        items = uiState.filteredLogs,
                        key = { it.id }
                    ) { entry ->
                        LogcatEntryRow(
                            entry = entry,
                            onCopy = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAction(LogcatUiAction.CopyLogEntry(entry))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogcatEntryRow(
    entry: LogcatEntry,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val levelColor = getLogLevelColor(entry.level)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCopy)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (entry.timestamp.isNotBlank()) {
            Text(
                text = entry.timestamp.takeLast(12),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Color.Gray,
                maxLines = 1,
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .background(levelColor.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
                .padding(horizontal = 3.dp, vertical = 1.dp)
        ) {
            Text(
                text = entry.level.code,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = levelColor
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        if (entry.tag.isNotBlank()) {
            Text(
                text = "${entry.tag}:",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = LogTagColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        Text(
            text = entry.message,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = Color(0xFFECEFF1),
            lineHeight = 15.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun getLogLevelColor(level: LogLevel): Color = when (level) {
    LogLevel.VERBOSE -> LogVerboseColor
    LogLevel.DEBUG -> LogDebugColor
    LogLevel.INFO -> LogInfoColor
    LogLevel.WARN -> LogWarnColor
    LogLevel.ERROR -> LogErrorColor
    LogLevel.FATAL -> LogFatalColor
    LogLevel.UNKNOWN -> LogVerboseColor
}
