package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.repository.PodRepository

class DeletePodUseCase(private val podRepository: PodRepository) {
    suspend operator fun invoke(
        clusterId: String?,
        namespace: String,
        podName: String
    ): Result<Unit> {
        return podRepository.deletePod(clusterId, namespace, podName)
    }
}
