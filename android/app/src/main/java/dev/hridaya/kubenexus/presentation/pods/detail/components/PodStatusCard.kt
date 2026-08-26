package dev.hridaya.kubenexus.presentation.pods.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.hridaya.kubenexus.core.common.util.TimeFormatter
import dev.hridaya.kubenexus.domain.model.PodDetails
import dev.hridaya.kubenexus.ui.theme.LocalStatusColors

@Composable
internal fun PodStatusCard(
    details: PodDetails,
    lastRefreshedAt: Long?,
    modifier: Modifier = Modifier,
) {
    val statusColors = LocalStatusColors.current

    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val dotColor = statusColors.forPodStatus(details.status)
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(dotColor, CircleShape),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = details.status.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = dotColor,
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            DetailItem("Node", details.node ?: "Not assigned")
            DetailItem("Pod IP", details.ip ?: "Pending")
            DetailItem("Host IP", details.hostIp ?: "Pending")
            DetailItem("Restart Policy", details.restartPolicy ?: "Always")
            DetailItem("Start Time", TimeFormatter.formatIsoToLocal(details.startTime))
            if (lastRefreshedAt != null) {
                DetailItem(
                    "Last Refreshed",
                    TimeFormatter.formatLastRefreshed(lastRefreshedAt),
                )
            }
        }
    }
}
