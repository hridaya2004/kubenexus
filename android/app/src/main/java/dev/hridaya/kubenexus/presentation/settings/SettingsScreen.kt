package dev.hridaya.kubenexus.presentation.settings

import android.content.Intent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.hridaya.kubenexus.BuildConfig
import dev.hridaya.kubenexus.presentation.settings.components.AboutCard
import dev.hridaya.kubenexus.presentation.settings.components.AppThemeModePreferenceWidget
import dev.hridaya.kubenexus.presentation.settings.components.ModuleInfo
import dev.hridaya.kubenexus.presentation.settings.components.ModulesCard
import dev.hridaya.kubenexus.presentation.settings.components.PreferenceNavigationCard
import dev.hridaya.kubenexus.presentation.settings.components.PreferenceSwitchCard
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
                AboutCard(versionName = BuildConfig.VERSION_NAME)
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

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    KubeNexusTheme {
        SettingsScreen()
    }
}
