package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.domain.repository.DeploymentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDeploymentsLastRefreshedUseCase @Inject constructor(
    private val deploymentRepository: DeploymentRepository,
) {
    operator fun invoke(clusterId: String?): Flow<Long?> {
        return deploymentRepository.getLastRefreshedStream(clusterId)
    }
}
