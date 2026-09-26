package dev.hridaya.kubenexus.core.common.util

import dev.hridaya.kubenexus.core.common.paste.DpasteLogPasteProvider
import dev.hridaya.kubenexus.core.common.paste.LogPasteProvider
import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogExportHelperTest {

    private class RecordingProvider : LogPasteProvider {
        override val name: String = "paste.test"
        override val retention: String = "1 day"
        var uploaded: String? = null

        override suspend fun upload(content: String): Result<String> {
            uploaded = content
            return Result.Success("https://paste.test/12345")
        }
    }

    @Test
    fun `uploadToPastebin refuses blank content without contacting the provider`() = runTest {
        val provider = RecordingProvider()
        val result = LogExportHelper.uploadToPastebin(
            content = "   ",
            provider = provider,
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.Validation)
        assertEquals("Cannot export empty logs", error.message)
        assertNull(provider.uploaded)
    }

    @Test
    fun `uploadToPastebin redacts credentials before they leave the device`() = runTest {
        val provider = RecordingProvider()
        val result = LogExportHelper.uploadToPastebin(
            content = "2026-08-27 connecting with password=hunter2 as Bearer abc.def",
            provider = provider,
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        assertEquals("https://paste.test/12345", (result as Result.Success).data)
        val sent = provider.uploaded.orEmpty()
        assertFalse(sent.contains("hunter2"))
        assertFalse(sent.contains("abc.def"))
        assertTrue(sent.startsWith("2026-08-27 connecting with password=[REDACTED]"))
    }

    @Test
    fun `the default paste service is the one the user is told about`() {
        val provider = LogExportHelper.pasteProvider

        assertEquals("dpaste.org", provider.name)
        assertEquals("7 days", provider.retention)
    }

    @Test
    fun `dpaste form body uses dpaste org's expires field in seconds`() {
        val body = DpasteLogPasteProvider.formBody("line one\nline two")

        assertTrue(body.contains("expires=604800"))
        assertFalse("expiry_days is dpaste.com's field and is ignored by dpaste.org", body.contains("expiry_days"))
        assertTrue(body.contains("format=url"))
        assertTrue(body.contains("content=line+one%0Aline+two"))
    }
}
