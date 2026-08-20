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
import androidx.compose.material3.CircularProgressIndicator
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
        uiState.podDetails?.containers.orEmpty() + uiState.podDetails?.initContainers.orEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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

        val isConnected =
            uiState.isOnline && uiState.clusterConnectionStatus == dev.hridaya.kubenexus.domain.model.ClusterConnectionStatus.CONNECTED

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { onAction(PodDetailUiAction.FetchLogs) },
                enabled = isConnected && !uiState.isLoadingLogs && !uiState.isStreamingLogs,
                modifier = Modifier.weight(1f),
            ) {
                if (uiState.isLoadingLogs) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fetching", style = MaterialTheme.typography.labelLarge)
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isConnected) "Fetch Logs" else "Offline", style = MaterialTheme.typography.labelLarge)
                }
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
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop Stream", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Button(
                    onClick = { onAction(PodDetailUiAction.StartStreamingLogs) },
                    enabled = isConnected && !uiState.isLoadingLogs,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isConnected) "Stream Logs" else "Offline", style = MaterialTheme.typography.labelLarge)
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
