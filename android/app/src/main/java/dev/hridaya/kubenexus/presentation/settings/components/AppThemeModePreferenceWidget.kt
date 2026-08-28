package dev.hridaya.kubenexus.presentation.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hridaya.kubenexus.presentation.common.components.ConnectedButtonGroup
import dev.hridaya.kubenexus.presentation.common.components.ConnectedButtonGroupDefaults
import dev.hridaya.kubenexus.ui.theme.KubeNexusTheme
import dev.hridaya.kubenexus.domain.model.ThemeMode

@Composable
fun AppThemeModePreferenceWidget(
    value: ThemeMode,
    onItemClick: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(ThemeMode.DARK, ThemeMode.SYSTEM, ThemeMode.LIGHT)

    ConnectedButtonGroup(modifier = modifier) {
        options.forEachIndexed { index, mode ->
            val isSelected = value == mode
            val icon = when (mode) {
                ThemeMode.DARK -> Icons.Outlined.DarkMode
                ThemeMode.SYSTEM -> Icons.Outlined.SettingsBrightness
                ThemeMode.LIGHT -> Icons.Outlined.LightMode
            }

            OutlinedButton(
                onClick = { onItemClick(mode) },
                modifier = Modifier
                    .weight(1f)
                    .height(ConnectedButtonGroupDefaults.Height),
                shape = ConnectedButtonGroupDefaults.itemShape(index = index, count = options.size),
                colors = ConnectedButtonGroupDefaults.buttonColors(isSelected = isSelected),
                border = ConnectedButtonGroupDefaults.border(isSelected = isSelected),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = mode.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppThemeModePreferenceWidgetPreview() {
    KubeNexusTheme {
        AppThemeModePreferenceWidget(
            value = ThemeMode.SYSTEM,
            onItemClick = {},
        )
    }
}
