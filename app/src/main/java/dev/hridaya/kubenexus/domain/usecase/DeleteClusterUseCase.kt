package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.repository.ClusterRepository
import kotlinx.coroutines.withContext

class DeleteClusterUseCase(
    private val repository: ClusterRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(clusterId: String): Result<Unit> =
        withContext(dispatcherProvider.io) {
            repository.deleteCluster(clusterId)
        }
}
