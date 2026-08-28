package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.DeploymentDetails
import dev.hridaya.kubenexus.domain.model.DeploymentSummary
import dev.hridaya.kubenexus.domain.repository.DeploymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaleDeploymentUseCaseTest {

    private class FakeDeploymentRepository : DeploymentRepository {
        var capturedClusterId: String? = null
        var capturedNamespace: String? = null
        var capturedName: String? = null
        var capturedReplicas: Int? = null
        var resultToReturn: Result<Unit> = Result.Success(Unit)

        override suspend fun scaleDeployment(
            clusterId: String?,
            namespace: String,
            name: String,
            replicas: Int,
        ): Result<Unit> {
            capturedClusterId = clusterId
            capturedNamespace = namespace
            capturedName = name
            capturedReplicas = replicas
            return resultToReturn
        }

        override suspend fun createFromManifest(clusterId: String?, manifestYaml: String): Result<Unit> = Result.Success(Unit)
        override suspend fun getDeployments(clusterId: String?, namespace: String?): Result<List<DeploymentSummary>> = Result.Success(emptyList())
        override fun getDeploymentsStream(clusterId: String?, namespace: String?): Flow<List<DeploymentSummary>> = flowOf(emptyList())
        override suspend fun syncDeployments(clusterId: String?, namespace: String?): Result<Unit> = Result.Success(Unit)
        override suspend fun getDeploymentDetails(clusterId: String?, namespace: String, name: String): Result<DeploymentDetails> =
            Result.Error(AppError.NotFound("not found"))
        override fun getLastRefreshedStream(clusterId: String?): Flow<Long?> = flowOf(null)
        override suspend fun restartDeployment(clusterId: String?, namespace: String, name: String): Result<Unit> = Result.Success(Unit)
        override suspend fun deleteDeployment(clusterId: String?, namespace: String, name: String): Result<Unit> = Result.Success(Unit)
    }

    @Test
    fun `rejects negative replicas without delegating to repository`() = runTest {
        val repo = FakeDeploymentRepository()
        val useCase = ScaleDeploymentUseCase(repo)

        val result = useCase(clusterId = "c1", namespace = "default", name = "nginx", replicas = -1)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Validation)
        assertEquals("Replica count cannot be negative", result.error.message)
        assertEquals(null, repo.capturedReplicas)
    }

    @Test
    fun `delegates valid replica scale to repository`() = runTest {
        val repo = FakeDeploymentRepository()
        val useCase = ScaleDeploymentUseCase(repo)

        val result = useCase(clusterId = "c1", namespace = "prod", name = "api-server", replicas = 3)

        assertTrue(result is Result.Success)
        assertEquals("c1", repo.capturedClusterId)
        assertEquals("prod", repo.capturedNamespace)
        assertEquals("api-server", repo.capturedName)
        assertEquals(3, repo.capturedReplicas)
    }
}
