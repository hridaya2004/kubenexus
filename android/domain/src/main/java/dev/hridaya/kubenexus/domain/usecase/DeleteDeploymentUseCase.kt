package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.repository.DeploymentRepository
import javax.inject.Inject

class DeleteDeploymentUseCase @Inject constructor(
    private val deploymentRepository: DeploymentRepository,
) {
    suspend operator fun invoke(
        clusterId: String?,
        namespace: String,
        name: String,
    ): Result<Unit> {
        return deploymentRepository.deleteDeployment(clusterId, namespace, name)
    }
}
