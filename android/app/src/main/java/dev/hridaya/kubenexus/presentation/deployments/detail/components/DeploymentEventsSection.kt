package dev.hridaya.kubenexus.presentation.deployments.detail.components

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.hridaya.kubenexus.domain.model.K8sEventSummary
import dev.hridaya.kubenexus.ui.theme.LocalStatusColors

/**
 * Recent Deployment events, cloned from the pods EventCard visual language.
 * The section title carries the event count; each row shows its own age, so
 * the latest event's age is always the first row's. Ages render from
 * [K8sEventSummary.lastTimestampMillis] on every composition instead of a
 * frozen preformatted string, matching how pod rows behave.
 */
@Composable
internal fun DeploymentEventsSection(
    events: List<K8sEventSummary>,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        DescribeSectionTitle(text = "Events (${events.size})")
        events.forEach { event ->
            DeploymentEventCard(event = event)
        }
    }
}

@Composable
private fun DeploymentEventCard(
    event: K8sEventSummary,
    modifier: Modifier = Modifier,
) {
    val statusColors = LocalStatusColors.current
    Card(
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.Event,
                contentDescription = null,
                tint = if (event.type.equals("Warning", ignoreCase = true)) {
                    statusColors.connecting
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = event.reason.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                    )
                    event.lastTimestampMillis?.let { lastTimestampMillis ->
                        Text(
                            text = DateUtils
                                .getRelativeTimeSpanString(lastTimestampMillis)
                                .toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (!event.message.isNullOrBlank()) {
                    Text(
                        text = event.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (event.count > 1) {
                    Text(
                        text = "seen ${event.count} times",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
