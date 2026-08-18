package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.repository.ClusterRepository

class UpdateClusterNameUseCase(private val clusterRepository: ClusterRepository) {
    suspend operator fun invoke(clusterId: String, newName: String): Result<Unit> {
        return clusterRepository.updateClusterName(clusterId, newName)
    }
}
