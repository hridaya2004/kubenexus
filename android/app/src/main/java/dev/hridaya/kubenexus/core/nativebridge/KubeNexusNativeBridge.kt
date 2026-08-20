package dev.hridaya.kubenexus.core.nativebridge

import client.Client_
import client.ExecCallback
import client.ExecResult
import client.ExecSession
import client.LogCallback
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.APIResource
import dev.hridaya.kubenexus.domain.model.ResourceExplain
import client.Namespace as NativeNamespace
import client.Pod as NativePod
import client.PodDetails as NativePodDetails

/**
 * Bridge interface defining the contract for interacting with the native Go runtime
 * provided by kubenexus.aar.
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
