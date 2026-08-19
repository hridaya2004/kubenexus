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
import dagger.hilt.android.AndroidEntryPoint
import dev.hridaya.kubenexus.domain.repository.ThemePreferencesRepository
import dev.hridaya.kubenexus.presentation.home.HomeViewModel
import dev.hridaya.kubenexus.presentation.main.MainScreen
import dev.hridaya.kubenexus.presentation.pods.detail.PodDetailViewModel
import dev.hridaya.kubenexus.ui.theme.KubeNexusTheme
import dev.hridaya.kubenexus.ui.theme.LocalAmoledDark
import dev.hridaya.kubenexus.ui.theme.LocalOnAmoledDarkChange
import dev.hridaya.kubenexus.ui.theme.LocalOnThemeModeChange
import dev.hridaya.kubenexus.ui.theme.LocalThemeMode
import dev.hridaya.kubenexus.ui.theme.ThemeMode
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint(ComponentActivity::class)
class MainActivity : Hilt_MainActivity() {

    @Inject
    lateinit var themePreferencesRepository: ThemePreferencesRepository

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        val app = application as KubeNexusApp
        val themeRepo = themePreferencesRepository

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
