package dev.hridaya.kubenexus.presentation.pods

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hridaya.kubenexus.core.common.util.TimeFormatter
import dev.hridaya.kubenexus.domain.model.Cluster
import dev.hridaya.kubenexus.domain.model.Pod
import dev.hridaya.kubenexus.domain.model.PodStatus
import dev.hridaya.kubenexus.presentation.home.HomeUiAction
import dev.hridaya.kubenexus.presentation.home.HomeUiState
import dev.hridaya.kubenexus.presentation.home.components.PodCard
import dev.hridaya.kubenexus.presentation.pods.components.DeleteNamespaceDialog
import dev.hridaya.kubenexus.ui.theme.KubeNexusTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodsScreen(
    uiState: HomeUiState,
    onAction: (HomeUiAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToPodDetail: (Pod) -> Unit = {},
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val filteredPods = remember(uiState.pods, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.pods
        } else {
            val query = searchQuery.trim()
            uiState.pods.filter { pod ->
                pod.name.contains(query, ignoreCase = true) ||
                    (pod.ip?.contains(query, ignoreCase = true) == true) ||
                    (pod.node?.contains(query, ignoreCase = true) == true) ||
                    pod.status.title.contains(query, ignoreCase = true) ||
                    pod.namespace.contains(query, ignoreCase = true)
            }
        }
    }


    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Pods",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        uiState.activeCluster?.let { cluster ->
                            Text(
                                text = "Cluster: ${cluster.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
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
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { onAction(HomeUiAction.RefreshWorkloads) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (uiState.isRefreshing || uiState.isDeletingNamespace) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search pods by name, IP, node...",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Outlined.Clear,
                                    contentDescription = "Clear search",
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Namespace:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = TimeFormatter.formatLastRefreshed(uiState.lastRefreshedAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(uiState.availableNamespaces) { ns ->
                            val isSelected = ns == uiState.selectedNamespace
                            FilterChip(
                                selected = isSelected,
                                onClick = { onAction(HomeUiAction.SelectNamespace(ns)) },
                                label = { Text(ns) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            )
                        }
                    }
                }

                when {
                    uiState.pods.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Layers,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp),
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (uiState.isRefreshing) "Fetching Pods..." else "0 Pods Found",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                val emptyMessage = when {
                                    uiState.isRefreshing -> "Retrieving live workload state from cluster"
                                    uiState.selectedNamespace != "All Namespaces" && uiState.selectedNamespace.isNotBlank() ->
                                        "No pods are running in namespace '${uiState.selectedNamespace}'."
                                    else -> "No pods are currently running in the cluster."
                                }
                                Text(
                                    text = emptyMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }


                    filteredPods.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.SearchOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp),
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No Matching Pods",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "No pods match '$searchQuery' in ${uiState.selectedNamespace}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Clear,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Clear Search")
                                }
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 32.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(filteredPods, key = { it.id }) { pod ->
                                PodCard(
                                    pod = pod,
                                    onClick = { onNavigateToPodDetail(pod) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    uiState.namespaceToDelete?.let { ns ->
        DeleteNamespaceDialog(
            namespace = ns,
            clusterName = uiState.activeCluster?.name,
            onDismiss = { onAction(HomeUiAction.DismissDeleteNamespace) },
            onConfirmDelete = { onAction(HomeUiAction.ConfirmDeleteNamespace(it)) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PodsScreenPreview() {
    KubeNexusTheme {
        PodsScreen(
            uiState = HomeUiState(
                activeCluster = Cluster(
                    id = "1",
                    name = "production-cluster",
                    serverUrl = "https://10.0.0.1:6443",
                    contextName = "prod",
                    userName = "admin",
                    namespace = "default",
                    rawKubeconfig = "",
                ),
                availableNamespaces = listOf("default", "kube-system", "monitoring"),
                selectedNamespace = "default",
                pods = listOf(
                    Pod(
                        id = "1",
                        name = "coredns-559bb7b579-24r9k",
                        namespace = "kube-system",
                        status = PodStatus.RUNNING,
                        readyContainers = "1/1",
                        restarts = 0,
                        age = "12d",
                        ip = "10.244.0.2",
                    ),
                    Pod(
                        id = "2",
                        name = "nginx-deployment-78f56c879d-gqw87",
                        namespace = "default",
                        status = PodStatus.RUNNING,
                        readyContainers = "1/1",
                        restarts = 1,
                        age = "2d",
                        ip = "10.244.0.15",
                    ),
                ),
            ),
            onAction = {},
            onNavigateBack = {},
            onNavigateToPodDetail = {},
        )
    }
}
