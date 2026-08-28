package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.DeploymentDetails
import dev.hridaya.kubenexus.domain.repository.DeploymentRepository
import javax.inject.Inject

/** Describes one Deployment, including conditions and best-effort events. */
class GetDeploymentDetailsUseCase @Inject constructor(
    private val deploymentRepository: DeploymentRepository,
) {

    suspend operator fun invoke(
        clusterId: String?,
        namespace: String,
        name: String,
    ): Result<DeploymentDetails> {
        return deploymentRepository.getDeploymentDetails(clusterId, namespace, name)
    }
}
