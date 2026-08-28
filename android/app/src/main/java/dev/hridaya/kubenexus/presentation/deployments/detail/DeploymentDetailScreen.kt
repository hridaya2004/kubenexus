package dev.hridaya.kubenexus.presentation.deployments.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hridaya.kubenexus.domain.model.DeploymentSummary
import dev.hridaya.kubenexus.presentation.deployments.detail.components.DeploymentDetailsSection
import dev.hridaya.kubenexus.presentation.deployments.detail.components.DeploymentImagesCard
import dev.hridaya.kubenexus.presentation.deployments.detail.components.DeploymentStatusCard
import dev.hridaya.kubenexus.ui.theme.KubeNexusTheme

@Composable
fun DeploymentDetailRoute(
    viewModel: DeploymentDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DeploymentDetailScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeploymentDetailScreen(
    uiState: DeploymentDetailUiState,
    onAction: (DeploymentDetailUiAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.deploymentName,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                    )
                }
            }

            uiState.errorMessage != null && uiState.deployment == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = uiState.errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { onAction(DeploymentDetailUiAction.Refresh) },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .height(48.dp)
                                .padding(horizontal = 16.dp),
                        ) {
                            Text(text = "Retry", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            else -> uiState.deployment?.let { deployment ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    DeploymentStatusCard(
                        deployment = deployment,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    DeploymentImagesCard(
                        images = deployment.images,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Describe sections load independently; the overview above
                    // keeps rendering while they stream in or fail.
                    DeploymentDetailsSection(
                        isDetailsLoading = uiState.isDetailsLoading,
                        details = uiState.details,
                        detailsErrorMessage = uiState.detailsErrorMessage,
                        onRetry = { onAction(DeploymentDetailUiAction.Refresh) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeploymentDetailScreenPreview() {
    KubeNexusTheme {
        DeploymentDetailScreen(
            uiState = DeploymentDetailUiState(
                deploymentName = "web-frontend",
                namespace = "default",
                isLoading = false,
                deployment = DeploymentSummary(
                    id = "default/web-frontend",
                    name = "web-frontend",
                    namespace = "default",
                    desiredReplicas = 3,
                    readyReplicas = 2,
                    availableReplicas = 2,
                    images = listOf("nginx:1.27", "registry.example.com/sidecar:v0.9"),
                    creationTimestampMillis = System.currentTimeMillis(),
                ),
                isDetailsLoading = false,
                detailsErrorMessage = "Couldn't load the describe details for this deployment.",
            ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}
