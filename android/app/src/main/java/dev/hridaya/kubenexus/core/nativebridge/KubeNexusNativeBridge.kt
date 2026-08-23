package dev.hridaya.kubenexus.core.nativebridge

import client.ExecCallback
import client.ExecResult
import client.ExecSession
import client.LogCallback
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.APIResource
import dev.hridaya.kubenexus.domain.model.Namespace
import dev.hridaya.kubenexus.domain.model.Pod
import dev.hridaya.kubenexus.domain.model.PodDetails
import dev.hridaya.kubenexus.domain.model.ResourceExplain

/**
 * Bridge interface defining the contract for interacting with the native Go
 * runtime provided by kubenexus.aar.
 *
 * Resource reads cross the boundary as verbatim Kubernetes JSON and are decoded
 * into domain models here, so no `client.*` type appears in a resource signature.
 * The remaining `client.*` references are the streaming primitives — log and exec
 * callbacks and the exec session handle — which are genuinely not expressible as
 * a request/response pair and therefore stay purpose-built Gomobile bindings.
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
     * Lists pods in a namespace, or across all namespaces when [namespace] is
     * null or blank.
     *
     * [labelSelector] and [limit] map directly onto the Kubernetes list options;
     * pass an empty selector and a zero limit to list everything.
     */
    fun listPods(
        rawKubeconfig: String,
        namespace: String? = null,
        labelSelector: String = "",
        limit: Long = 0,
    ): Result<List<Pod>>

    /**
     * Retrieves existing cluster namespaces from native runtime.
     */
    fun listNamespaces(rawKubeconfig: String): Result<List<Namespace>>

    /**
     * Deletes a namespace from native runtime.
     */
    fun deleteNamespace(rawKubeconfig: String, namespace: String): Result<Unit>

    /**
     * Retrieves all discovered Kubernetes API resources from native runtime.
     */
    fun listAPIResources(rawKubeconfig: String): Result<List<APIResource>>

    /**
     * Fetches the cluster's OpenAPI v2 schema document verbatim. The document
     * is large; callers are expected to persist it rather than refetch.
     */
    fun openAPISchemaJSON(rawKubeconfig: String): Result<String>

    /**
     * Describes a pod in detail, including its events.
     */
    fun describePod(
        rawKubeconfig: String,
        namespace: String,
        podName: String,
    ): Result<PodDetails>

    /**
     * Deletes a pod from native runtime.
     */
    fun deletePod(rawKubeconfig: String, namespace: String, podName: String): Result<Unit>

    /**
     * Fetches historical logs for a pod container with optional tail line limit.
     */
    fun getPodLogs(
        rawKubeconfig: String,
        namespace: String,
        podName: String,
        container: String? = null,
        tailLines: Long? = null,
    ): Result<String>

    /**
     * Streams live logs for a pod container via callback with optional tail line limit.
     */
    fun streamPodLogs(
        rawKubeconfig: String,
        namespace: String,
        podName: String,
        container: String? = null,
        tailLines: Long? = null,
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
