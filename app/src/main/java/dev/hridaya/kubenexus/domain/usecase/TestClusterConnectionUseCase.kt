package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.repository.ClusterRepository
import kotlinx.coroutines.withContext

class TestClusterConnectionUseCase(
    private val repository: ClusterRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend fun testKubeconfig(kubeconfigRaw: String): Result<String> = withContext(dispatcherProvider.io) {
        repository.testConnection(kubeconfigRaw)
    }

    suspend fun testCluster(clusterId: String): Result<String> = withContext(dispatcherProvider.io) {
        repository.testClusterById(clusterId)
    }
}
