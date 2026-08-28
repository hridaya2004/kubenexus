package dev.hridaya.kubenexus.core.common.util

import dev.hridaya.kubenexus.core.common.paste.CompositeLogPasteProvider
import dev.hridaya.kubenexus.core.common.paste.LogPasteProvider
import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogExportHelperTest {

    @Test
    fun `uploadToPastebin returns Validation error when content is blank`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val result = LogExportHelper.uploadToPastebin(
            content = "   ",
            title = "Test",
            dispatcher = testDispatcher,
        )

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.Validation)
        assertEquals("Cannot export empty logs", error.message)
    }

    @Test
    fun `uploadToPastebin returns Validation error when content is empty`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val result = LogExportHelper.uploadToPastebin(
            content = "",
            dispatcher = testDispatcher,
        )

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.Validation)
    }

    @Test
    fun `uploadToPastebin delegates to custom provider successfully`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val mockProvider = object : LogPasteProvider {
            override val name: String = "MockProvider"
            override suspend fun upload(content: String, title: String?): Result<String> {
                return Result.Success("https://mockpaste.org/12345")
            }
        }

        val result = LogExportHelper.uploadToPastebin(
            content = "2026-08-27 pod log line",
            title = "my-nginx-pod",
            provider = mockProvider,
            dispatcher = testDispatcher,
        )

        assertTrue(result is Result.Success)
        assertEquals("https://mockpaste.org/12345", (result as Result.Success).data)
    }

    @Test
    fun `CompositeLogPasteProvider falls back to second provider when first fails`() = runTest {
        val failingProvider = object : LogPasteProvider {
            override val name: String = "FailingProvider"
            override suspend fun upload(content: String, title: String?): Result<String> {
                return Result.Error(AppError.Network("Primary down"))
            }
        }
        val fallbackProvider = object : LogPasteProvider {
            override val name: String = "FallbackProvider"
            override suspend fun upload(content: String, title: String?): Result<String> {
                return Result.Success("https://fallbackpaste.org/abc")
            }
        }

        val composite = CompositeLogPasteProvider(listOf(failingProvider, fallbackProvider))
        val result = composite.upload("sample log content", "pod-title")

        assertTrue(result is Result.Success)
        assertEquals("https://fallbackpaste.org/abc", (result as Result.Success).data)
    }

    @Test
    fun `CompositeLogPasteProvider returns error when all providers fail`() = runTest {
        val failingProvider1 = object : LogPasteProvider {
            override val name: String = "Fail1"
            override suspend fun upload(content: String, title: String?): Result<String> {
                return Result.Error(AppError.Network("Endpoint 1 unreachable"))
            }
        }
        val failingProvider2 = object : LogPasteProvider {
            override val name: String = "Fail2"
            override suspend fun upload(content: String, title: String?): Result<String> {
                return Result.Error(AppError.Network("Endpoint 2 unreachable"))
            }
        }

        val composite = CompositeLogPasteProvider(listOf(failingProvider1, failingProvider2))
        val result = composite.upload("sample log content")

        assertTrue(result is Result.Error)
        assertEquals("Endpoint 2 unreachable", (result as Result.Error).error.message)
    }
}
