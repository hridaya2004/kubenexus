package dev.hridaya.kubenexus.presentation.services.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.hridaya.kubenexus.presentation.common.components.MetadataKeyValueRow

/** Labels and annotations rendered as monospace key/value sections with collapsible JSON support. */
@Composable
internal fun ServiceMetadataCard(
    labels: Map<String, String>,
    annotations: Map<String, String>,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Text(
                text = "Labels",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (labels.isEmpty()) {
                Text(
                    text = "None",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                labels.forEach { (labelKey, labelValue) ->
                    MetadataKeyValueRow(key = labelKey, value = labelValue)
                }
            }

            Text(
                text = "Annotations",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (annotations.isEmpty()) {
                Text(
                    text = "None",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                annotations.forEach { (annotationKey, annotationValue) ->
                    MetadataKeyValueRow(key = annotationKey, value = annotationValue)
                }
            }
        }
    }
}
