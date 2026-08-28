package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.repository.DeploymentRepository
import javax.inject.Inject

class ScaleDeploymentUseCase @Inject constructor(
    private val deploymentRepository: DeploymentRepository,
) {
    suspend operator fun invoke(
        clusterId: String?,
        namespace: String,
        name: String,
        replicas: Int,
    ): Result<Unit> {
        if (replicas < 0) {
            return Result.Error(AppError.Validation("Replica count cannot be negative"))
        }
        return deploymentRepository.scaleDeployment(clusterId, namespace, name, replicas)
    }
}
