package dev.hridaya.kubenexus.presentation.pods.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hridaya.kubenexus.presentation.pods.components.GhosttyTerminalLogViewer
import dev.hridaya.kubenexus.presentation.pods.detail.PodDetailUiAction
import dev.hridaya.kubenexus.presentation.pods.detail.PodDetailUiState
import dev.hridaya.kubenexus.ui.theme.KubeNexusTheme

@Composable
fun LogsTabContent(
    uiState: PodDetailUiState,
    onAction: (PodDetailUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val containers =
        (uiState.podDetails?.initContainers.orEmpty() + uiState.podDetails?.containers.orEmpty()).distinctBy { it.name }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
    ) {
        if (containers.size > 1) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                Text(
                    text = "Container:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(containers) { c ->
                        FilterChip(
                            selected = c.name == uiState.selectedContainer,
                            onClick = { onAction(PodDetailUiAction.SelectContainer(c.name)) },
                            label = { Text(c.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { onAction(PodDetailUiAction.FetchLogs) },
                enabled = uiState.isOnline && !uiState.isLoadingLogs,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (uiState.isOnline) "Fetch Logs" else "Offline")
            }

            if (uiState.isStreamingLogs) {
                Button(
                    onClick = { onAction(PodDetailUiAction.StopStreamingLogs) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop Stream")
                }
            } else {
                Button(
                    onClick = { onAction(PodDetailUiAction.StartStreamingLogs) },
                    enabled = uiState.isOnline,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (uiState.isOnline) "Stream Logs" else "Offline")
                }
            }
        }

        GhosttyTerminalLogViewer(
            logs = uiState.logs,
            isStreaming = uiState.isStreamingLogs,
            onClearLogs = { onAction(PodDetailUiAction.ClearLogs) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LogsTabContentPreview() {
    KubeNexusTheme {
        LogsTabContent(
            uiState = PodDetailUiState(
                podName = "nginx",
                namespace = "default",
                logs = listOf(
                    "2026-08-19 12:00:00 [info] Server started",
                    "2026-08-19 12:00:01 [info] Ready for connections",
                ),
                isOnline = true,
            ),
            onAction = {},
        )
    }
}
