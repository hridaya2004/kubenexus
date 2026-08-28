package dev.hridaya.kubenexus.core.common.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Age formatting moved from Go to Kotlin as part of dropping the flattened
 * bindings. These cases mirror the Go TestFormatAge table exactly so the
 * displayed format is unchanged by the migration.
 */
class K8sTimeTest {

    private val now = 1_800_000_000_000L

    private fun ageAgo(millisAgo: Long): String =
        K8sTime.formatAge(now - millisAgo, now)

    @Test
    fun `formats minutes only under an hour`() {
        assertEquals("4m", ageAgo(4.minutes.inWholeMilliseconds))
    }

    @Test
    fun `formats hours and minutes under a day`() {
        assertEquals("2h30m", ageAgo((2.hours + 30.minutes).inWholeMilliseconds))
    }

    @Test
    fun `formats days and hours beyond a day`() {
        assertEquals("1d3h", ageAgo((1.days + 3.hours).inWholeMilliseconds))
    }

    // The Go implementation printed a zero hour component rather than "7d".
    @Test
    fun `keeps a zero hour component for whole days`() {
        assertEquals("7d0h", ageAgo(7.days.inWholeMilliseconds))
    }

    @Test
    fun `formats a brand new object as zero minutes`() {
        assertEquals("0m", ageAgo(0L))
    }

    @Test
    fun `hour boundary switches away from a minutes only format`() {
        assertEquals("59m", ageAgo(59.minutes.inWholeMilliseconds))
        assertEquals("1h0m", ageAgo(1.hours.inWholeMilliseconds))
    }

    @Test
    fun `day boundary switches to a days format`() {
        assertEquals("23h59m", ageAgo((23.hours + 59.minutes).inWholeMilliseconds))
        assertEquals("1d0h", ageAgo(1.days.inWholeMilliseconds))
    }

    // Clock skew between the device and the cluster must not produce a negative age.
    @Test
    fun `clamps a future timestamp to zero`() {
        assertEquals("0m", K8sTime.formatAge(now + 5.minutes.inWholeMilliseconds, now))
    }

    @Test
    fun `renders a missing timestamp as unknown`() {
        assertEquals(K8sTime.UNKNOWN_AGE, K8sTime.formatAge(null, now))
    }

    @Test
    fun `parses RFC 3339 timestamps`() {
        assertEquals(
            1_756_002_900_000L,
            K8sTime.parseTimestampMillis("2025-08-24T02:35:00Z"),
        )
    }

    @Test
    fun `parses offset timestamps`() {
        assertEquals(
            K8sTime.parseTimestampMillis("2025-08-24T02:35:00Z"),
            K8sTime.parseTimestampMillis("2025-08-24T04:35:00+02:00"),
        )
    }

    @Test
    fun `returns null for absent or malformed timestamps`() {
        assertNull(K8sTime.parseTimestampMillis(null))
        assertNull(K8sTime.parseTimestampMillis(""))
        assertNull(K8sTime.parseTimestampMillis("   "))
        assertNull(K8sTime.parseTimestampMillis("not-a-timestamp"))
        assertNull(K8sTime.parseTimestampMillis("2025-13-45T99:99:99Z"))
    }
}
