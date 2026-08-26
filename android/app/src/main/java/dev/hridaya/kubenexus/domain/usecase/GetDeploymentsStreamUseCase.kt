package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.domain.model.DeploymentSummary
import dev.hridaya.kubenexus.domain.repository.DeploymentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observes the offline-first Deployment list for the active cluster. Emits
 * from the Room cache immediately and re-emits on every [DeploymentRepository.syncDeployments]
 * write, so callers never block on the network to render.
 */
class GetDeploymentsStreamUseCase @Inject constructor(
    private val deploymentRepository: DeploymentRepository,
) {
    operator fun invoke(
        clusterId: String?,
        namespace: String? = null,
    ): Flow<List<DeploymentSummary>> {
        return deploymentRepository.getDeploymentsStream(clusterId, namespace)
    }
}
