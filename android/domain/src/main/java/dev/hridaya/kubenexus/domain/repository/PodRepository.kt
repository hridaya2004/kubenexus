package dev.hridaya.kubenexus.domain.repository

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.CommandExecResult
import dev.hridaya.kubenexus.domain.model.Pod
import dev.hridaya.kubenexus.domain.model.PodDetails
import dev.hridaya.kubenexus.domain.model.PodMetricSample
import dev.hridaya.kubenexus.domain.model.TerminalSession
import kotlinx.coroutines.flow.Flow

interface PodRepository {
    fun getPodsStream(clusterId: String?, namespace: String? = null): Flow<List<Pod>>
    fun getNamespacesStream(clusterId: String?): Flow<List<String>>
    fun getLastRefreshedStream(clusterId: String?): Flow<Long?>
    suspend fun refreshWorkloads(clusterId: String?, namespace: String? = null): Result<Unit>
    suspend fun listPodsBySelector(
        rawKubeconfig: String,
        namespace: String?,
        labelSelector: String,
    ): Result<List<Pod>>

    suspend fun describePod(
        clusterId: String?,
        namespace: String,
        podName: String
    ): Result<PodDetails>

    suspend fun getPodMetrics(clusterId: String?, namespace: String?): Result<List<PodMetricSample>>

    /**
     * Usage for a single pod. Preferred over [getPodMetrics] when polling one
     * pod, since it does not transfer usage for the whole namespace.
     */
    suspend fun getSinglePodMetrics(
        clusterId: String?,
        namespace: String,
        podName: String,
    ): Result<PodMetricSample?>

    suspend fun deletePod(clusterId: String?, namespace: String, podName: String): Result<Unit>
    suspend fun deleteNamespace(clusterId: String?, namespace: String): Result<Unit>

    /**
     * Creates an empty namespace with the given name. The name must be a valid
     * DNS-1123 label; callers validate UX-side, the API server has final say.
     */
    suspend fun createNamespace(clusterId: String?, name: String): Result<Unit>

    /**
     * Creates the Pod described by [manifestYaml] (the exact text the user
     * reviewed). The reviewed manifest carries its own namespace in
     * metadata.namespace, so none is passed separately. The manifest may be
     * YAML or JSON.
     */
    suspend fun createPodFromManifest(clusterId: String?, manifestYaml: String): Result<Unit>

    suspend fun getPodLogs(
        clusterId: String?,
        namespace: String,
        podName: String,
        containerName: String? = null,
        tailLines: Long? = null,
    ): Result<String>

    fun streamPodLogs(
        clusterId: String?,
        namespace: String,
        podName: String,
        containerName: String? = null,
        tailLines: Long? = null,
    ): Flow<String>

    suspend fun execCommand(
        clusterId: String?,
        namespace: String,
        podName: String,
        containerName: String,
        command: String,
        stdin: String = "",
    ): Result<CommandExecResult>

    suspend fun startTerminalSession(
        clusterId: String?,
        namespace: String,
        podName: String,
        containerName: String,
        onStdout: (String) -> Unit,
        onStderr: (String) -> Unit,
        onError: (String) -> Unit,
        onDone: () -> Unit,
    ): Result<TerminalSession>

    suspend fun startExecSession(
        clusterId: String?,
        namespace: String,
        podName: String,
        containerName: String,
        command: String,
        tty: Boolean,
        onStdout: (String) -> Unit,
        onStderr: (String) -> Unit,
        onError: (String) -> Unit,
        onDone: () -> Unit,
    ): Result<TerminalSession>
}
