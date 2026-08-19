package dev.hridaya.kubenexus.presentation.logcat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hridaya.kubenexus.presentation.logcat.LogcatUiAction
import dev.hridaya.kubenexus.ui.theme.KubeNexusTheme

@Composable
fun LogcatBottomBar(
    autoScroll: Boolean,
    onAction: (LogcatUiAction) -> Unit,
    onScrollToTop: () -> Unit,
    onScrollToBottom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = GhosttySurface,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Auto-scroll",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = GhosttyText,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Switch(
                    checked = autoScroll,
                    onCheckedChange = { onAction(LogcatUiAction.ToggleAutoScroll) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GhosttyGreen,
                        checkedTrackColor = GhosttySurface,
                    ),
                    modifier = Modifier.size(width = 36.dp, height = 20.dp),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onScrollToTop,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowUpward,
                        contentDescription = "Scroll to top",
                        tint = GhosttyText,
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(
                    onClick = onScrollToBottom,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowDownward,
                        contentDescription = "Scroll to bottom",
                        tint = GhosttyText,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LogcatBottomBarPreview() {
    KubeNexusTheme {
        LogcatBottomBar(
            autoScroll = true,
            onAction = {},
            onScrollToTop = {},
            onScrollToBottom = {},
        )
    }
}
