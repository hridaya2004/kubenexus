package dev.hridaya.kubenexus.domain.repository

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.CommandExecResult
import dev.hridaya.kubenexus.domain.model.Pod
import dev.hridaya.kubenexus.domain.model.PodDetails
import dev.hridaya.kubenexus.domain.model.TerminalSession
import kotlinx.coroutines.flow.Flow

interface PodRepository {
    fun getPodsStream(clusterId: String?, namespace: String? = null): Flow<List<Pod>>
    fun getNamespacesStream(clusterId: String?): Flow<List<String>>
    fun getLastRefreshedStream(clusterId: String?): Flow<Long?>
    suspend fun refreshWorkloads(clusterId: String?, namespace: String? = null): Result<Unit>
    suspend fun describePod(
        clusterId: String?,
        namespace: String,
        podName: String
    ): Result<PodDetails>

    suspend fun deletePod(clusterId: String?, namespace: String, podName: String): Result<Unit>
    suspend fun getPodLogs(
        clusterId: String?,
        namespace: String,
        podName: String,
        containerName: String? = null
    ): Result<String>

    fun streamPodLogs(
        clusterId: String?,
        namespace: String,
        podName: String,
        containerName: String? = null
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
