package dev.hridaya.kubenexus.presentation.settings.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.hridaya.kubenexus.ui.theme.KubeNexusTheme
import dev.hridaya.kubenexus.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppThemeModePreferenceWidget(
    value: ThemeMode,
    onItemClick: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(ThemeMode.DARK, ThemeMode.SYSTEM, ThemeMode.LIGHT)

    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth(),
    ) {
        options.forEachIndexed { index, mode ->
            val isSelected = value == mode
            SegmentedButton(
                selected = isSelected,
                onClick = { onItemClick(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon = {
                    SegmentedButtonDefaults.Icon(active = isSelected) {
                        val icon = when (mode) {
                            ThemeMode.DARK -> Icons.Outlined.DarkMode
                            ThemeMode.SYSTEM -> Icons.Outlined.SettingsBrightness
                            ThemeMode.LIGHT -> Icons.Outlined.LightMode
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                        )
                    }
                },
                label = {
                    Text(
                        text = mode.title,
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
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
