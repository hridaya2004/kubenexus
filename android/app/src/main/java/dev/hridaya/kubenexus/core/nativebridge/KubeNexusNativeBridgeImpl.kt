package dev.hridaya.kubenexus.core.nativebridge

import android.content.Context
import android.util.Log
import client.Client
import client.Client_
import client.ExecCallback
import client.ExecResult
import client.ExecSession
import client.LogCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.core.security.LogSanitizer
import dev.hridaya.kubenexus.domain.model.APIResource
import dev.hridaya.kubenexus.domain.model.ResourceExplain
import go.Seq
import javax.inject.Inject
import javax.inject.Singleton
import client.Namespace as NativeNamespace
import client.Pod as NativePod
import client.PodDetails as NativePodDetails

/**
 * Concrete implementation of [KubeNexusNativeBridge] that bridges calls to the Go Mobile
 * runtime and kubenexus.aar native client library.
 */
@Singleton
class KubeNexusNativeBridgeImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val jsonParser: NativeBridgeJsonParser = NativeBridgeJsonParser(),
) : KubeNexusNativeBridge {

    companion object {
        private const val TAG = "KubeNexusNativeBridge"
    }

    private var initialized = false

    override fun initialize() {
        try {
            Seq.setContext(context)
            Client.touch()
            initialized = true
            Log.d(TAG, "Successfully initialized Go runtime Seq context and Client package")
        } catch (t: Throwable) {
            initialized = false
            Log.e(
                TAG,
                "Failed to initialize native Go runtime: ${LogSanitizer.sanitize(t.message)}",
                t,
            )
        }
    }

    override fun isAvailable(): Boolean = initialized

    override fun touch(): Boolean {
        return try {
            Client.touch()
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to touch native Client: ${LogSanitizer.sanitize(t.message)}", t)
            false
        }
    }

    private fun ensureInitialized() {
        if (!initialized) {
            initialize()
        }
    }

    private inline fun <T> nativeCatching(errorMsg: String, block: () -> T): Result<T> {
        return try {
            ensureInitialized()
            Result.Success(block())
        } catch (t: Throwable) {
            Log.e(TAG, "$errorMsg: ${LogSanitizer.sanitize(t.message)}", t)
            Result.Error(AppError.Unknown(t.message ?: errorMsg, t))
        }
    }

    override fun createClient(rawKubeconfig: String): Result<Client_> =
        nativeCatching("Failed to create Client_ instance") {
            Client.newClient(rawKubeconfig)
        }

    override fun createClientWithOptions(
        rawKubeconfig: String,
        timeoutSec: Long,
        insecure: Boolean
    ): Result<Client_> =
        nativeCatching("Failed to create Client_ with options") {
            Client.newClientWithOptions(
                rawKubeconfig.toByteArray(Charsets.UTF_8),
                timeoutSec,
                insecure,
            )
        }

    override fun listPods(rawKubeconfig: String, namespace: String?): Result<List<String>> =
        nativeCatching("Failed to listPods from native client") {
            val client = Client.newClient(rawKubeconfig)
            val ns = normalizeNamespace(namespace)
            val nativeList = client.listPods(ns)
            val result = mutableListOf<String>()
            val len = nativeList.len()
            for (i in 0 until len) {
                val item = nativeList.get(i)
                if (!item.isNullOrBlank()) {
                    result.add(item)
                }
            }
            result
        }

    override fun listPodsWide(rawKubeconfig: String, namespace: String?): Result<List<NativePod>> =
        nativeCatching("Failed to listPodsWide from native client") {
            val client = Client.newClient(rawKubeconfig)
            val ns = normalizeNamespace(namespace)
            val nativeList = client.listPodsWide(ns)
            val result = mutableListOf<NativePod>()
            val len = nativeList.len()
            for (i in 0 until len) {
                val pod = nativeList.get(i)
                if (pod != null) {
                    result.add(pod)
                }
            }
            result
        }

    override fun listNamespaces(rawKubeconfig: String): Result<List<NativeNamespace>> =
        nativeCatching("Failed to listNamespaces from native client") {
            val client = Client.newClient(rawKubeconfig)
            val nativeList = client.listNamespaces()
            val result = mutableListOf<NativeNamespace>()
            val len = nativeList.len()
            for (i in 0 until len) {
                val ns = nativeList.get(i)
                if (ns != null) {
                    result.add(ns)
                }
            }
            result
        }

    override fun deleteNamespace(
        rawKubeconfig: String,
        namespace: String
    ): Result<Unit> =
        nativeCatching("Failed to deleteNamespace '$namespace' from native client") {
            val client = Client.newClient(rawKubeconfig)
            client.deleteNamespace(namespace)
        }

    override fun listAPIResources(rawKubeconfig: String): Result<List<APIResource>> =
        nativeCatching("Failed to listAPIResources from native client") {
            val client = Client.newClient(rawKubeconfig)
            val jsonStr = client.listAPIResourcesJSON()
            jsonParser.parseAPIResources(jsonStr)
        }

    override fun explainResource(
        rawKubeconfig: String,
        resourceOrKind: String,
        groupVersion: String,
    ): Result<ResourceExplain> =
        nativeCatching("Failed to explainResource '$resourceOrKind' from native client") {
            val client = Client.newClient(rawKubeconfig)
            val jsonStr = client.explainResourceJSON(resourceOrKind, groupVersion)
            jsonParser.parseResourceExplain(
                jsonStr = jsonStr,
                fallbackKind = resourceOrKind,
                fallbackGroupVersion = groupVersion,
            )
        }

    override fun describePod(
        rawKubeconfig: String,
        namespace: String,
        podName: String
    ): Result<NativePodDetails> =
        nativeCatching("Failed to describePod '$podName' from native client") {
            val client = Client.newClient(rawKubeconfig)
            client.describePod(namespace, podName)
        }

    override fun deletePod(
        rawKubeconfig: String,
        namespace: String,
        podName: String
    ): Result<Unit> =
        nativeCatching("Failed to deletePod '$podName' from native client") {
            val client = Client.newClient(rawKubeconfig)
            client.deletePod(namespace, podName)
        }

    override fun getPodLogs(
        rawKubeconfig: String,
        namespace: String,
        podName: String,
        container: String?
    ): Result<String> =
        nativeCatching("Failed to getPodLogs for '$podName' from native client") {
            val client = Client.newClient(rawKubeconfig)
            client.logs(namespace, podName, container.orEmpty())
        }

    override fun streamPodLogs(
        rawKubeconfig: String,
        namespace: String,
        podName: String,
        container: String?,
        callback: LogCallback,
    ): Result<Unit> =
        nativeCatching("Failed to streamPodLogs for '$podName' from native client") {
            val client = Client.newClient(rawKubeconfig)
            client.streamLogs(namespace, podName, container.orEmpty(), callback)
        }

    override fun exec(
        rawKubeconfig: String,
        namespace: String,
        podName: String,
        container: String,
        command: String,
        stdin: String,
    ): Result<ExecResult> =
        nativeCatching("Failed to exec command in pod '$podName'") {
            val client = Client.newClient(rawKubeconfig)
            client.exec(namespace, podName, container, command, stdin)
        }

    override fun startTerminal(
        rawKubeconfig: String,
        namespace: String,
        podName: String,
        container: String,
        callback: ExecCallback,
    ): Result<ExecSession> =
        nativeCatching("Failed to start terminal session for pod '$podName'") {
            val client = Client.newClient(rawKubeconfig)
            client.startTerminal(namespace, podName, container, callback)
        }

    override fun startExecSession(
        rawKubeconfig: String,
        namespace: String,
        podName: String,
        container: String,
        command: String,
        tty: Boolean,
        callback: ExecCallback,
    ): Result<ExecSession> =
        nativeCatching("Failed to start exec session for pod '$podName'") {
            val client = Client.newClient(rawKubeconfig)
            client.startExecSession(namespace, podName, container, command, tty, callback)
        }

    override fun ping(rawKubeconfig: String): Result<String> =
        nativeCatching("Failed to ping cluster") {
            val client = Client.newClient(rawKubeconfig)
            client.ping()
        }

    override fun checkLivez(rawKubeconfig: String): Result<Boolean> =
        nativeCatching("Failed to check /livez") {
            val client = Client.newClient(rawKubeconfig)
            client.checkLivez()
        }

    override fun checkReadyz(rawKubeconfig: String): Result<Boolean> =
        nativeCatching("Failed to check /readyz") {
            val client = Client.newClient(rawKubeconfig)
            client.checkReadyz()
        }

    override fun checkHealthz(rawKubeconfig: String): Result<Boolean> =
        nativeCatching("Failed to check /healthz") {
            val client = Client.newClient(rawKubeconfig)
            client.checkHealthz()
        }

    override fun serverVersion(rawKubeconfig: String): Result<String> =
        nativeCatching("Failed to retrieve server version") {
            val client = Client.newClient(rawKubeconfig)
            client.serverVersion()
        }

    override fun checkHealth(rawKubeconfig: String): Result<ClusterHealth> =
        nativeCatching("Failed to check cluster health") {
            val client = Client.newClient(rawKubeconfig)
            val jsonStr = client.checkHealthJSON()
            jsonParser.parseClusterHealth(jsonStr)
        }

    private fun normalizeNamespace(namespace: String?): String {
        return if (namespace.isNullOrBlank() ||
            namespace == "All Namespaces" ||
            namespace.equals("all", ignoreCase = true)
        ) {
            ""
        } else {
            namespace.trim()
        }
    }
}
