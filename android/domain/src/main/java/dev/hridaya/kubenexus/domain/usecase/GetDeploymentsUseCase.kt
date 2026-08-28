package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.DeploymentSummary
import dev.hridaya.kubenexus.domain.repository.DeploymentRepository
import javax.inject.Inject

/** Lists Deployment summaries for the workloads screen. */
class GetDeploymentsUseCase @Inject constructor(
    private val deploymentRepository: DeploymentRepository,
) {

    suspend operator fun invoke(clusterId: String?, namespace: String?): Result<List<DeploymentSummary>> {
        return deploymentRepository.getDeployments(clusterId, namespace)
    }
}
