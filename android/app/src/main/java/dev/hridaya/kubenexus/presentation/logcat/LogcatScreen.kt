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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.WrapText
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hridaya.kubenexus.domain.model.LogLevel
import dev.hridaya.kubenexus.domain.model.LogcatEntry
import dev.hridaya.kubenexus.ui.theme.KubeNexusTheme
import androidx.compose.foundation.text.selection.SelectionContainer
import kotlinx.coroutines.launch

private val GhosttyBg = Color(0xFF000000)
private val GhosttySurface = Color(0xFF000000)
private val GhosttyText = Color(0xFFFFFFFF)
private val GhosttyGutter = Color(0xFF777777)
private val GhosttyGreen = Color(0xFF3FB950)
private val GhosttyYellow = Color(0xFFD29922)
private val GhosttyRed = Color(0xFFF85149)
private val GhosttyCyan = Color(0xFF58A6FF)
private val GhosttyPurple = Color(0xFFBC8CFF)
private val GhosttyBorderHighlight = Color(0xFF222222)

private val LogVerboseColor = Color(0xFF888888)
private val LogDebugColor = Color(0xFF58A6FF)
private val LogInfoColor = Color(0xFF3FB950)
private val LogWarnColor = Color(0xFFD29922)
private val LogErrorColor = Color(0xFFF85149)
private val LogFatalColor = Color(0xFFBC8CFF)
private val LogTagColor = Color(0xFFE0E0E0)

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
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogcatScreen(
    uiState: LogcatUiState,
    onAction: (LogcatUiAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var wrapLines by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.filteredLogs.size, uiState.autoScroll) {
        if (uiState.autoScroll && uiState.filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(uiState.filteredLogs.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GhosttyBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
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
                        IconButton(onClick = { wrapLines = !wrapLines }) {
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
                                    Icon(Icons.Outlined.Clear, contentDescription = "Clear query", tint = GhosttyText)
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
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
        },
        bottomBar = {
            Surface(
                color = GhosttySurface,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Auto-scroll",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = GhosttyText,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = uiState.autoScroll,
                            onCheckedChange = { onAction(LogcatUiAction.ToggleAutoScroll) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GhosttyGreen,
                                checkedTrackColor = GhosttySurface,
                            ),
                            modifier = Modifier.size(width = 36.dp, height = 20.dp),
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (uiState.filteredLogs.isNotEmpty()) {
                                        listState.animateScrollToItem(0)
                                    }
                                }
                            },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowUpward,
                                contentDescription = "Scroll to top",
                                tint = GhosttyText,
                                modifier = Modifier.size(16.dp),
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
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowDownward,
                                contentDescription = "Scroll to bottom",
                                tint = GhosttyText,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(GhosttyBg),
        ) {
            if (uiState.isLoading && uiState.logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = GhosttyCyan)
                }
            } else if (uiState.filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (uiState.searchQuery.isNotEmpty() || uiState.selectedLogLevel != null) {
                            "No logs match your filter"
                        } else {
                            "No logcat entries available"
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = GhosttyGutter,
                    )
                }
            } else {
                val horizontalScrollState = rememberScrollState()
                val listModifier = if (wrapLines) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxSize()
                        .horizontalScroll(horizontalScrollState)
                }

                SelectionContainer(modifier = listModifier) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        itemsIndexed(
                            items = uiState.filteredLogs,
                            key = { _, item -> item.id },
                        ) { index, entry ->
                            LogcatEntryRow(
                                index = index + 1,
                                entry = entry,
                                searchQuery = uiState.searchQuery,
                                onCopy = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onAction(LogcatUiAction.CopyLogEntry(entry))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogcatEntryRow(
    index: Int,
    entry: LogcatEntry,
    searchQuery: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val levelColor = getLogLevelColor(entry.level)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCopy)
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = index.toString().padStart(4, ' '),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = GhosttyGutter,
            modifier = Modifier.width(32.dp),
        )

        if (entry.timestamp.isNotBlank()) {
            Text(
                text = entry.timestamp.takeLast(12),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = GhosttyGutter,
                maxLines = 1,
                modifier = Modifier.padding(end = 4.dp),
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(2.dp))
                .background(levelColor.copy(alpha = 0.2f))
                .padding(horizontal = 3.dp, vertical = 0.5.dp),
        ) {
            Text(
                text = entry.level.code,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = levelColor,
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
                modifier = Modifier.padding(end = 4.dp),
            )
        }

        val annotatedMessage = remember(entry.message, searchQuery) {
            buildAnnotatedMessage(entry.message, searchQuery)
        }

        Text(
            text = annotatedMessage,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = GhosttyText,
            lineHeight = 15.sp,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

private fun buildAnnotatedMessage(message: String, query: String): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(message)
    val builder = AnnotatedString.Builder()
    var lastIndex = 0
    var index = message.indexOf(query, 0, ignoreCase = true)
    while (index >= 0) {
        builder.append(message.substring(lastIndex, index))
        builder.pushStyle(
            SpanStyle(
                background = GhosttyYellow.copy(alpha = 0.35f),
                color = Color.White,
                fontWeight = FontWeight.Bold,
            ),
        )
        builder.append(message.substring(index, index + query.length))
        builder.pop()
        lastIndex = index + query.length
        index = message.indexOf(query, lastIndex, ignoreCase = true)
    }
    if (lastIndex < message.length) {
        builder.append(message.substring(lastIndex))
    }
    return builder.toAnnotatedString()
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

@Preview(showBackground = true)
@Composable
private fun LogcatScreenPreview() {
    KubeNexusTheme {
        LogcatScreen(
            uiState = LogcatUiState(
                logs = listOf(
                    LogcatEntry(
                        id = 1L,
                        timestamp = "16:42:05.123",
                        pid = "1234",
                        tid = "5678",
                        level = LogLevel.INFO,
                        tag = "KubeNexusNative",
                        message = "Connected to Kubernetes API at https://10.0.0.1:6443",
                        raw = "08-19 16:42:05.123  1234  5678 I KubeNexusNative: Connected to Kubernetes API",
                    ),
                    LogcatEntry(
                        id = 2L,
                        timestamp = "16:42:05.150",
                        pid = "1234",
                        tid = "5678",
                        level = LogLevel.DEBUG,
                        tag = "GhosttyBridge",
                        message = "VT stream parser initialized with DEC mode 2027",
                        raw = "08-19 16:42:05.150  1234  5678 D GhosttyBridge: VT stream parser initialized with DEC mode 2027",
                    ),
                ),
                filteredLogs = listOf(
                    LogcatEntry(
                        id = 1L,
                        timestamp = "16:42:05.123",
                        pid = "1234",
                        tid = "5678",
                        level = LogLevel.INFO,
                        tag = "KubeNexusNative",
                        message = "Connected to Kubernetes API at https://10.0.0.1:6443",
                        raw = "08-19 16:42:05.123  1234  5678 I KubeNexusNative: Connected to Kubernetes API",
                    ),
                ),
                isLoading = false,
            ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}
