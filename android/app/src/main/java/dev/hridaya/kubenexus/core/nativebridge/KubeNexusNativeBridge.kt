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
import dev.hridaya.kubenexus.domain.model.ResourceField
import go.Seq
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import client.Namespace as NativeNamespace
import client.Pod as NativePod
import client.PodDetails as NativePodDetails

/**
 * Bridge interface for interacting with the native Go runtime provided by kubenexus.aar.
 */
interface KubeNexusNativeBridge {
    /**
     * Initializes the Go Mobile Seq context and loads native client bindings.
     */
    fun initialize()

    /**
     * Returns true if the native runtime and library were successfully loaded and initialized.
     */
    fun isAvailable(): Boolean

    /**
     * Touches the native package to trigger runtime static initialization.
     */
    fun touch(): Boolean

    /**
     * Creates a new instance of [Client_] using the provided kubeconfig content.
     */
    fun createClient(rawKubeconfig: String): Result<Client_>

    /**
     * Creates a new instance of [Client_] with custom timeout and insecure TLS option.
     */
    fun createClientWithOptions(
        rawKubeconfig: String,
        timeoutSec: Long = 30,
        insecure: Boolean = false
    ): Result<Client_>

    /**
     * Retrieves a quick list of pod names for the specified namespace from native runtime.
     */
    fun listPods(rawKubeconfig: String, namespace: String? = null): Result<List<String>>

    /**
     * Retrieves full pod details list from native runtime.
     */
    fun listPodsWide(rawKubeconfig: String, namespace: String? = null): Result<List<NativePod>>

    /**
     * Retrieves existing cluster namespaces from native runtime.
     */
    fun listNamespaces(rawKubeconfig: String): Result<List<NativeNamespace>>

    /**
     * Deletes a namespace from native runtime.
     */
    fun deleteNamespace(rawKubeconfig: String, namespace: String): Result<Unit>

    /**
     * Retrieves all discovered Kubernetes API resources from native runtime.
     */
    fun listAPIResources(rawKubeconfig: String): Result<List<APIResource>>

    /**
     * Retrieves resource explanation details (kubectl explain) from native runtime.
     */
    fun explainResource(
        rawKubeconfig: String,
        resourceOrKind: String,
        groupVersion: String = ""
    ): Result<ResourceExplain>


    /**
     * Describes a pod in detail from native runtime.
     */
    fun describePod(
        rawKubeconfig: String,
        namespace: String,
        podName: String
    ): Result<NativePodDetails>

    /**
     * Deletes a pod from native runtime.
     */
    fun deletePod(rawKubeconfig: String, namespace: String, podName: String): Result<Unit>

    /**
     * Fetches historical logs for a pod container.
     */
    fun getPodLogs(
        rawKubeconfig: String,
        namespace: String,
        podName: String,
        container: String? = null
    ): Result<String>

    /**
     * Streams live logs for a pod container via callback.
     */
    fun streamPodLogs(
        rawKubeconfig: String,
        namespace: String,
        podName: String,
        container: String? = null,
        callback: LogCallback,
    ): Result<Unit>

    /**
     * Executes a non-interactive command inside a container and captures stdout and stderr.
     */
    fun exec(
        rawKubeconfig: String,
        namespace: String,
        podName: String,
        container: String,
        command: String,
        stdin: String = "",
    ): Result<ExecResult>

    /**
     * Starts an interactive terminal shell session attached to the container.
     */
    fun startTerminal(
        rawKubeconfig: String,
        namespace: String,
        podName: String,
        container: String,
        callback: ExecCallback,
    ): Result<ExecSession>

    /**
     * Starts an interactive exec session for a specific command with TTY support.
     */
    fun startExecSession(
        rawKubeconfig: String,
        namespace: String,
        podName: String,
        container: String,
        command: String,
        tty: Boolean,
        callback: ExecCallback,
    ): Result<ExecSession>

    /**
     * Pings the Kubernetes cluster verifying connectivity and health endpoints (/readyz, /livez, /healthz, /version).
     */
    fun ping(rawKubeconfig: String): Result<String>

    /**
     * Performs a liveness check against the /livez endpoint.
     */
    fun checkLivez(rawKubeconfig: String): Result<Boolean>

    /**
     * Performs a readiness check against the /readyz endpoint.
     */
    fun checkReadyz(rawKubeconfig: String): Result<Boolean>

    /**
     * Performs a legacy health check against the /healthz endpoint.
     */
    fun checkHealthz(rawKubeconfig: String): Result<Boolean>

    /**
     * Fetches the Kubernetes server version.
     */
    fun serverVersion(rawKubeconfig: String): Result<String>

    /**
     * Returns detailed cluster health inspection data.
     */
    fun checkHealth(rawKubeconfig: String): Result<ClusterHealth>
}

data class ClusterHealth(
    val livez: Boolean = false,
    val readyz: Boolean = false,
    val healthz: Boolean = false,
    val serverVersion: String = "",
    val statusMessage: String = "",
)

class KubeNexusNativeBridgeImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
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
            val ns =
                if (namespace.isNullOrBlank() ||
                    namespace == "All Namespaces" ||
                    namespace.equals("all", ignoreCase = true)
                ) {
                    ""
                } else {
                    namespace.trim()
                }
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
            val ns =
                if (namespace.isNullOrBlank() ||
                    namespace == "All Namespaces" ||
                    namespace.equals("all", ignoreCase = true)
                ) {
                    ""
                } else {
                    namespace.trim()
                }
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
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<APIResource>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val verbs = mutableListOf<String>()
                val verbsArray = obj.optJSONArray("verbs")
                if (verbsArray != null) {
                    for (v in 0 until verbsArray.length()) {
                        verbs.add(verbsArray.getString(v))
                    }
                }
                val shortNames = mutableListOf<String>()
                val shortNamesArray = obj.optJSONArray("shortNames")
                if (shortNamesArray != null) {
                    for (s in 0 until shortNamesArray.length()) {
                        shortNames.add(shortNamesArray.getString(s))
                    }
                }
                val categories = mutableListOf<String>()
                val categoriesArray = obj.optJSONArray("categories")
                if (categoriesArray != null) {
                    for (c in 0 until categoriesArray.length()) {
                        categories.add(categoriesArray.getString(c))
                    }
                }
                list.add(
                    APIResource(
                        name = obj.optString("name", ""),
                        singularName = obj.optString("singularName", ""),
                        namespaced = obj.optBoolean("namespaced", true),
                        kind = obj.optString("kind", ""),
                        group = obj.optString("group", ""),
                        version = obj.optString("version", ""),
                        groupVersion = obj.optString("groupVersion", ""),
                        verbs = verbs,
                        shortNames = shortNames,
                        categories = categories,
                    )
                )
            }
            list
        }

    override fun explainResource(
        rawKubeconfig: String,
        resourceOrKind: String,
        groupVersion: String,
    ): Result<ResourceExplain> =
        nativeCatching("Failed to explainResource '$resourceOrKind' from native client") {
            val client = Client.newClient(rawKubeconfig)
            val jsonStr = client.explainResourceJSON(resourceOrKind, groupVersion)
            val obj = JSONObject(jsonStr)
            val fieldsList = mutableListOf<ResourceField>()
            val fieldsArray = obj.optJSONArray("fields")
            if (fieldsArray != null) {
                for (i in 0 until fieldsArray.length()) {
                    val fObj = fieldsArray.getJSONObject(i)
                    fieldsList.add(
                        ResourceField(
                            name = fObj.optString("name", ""),
                            type = fObj.optString("type", ""),
                            description = fObj.optString("description", ""),
                            required = fObj.optBoolean("required", false),
                        )
                    )
                }
            }
            ResourceExplain(
                kind = obj.optString("kind", resourceOrKind),
                group = obj.optString("group", ""),
                version = obj.optString("version", ""),
                groupVersion = obj.optString("groupVersion", groupVersion),
                description = obj.optString("description", ""),
                fields = fieldsList,
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
            val obj = JSONObject(jsonStr)
            ClusterHealth(
                livez = obj.optBoolean("livez", false),
                readyz = obj.optBoolean("readyz", false),
                healthz = obj.optBoolean("healthz", false),
                serverVersion = obj.optString("serverVersion", ""),
                statusMessage = obj.optString("statusMessage", ""),
            )
        }
}
