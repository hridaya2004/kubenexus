package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.repository.PodRepository
import javax.inject.Inject

class RefreshWorkloadsUseCase @Inject constructor(private val podRepository: PodRepository) {
    suspend operator fun invoke(clusterId: String?, namespace: String? = null): Result<Unit> {
        return podRepository.refreshWorkloads(clusterId, namespace)
    }
}
