package dev.hridaya.kubenexus.presentation.logcat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hridaya.kubenexus.domain.model.LogLevel
import dev.hridaya.kubenexus.domain.model.LogcatEntry
import dev.hridaya.kubenexus.presentation.logcat.components.GhosttyBg
import dev.hridaya.kubenexus.presentation.logcat.components.GhosttyCyan
import dev.hridaya.kubenexus.presentation.logcat.components.GhosttyGutter
import dev.hridaya.kubenexus.presentation.logcat.components.LogcatBottomBar
import dev.hridaya.kubenexus.presentation.logcat.components.LogcatEntryRow
import dev.hridaya.kubenexus.presentation.logcat.components.LogcatTopBar
import dev.hridaya.kubenexus.ui.theme.KubeNexusTheme
import kotlinx.coroutines.launch

@Composable
fun LogcatRoute(
    viewModel: LogcatViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
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
    val scope = rememberCoroutineScope()
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
            LogcatTopBar(
                uiState = uiState,
                wrapLines = wrapLines,
                onToggleWrapLines = { wrapLines = !wrapLines },
                onAction = onAction,
                onNavigateBack = onNavigateBack,
            )
        },
        bottomBar = {
            LogcatBottomBar(
                autoScroll = uiState.autoScroll,
                onAction = onAction,
                onScrollToTop = {
                    scope.launch {
                        if (uiState.filteredLogs.isNotEmpty()) {
                            listState.animateScrollToItem(0)
                        }
                    }
                },
                onScrollToBottom = {
                    scope.launch {
                        if (uiState.filteredLogs.isNotEmpty()) {
                            listState.animateScrollToItem(uiState.filteredLogs.size - 1)
                        }
                    }
                },
            )
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
