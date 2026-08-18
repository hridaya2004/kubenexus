package dev.hridaya.kubenexus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dev.hridaya.kubenexus.presentation.home.HomeViewModel
import dev.hridaya.kubenexus.presentation.main.MainScreen
import dev.hridaya.kubenexus.ui.theme.KubeNexusTheme
import dev.hridaya.kubenexus.ui.theme.LocalAmoledDark
import dev.hridaya.kubenexus.ui.theme.LocalOnAmoledDarkChange
import dev.hridaya.kubenexus.ui.theme.LocalOnThemeModeChange
import dev.hridaya.kubenexus.ui.theme.LocalThemeMode
import dev.hridaya.kubenexus.ui.theme.ThemeMode
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels {
        val app = application as KubeNexusApp
        HomeViewModel.provideFactory(app.container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val app = application as KubeNexusApp
        val themeRepo = app.container.themePreferencesRepository

        setContent {
            val themeMode by themeRepo.getThemeModeStream()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val amoledDark by themeRepo.getAmoledDarkStream()
                .collectAsStateWithLifecycle(initialValue = false)

            CompositionLocalProvider(
                LocalThemeMode provides themeMode,
                LocalOnThemeModeChange provides { newMode ->
                    lifecycleScope.launch { themeRepo.setThemeMode(newMode) }
                },
                LocalAmoledDark provides amoledDark,
                LocalOnAmoledDarkChange provides { newAmoled ->
                    lifecycleScope.launch { themeRepo.setAmoledDark(newAmoled) }
                },
            ) {
                KubeNexusTheme(
                    themeMode = themeMode,
                    amoledDark = amoledDark,
                ) {
                    MainScreen(
                        homeViewModel = viewModel,
                        appContainer = app.container,
                    )
                }
            }
        }
    }
}
