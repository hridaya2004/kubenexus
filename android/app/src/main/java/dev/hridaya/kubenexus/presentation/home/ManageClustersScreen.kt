package dev.hridaya.kubenexus.presentation.home

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.hridaya.kubenexus.domain.model.Cluster
import dev.hridaya.kubenexus.presentation.home.components.AddClusterBottomSheet
import dev.hridaya.kubenexus.presentation.home.components.DeleteClusterDialog
import dev.hridaya.kubenexus.presentation.home.components.EditClusterDialog
import dev.hridaya.kubenexus.presentation.home.components.EmptyClustersView
import dev.hridaya.kubenexus.presentation.home.components.ErrorDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageClustersScreen(
    uiState: HomeUiState,
    onAction: (HomeUiAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Manage Clusters",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAction(HomeUiAction.OpenAddClusterSheet) },
                icon = { Icon(imageVector = Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Add Cluster") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        },
    ) { innerPadding ->
        if (uiState.clusters.isEmpty()) {
            EmptyClustersView(
                onAddClusterClick = { onAction(HomeUiAction.OpenAddClusterSheet) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(uiState.clusters, key = { it.id }) { cluster ->
                    ManageClusterCard(
                        cluster = cluster,
                        onSelectActive = { onAction(HomeUiAction.SelectClusterClicked(cluster.id)) },
                        onTestConnection = {
                            onAction(
                                HomeUiAction.TestClusterConnectionClicked(
                                    cluster.id,
                                ),
                            )
                        },
                        onEdit = { onAction(HomeUiAction.RequestEditCluster(cluster)) },
                        onDelete = { onAction(HomeUiAction.RequestDeleteCluster(cluster)) },
                    )
                }
            }
        }
    }

    if (uiState.showAddClusterSheet) {
        AddClusterBottomSheet(
            kubeconfigInput = uiState.kubeconfigInput,
            customClusterName = uiState.customClusterName,
            kubeconfigError = uiState.kubeconfigError,
            isConnecting = uiState.isConnecting,
            onKubeconfigChanged = { onAction(HomeUiAction.KubeconfigInputChanged(it)) },
            onClusterNameChanged = { onAction(HomeUiAction.ClusterNameChanged(it)) },
            onFileImported = { content, name ->
                onAction(
                    HomeUiAction.FileImported(
                        content,
                        name,
                    ),
                )
            },
            onConnectAndSave = { onAction(HomeUiAction.ConnectAndSaveSubmitted) },
            onDismiss = { onAction(HomeUiAction.DismissAddClusterSheet) },
        )
    }

    uiState.editingCluster?.let { cluster ->
        EditClusterDialog(
            cluster = cluster,
            onDismiss = { onAction(HomeUiAction.DismissEditCluster) },
            onSaveName = { id, newName -> onAction(HomeUiAction.SaveClusterName(id, newName)) },
        )
    }

    uiState.clusterToDelete?.let { cluster ->
        DeleteClusterDialog(
            cluster = cluster,
            onDismiss = { onAction(HomeUiAction.DismissDeleteCluster) },
            onConfirmDelete = { id -> onAction(HomeUiAction.ConfirmDeleteCluster(id)) },
        )
    }

    uiState.errorDialogData?.let { data ->
        ErrorDialog(
            data = data,
            onDismiss = { onAction(HomeUiAction.DismissErrorDialog) },
            onCopy = { onAction(HomeUiAction.CopyErrorClicked(it)) },
        )
    }
}

@Composable
private fun ManageClusterCard(
    cluster: Cluster,
    onSelectActive: () -> Unit,
    onTestConnection: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (cluster.isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Dns,
                        contentDescription = null,
                        tint = if (cluster.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = cluster.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = cluster.serverUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (cluster.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (cluster.isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Text(
                        text = if (cluster.isActive) "ACTIVE" else "INACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Context: ${cluster.contextName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Namespace: ${cluster.namespace}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (!cluster.isActive) {
                    FilledTonalButton(
                        onClick = onSelectActive,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Set Active", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    OutlinedButton(
                        onClick = onTestConnection,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Connection", style = MaterialTheme.typography.labelMedium)
                    }
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit cluster alias",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete cluster",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
