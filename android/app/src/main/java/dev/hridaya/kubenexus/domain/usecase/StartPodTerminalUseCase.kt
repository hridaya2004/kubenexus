package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.TerminalSession
import dev.hridaya.kubenexus.domain.repository.PodRepository
import javax.inject.Inject

class StartPodTerminalUseCase @Inject constructor(private val podRepository: PodRepository) {
    suspend operator fun invoke(
        clusterId: String?,
        namespace: String,
        podName: String,
        containerName: String,
        onStdout: (String) -> Unit,
        onStderr: (String) -> Unit,
        onError: (String) -> Unit,
        onDone: () -> Unit,
    ): Result<TerminalSession> {
        return podRepository.startTerminalSession(
            clusterId = clusterId,
            namespace = namespace,
            podName = podName,
            containerName = containerName,
            onStdout = onStdout,
            onStderr = onStderr,
            onError = onError,
            onDone = onDone,
        )
    }
}
