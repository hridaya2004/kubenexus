package dev.hridaya.kubenexus.presentation.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hridaya.kubenexus.ui.theme.KubeNexusTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FabActionBottomSheet(
    hasClustersConfigured: Boolean,
    onAddClusterClick: () -> Unit,
    onAddPodClick: () -> Unit,
    onAddDeploymentClick: () -> Unit,
    onAddServiceClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                text = if (hasClustersConfigured) "Create Resource" else "Quick Actions",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (hasClustersConfigured) "Select a Kubernetes workload or manage clusters" else "Connect a Kubernetes cluster to get started",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (hasClustersConfigured) {
                ActionMenuItem(
                    icon = Icons.Outlined.Layers,
                    title = "Add Pod",
                    subtitle = "Deploy a containerized workload in active namespace",
                    onClick = {
                        onDismiss()
                        onAddPodClick()
                    },
                )

                ActionMenuItem(
                    icon = Icons.Outlined.Apps,
                    title = "Add Deployment",
                    subtitle = "Create a scalable replica-set workload",
                    onClick = {
                        onDismiss()
                        onAddDeploymentClick()
                    },
                )

                ActionMenuItem(
                    icon = Icons.Outlined.Lan,
                    title = "Add Service",
                    subtitle = "Expose cluster endpoints and networking",
                    onClick = {
                        onDismiss()
                        onAddServiceClick()
                    },
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))
            }

            ActionMenuItem(
                icon = if (hasClustersConfigured) Icons.Outlined.Add else Icons.Outlined.CloudQueue,
                title = if (hasClustersConfigured) "Add Another Cluster" else "Add Cluster",
                subtitle = "Import a kubeconfig YAML or file to connect",
                onClick = {
                    onDismiss()
                    onAddClusterClick()
                },
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ActionMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FabActionBottomSheetPreview() {
    KubeNexusTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Column {
                Text(
                    text = "Create Resource",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.height(16.dp))
                ActionMenuItem(
                    icon = Icons.Outlined.Layers,
                    title = "Add Pod",
                    subtitle = "Deploy a containerized workload in active namespace",
                    onClick = {},
                )
                ActionMenuItem(
                    icon = Icons.Outlined.Apps,
                    title = "Add Deployment",
                    subtitle = "Create a scalable replica-set workload",
                    onClick = {},
                )
                ActionMenuItem(
                    icon = Icons.Outlined.Add,
                    title = "Add Another Cluster",
                    subtitle = "Import a kubeconfig YAML or file to connect",
                    onClick = {},
                )
            }
        }
    }
}
