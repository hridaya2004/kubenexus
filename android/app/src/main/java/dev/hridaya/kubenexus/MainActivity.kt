package dev.hridaya.kubenexus

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.hridaya.kubenexus.presentation.common.KubeNexusLogo
import kotlinx.coroutines.delay
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.hridaya.kubenexus.domain.model.ThemeMode
import dev.hridaya.kubenexus.domain.repository.ThemePreferencesRepository
import dev.hridaya.kubenexus.presentation.home.HomeViewModel
import dev.hridaya.kubenexus.presentation.main.AppSplashScreen
import dev.hridaya.kubenexus.presentation.main.MainScreen
import dev.hridaya.kubenexus.ui.theme.KubeNexusTheme
import dev.hridaya.kubenexus.ui.theme.LocalAmoledDark
import dev.hridaya.kubenexus.ui.theme.LocalOnAmoledDarkChange
import dev.hridaya.kubenexus.ui.theme.LocalOnThemeModeChange
import dev.hridaya.kubenexus.ui.theme.LocalThemeMode
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themePreferencesRepository: ThemePreferencesRepository

    private val viewModel: HomeViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Permission result handled gracefully */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

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
                    var isMinSplashDurationElapsed by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(3000L)
                        isMinSplashDurationElapsed = true
                    }

                    val homeState by viewModel.uiState.collectAsStateWithLifecycle()
                    val showSplash = homeState.isLoading || !isMinSplashDurationElapsed

                    Crossfade(
                        targetState = showSplash,
                        label = "SplashScreenTransition",
                    ) { isSplash ->
                        if (isSplash) {
                            AppSplashScreen(logo = KubeNexusLogo)
                        } else {
                            MainScreen(
                                homeViewModel = viewModel,
                            )
                        }
                    }
                }
            }
        }
    }
}
