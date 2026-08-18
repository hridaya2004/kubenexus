package dev.hridaya.kubenexus.data.source.remote

import android.annotation.SuppressLint
import android.util.Base64
import android.util.Log
import dev.hridaya.kubenexus.core.security.LogSanitizer
import dev.hridaya.kubenexus.domain.model.ContainerDetail
import dev.hridaya.kubenexus.domain.model.Pod
import dev.hridaya.kubenexus.domain.model.PodConditionDetail
import dev.hridaya.kubenexus.domain.model.PodDetails
import dev.hridaya.kubenexus.domain.model.PodEventDetail
import dev.hridaya.kubenexus.domain.model.PodStatus
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import org.json.JSONObject

class KubernetesApiClient {

    companion object {
        private const val TAG = "KubernetesApiClient"
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 15000
    }

    fun fetchPods(serverUrl: String, rawKubeconfig: String, namespace: String?): List<Pod> {
        val normalizedServer = normalizeUrl(serverUrl)
        val endpoint = if (namespace.isNullOrBlank() || namespace == "All Namespaces") {
            "$normalizedServer/api/v1/pods"
        } else {
            "$normalizedServer/api/v1/namespaces/$namespace/pods"
        }

        val jsonString = executeGet(endpoint, rawKubeconfig)
        return parsePodsJson(jsonString)
    }

    fun fetchNamespaces(serverUrl: String, rawKubeconfig: String): List<String> {
        val normalizedServer = normalizeUrl(serverUrl)
        val endpoint = "$normalizedServer/api/v1/namespaces"

        return try {
            val jsonString = executeGet(endpoint, rawKubeconfig)
            parseNamespacesJson(jsonString)
        } catch (t: Throwable) {
            Log.w(
                TAG,
                "Failed to fetch namespaces from $endpoint: ${LogSanitizer.sanitize(t.message)}",
            )
            listOf("All Namespaces", "default", "kube-system")
        }
    }

    fun describePod(serverUrl: String, rawKubeconfig: String, namespace: String, podName: String): PodDetails {
        val normalizedServer = normalizeUrl(serverUrl)
        val podEndpoint = "$normalizedServer/api/v1/namespaces/$namespace/pods/$podName"
        val eventsEndpoint =
            "$normalizedServer/api/v1/namespaces/$namespace/events?fieldSelector=involvedObject.name=$podName"

        val podJson = executeGet(podEndpoint, rawKubeconfig)
        val eventsJson = try {
            executeGet(eventsEndpoint, rawKubeconfig)
        } catch (ignored: Exception) {
            null
        }

        return parsePodDetails(podJson, eventsJson, namespace, podName)
    }

    fun fetchPodLogs(
        serverUrl: String,
        rawKubeconfig: String,
        namespace: String,
        podName: String,
        containerName: String? = null,
        tailLines: Int = 1000,
    ): String {
        val normalizedServer = normalizeUrl(serverUrl)
        val containerParam = if (!containerName.isNullOrBlank()) "&container=$containerName" else ""
        val endpoint =
            "$normalizedServer/api/v1/namespaces/$namespace/pods/$podName/log?tailLines=$tailLines$containerParam"
        return executeGet(endpoint, rawKubeconfig)
    }

    fun streamPodLogs(
        serverUrl: String,
        rawKubeconfig: String,
        namespace: String,
        podName: String,
        containerName: String? = null,
    ): Flow<String> = flow {
        val normalizedServer = normalizeUrl(serverUrl)
        val containerParam = if (!containerName.isNullOrBlank()) "&container=$containerName" else ""
        val endpoint =
            "$normalizedServer/api/v1/namespaces/$namespace/pods/$podName/log?follow=true&tailLines=200$containerParam"

        var connection: HttpURLConnection? = null
        try {
            val url = URL(endpoint)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = 0 // Infinite for streaming
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "KubeNexus/1.0 (Android)")
            connection.setRequestProperty("Accept", "text/plain, */*")

            val token = extractToken(rawKubeconfig)
            if (token.isNotBlank()) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            } else {
                val basicAuth = extractBasicAuth(rawKubeconfig)
                if (basicAuth.isNotBlank()) {
                    connection.setRequestProperty("Authorization", "Basic $basicAuth")
                }
            }

            if (connection is HttpsURLConnection) {
                configureSsl(connection, rawKubeconfig)
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                var line: String? = null
                while (currentCoroutineContext().isActive &&
                    reader.readLine()
                        .also { line = it } != null
                ) {
                    emit(line ?: "")
                }
            } else {
                val err =
                    connection.errorStream?.let { BufferedReader(InputStreamReader(it)).readText() }
                        ?: ""
                emit("Error streaming logs (HTTP $responseCode): ${LogSanitizer.sanitize(err)}")
            }
        } catch (t: Throwable) {
            emit("Log stream terminated: ${LogSanitizer.sanitize(t.message)}")
        } finally {
            connection?.disconnect()
        }
    }

    private fun executeGet(endpointUrl: String, rawKubeconfig: String): String {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(endpointUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "KubeNexus/1.0 (Android)")
            connection.setRequestProperty("Accept", "application/json, text/plain, */*")

            val token = extractToken(rawKubeconfig)
            if (token.isNotBlank()) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            } else {
                val basicAuth = extractBasicAuth(rawKubeconfig)
                if (basicAuth.isNotBlank()) {
                    connection.setRequestProperty("Authorization", "Basic $basicAuth")
                }
            }

            if (connection is HttpsURLConnection) {
                configureSsl(connection, rawKubeconfig)
            }

            val responseCode = connection.responseCode
            val responseMessage = connection.responseMessage.orEmpty()

            if (responseCode in 200..299) {
                return BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    reader.readText()
                }
            } else {
                val errorBody = connection.errorStream?.let { stream ->
                    BufferedReader(InputStreamReader(stream)).use { it.readText() }
                }.orEmpty()

                val failureMsg = when (responseCode) {
                    401 -> "Authentication failed (HTTP 401 Unauthorized): Check token or credentials."
                    403 -> "Forbidden (HTTP 403): User lacks RBAC permissions for $endpointUrl."
                    404 -> "Resource not found (HTTP 404): $endpointUrl"
                    else ->
                        "API server returned HTTP $responseCode $responseMessage ${
                            LogSanitizer.sanitize(
                                errorBody.take(200),
                            )
                        }"
                }
                Log.e(TAG, LogSanitizer.sanitize(failureMsg))
                throw RuntimeException(failureMsg)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to execute GET $endpointUrl: ${LogSanitizer.sanitize(t.message)}", t)
            throw t
        } finally {
            connection?.disconnect()
        }
    }

    private fun parsePodsJson(jsonString: String): List<Pod> {
        val podsList = mutableListOf<Pod>()
        try {
            val root = JSONObject(jsonString)
            val items = root.optJSONArray("items") ?: return emptyList()

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val metadata = item.optJSONObject("metadata") ?: continue
                val status = item.optJSONObject("status")
                val spec = item.optJSONObject("spec")

                val name = metadata.optString("name", "unknown")
                val namespace = metadata.optString("namespace", "default")
                val creationTimestamp = metadata.optString("creationTimestamp")
                val phase = status?.optString("phase", "Unknown") ?: "Unknown"
                val podIP = status?.optString("podIP")?.takeIf { it.isNotBlank() }
                val nodeName = spec?.optString("nodeName")?.takeIf { it.isNotBlank() }

                var readyCount = 0
                var totalCount = 0
                var restartCount = 0
                var primaryImage: String? = null

                val containerStatuses = status?.optJSONArray("containerStatuses")
                if (containerStatuses != null && containerStatuses.length() > 0) {
                    totalCount = containerStatuses.length()
                    for (j in 0 until containerStatuses.length()) {
                        val cs = containerStatuses.getJSONObject(j)
                        if (cs.optBoolean("ready", false)) {
                            readyCount++
                        }
                        restartCount += cs.optInt("restartCount", 0)
                        if (primaryImage == null) {
                            primaryImage = cs.optString("image").takeIf { it.isNotBlank() }
                        }
                    }
                } else {
                    val containers = spec?.optJSONArray("containers")
                    if (containers != null) {
                        totalCount = containers.length()
                        if (containers.length() > 0) {
                            primaryImage = containers.getJSONObject(0).optString("image")
                                .takeIf { it.isNotBlank() }
                        }
                    }
                }

                val readyStr = if (totalCount > 0) "$readyCount/$totalCount" else "1/1"
                val ageStr = calculateAge(creationTimestamp)
                val podStatus = mapPhaseToStatus(phase, status)

                podsList.add(
                    Pod(
                        id = "${namespace}_$name",
                        name = name,
                        namespace = namespace,
                        status = podStatus,
                        readyContainers = readyStr,
                        restarts = restartCount,
                        age = ageStr,
                        ip = podIP,
                        node = nodeName,
                        image = primaryImage,
                    ),
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Pods JSON: ${e.message}", e)
        }
        return podsList
    }

    private fun parsePodDetails(podJson: String, eventsJson: String?, namespace: String, podName: String): PodDetails {
        val root = JSONObject(podJson)
        val metadata = root.optJSONObject("metadata")
        val spec = root.optJSONObject("spec")
        val status = root.optJSONObject("status")

        val phase = status?.optString("phase", "Running") ?: "Running"
        val podStatus = mapPhaseToStatus(phase, status)
        val nodeName = spec?.optString("nodeName")
        val podIP = status?.optString("podIP")
        val hostIP = status?.optString("hostIP")
        val restartPolicy = spec?.optString("restartPolicy", "Always")
        val startTime = status?.optString("startTime")

        val labels = mutableMapOf<String, String>()
        metadata?.optJSONObject("labels")?.let { obj ->
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                labels[k] = obj.optString(k)
            }
        }

        val annotations = mutableMapOf<String, String>()
        metadata?.optJSONObject("annotations")?.let { obj ->
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                annotations[k] = obj.optString(k)
            }
        }

        val containerDetails = mutableListOf<ContainerDetail>()
        val containers = spec?.optJSONArray("containers")
        val containerStatuses = status?.optJSONArray("containerStatuses")

        val statusMap = mutableMapOf<String, JSONObject>()
        if (containerStatuses != null) {
            for (i in 0 until containerStatuses.length()) {
                val cs = containerStatuses.getJSONObject(i)
                val cName = cs.optString("name")
                statusMap[cName] = cs
            }
        }

        if (containers != null) {
            for (i in 0 until containers.length()) {
                val c = containers.getJSONObject(i)
                val cName = c.optString("name", "container-$i")
                val image = c.optString("image", "")
                val cs = statusMap[cName]

                val ready = cs?.optBoolean("ready", true) ?: true
                val restartCount = cs?.optInt("restartCount", 0) ?: 0
                val stateObj = cs?.optJSONObject("state")
                val stateStr = when {
                    stateObj?.has("running") == true -> "Running"
                    stateObj?.has("waiting") == true ->
                        "Waiting: ${
                            stateObj.optJSONObject("waiting")?.optString("reason", "Waiting")
                        }"

                    stateObj?.has("terminated") == true -> "Terminated"
                    else -> "Running"
                }

                val portsList = mutableListOf<String>()
                val portsArr = c.optJSONArray("ports")
                if (portsArr != null) {
                    for (p in 0 until portsArr.length()) {
                        val portObj = portsArr.getJSONObject(p)
                        val containerPort = portObj.optInt("containerPort")
                        val protocol = portObj.optString("protocol", "TCP")
                        portsList.add("$containerPort/$protocol")
                    }
                }

                containerDetails.add(
                    ContainerDetail(
                        name = cName,
                        image = image,
                        ready = ready,
                        restartCount = restartCount,
                        state = stateStr,
                        ports = portsList,
                    ),
                )
            }
        }

        val conditionDetails = mutableListOf<PodConditionDetail>()
        val conditionsArr = status?.optJSONArray("conditions")
        if (conditionsArr != null) {
            for (i in 0 until conditionsArr.length()) {
                val cond = conditionsArr.getJSONObject(i)
                conditionDetails.add(
                    PodConditionDetail(
                        type = cond.optString("type"),
                        status = cond.optString("status"),
                        lastTransitionTime = cond.optString("lastTransitionTime"),
                        reason = cond.optString("reason").takeIf { it.isNotBlank() },
                        message = cond.optString("message").takeIf { it.isNotBlank() },
                    ),
                )
            }
        }

        val eventDetails = mutableListOf<PodEventDetail>()
        if (!eventsJson.isNullOrBlank()) {
            try {
                val eventsRoot = JSONObject(eventsJson)
                val items = eventsRoot.optJSONArray("items")
                if (items != null) {
                    for (i in 0 until items.length()) {
                        val ev = items.getJSONObject(i)
                        val evType = ev.optString("type", "Normal")
                        val reason = ev.optString("reason", "")
                        val message = ev.optString("message", "")
                        val lastTimestamp = ev.optString("lastTimestamp").ifBlank {
                            ev.optJSONObject("metadata")?.optString("creationTimestamp")
                        }
                        val age = calculateAge(lastTimestamp)
                        eventDetails.add(
                            PodEventDetail(
                                type = evType,
                                reason = reason,
                                message = message,
                                age = age,
                            ),
                        )
                    }
                }
            } catch (ignored: Exception) {
            }
        }

        val volumesList = mutableListOf<String>()
        val volumesArr = spec?.optJSONArray("volumes")
        if (volumesArr != null) {
            for (i in 0 until volumesArr.length()) {
                val v = volumesArr.getJSONObject(i)
                val vName = v.optString("name")
                if (vName.isNotBlank()) volumesList.add(vName)
            }
        }

        return PodDetails(
            name = metadata?.optString("name", podName) ?: podName,
            namespace = metadata?.optString("namespace", namespace) ?: namespace,
            status = podStatus,
            node = nodeName,
            ip = podIP,
            hostIp = hostIP,
            restartPolicy = restartPolicy,
            startTime = startTime,
            containers = containerDetails,
            conditions = conditionDetails,
            events = eventDetails,
            labels = labels,
            annotations = annotations,
            volumes = volumesList,
            rawDescribeText = podJson,
        )
    }

    private fun parseNamespacesJson(jsonString: String): List<String> {
        val namespaces = mutableListOf<String>()
        try {
            val root = JSONObject(jsonString)
            val items = root.optJSONArray("items") ?: return listOf("All Namespaces")
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val metadata = item.optJSONObject("metadata") ?: continue
                val name = metadata.optString("name")
                if (name.isNotBlank()) {
                    namespaces.add(name)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Namespaces JSON: ${e.message}", e)
        }

        val sorted = namespaces.sorted()
        return if (sorted.isNotEmpty()) {
            listOf("All Namespaces") + sorted
        } else {
            listOf("All Namespaces", "default", "kube-system")
        }
    }

    private fun mapPhaseToStatus(phase: String, status: JSONObject?): PodStatus {
        val containerStatuses = status?.optJSONArray("containerStatuses")
        if (containerStatuses != null) {
            for (i in 0 until containerStatuses.length()) {
                val cs = containerStatuses.getJSONObject(i)
                val state = cs.optJSONObject("state")
                val waiting = state?.optJSONObject("waiting")
                val reason = waiting?.optString("reason")
                if (reason.equals("CrashLoopBackOff", ignoreCase = true)) {
                    return PodStatus.CRASH_LOOP
                }
            }
        }

        return when (phase.lowercase()) {
            "running" -> PodStatus.RUNNING
            "pending" -> PodStatus.PENDING
            "succeeded" -> PodStatus.COMPLETED
            "failed" -> PodStatus.FAILED
            else -> PodStatus.UNKNOWN
        }
    }

    private fun calculateAge(timestamp: String?): String {
        if (timestamp.isNullOrBlank()) return "0m"
        return try {
            val created = Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(timestamp))
            val now = Instant.now()
            val days = ChronoUnit.DAYS.between(created, now)
            if (days > 0) return "${days}d"
            val hours = ChronoUnit.HOURS.between(created, now)
            if (hours > 0) return "${hours}h"
            val minutes = ChronoUnit.MINUTES.between(created, now)
            "${minutes.coerceAtLeast(1)}m"
        } catch (e: Exception) {
            "0m"
        }
    }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        return withScheme.removeSuffix("/")
    }

    private fun extractToken(content: String): String {
        val regex =
            Regex("""(?:^|\n)\s*token\s*:\s*["']?([^"'\r\n#\s]+)["']?""", RegexOption.IGNORE_CASE)
        return regex.find(content)?.groupValues?.get(1)?.trim().orEmpty()
    }

    private fun extractBasicAuth(content: String): String {
        val usernameRegex = Regex(
            """(?:^|\n)\s*username\s*:\s*["']?([^"'\r\n#\s]+)["']?""",
            RegexOption.IGNORE_CASE,
        )
        val passwordRegex = Regex(
            """(?:^|\n)\s*password\s*:\s*["']?([^"'\r\n#\s]+)["']?""",
            RegexOption.IGNORE_CASE,
        )
        val user = usernameRegex.find(content)?.groupValues?.get(1)?.trim().orEmpty()
        val pass = passwordRegex.find(content)?.groupValues?.get(1)?.trim().orEmpty()
        return if (user.isNotBlank() && pass.isNotBlank()) {
            Base64.encodeToString("$user:$pass".toByteArray(), Base64.NO_WRAP)
        } else {
            ""
        }
    }

    @SuppressLint("TrustAllX509TrustManager", "CustomX509TrustManager", "BadHostnameVerifier")
    private fun configureSsl(httpsConnection: HttpsURLConnection, rawKubeconfig: String) {
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
            Log.w(TAG, "SSL configuration fallback: ${LogSanitizer.sanitize(e.message)}")
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

            val cert =
                dev.hridaya.kubenexus.data.kubeconfig.PemKeyParser.parseCertificate(certBase64)
            val privateKey =
                dev.hridaya.kubenexus.data.kubeconfig.PemKeyParser.parsePrivateKey(keyBase64)

            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)
            keyStore.setKeyEntry("client-key", privateKey, "".toCharArray(), arrayOf(cert))

            val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            kmf.init(keyStore, "".toCharArray())
            kmf.keyManagers
        } catch (e: Exception) {
            Log.e(TAG, "Client mTLS cert setup failed: ${LogSanitizer.sanitize(e.message)}")
            null
        }
    }
}
