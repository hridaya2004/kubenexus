package dev.hridaya.kubenexus.core.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TimeFormatterTest {

    @Test
    fun `formatIsoToLocal returns NA for null or blank input`() {
        assertEquals("N/A", TimeFormatter.formatIsoToLocal(null))
        assertEquals("N/A", TimeFormatter.formatIsoToLocal(""))
        assertEquals("N/A", TimeFormatter.formatIsoToLocal("   "))
    }

    @Test
    fun `formatIsoToLocal correctly parses ISO-8601 UTC timestamp`() {
        val result = TimeFormatter.formatIsoToLocal("2026-08-17T06:30:00Z")
        assertNotEquals("N/A", result)
        assertNotEquals("2026-08-17T06:30:00Z", result)
    }

    @Test
    fun `formatIsoToLocal returns original string on unparseable invalid format`() {
        val invalid = "not-a-timestamp"
        val result = TimeFormatter.formatIsoToLocal(invalid)
        assertEquals(invalid, result)
    }
}
