package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.domain.repository.PodRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class StreamPodLogsUseCase @Inject constructor(private val podRepository: PodRepository) {
    operator fun invoke(
        clusterId: String?,
        namespace: String,
        podName: String,
        containerName: String? = null
    ): Flow<String> {
        return podRepository.streamPodLogs(clusterId, namespace, podName, containerName)
    }
}
