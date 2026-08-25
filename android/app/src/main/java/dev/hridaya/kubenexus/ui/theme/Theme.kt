package dev.hridaya.kubenexus.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode(val title: String) {
    DARK("Dark"),
    SYSTEM("System"),
    LIGHT("Light"),
}

val LocalThemeMode = compositionLocalOf { ThemeMode.SYSTEM }
val LocalOnThemeModeChange = compositionLocalOf<(ThemeMode) -> Unit> { {} }
val LocalAmoledDark = compositionLocalOf { false }
val LocalOnAmoledDarkChange = compositionLocalOf<(Boolean) -> Unit> { {} }

// M3 type scale over the device font families from Type.kt: every slot keeps
// its default size, weight and line-height, only the family is swapped, so the
// theme tracks the platform's variable fonts without re-specifying the scale.
private val AppTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = DisplayLargeFamily),
        displayMedium = base.displayMedium.copy(fontFamily = DisplayMediumFamily),
        displaySmall = base.displaySmall.copy(fontFamily = DisplaySmallFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = HeadlineLargeFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = HeadlineMediumFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = HeadlineSmallFamily),
        titleLarge = base.titleLarge.copy(fontFamily = TitleLargeEmphasized),
        titleMedium = base.titleMedium.copy(fontFamily = TitleMediumFamily),
        titleSmall = base.titleSmall.copy(fontFamily = TitleSmallFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = BodyLargeFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = BodyMediumFamily),
        bodySmall = base.bodySmall.copy(fontFamily = BodySmallFamily),
        labelLarge = base.labelLarge.copy(fontFamily = LabelLargeFamily),
        labelMedium = base.labelMedium.copy(fontFamily = LabelMediumFamily),
        labelSmall = base.labelSmall.copy(fontFamily = LabelSmallFamily),
    )
}

@Composable
fun KubeNexusTheme(
    themeMode: ThemeMode = LocalThemeMode.current,
    amoledDark: Boolean = LocalAmoledDark.current,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemInDark
    }
    val context = LocalContext.current

    val baseColorScheme = remember(isDark, dynamicColor, context) {
        if (dynamicColor) {
            try {
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } catch (_: Exception) {
                if (isDark) DarkColorScheme else LightColorScheme
            }
        } else {
            if (isDark) DarkColorScheme else LightColorScheme
        }
    }

    val colorScheme = remember(isDark, amoledDark, baseColorScheme) {
        if (isDark && amoledDark) {
            baseColorScheme.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceContainer = Color(0xFF121212),
                surfaceContainerLow = Color(0xFF0A0A0A),
                surfaceContainerLowest = Color.Black,
                surfaceContainerHigh = Color(0xFF1E1E1E),
                surfaceContainerHighest = Color(0xFF2C2C2C),
                surfaceVariant = Color(0xFF1E1E1E),
            )
        } else {
            baseColorScheme
        }
    }

    val statusColors = remember(isDark) {
        if (isDark) DarkStatusColors else LightStatusColors
    }

    val logColors = remember(isDark) {
        if (isDark) DarkLogLevelColors else LightLogLevelColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(isDark) {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    val expressiveShapes = remember { ExpressiveShapes() }
    val spacing = remember { Spacing() }

    CompositionLocalProvider(
        LocalStatusColors provides statusColors,
        LocalLogLevelColors provides logColors,
        LocalExpressiveShapes provides expressiveShapes,
        LocalSpacing provides spacing,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = Shapes,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
                content = content,
            )
        }
    }
}
