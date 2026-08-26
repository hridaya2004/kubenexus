package dev.hridaya.kubenexus.presentation.pods.detail.components

import dev.hridaya.kubenexus.presentation.pods.detail.MetricsRange
import kotlin.math.roundToInt

internal fun formatSpan(spanMs: Long, range: MetricsRange): String {
    val minutes = spanMs / 60_000f
    return when {
        minutes >= 1f -> "${minutes.roundToInt()}m"
        else -> "${range.label}+"
    }
}

internal fun formatCores(cores: Double): String =
    if (cores >= 1.0) {
        "%.2f core".format(cores)
    } else {
        "${(cores * 1000).roundToInt()}m"
    }

internal fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> "%.1f GiB".format(bytes / (1024.0 * 1024 * 1024))
    bytes >= 1024L * 1024 -> "${(bytes / (1024.0 * 1024)).roundToInt()} MiB"
    else -> "${(bytes / 1024.0).roundToInt()} KiB"
}
