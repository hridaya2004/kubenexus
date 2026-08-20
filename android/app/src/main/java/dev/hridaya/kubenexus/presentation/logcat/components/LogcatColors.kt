package dev.hridaya.kubenexus.presentation.logcat.components

import androidx.compose.ui.graphics.Color
import dev.hridaya.kubenexus.domain.model.LogLevel

val GhosttyBg = Color(0xFF000000)
val GhosttySurface = Color(0xFF000000)
val GhosttyText = Color(0xFFFFFFFF)
val GhosttyGutter = Color(0xFF777777)
val GhosttyGreen = Color(0xFF3FB950)
val GhosttyYellow = Color(0xFFD29922)
val GhosttyRed = Color(0xFFF85149)
val GhosttyCyan = Color(0xFF58A6FF)
val GhosttyPurple = Color(0xFFBC8CFF)
val GhosttyBorderHighlight = Color(0xFF222222)

val LogVerboseColor = Color(0xFF9E9E9E)
val LogDebugColor = Color(0xFF29B6F6)
val LogInfoColor = Color(0xFF66BB6A)
val LogWarnColor = Color(0xFFFFA726)
val LogErrorColor = Color(0xFFEF5350)
val LogFatalColor = Color(0xFFAB47BC)

fun getLogLevelColor(level: LogLevel): Color = when (level) {
    LogLevel.VERBOSE -> LogVerboseColor
    LogLevel.DEBUG -> LogDebugColor
    LogLevel.INFO -> LogInfoColor
    LogLevel.WARN -> LogWarnColor
    LogLevel.ERROR -> LogErrorColor
    LogLevel.FATAL -> LogFatalColor
    LogLevel.UNKNOWN -> LogVerboseColor
}
