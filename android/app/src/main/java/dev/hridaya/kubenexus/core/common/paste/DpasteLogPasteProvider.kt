package dev.hridaya.kubenexus.core.common.paste

import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * [LogPasteProvider] implementation using dpaste.org's public API (7-day retention).
 */
class DpasteLogPasteProvider(
    private val endpointUrl: String = "https://dpaste.org/api/",
    private val expiryDays: Int = 7,
    private val connectTimeoutMs: Int = 10000,
    private val readTimeoutMs: Int = 10000,
) : LogPasteProvider {

    override val name: String = "dpaste.org"

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
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
                setRequestProperty("User-Agent", "KubeNexus-Android/1.0")
            }

            val postData = buildString {
                append("format=url")
                append("&expiry_days=").append(expiryDays)
                title?.let { append("&title=").append(URLEncoder.encode(it, "UTF-8")) }
                append("&content=").append(URLEncoder.encode(content, "UTF-8"))
            }.toByteArray(StandardCharsets.UTF_8)

            connection.outputStream.use { os ->
                os.write(postData)
                os.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val resultUrl = connection.inputStream.bufferedReader().use { it.readText() }.trim()
                if (resultUrl.isNotBlank() && resultUrl.startsWith("http")) {
                    Result.Success(resultUrl)
                } else {
                    Result.Error(AppError.Network("Invalid response URL from dpaste.org"))
                }
            } else {
                Result.Error(AppError.Network("dpaste.org returned status $responseCode"))
            }
        } catch (e: Exception) {
            Result.Error(AppError.Network("dpaste.org upload failed: ${e.localizedMessage ?: e.message}"))
        }
    }
}
