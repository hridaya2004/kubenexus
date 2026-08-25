package dev.hridaya.kubenexus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import dev.hridaya.kubenexus.domain.model.ClusterConnectionStatus
import dev.hridaya.kubenexus.domain.model.ClusterStatus
import dev.hridaya.kubenexus.domain.model.LogLevel
import dev.hridaya.kubenexus.domain.model.PodStatus

// Fallback Brand Light Palette (Kubernetes Blue seed #326CE5)
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF005AC1),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E2FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF575E71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDBE2F9),
    onSecondaryContainer = Color(0xFF141B2C),
    tertiary = Color(0xFF715573),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFBD7FC),
    onTertiaryContainer = Color(0xFF29132D),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFEF7FF),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFEF7FF),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainerHighest = Color(0xFFE6E0E9),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
)

// Fallback Brand Dark Palette
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFADC6FF),
    onPrimary = Color(0xFF002E69),
    primaryContainer = Color(0xFF004494),
    onPrimaryContainer = Color(0xFFD8E2FF),
    secondary = Color(0xFFBFC6DC),
    onSecondary = Color(0xFF293041),
    secondaryContainer = Color(0xFF3F4759),
    onSecondaryContainer = Color(0xFFDBE2F9),
    tertiary = Color(0xFFDEBCDF),
    onTertiary = Color(0xFF402843),
    tertiaryContainer = Color(0xFF583E5B),
    onTertiaryContainer = Color(0xFFFBD7FC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF131316),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF131316),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF44474F),
    onSurfaceVariant = Color(0xFFC4C6D0),
    surfaceContainerLowest = Color(0xFF0E0E11),
    surfaceContainerLow = Color(0xFF1B1B1F),
    surfaceContainer = Color(0xFF1F1F23),
    surfaceContainerHigh = Color(0xFF2A292D),
    surfaceContainerHighest = Color(0xFF353438),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474F),
)

/**
 * Semantic status colors for Kubernetes workloads and cluster connection states.
 */
@Immutable
data class StatusColors(
    val connected: Color,
    val connecting: Color,
    val disconnected: Color,
    val offline: Color,
    val running: Color,
    val pending: Color,
    val completed: Color,
    val failed: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
) {
    fun forClusterStatus(status: ClusterStatus): Color = when (status) {
        ClusterStatus.CONNECTED -> connected
        ClusterStatus.ERROR -> failed
        ClusterStatus.DISCONNECTED -> disconnected
    }

    fun forConnectionStatus(status: ClusterConnectionStatus): Color = when (status) {
        ClusterConnectionStatus.CONNECTED -> connected
        ClusterConnectionStatus.CONNECTING -> connecting
        ClusterConnectionStatus.DISCONNECTED -> disconnected
        ClusterConnectionStatus.OFFLINE -> offline
    }

    fun forPodStatus(status: PodStatus): Color = when (status) {
        PodStatus.RUNNING -> running
        PodStatus.PENDING -> pending
        PodStatus.COMPLETED -> completed
        PodStatus.FAILED, PodStatus.CRASH_LOOP -> failed
        PodStatus.UNKNOWN -> offline
    }
}

val LightStatusColors = StatusColors(
    connected = Color(0xFF16A34A),
    connecting = Color(0xFFD97706),
    disconnected = Color(0xFFDC2626),
    offline = Color(0xFF64748B),
    running = Color(0xFF16A34A),
    pending = Color(0xFFD97706),
    completed = Color(0xFF2563EB),
    failed = Color(0xFFDC2626),
    successContainer = Color(0xFFDCFCE7),
    onSuccessContainer = Color(0xFF14532D),
    warningContainer = Color(0xFFFEF3C7),
    onWarningContainer = Color(0xFF78350F),
)

val DarkStatusColors = StatusColors(
    connected = Color(0xFF4ADE80),
    connecting = Color(0xFFFBBF24),
    disconnected = Color(0xFFF87171),
    offline = Color(0xFF94A3B8),
    running = Color(0xFF4ADE80),
    pending = Color(0xFFFBBF24),
    completed = Color(0xFF60A5FA),
    failed = Color(0xFFF87171),
    successContainer = Color(0xFF14532D),
    onSuccessContainer = Color(0xFFDCFCE7),
    warningContainer = Color(0xFF78350F),
    onWarningContainer = Color(0xFFFEF3C7),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

val MaterialTheme.statusColors: StatusColors
    @Composable
    @ReadOnlyComposable
    get() = LocalStatusColors.current

/**
 * Semantic log-level colors for the logcat viewer. Resolved per theme so log text
 * keeps sufficient contrast on both light and dark surfaces (the previous fixed
 * palette was tuned for dark terminals and failed contrast in light mode).
 */
@Immutable
data class LogLevelColors(
    val verbose: Color,
    val debug: Color,
    val info: Color,
    val warn: Color,
    val error: Color,
    val fatal: Color,
) {
    fun forLevel(level: LogLevel): Color = when (level) {
        LogLevel.VERBOSE -> verbose
        LogLevel.DEBUG -> debug
        LogLevel.INFO -> info
        LogLevel.WARN -> warn
        LogLevel.ERROR -> error
        LogLevel.FATAL -> fatal
        LogLevel.UNKNOWN -> verbose
    }
}

// On light surfaces (all >= 4.5:1 against surfaceContainer range)
val LightLogLevelColors = LogLevelColors(
    verbose = Color(0xFF5F6368),
    debug = Color(0xFF0B57D0),
    info = Color(0xFF137333),
    warn = Color(0xFF92400E),
    error = Color(0xFFBA1A1A),
    fatal = Color(0xFF6B21A8),
)

// On dark surfaces (all >= 4.5:1 against background/surfaceContainer range)
val DarkLogLevelColors = LogLevelColors(
    verbose = Color(0xFF9AA0A6),
    debug = Color(0xFF8AB4F8),
    info = Color(0xFF81C995),
    warn = Color(0xFFFDD663),
    error = Color(0xFFFFB4AB),
    fatal = Color(0xFFD8B4FE),
)

val LocalLogLevelColors = staticCompositionLocalOf { LightLogLevelColors }

val MaterialTheme.logColors: LogLevelColors
    @Composable
    @ReadOnlyComposable
    get() = LocalLogLevelColors.current
