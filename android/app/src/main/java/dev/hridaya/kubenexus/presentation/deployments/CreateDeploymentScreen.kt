package dev.hridaya.kubenexus.presentation.deployments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hridaya.kubenexus.presentation.common.components.CreateNamespaceDialog
import dev.hridaya.kubenexus.presentation.common.components.ErrorBanner
import dev.hridaya.kubenexus.presentation.common.components.NamespacePicker
import dev.hridaya.kubenexus.presentation.common.components.StepHeader
import dev.hridaya.kubenexus.ui.theme.KubeNexusTheme

@Composable
fun CreateDeploymentRoute(
    viewModel: CreateDeploymentViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CreateDeploymentScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDeploymentScreen(
    uiState: CreateDeploymentUiState,
    onAction: (CreateDeploymentUiAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create Deployment",
                        style = MaterialTheme.typography.titleLarge,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (uiState.step) {
                CreateDeploymentStep.FORM -> FormStepContent(
                    uiState = uiState,
                    onAction = onAction,
                )

                CreateDeploymentStep.REVIEW -> ReviewStepContent(
                    uiState = uiState,
                    onAction = onAction,
                )
            }
        }
    }

    if (uiState.showCreateNamespaceDialog) {
        CreateNamespaceDialog(
            namespaceName = uiState.newNamespaceName,
            errorMessage = uiState.newNamespaceError,
            isCreating = uiState.isCreatingNamespace,
            onNameChanged = { onAction(CreateDeploymentUiAction.NewNamespaceNameChanged(it)) },
            onConfirm = { onAction(CreateDeploymentUiAction.CreateNamespaceSubmitted) },
            onDismiss = { onAction(CreateDeploymentUiAction.DismissCreateNamespaceClicked) },
        )
    }
}

@Composable
private fun FormStepContent(
    uiState: CreateDeploymentUiState,
    onAction: (CreateDeploymentUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StepHeader(stepLabel = "Step 1 of 2", description = "Configure the workload")

        uiState.errorMessage?.let { message ->
            ErrorBanner(
                message = message,
                onDismiss = { onAction(CreateDeploymentUiAction.DismissError) },
            )
        }

        val nameError = uiState.fieldErrors["name"]
        OutlinedTextField(
            value = uiState.name,
            onValueChange = { onAction(CreateDeploymentUiAction.NameChanged(it)) },
            label = { Text(text = "Name") },
            placeholder = { Text(text = "my-web-app") },
            isError = nameError != null,
            supportingText = nameError?.let { error ->
                { Text(text = error, color = MaterialTheme.colorScheme.error) }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        NamespacePicker(
            selectedNamespace = uiState.namespace,
            namespaceError = uiState.fieldErrors["namespace"],
            availableNamespaces = uiState.availableNamespaces,
            onSelectNamespace = { onAction(CreateDeploymentUiAction.NamespaceSelected(it)) },
            onCreateNamespaceClick = { onAction(CreateDeploymentUiAction.CreateNamespaceClicked) },
        )

        val imageError = uiState.fieldErrors["image"]
        OutlinedTextField(
            value = uiState.image,
            onValueChange = { onAction(CreateDeploymentUiAction.ImageChanged(it)) },
            label = { Text(text = "Image") },
            placeholder = { Text(text = "nginx:1.27") },
            isError = imageError != null,
            supportingText = imageError?.let { error ->
                { Text(text = error, color = MaterialTheme.colorScheme.error) }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            val replicasError = uiState.fieldErrors["replicas"]
            OutlinedTextField(
                value = uiState.replicas,
                onValueChange = { onAction(CreateDeploymentUiAction.ReplicasChanged(it)) },
                label = { Text(text = "Replicas") },
                isError = replicasError != null,
                supportingText = replicasError?.let { error ->
                    { Text(text = error, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )

            val portError = uiState.fieldErrors["containerPort"]
            OutlinedTextField(
                value = uiState.containerPort,
                onValueChange = { onAction(CreateDeploymentUiAction.ContainerPortChanged(it)) },
                label = { Text(text = "Container port") },
                isError = portError != null,
                supportingText = portError?.let { error ->
                    { Text(text = error, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        Button(
            onClick = { onAction(CreateDeploymentUiAction.PreviewSubmitted) },
            enabled = uiState.fieldErrors.isEmpty(),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(
                text = "Preview manifest",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun ReviewStepContent(
    uiState: CreateDeploymentUiState,
    onAction: (CreateDeploymentUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StepHeader(stepLabel = "Step 2 of 2", description = "Review the manifest before applying")

        uiState.errorMessage?.let { message ->
            ErrorBanner(
                message = message,
                onDismiss = { onAction(CreateDeploymentUiAction.DismissError) },
            )
        }

        Text(
            text = "You can edit the manifest before applying it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = uiState.reviewedYaml,
            onValueChange = { onAction(CreateDeploymentUiAction.ReviewedYamlChanged(it)) },
            textStyle = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
            ),
            enabled = !uiState.isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 340.dp),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedButton(
                onClick = { onAction(CreateDeploymentUiAction.BackToFormClicked) },
                enabled = !uiState.isSubmitting,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
            ) {
                Text(
                    text = "Edit",
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Button(
                onClick = { onAction(CreateDeploymentUiAction.ApplySubmitted) },
                enabled = !uiState.isSubmitting && uiState.reviewedYaml.isNotBlank(),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Applying",
                        style = MaterialTheme.typography.labelLarge,
                    )
                } else {
                    Text(
                        text = "Apply",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateDeploymentFormPreview() {
    KubeNexusTheme {
        CreateDeploymentScreen(
            uiState = CreateDeploymentUiState(
                name = "my-web-app",
                namespace = "default",
                image = "nginx:1.27",
                availableNamespaces = listOf("default", "kube-system", "monitoring"),
            ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateDeploymentReviewPreview() {
    KubeNexusTheme {
        CreateDeploymentScreen(
            uiState = CreateDeploymentUiState(
                name = "my-web-app",
                namespace = "default",
                image = "nginx:1.27",
                step = CreateDeploymentStep.REVIEW,
                generatedYaml = DeploymentYamlPreviewSample,
                reviewedYaml = DeploymentYamlPreviewSample,
                availableNamespaces = listOf("default", "kube-system"),
            ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

private val DeploymentYamlPreviewSample = """
    apiVersion: apps/v1
    kind: Deployment
    metadata:
      name: my-web-app
      namespace: default
""".trimIndent()
