package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.core.nativebridge.ClusterHealth
import dev.hridaya.kubenexus.domain.repository.ClusterRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CheckClusterHealthUseCase @Inject constructor(
    private val repository: ClusterRepository,
    private val dispatcherProvider: DispatcherProvider,
) {
    suspend fun checkHealth(clusterId: String): Result<ClusterHealth> =
        withContext(dispatcherProvider.io) {
            repository.checkClusterHealth(clusterId)
        }

    suspend fun checkHealthByKubeconfig(kubeconfigRaw: String): Result<ClusterHealth> =
        withContext(dispatcherProvider.io) {
            repository.checkClusterHealthByKubeconfig(kubeconfigRaw)
        }
}
