package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.PodDetails
import dev.hridaya.kubenexus.domain.repository.PodRepository

class DescribePodUseCase(
    private val podRepository: PodRepository
) {
    suspend operator fun invoke(
        clusterId: String?,
        namespace: String,
        podName: String
    ): Result<PodDetails> {
        return podRepository.describePod(clusterId, namespace, podName)
    }
}
