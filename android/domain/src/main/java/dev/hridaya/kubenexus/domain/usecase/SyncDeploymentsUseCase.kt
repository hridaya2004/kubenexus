package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.repository.DeploymentRepository
import javax.inject.Inject

/**
 * Pulls the live Deployment list into the Room cache. Screens pair this with
 * [GetDeploymentsUseCase]/`getDeploymentsStream` so reads stay offline-first.
 */
class SyncDeploymentsUseCase @Inject constructor(
    private val deploymentRepository: DeploymentRepository,
) {

    suspend operator fun invoke(clusterId: String?, namespace: String?): Result<Unit> {
        return deploymentRepository.syncDeployments(clusterId, namespace)
    }
}
