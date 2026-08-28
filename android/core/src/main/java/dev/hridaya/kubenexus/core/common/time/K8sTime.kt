package dev.hridaya.kubenexus.core.common.time

import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * Timestamp helpers for Kubernetes RFC 3339 values.
 *
 * Age used to be formatted in Go and shipped across the bridge as a finished
 * string, which meant the value written into the Room cache froze at sync time
 * and drifted until the next refresh. Now that the raw `creationTimestamp`
 * crosses the boundary, age is derived at read time instead.
 */
object K8sTime {

    private const val MILLIS_PER_MINUTE = 60_000L
    private const val MINUTES_PER_HOUR = 60L
    private const val HOURS_PER_DAY = 24L

    /** Unknown or unparseable timestamps render as this rather than throwing. */
    const val UNKNOWN_AGE: String = "unknown"

    /**
     * Parses an RFC 3339 timestamp to epoch milliseconds, returning null when the
     * value is absent or malformed.
     *
     * Kubernetes emits UTC with a trailing `Z`, which [Instant.parse] handles.
     * The [OffsetDateTime] fallback covers explicit numeric offsets, whose
     * acceptance by `ISO_INSTANT` varies across JDK versions.
     */
    fun parseTimestampMillis(timestamp: String?): Long? {
        if (timestamp.isNullOrBlank()) return null
        return try {
            Instant.parse(timestamp).toEpochMilli()
        } catch (_: DateTimeParseException) {
            try {
                OffsetDateTime.parse(timestamp).toInstant().toEpochMilli()
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    /**
     * Formats an age the way kubectl does, matching the Go implementation this
     * replaces: "5m", "3h20m", "2d7h".
     */
    fun formatAge(creationMillis: Long?, nowMillis: Long = System.currentTimeMillis()): String {
        if (creationMillis == null) return UNKNOWN_AGE

        val elapsed = (nowMillis - creationMillis).coerceAtLeast(0L)
        val totalMinutes = elapsed / MILLIS_PER_MINUTE
        val totalHours = totalMinutes / MINUTES_PER_HOUR
        val days = totalHours / HOURS_PER_DAY
        val hours = totalHours % HOURS_PER_DAY
        val minutes = totalMinutes % MINUTES_PER_HOUR

        return when {
            days > 0L -> "${days}d${hours}h"
            totalHours > 0L -> "${hours}h${minutes}m"
            else -> "${minutes}m"
        }
    }

    /** Convenience overload for a raw RFC 3339 timestamp. */
    fun formatAge(timestamp: String?): String = formatAge(parseTimestampMillis(timestamp))
}
