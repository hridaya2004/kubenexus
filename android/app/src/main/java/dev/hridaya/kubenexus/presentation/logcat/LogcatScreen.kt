package dev.hridaya.kubenexus.presentation.logcat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hridaya.kubenexus.domain.model.LogLevel
import dev.hridaya.kubenexus.domain.model.LogcatEntry
import dev.hridaya.kubenexus.presentation.logcat.components.LogcatEmptyState
import dev.hridaya.kubenexus.presentation.logcat.components.LogcatLogList
import dev.hridaya.kubenexus.presentation.logcat.components.LogcatScrollToLatestButton
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

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LogcatUiEvent.ShowMessage -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
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
        modifier = modifier,
    )
}

@Composable
fun LogcatScreen(
    uiState: LogcatUiState,
    onAction: (LogcatUiAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var wrapLines by remember { mutableStateOf(false) }

    // Scroll-aware bottom detection
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                true
            } else {
                val lastVisibleItem = visibleItems.last()
                lastVisibleItem.index >= layoutInfo.totalItemsCount - 1
            }
        }
    }

    var autoScrollEnabled by remember { mutableStateOf(true) }

    // Update autoScrollEnabled when user scrolls manually
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            autoScrollEnabled = isAtBottom
        }
    }

    // Auto-scroll when new logs arrive ONLY if auto-scroll is currently active
    LaunchedEffect(uiState.filteredLogs.size) {
        if (autoScrollEnabled && uiState.filteredLogs.isNotEmpty()) {
            listState.scrollToItem(uiState.filteredLogs.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LogcatTopBar(
                uiState = uiState,
                wrapLines = wrapLines,
                onToggleWrapLines = { wrapLines = !wrapLines },
                onAction = onAction,
                onNavigateBack = onNavigateBack,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            ) {
                if (uiState.isLoading && uiState.logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (uiState.filteredLogs.isEmpty()) {
                    LogcatEmptyState(
                        isFilterActive = uiState.searchQuery.isNotEmpty() || uiState.selectedLogLevel != null,
                    )
                } else {
                    LogcatLogList(
                        listState = listState,
                        logs = uiState.filteredLogs,
                        searchQuery = uiState.searchQuery,
                        wrapLines = wrapLines,
                        onCopyEntry = { entry ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAction(LogcatUiAction.CopyLogEntry(entry))
                        },
                    )
                }
            }

            // Floating Down Arrow Button to scroll to latest logs
            LogcatScrollToLatestButton(
                isVisible = !isAtBottom && uiState.filteredLogs.isNotEmpty(),
                onClick = {
                    autoScrollEnabled = true
                    scope.launch {
                        if (uiState.filteredLogs.isNotEmpty()) {
                            listState.animateScrollToItem(uiState.filteredLogs.size - 1)
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 18.dp),
            )
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
