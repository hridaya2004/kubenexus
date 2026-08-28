package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.CommandExecResult
import dev.hridaya.kubenexus.domain.model.Pod
import dev.hridaya.kubenexus.domain.model.PodDetails
import dev.hridaya.kubenexus.domain.model.PodMetricSample
import dev.hridaya.kubenexus.domain.model.PodStatus
import dev.hridaya.kubenexus.domain.model.TerminalSession
import dev.hridaya.kubenexus.domain.repository.PodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetPodsBySelectorUseCaseTest {

    private class FakePodRepository : PodRepository {
        var capturedClusterId: String? = null
        var capturedNamespace: String? = null
        var capturedSelector: String? = null
        var podsToReturn: List<Pod> = emptyList()

        override suspend fun getPodsBySelector(
            clusterId: String?,
            namespace: String?,
            labelSelector: String,
        ): Result<List<Pod>> {
            capturedClusterId = clusterId
            capturedNamespace = namespace
            capturedSelector = labelSelector
            return Result.Success(podsToReturn)
        }

        override fun getPodsStream(clusterId: String?, namespace: String?): Flow<List<Pod>> = flowOf(emptyList())
        override fun getNamespacesStream(clusterId: String?): Flow<List<String>> = flowOf(emptyList())
        override fun getLastRefreshedStream(clusterId: String?): Flow<Long?> = flowOf(null)
        override suspend fun refreshWorkloads(clusterId: String?, namespace: String?): Result<Unit> = Result.Success(Unit)
        override suspend fun listPodsBySelector(rawKubeconfig: String, namespace: String?, labelSelector: String): Result<List<Pod>> = Result.Success(emptyList())
        override suspend fun describePod(clusterId: String?, namespace: String, podName: String): Result<PodDetails> = Result.Error(AppError.NotFound("not found"))
        override suspend fun getPodMetrics(clusterId: String?, namespace: String?): Result<List<PodMetricSample>> = Result.Success(emptyList())
        override suspend fun getSinglePodMetrics(clusterId: String?, namespace: String, podName: String): Result<PodMetricSample?> = Result.Success(null)
        override suspend fun deletePod(clusterId: String?, namespace: String, podName: String): Result<Unit> = Result.Success(Unit)
        override suspend fun deleteNamespace(clusterId: String?, namespace: String): Result<Unit> = Result.Success(Unit)
        override suspend fun createNamespace(clusterId: String?, name: String): Result<Unit> = Result.Success(Unit)
        override suspend fun createPodFromManifest(clusterId: String?, manifestYaml: String): Result<Unit> = Result.Success(Unit)
        override suspend fun getPodLogs(clusterId: String?, namespace: String, podName: String, containerName: String?, tailLines: Long?): Result<String> = Result.Success("")
        override fun streamPodLogs(clusterId: String?, namespace: String, podName: String, containerName: String?, tailLines: Long?): Flow<String> = flowOf()
        override suspend fun execCommand(clusterId: String?, namespace: String, podName: String, containerName: String, command: String, stdin: String): Result<CommandExecResult> = Result.Error(AppError.Unknown("not implemented"))
        override suspend fun startTerminalSession(clusterId: String?, namespace: String, podName: String, containerName: String, onStdout: (String) -> Unit, onStderr: (String) -> Unit, onError: (String) -> Unit, onDone: () -> Unit): Result<TerminalSession> = Result.Error(AppError.Unknown("not implemented"))
        override suspend fun startExecSession(clusterId: String?, namespace: String, podName: String, containerName: String, command: String, tty: Boolean, onStdout: (String) -> Unit, onStderr: (String) -> Unit, onError: (String) -> Unit, onDone: () -> Unit): Result<TerminalSession> = Result.Error(AppError.Unknown("not implemented"))
    }

    @Test
    fun `delegates selector query to pod repository`() = runTest {
        val repo = FakePodRepository()
        val expectedPod = Pod(
            id = "pod-1",
            name = "nginx-123",
            namespace = "web",
            status = PodStatus.RUNNING,
            readyContainers = "1/1",
            restarts = 0,
            creationTimestampMillis = 1000L,
        )
        repo.podsToReturn = listOf(expectedPod)
        val useCase = GetPodsBySelectorUseCase(repo)

        val result = useCase(clusterId = "c1", namespace = "web", labelSelector = "app=nginx")

        assertTrue(result is Result.Success)
        assertEquals(listOf(expectedPod), (result as Result.Success).data)
        assertEquals("c1", repo.capturedClusterId)
        assertEquals("web", repo.capturedNamespace)
        assertEquals("app=nginx", repo.capturedSelector)
    }
}
