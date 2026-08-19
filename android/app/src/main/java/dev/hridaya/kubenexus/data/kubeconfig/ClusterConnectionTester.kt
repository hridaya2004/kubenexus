package dev.hridaya.kubenexus.data.kubeconfig

import android.annotation.SuppressLint
import android.util.Log
import dev.hridaya.kubenexus.core.nativebridge.KubeNexusNativeBridge
import dev.hridaya.kubenexus.core.security.LogSanitizer
import dev.hridaya.kubenexus.domain.model.ParsedKubeconfig
import java.io.PrintWriter
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.inject.Inject

open class ClusterConnectionTester @Inject constructor(private val nativeBridge: KubeNexusNativeBridge) {

    companion object {
        private const val TAG = "ClusterConnTester"
        private const val CONNECT_TIMEOUT_MS = 6000
        private const val READ_TIMEOUT_MS = 6000
    }

    /**
     * Attempts to verify connectivity to the Kubernetes cluster API server specified in the parsed kubeconfig.
     * Also verifies native client bridge readiness.
     * Returns a descriptive string on success, or throws an exception with detailed error trace on failure.
     */
    open fun testConnection(parsed: ParsedKubeconfig): String {
        Log.d(
            TAG,
            "Testing connection to server: ${parsed.serverUrl} (context: ${parsed.contextName})",
        )

        val clientResult = nativeBridge.createClient(parsed.rawKubeconfig)
        if (clientResult.isSuccess) {
            Log.d(
                TAG,
                "Native Client_ instance successfully created for cluster: ${parsed.clusterName}",
            )
        } else {
            Log.w(
                TAG,
                "Native Client_ instance init note: ${LogSanitizer.sanitize(clientResult.exceptionOrNull()?.message)}",
            )
        }

        val rawUrl = parsed.serverUrl.trim()
        val normalizedUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            rawUrl
        } else {
            "https://$rawUrl"
        }

        val versionEndpoint =
            if (normalizedUrl.endsWith("/")) "${normalizedUrl}version" else "$normalizedUrl/version"

        val targetUrl = try {
            URL(versionEndpoint)
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "Invalid Kubernetes Server URL: '$normalizedUrl'.\nError: ${
                    LogSanitizer.sanitize(
                        e.message,
                    )
                }",
                e,
            )
        }

        var connection: HttpURLConnection? = null
        try {
            connection = targetUrl.openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "KubeNexus/1.0 (Android)")
            connection.setRequestProperty("Accept", "application/json, text/plain, */*")

            // Extract bearer token if present
            val token = extractToken(parsed.rawKubeconfig)
            if (token.isNotBlank()) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }

            // Trust all certs for probe if insecure or self-signed, configure client certificates for mTLS
            if (connection is HttpsURLConnection) {
                configureTls(connection, parsed.rawKubeconfig)
            }

            val responseCode = connection.responseCode
            val responseMessage = connection.responseMessage.orEmpty()

            // 200 OK, 401 Unauthorized, 403 Forbidden all indicate the API server is up and responding!
            if (responseCode in 200..499) {
                val statusDetail = when (responseCode) {
                    200 -> "API Server reachable & healthy (HTTP 200 OK)"
                    401 -> "API Server reachable (HTTP 401: Authentication required / Token expired)"
                    403 -> "API Server reachable (HTTP 403: Forbidden / Insufficient RBAC permissions)"
                    else -> "API Server responded (HTTP $responseCode $responseMessage)"
                }
                return statusDetail
            } else {
                throw IllegalStateException("API Server returned unexpected error response: HTTP $responseCode $responseMessage")
            }
        } catch (e: Throwable) {
            // Re-throw formatted with sanitized diagnostic information
            val stringWriter = StringWriter()
            e.printStackTrace(PrintWriter(stringWriter))
            val sanitizedTrace = LogSanitizer.sanitize(stringWriter.toString().take(1200))
            val sanitizedErrorMsg =
                LogSanitizer.sanitize(e.message) ?: "Connection timed out or host unreachable"
            val errorReport = buildString {
                appendLine("Failed to connect to Kubernetes Cluster: '${parsed.clusterName}'")
                appendLine("Target Server: $normalizedUrl")
                appendLine("Context: ${parsed.contextName}")
                appendLine("Error Type: ${e.javaClass.simpleName}")
                appendLine("Message: $sanitizedErrorMsg")
                appendLine("\n--- Stack Trace ---")
                appendLine(sanitizedTrace)
            }
            throw Exception(errorReport, e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun extractToken(content: String): String {
        val regex =
            Regex("""(?:^|\n)\s*token\s*:\s*["']?([^"'\r\n#\s]+)["']?""", RegexOption.IGNORE_CASE)
        return regex.find(content)?.groupValues?.get(1)?.trim().orEmpty()
    }

    @SuppressLint("TrustAllX509TrustManager", "CustomX509TrustManager", "BadHostnameVerifier")
    private fun configureTls(httpsConnection: HttpsURLConnection, rawKubeconfig: String) {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                override fun checkClientTrusted(certs: Array<X509Certificate>?, authType: String?) {
                }

                override fun checkServerTrusted(certs: Array<X509Certificate>?, authType: String?) {
                }
            })

            val keyManagers = createKeyManagersFromKubeconfig(rawKubeconfig)

            val sc = SSLContext.getInstance("TLS")
            sc.init(keyManagers, trustAllCerts, SecureRandom())
            httpsConnection.sslSocketFactory = sc.socketFactory
            httpsConnection.setHostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            Log.w(TAG, "TLS configuration fallback: ${LogSanitizer.sanitize(e.message)}")
        }
    }

    private fun createKeyManagersFromKubeconfig(rawKubeconfig: String): Array<KeyManager>? {
        return try {
            val certDataRegex = Regex(
                """client-certificate-data\s*:\s*["']?([^"'\r\n#\s]+)["']?""",
                RegexOption.IGNORE_CASE,
            )
            val keyDataRegex = Regex(
                """client-key-data\s*:\s*["']?([^"'\r\n#\s]+)["']?""",
                RegexOption.IGNORE_CASE,
            )

            val certBase64 =
                certDataRegex.find(rawKubeconfig)?.groupValues?.get(1)?.trim().orEmpty()
            val keyBase64 = keyDataRegex.find(rawKubeconfig)?.groupValues?.get(1)?.trim().orEmpty()

            if (certBase64.isBlank() || keyBase64.isBlank()) return null

            val cert = PemKeyParser.parseCertificate(certBase64)
            val privateKey = PemKeyParser.parsePrivateKey(keyBase64)

            val keyStore =
                KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)
            keyStore.setKeyEntry("client-key", privateKey, "".toCharArray(), arrayOf(cert))

            val kmf =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            kmf.init(keyStore, "".toCharArray())
            kmf.keyManagers
        } catch (e: Exception) {
            Log.e(TAG, "Client mTLS cert setup failed: ${LogSanitizer.sanitize(e.message)}")
            null
        }
    }
}
