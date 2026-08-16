package dev.hridaya.kubenexus.presentation.home

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hridaya.kubenexus.presentation.common.components.LoadingContent
import dev.hridaya.kubenexus.presentation.home.components.AddClusterBottomSheet
import dev.hridaya.kubenexus.presentation.home.components.ClusterPill
import dev.hridaya.kubenexus.presentation.home.components.ClusterSwitcherDrawer
import dev.hridaya.kubenexus.presentation.home.components.EmptyClustersView
import dev.hridaya.kubenexus.presentation.home.components.ErrorDialog
import dev.hridaya.kubenexus.presentation.home.components.FabActionBottomSheet
import dev.hridaya.kubenexus.presentation.home.components.ResourcePreferenceCard

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onNavigateToManageClusters: () -> Unit,
    onNavigateToPods: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HomeUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }

                is HomeUiEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }

                is HomeUiEffect.NavigateToHome -> Unit
            }
        }
    }

    HomeScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onNavigateToManageClusters = onNavigateToManageClusters,
        onNavigateToPods = onNavigateToPods,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (HomeUiAction) -> Unit,
    onNavigateToManageClusters: () -> Unit,
    onNavigateToPods: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "KubeNexus",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        ClusterPill(
                            activeCluster = uiState.activeCluster,
                            totalClusters = uiState.clusters.size,
                            onClick = { onAction(HomeUiAction.OpenClusterDrawer) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAction(HomeUiAction.OpenFabActionSheet) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Quick Actions"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingContent(message = "Loading cluster...")
                }

                uiState.clusters.isEmpty() -> {
                    EmptyClustersView(
                        onAddClusterClick = { onAction(HomeUiAction.OpenAddClusterSheet) }
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Text(
                                text = "Workloads",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                            )
                        }

                        item {
                            ResourcePreferenceCard(
                                title = "Pods",
                                subtitle = "Container instances and workload state",
                                icon = Icons.Outlined.Layers,
                                badgeText = "${uiState.pods.size}",
                                onClick = onNavigateToPods
                            )
                        }

                        item {
                            ResourcePreferenceCard(
                                title = "Deployments",
                                subtitle = "Declarative updates for Pods and ReplicaSets",
                                icon = Icons.Outlined.Apps,
                                badgeText = "Workload",
                                onClick = {
                                    onAction(HomeUiAction.TriggerNoopAction("Deployment management coming soon"))
                                }
                            )
                        }

                        item {
                            ResourcePreferenceCard(
                                title = "ReplicaSets",
                                subtitle = "Maintain stable set of replica Pods",
                                icon = Icons.Outlined.Widgets,
                                badgeText = "Workload",
                                onClick = {
                                    onAction(HomeUiAction.TriggerNoopAction("ReplicaSet management coming soon"))
                                }
                            )
                        }
                    }
                }
            }
        }

        if (uiState.showClusterDrawer) {
            ClusterSwitcherDrawer(
                clusters = uiState.clusters,
                activeCluster = uiState.activeCluster,
                onSelectCluster = { onAction(HomeUiAction.SelectClusterClicked(it)) },
                onManageClustersClick = onNavigateToManageClusters,
                onDismiss = { onAction(HomeUiAction.DismissClusterDrawer) }
            )
        }

        if (uiState.showFabActionSheet) {
            FabActionBottomSheet(
                hasClustersConfigured = uiState.clusters.isNotEmpty(),
                onAddClusterClick = { onAction(HomeUiAction.OpenAddClusterSheet) },
                onAddPodClick = onNavigateToPods,
                onAddDeploymentClick = { onAction(HomeUiAction.TriggerNoopAction("Deployment creation coming soon")) },
                onAddServiceClick = { onAction(HomeUiAction.TriggerNoopAction("Service creation coming soon")) },
                onDismiss = { onAction(HomeUiAction.DismissFabActionSheet) }
            )
        }

        if (uiState.showAddClusterSheet) {
            AddClusterBottomSheet(
                kubeconfigInput = uiState.kubeconfigInput,
                customClusterName = uiState.customClusterName,
                kubeconfigError = uiState.kubeconfigError,
                isConnecting = uiState.isConnecting,
                onKubeconfigChanged = { onAction(HomeUiAction.KubeconfigInputChanged(it)) },
                onClusterNameChanged = { onAction(HomeUiAction.ClusterNameChanged(it)) },
                onFileImported = { content, name -> onAction(HomeUiAction.FileImported(content, name)) },
                onConnectAndSave = { onAction(HomeUiAction.ConnectAndSaveSubmitted) },
                onDismiss = { onAction(HomeUiAction.DismissAddClusterSheet) }
            )
        }

        uiState.errorDialogData?.let { errorData ->
            ErrorDialog(
                data = errorData,
                onDismiss = { onAction(HomeUiAction.DismissErrorDialog) },
                onCopy = { text -> onAction(HomeUiAction.CopyErrorClicked(text)) }
            )
        }
    }
}
