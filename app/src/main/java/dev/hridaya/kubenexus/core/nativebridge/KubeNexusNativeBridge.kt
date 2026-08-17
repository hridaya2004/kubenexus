package dev.hridaya.kubenexus.core.nativebridge

import android.content.Context
import android.util.Log
import client.Client
import client.Client_
import client.ExecCallback
import client.ExecResult
import client.ExecSession
import client.LogCallback
import client.Namespace as NativeNamespace
import client.Pod as NativePod
import client.PodDetails as NativePodDetails
import go.Seq

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
    fun createClientWithOptions(rawKubeconfig: String, timeoutSec: Long = 30, insecure: Boolean = false): Result<Client_>

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
     * Describes a pod in detail from native runtime.
     */
    fun describePod(rawKubeconfig: String, namespace: String, podName: String): Result<NativePodDetails>

    /**
     * Deletes a pod from native runtime.
     */
    fun deletePod(rawKubeconfig: String, namespace: String, podName: String): Result<Unit>

    /**
     * Fetches historical logs for a pod container.
     */
    fun getPodLogs(rawKubeconfig: String, namespace: String, podName: String, container: String? = null): Result<String>

    /**
     * Streams live logs for a pod container via callback.
     */
    fun streamPodLogs(
        rawKubeconfig: String,
        namespace: String,
        podName: String,
        container: String? = null,
        callback: LogCallback
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
        stdin: String = ""
    ): Result<ExecResult>

    /**
     * Starts an interactive terminal shell session attached to the container.
     */
    fun startTerminal(
        rawKubeconfig: String,
        namespace: String,
        podName: String,
        container: String,
        callback: ExecCallback
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
        callback: ExecCallback
    ): Result<ExecSession>
}

class KubeNexusNativeBridgeImpl(
    private val context: Context
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
            Log.e(TAG, "Failed to initialize native Go runtime: ${t.message}", t)
        }
    }

    override fun isAvailable(): Boolean = initialized

    override fun touch(): Boolean {
        return try {
            Client.touch()
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to touch native Client: ${t.message}", t)
            false
        }
    }

    private fun ensureInitialized() {
        if (!initialized) {
            initialize()
        }
    }

    override fun createClient(rawKubeconfig: String): Result<Client_> {
        return runCatching {
            ensureInitialized()
            Client.newClient(rawKubeconfig)
        }.onFailure { error ->
            Log.e(TAG, "Failed to create Client_ instance: ${error.message}", error)
        }
    }

    override fun createClientWithOptions(rawKubeconfig: String, timeoutSec: Long, insecure: Boolean): Result<Client_> {
        return runCatching {
            ensureInitialized()
            Client.newClientWithOptions(rawKubeconfig.toByteArray(Charsets.UTF_8), timeoutSec, insecure)
        }.onFailure { error ->
            Log.e(TAG, "Failed to create Client_ with options: ${error.message}", error)
        }
    }

    override fun listPods(rawKubeconfig: String, namespace: String?): Result<List<String>> {
        return runCatching {
            ensureInitialized()
            val client = Client.newClient(rawKubeconfig)
            val nativeList = client.listPods(namespace.orEmpty())
            val result = mutableListOf<String>()
            val len = nativeList.len()
            for (i in 0 until len) {
                val item = nativeList.get(i)
                if (!item.isNullOrBlank()) {
                    result.add(item)
                }
            }
            result
        }.onFailure { error ->
            Log.e(TAG, "Failed to listPods from native client: ${error.message}", error)
        }
    }

    override fun listPodsWide(rawKubeconfig: String, namespace: String?): Result<List<NativePod>> {
        return runCatching {
            ensureInitialized()
            val client = Client.newClient(rawKubeconfig)
            val nativeList = client.listPodsWide(namespace.orEmpty())
            val result = mutableListOf<NativePod>()
            val len = nativeList.len()
            for (i in 0 until len) {
                val pod = nativeList.get(i)
                if (pod != null) {
                    result.add(pod)
                }
            }
            result
        }.onFailure { error ->
            Log.e(TAG, "Failed to listPodsWide from native client: ${error.message}", error)
        }
    }

    override fun listNamespaces(rawKubeconfig: String): Result<List<NativeNamespace>> {
        return runCatching {
            ensureInitialized()
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
        }.onFailure { error ->
            Log.e(TAG, "Failed to listNamespaces from native client: ${error.message}", error)
        }
    }

    override fun describePod(rawKubeconfig: String, namespace: String, podName: String): Result<NativePodDetails> {
        return runCatching {
            ensureInitialized()
            val client = Client.newClient(rawKubeconfig)
            client.describePod(namespace, podName)
        }.onFailure { error ->
            Log.e(TAG, "Failed to describePod '$podName' from native client: ${error.message}", error)
        }
    }

    override fun deletePod(rawKubeconfig: String, namespace: String, podName: String): Result<Unit> {
        return runCatching {
            ensureInitialized()
            val client = Client.newClient(rawKubeconfig)
            client.deletePod(namespace, podName)
        }.onFailure { error ->
            Log.e(TAG, "Failed to deletePod '$podName' from native client: ${error.message}", error)
        }
    }

    override fun getPodLogs(rawKubeconfig: String, namespace: String, podName: String, container: String?): Result<String> {
        return runCatching {
            ensureInitialized()
            val client = Client.newClient(rawKubeconfig)
            client.logs(namespace, podName, container.orEmpty())
        }.onFailure { error ->
            Log.e(TAG, "Failed to getPodLogs for '$podName' from native client: ${error.message}", error)
        }
    }

    override fun streamPodLogs(
        rawKubeconfig: String,
        namespace: String,
        podName: String,
        container: String?,
        callback: LogCallback
    ): Result<Unit> {
        return runCatching {
            ensureInitialized()
            val client = Client.newClient(rawKubeconfig)
            client.streamLogs(namespace, podName, container.orEmpty(), callback)
        }.onFailure { error ->
            Log.e(TAG, "Failed to streamPodLogs for '$podName' from native client: ${error.message}", error)
        }
    }

    override fun exec(
        rawKubeconfig: String,
        namespace: String,
        podName: String,
        container: String,
        command: String,
        stdin: String
    ): Result<ExecResult> {
        return runCatching {
            ensureInitialized()
            val client = Client.newClient(rawKubeconfig)
            client.exec(namespace, podName, container, command, stdin)
        }.onFailure { error ->
            Log.e(TAG, "Failed to exec command in pod '$podName': ${error.message}", error)
        }
    }

    override fun startTerminal(
        rawKubeconfig: String,
        namespace: String,
        podName: String,
        container: String,
        callback: ExecCallback
    ): Result<ExecSession> {
        return runCatching {
            ensureInitialized()
            val client = Client.newClient(rawKubeconfig)
            client.startTerminal(namespace, podName, container, callback)
        }.onFailure { error ->
            Log.e(TAG, "Failed to start terminal session for pod '$podName': ${error.message}", error)
        }
    }

    override fun startExecSession(
        rawKubeconfig: String,
        namespace: String,
        podName: String,
        container: String,
        command: String,
        tty: Boolean,
        callback: ExecCallback
    ): Result<ExecSession> {
        return runCatching {
            ensureInitialized()
            val client = Client.newClient(rawKubeconfig)
            client.startExecSession(namespace, podName, container, command, tty, callback)
        }.onFailure { error ->
            Log.e(TAG, "Failed to start exec session for pod '$podName': ${error.message}", error)
        }
    }
}
