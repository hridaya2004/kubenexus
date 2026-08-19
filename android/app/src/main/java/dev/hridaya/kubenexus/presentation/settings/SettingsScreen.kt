package dev.hridaya.kubenexus.presentation.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.hridaya.kubenexus.BuildConfig
import dev.hridaya.kubenexus.ui.theme.KubeNexusTheme
import dev.hridaya.kubenexus.ui.theme.LocalAmoledDark
import dev.hridaya.kubenexus.ui.theme.LocalOnAmoledDarkChange
import dev.hridaya.kubenexus.ui.theme.LocalOnThemeModeChange
import dev.hridaya.kubenexus.ui.theme.LocalThemeMode
import dev.hridaya.kubenexus.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToLogcat: () -> Unit = {},
) {
    val context = LocalContext.current
    val currentThemeMode = LocalThemeMode.current
    val onThemeModeChange = LocalOnThemeModeChange.current
    val amoledDark = LocalAmoledDark.current
    val onAmoledDarkChange = LocalOnAmoledDarkChange.current

    val systemInDark = isSystemInDarkTheme()
    val isDark = when (currentThemeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemInDark
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            item {
                AppThemeModePreferenceWidget(
                    value = currentThemeMode,
                    onItemClick = onThemeModeChange,
                )
            }

            item {
                PreferenceSwitchCard(
                    icon = Icons.Outlined.Contrast,
                    title = "Pure black dark mode (AMOLED)",
                    subtitle = "Pitch-black background for dark theme on OLED screens",
                    checked = amoledDark && isDark,
                    enabled = isDark,
                    onCheckedChange = onAmoledDarkChange,
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Diagnostics",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            item {
                PreferenceNavigationCard(
                    icon = Icons.Outlined.BugReport,
                    title = "Logcat",
                    subtitle = "Inspect, filter, and share runtime application logs",
                    onClick = onNavigateToLogcat,
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "About",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "KubeNexus",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "A Kubernetes client for Android • v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Modules & Versions",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            item {
                ModulesCard(
                    modules = listOf(
                        ModuleInfo(
                            title = "Android App",
                            subtitle = "dev.hridaya.kubenexus",
                            commitSha = BuildConfig.APP_COMMIT_SHA,
                            icon = Icons.Outlined.Android,
                        ),
                        ModuleInfo(
                            title = "libghostty",
                            subtitle = "ghostty-org/ghostty VT engine",
                            commitSha = BuildConfig.LIBGHOSTTY_COMMIT_SHA,
                            icon = Icons.Outlined.Terminal,
                        ),
                        ModuleInfo(
                            title = "Ghostty Bridge",
                            subtitle = "terminal-native JNI bridge",
                            commitSha = BuildConfig.GHOSTTY_BRIDGE_COMMIT_SHA,
                            icon = Icons.Outlined.Build,
                        ),
                        ModuleInfo(
                            title = "Go-core Client",
                            subtitle = "kubenexus-go-client runtime",
                            commitSha = BuildConfig.GO_CORE_COMMIT_SHA,
                            icon = Icons.Outlined.DataObject,
                        ),
                        ModuleInfo(
                            title = "k8s client-go",
                            subtitle = "k8s.io/client-go upstream",
                            commitSha = BuildConfig.CLIENT_GO_COMMIT_SHA,
                            icon = Icons.Outlined.Hub,
                        ),
                    ),
                )
            }

            item {
                PreferenceNavigationCard(
                    icon = Icons.Outlined.Code,
                    title = "GitHub",
                    subtitle = "https://github.com/hridaya2004/kubenexus",
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            "https://github.com/hridaya2004/kubenexus".toUri(),
                        )
                        context.startActivity(intent)
                    },
                )
            }

        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppThemeModePreferenceWidget(
    value: ThemeMode,
    onItemClick: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
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

@Composable
private fun PreferenceSwitchCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val contentAlpha = if (enabled) 1f else 0.45f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha),
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun PreferenceNavigationCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private data class ModuleInfo(
    val title: String,
    val subtitle: String,
    val commitSha: String,
    val icon: ImageVector,
)

@Composable
private fun ModulesCard(
    modules: List<ModuleInfo>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            modules.forEachIndexed { index, module ->
                ModuleInfoItem(
                    module = module,
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Commit SHA", module.commitSha))
                        Toast.makeText(
                            context,
                            "Copied ${module.title} commit SHA (${module.commitSha})",
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                )
                if (index < modules.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleInfoItem(
    module: ModuleInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = module.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = module.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = module.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = module.commitSha,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = "Copy commit SHA",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    KubeNexusTheme {
        SettingsScreen()
    }
}
