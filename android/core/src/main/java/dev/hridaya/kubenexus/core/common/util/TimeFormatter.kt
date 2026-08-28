package dev.hridaya.kubenexus.core.common.util

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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

    fun formatIsoToLocal(isoTimestamp: String?): String {
        if (isoTimestamp.isNullOrBlank()) return "N/A"
        return try {
            val instant = Instant.parse(isoTimestamp)
            val formatter = DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withZone(ZoneId.systemDefault())
            formatter.format(instant)
        } catch (_: Exception) {
            try {
                val zonedDateTime = ZonedDateTime.parse(isoTimestamp)
                val formatter = DateTimeFormatter
                    .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                    .withZone(ZoneId.systemDefault())
                formatter.format(zonedDateTime)
            } catch (_: Exception) {
                isoTimestamp
            }
        }
    }
}
