package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.CommandExecResult
import dev.hridaya.kubenexus.domain.repository.PodRepository

class ExecPodCommandUseCase(
    private val podRepository: PodRepository
) {
    suspend operator fun invoke(
        clusterId: String?,
        namespace: String,
        podName: String,
        containerName: String,
        command: String,
        stdin: String = ""
    ): Result<CommandExecResult> {
        return podRepository.execCommand(clusterId, namespace, podName, containerName, command, stdin)
    }
}
