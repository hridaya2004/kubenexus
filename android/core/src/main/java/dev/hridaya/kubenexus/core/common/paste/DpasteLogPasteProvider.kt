package dev.hridaya.kubenexus.core.common.paste

import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * [LogPasteProvider] backed by dpaste.org's public API.
 *
 * See https://docs.dpaste.org/api/. Pastes are public to anyone with the link and expire
 * after [EXPIRES_SECONDS]; dpaste.org only accepts a fixed set of lifetimes, and its default
 * when none is sent is one month.
 */
class DpasteLogPasteProvider(
    private val endpointUrl: String = "https://dpaste.org/api/",
    private val connectTimeoutMs: Int = 10000,
    private val readTimeoutMs: Int = 30000,
) : LogPasteProvider {

    override val name: String = "dpaste.org"

    override val retention: String = "7 days"

    override suspend fun upload(content: String): Result<String> {
        if (content.isBlank()) {
            return Result.Error(AppError.Validation("Cannot export empty logs"))
        }

        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded; charset=utf-8",
                )
                setRequestProperty("User-Agent", "KubeNexus-Android/1.0")
            }

            val postData = formBody(content).toByteArray(StandardCharsets.UTF_8)
            connection.outputStream.use { os -> os.write(postData) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val resultUrl = connection.inputStream.bufferedReader().use { it.readText() }.trim()
                if (resultUrl.startsWith("https://")) {
                    Result.Success(resultUrl)
                } else {
                    Result.Error(AppError.Network("Invalid response URL from dpaste.org"))
                }
            } else {
                Result.Error(AppError.Network("dpaste.org returned status $responseCode"))
            }
        } catch (e: Exception) {
            Result.Error(AppError.Network("dpaste.org upload failed: ${e.localizedMessage ?: e.message}"))
        } finally {
            connection?.disconnect()
        }
    }

    internal companion object {
        /** 7 days, one of the lifetimes dpaste.org accepts (3600, 604800, 2592000). */
        const val EXPIRES_SECONDS = 604800

        fun formBody(content: String): String = buildString {
            append("format=url")
            append("&lexer=_text")
            append("&expires=").append(EXPIRES_SECONDS)
            append("&content=").append(URLEncoder.encode(content, "UTF-8"))
        }
    }
}
