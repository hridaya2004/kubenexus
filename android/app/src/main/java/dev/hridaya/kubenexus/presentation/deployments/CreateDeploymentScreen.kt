package dev.hridaya.kubenexus.presentation.deployments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
private fun NamespacePicker(
    selectedNamespace: String,
    namespaceError: String?,
    availableNamespaces: List<String>,
    onSelectNamespace: (String) -> Unit,
    onCreateNamespaceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The workload must land in one concrete namespace; keep the current value
    // selectable even if it is not in the fetched list yet.
    val options = remember(availableNamespaces, selectedNamespace) {
        (availableNamespaces + selectedNamespace)
            .filter { it.isNotBlank() && it != "All Namespaces" }
            .distinct()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Namespace",
            style = MaterialTheme.typography.bodySmall,
            color = if (namespaceError != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(options) { namespace ->
                FilterChip(
                    selected = namespace == selectedNamespace,
                    onClick = { onSelectNamespace(namespace) },
                    label = { Text(namespace) },
                    shape = MaterialTheme.shapes.small,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
            item {
                FilterChip(
                    selected = false,
                    onClick = onCreateNamespaceClick,
                    label = { Text("Create new") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    shape = MaterialTheme.shapes.small,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = MaterialTheme.colorScheme.primary,
                        iconColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }
        namespaceError?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun CreateNamespaceDialog(
    namespaceName: String,
    errorMessage: String?,
    isCreating: Boolean,
    onNameChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create namespace",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            OutlinedTextField(
                value = namespaceName,
                onValueChange = onNameChanged,
                label = { Text(text = "Namespace name") },
                placeholder = { Text(text = "my-team") },
                isError = errorMessage != null,
                supportingText = errorMessage?.let { error ->
                    { Text(text = error, color = MaterialTheme.colorScheme.error) }
                },
                singleLine = true,
                enabled = !isCreating,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isCreating && namespaceName.isNotBlank(),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.height(48.dp),
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Creating",
                        style = MaterialTheme.typography.labelLarge,
                    )
                } else {
                    Text(
                        text = "Create",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating,
            ) {
                Text(text = "Cancel")
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
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

@Composable
private fun StepHeader(stepLabel: String, description: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stepLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Dismiss error",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
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
