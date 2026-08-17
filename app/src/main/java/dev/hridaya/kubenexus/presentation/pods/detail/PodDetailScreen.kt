package dev.hridaya.kubenexus.presentation.pods.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hridaya.kubenexus.core.common.util.TimeFormatter
import dev.hridaya.kubenexus.domain.model.ContainerDetail
import dev.hridaya.kubenexus.domain.model.PodConditionDetail
import dev.hridaya.kubenexus.domain.model.PodDetails
import dev.hridaya.kubenexus.domain.model.PodEventDetail
import dev.hridaya.kubenexus.domain.model.PodStatus
import dev.hridaya.kubenexus.presentation.common.components.LoadingContent
import dev.hridaya.kubenexus.presentation.pods.components.TermuxTerminalLogViewer

private val TermuxBg = Color(0xFF0D1117)
private val TermuxSurface = Color(0xFF161B22)
private val TermuxText = Color(0xFFC9D1D9)
private val TermuxGreen = Color(0xFF3FB950)
private val TermuxYellow = Color(0xFFD29922)
private val TermuxRed = Color(0xFFF85149)
private val TermuxCyan = Color(0xFF58A6FF)
private val TermuxPurple = Color(0xFFBC8CFF)

@Composable
fun PodDetailRoute(
    viewModel: PodDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PodDetailUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }

                is PodDetailUiEffect.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    PodDetailScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodDetailScreen(
    uiState: PodDetailUiState,
    onAction: (PodDetailUiAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.podName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Namespace: ${uiState.namespace}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onAction(PodDetailUiAction.RefreshDescribe) }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                    IconButton(
                        onClick = { onAction(PodDetailUiAction.ShowDeleteDialog(true)) },
                        colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete Pod"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = Color.Transparent,
                divider = { HorizontalDivider() }
            ) {
                PodDetailTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { onAction(PodDetailUiAction.SelectTab(tab)) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val icon = when (tab) {
                                    PodDetailTab.DESCRIBE -> Icons.Outlined.Info
                                    PodDetailTab.LOGS -> Icons.Outlined.Description
                                    PodDetailTab.TERMINAL -> Icons.Outlined.Terminal
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tab.title)
                            }
                        }
                    )
                }
            }

            when (uiState.selectedTab) {
                PodDetailTab.DESCRIBE -> {
                    DescribeTabContent(
                        uiState = uiState,
                        onNavigateToLogs = { containerName ->
                            onAction(PodDetailUiAction.SelectContainer(containerName))
                            onAction(PodDetailUiAction.SelectTab(PodDetailTab.LOGS))
                        },
                        onNavigateToTerminal = { containerName ->
                            onAction(PodDetailUiAction.SelectContainer(containerName))
                            onAction(PodDetailUiAction.SelectTab(PodDetailTab.TERMINAL))
                        }
                    )
                }

                PodDetailTab.LOGS -> {
                    LogsTabContent(
                        uiState = uiState,
                        onAction = onAction
                    )
                }

                PodDetailTab.TERMINAL -> {
                    TerminalTabContent(
                        uiState = uiState,
                        onAction = onAction
                    )
                }
            }
        }
    }

    if (uiState.showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isDeletingPod) {
                    onAction(PodDetailUiAction.ShowDeleteDialog(false))
                }
            },
            title = {
                Text(
                    text = "Delete Pod",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete pod '${uiState.podName}' in namespace '${uiState.namespace}'? This action will terminate running containers.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { onAction(PodDetailUiAction.ConfirmDeletePod) },
                    enabled = !uiState.isDeletingPod,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(if (uiState.isDeletingPod) "Deleting..." else "Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onAction(PodDetailUiAction.ShowDeleteDialog(false)) },
                    enabled = !uiState.isDeletingPod
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DescribeTabContent(
    uiState: PodDetailUiState,
    onNavigateToLogs: (String) -> Unit,
    onNavigateToTerminal: (String) -> Unit
) {
    if (uiState.isLoading) {
        LoadingContent(message = "Executing describe pod...")
        return
    }

    val details = uiState.podDetails
    if (details == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = uiState.errorMessage ?: "No pod details available.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Status",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val dotColor = when (details.status) {
                                PodStatus.RUNNING -> Color(0xFF22C55E)
                                PodStatus.COMPLETED -> Color(0xFF3B82F6)
                                PodStatus.PENDING -> Color(0xFFEAB308)
                                PodStatus.FAILED, PodStatus.CRASH_LOOP -> MaterialTheme.colorScheme.error
                                PodStatus.UNKNOWN -> MaterialTheme.colorScheme.outline
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(dotColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = details.status.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = dotColor
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    DetailItem("Node", details.node ?: "Not assigned")
                    DetailItem("Pod IP", details.ip ?: "Pending")
                    DetailItem("Host IP", details.hostIp ?: "Pending")
                    DetailItem("Restart Policy", details.restartPolicy ?: "Always")
                    DetailItem("Start Time", TimeFormatter.formatIsoToLocal(details.startTime))
                }
            }
        }

        if (details.initContainers.isNotEmpty()) {
            item {
                Text(
                    text = "Init Containers (${details.initContainers.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(details.initContainers) { container ->
                ContainerCard(
                    container = container,
                    isInitContainer = true,
                    isOnline = uiState.isOnline,
                    onViewLogsClick = { onNavigateToLogs(container.name) },
                    onOpenTerminalClick = { onNavigateToTerminal(container.name) }
                )
            }
        }

        item {
            Text(
                text = "Containers (${details.containers.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(details.containers) { container ->
            ContainerCard(
                container = container,
                isInitContainer = false,
                isOnline = uiState.isOnline,
                onViewLogsClick = { onNavigateToLogs(container.name) },
                onOpenTerminalClick = { onNavigateToTerminal(container.name) }
            )
        }

        if (details.conditions.isNotEmpty()) {
            item {
                Text(
                    text = "Conditions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        details.conditions.forEachIndexed { index, cond ->
                            ConditionRow(condition = cond)
                            if (index < details.conditions.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            }
                        }
                    }
                }
            }
        }

        if (details.events.isNotEmpty()) {
            item {
                Text(
                    text = "Events",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(details.events) { event ->
                EventCard(event = event)
            }
        }

        if (details.volumes.isNotEmpty()) {
            item {
                Text(
                    text = "Volumes (${details.volumes.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        details.volumes.forEach { volume ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Layers,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = volume,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        if (details.labels.isNotEmpty()) {
            item {
                Text(
                    text = "Labels",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        details.labels.forEach { (k, v) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = k,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = v,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogsTabContent(
    uiState: PodDetailUiState,
    onAction: (PodDetailUiAction) -> Unit
) {
    val containers = (uiState.podDetails?.initContainers.orEmpty() + uiState.podDetails?.containers.orEmpty()).distinctBy { it.name }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        if (containers.size > 1) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = "Container:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(containers) { c ->
                        FilterChip(
                            selected = c.name == uiState.selectedContainer,
                            onClick = { onAction(PodDetailUiAction.SelectContainer(c.name)) },
                            label = { Text(c.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onAction(PodDetailUiAction.FetchLogs) },
                enabled = uiState.isOnline && !uiState.isLoadingLogs,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (uiState.isOnline) "Fetch Logs" else "Offline")
            }

            if (uiState.isStreamingLogs) {
                Button(
                    onClick = { onAction(PodDetailUiAction.StopStreamingLogs) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop Stream")
                }
            } else {
                Button(
                    onClick = { onAction(PodDetailUiAction.StartStreamingLogs) },
                    enabled = uiState.isOnline,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (uiState.isOnline) "Stream Logs" else "Offline")
                }
            }
        }

        TermuxTerminalLogViewer(
            logs = uiState.logs,
            isStreaming = uiState.isStreamingLogs,
            onClearLogs = { onAction(PodDetailUiAction.ClearLogs) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TerminalTabContent(
    uiState: PodDetailUiState,
    onAction: (PodDetailUiAction) -> Unit
) {
    val context = LocalContext.current
    val containers = (uiState.podDetails?.initContainers.orEmpty() + uiState.podDetails?.containers.orEmpty()).distinctBy { it.name }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.terminalLines.size) {
        if (uiState.terminalLines.isNotEmpty()) {
            listState.animateScrollToItem(uiState.terminalLines.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .imePadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = "Target:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                if (containers.size > 1) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(containers) { c ->
                            FilterChip(
                                selected = c.name == uiState.selectedContainer,
                                onClick = { onAction(PodDetailUiAction.SelectContainer(c.name)) },
                                label = { Text(c.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                } else {
                    Text(
                        text = uiState.selectedContainer ?: "default",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (uiState.isTerminalActive) {
                Button(
                    onClick = { onAction(PodDetailUiAction.StopInteractiveTerminal) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Disconnect", fontSize = 12.sp)
                }
            } else {
                Button(
                    onClick = { onAction(PodDetailUiAction.StartInteractiveTerminal()) },
                    enabled = uiState.isContainerAttachable && !uiState.isExecutingCommand,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.Terminal, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when {
                            !uiState.isOnline -> "Offline"
                            !uiState.isContainerAttachable -> "Detached"
                            else -> "Attach"
                        },
                        fontSize = 12.sp
                    )
                }
            }
        }

        Surface(
            color = TermuxBg,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TermuxSurface)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (uiState.isTerminalActive) TermuxGreen else TermuxYellow, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isTerminalActive) "interactive session (${uiState.activeShellCommand})" else "exec terminal",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TermuxText
                        )
                    }

                    Row {
                        IconButton(
                            onClick = {
                                val fullOutput = uiState.terminalLines.joinToString("\n") { it.text }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Terminal Output", fullOutput))
                                Toast.makeText(context, "Terminal output copied", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = TermuxText, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { onAction(PodDetailUiAction.ClearTerminal) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.Clear, contentDescription = "Clear", tint = TermuxText, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp)
                ) {
                    if (uiState.terminalLines.isEmpty()) {
                        item {
                            Text(
                                text = "# Type a command below to exec into the pod container.\n# Tap 'Attach' above to open an interactive TTY shell session.",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color(0xFF6E7681)
                            )
                        }
                    } else {
                        itemsIndexed(uiState.terminalLines) { _, line ->
                            val textColor = when (line.type) {
                                TerminalLineType.INPUT -> TermuxGreen
                                TerminalLineType.STDOUT -> TermuxText
                                TerminalLineType.STDERR -> TermuxRed
                                TerminalLineType.SYSTEM -> TermuxCyan
                                TerminalLineType.ERROR -> TermuxRed
                            }
                            Text(
                                text = line.text,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = textColor,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = uiState.execInputText,
                onValueChange = { onAction(PodDetailUiAction.UpdateExecInput(it)) },
                enabled = uiState.isOnline && !uiState.isExecutingCommand,
                placeholder = {
                    Text(
                        text = when {
                            !uiState.isOnline -> "Network offline..."
                            uiState.isTerminalActive -> "Send stdin/command to shell..."
                            else -> "Run command (e.g. ps aux)..."
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                },
                prefix = {
                    Text(
                        text = "$ ",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (uiState.execInputText.isNotBlank() && uiState.isOnline) {
                            onAction(PodDetailUiAction.ExecuteCommand(uiState.execInputText))
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (uiState.execInputText.isNotBlank() && uiState.isOnline) {
                        onAction(PodDetailUiAction.ExecuteCommand(uiState.execInputText))
                    }
                },
                enabled = uiState.isOnline && uiState.execInputText.isNotBlank() && !uiState.isExecutingCommand,
                modifier = Modifier.height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ContainerCard(
    container: ContainerDetail,
    isInitContainer: Boolean = false,
    isOnline: Boolean = true,
    onViewLogsClick: () -> Unit,
    onOpenTerminalClick: () -> Unit
) {
    val isAttachable = isOnline && (container.ready || container.state.equals("Running", ignoreCase = true))

    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = container.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                    if (isInitContainer) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "Init",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (container.ready) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = if (container.ready) "Ready" else "Not Ready",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Image: ${container.image}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            DetailItem("State", container.state)
            DetailItem("Restarts", "${container.restartCount}")
            if (container.ports.isNotEmpty()) {
                DetailItem("Ports", container.ports.joinToString(", "))
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onViewLogsClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Logs", fontSize = 12.sp)
                }

                Button(
                    onClick = onOpenTerminalClick,
                    enabled = isAttachable,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Terminal,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isAttachable) "Exec Terminal" else "Detached", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ConditionRow(condition: PodConditionDetail) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = condition.type,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal
            )
            if (!condition.reason.isNullOrBlank()) {
                Text(
                    text = "Reason: ${condition.reason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val isTrue = condition.status.equals("True", ignoreCase = true)
        Icon(
            imageVector = if (isTrue) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
            contentDescription = condition.status,
            tint = if (isTrue) Color(0xFF22C55E) else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun EventCard(event: PodEventDetail) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.Event,
                contentDescription = null,
                tint = if (event.type.equals("Warning", ignoreCase = true)) Color(0xFFD29922) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = event.reason,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = event.age,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = event.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
