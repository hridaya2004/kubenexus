package dev.hridaya.kubenexus.core.common.paste

import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * [LogPasteProvider] implementation using paste.rs's simple REST API.
 */
class PasteRsLogPasteProvider(
    private val endpointUrl: String = "https://paste.rs",
    private val connectTimeoutMs: Int = 10000,
    private val readTimeoutMs: Int = 10000,
) : LogPasteProvider {

    override val name: String = "paste.rs"

    override suspend fun upload(content: String, title: String?): Result<String> {
        if (content.isBlank()) {
            return Result.Error(AppError.Validation("Cannot export empty logs"))
        }

        return try {
            val url = URL(endpointUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                setRequestProperty("Content-Type", "text/plain; charset=utf-8")
                setRequestProperty("User-Agent", "KubeNexus-Android/1.0")
            }

            connection.outputStream.use { os ->
                os.write(content.toByteArray(StandardCharsets.UTF_8))
                os.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val resultUrl = connection.inputStream.bufferedReader().use { it.readText() }.trim()
                if (resultUrl.isNotBlank() && resultUrl.startsWith("http")) {
                    Result.Success(resultUrl)
                } else {
                    Result.Error(AppError.Network("Invalid response URL from paste.rs"))
                }
            } else {
                Result.Error(AppError.Network("paste.rs returned status $responseCode"))
            }
        } catch (e: Exception) {
            Result.Error(AppError.Network("paste.rs upload failed: ${e.localizedMessage ?: e.message}"))
        }
    }
}
