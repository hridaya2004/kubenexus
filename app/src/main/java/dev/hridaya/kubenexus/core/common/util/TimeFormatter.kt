package dev.hridaya.kubenexus.core.common.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormatter {
    fun formatLastRefreshed(timestamp: Long?): String {
        if (timestamp == null || timestamp == 0L) return "Never refreshed"
        val now = System.currentTimeMillis()
        val diffMs = now - timestamp
        return when {
            diffMs < 5_000 -> "Refreshed just now"
            diffMs < 60_000 -> "Refreshed ${diffMs / 1000}s ago"
            diffMs < 3600_000 -> "Refreshed ${diffMs / 60000}m ago"
            else -> {
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                "Refreshed at ${sdf.format(Date(timestamp))}"
            }
        }
    }
}
