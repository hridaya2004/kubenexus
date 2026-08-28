package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.Pod
import dev.hridaya.kubenexus.domain.repository.PodRepository
import javax.inject.Inject

class GetPodsBySelectorUseCase @Inject constructor(
    private val podRepository: PodRepository,
) {
    suspend operator fun invoke(
        clusterId: String?,
        namespace: String?,
        labelSelector: String,
    ): Result<List<Pod>> {
        return podRepository.getPodsBySelector(clusterId, namespace, labelSelector)
    }
}
