package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.repository.PodRepository
import javax.inject.Inject

class GetPodLogsUseCase @Inject constructor(private val podRepository: PodRepository) {
    suspend operator fun invoke(
        clusterId: String?,
        namespace: String,
        podName: String,
        containerName: String? = null
    ): Result<String> {
        return podRepository.getPodLogs(clusterId, namespace, podName, containerName)
    }
}
