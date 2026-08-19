package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.repository.PodRepository
import javax.inject.Inject

class DeleteNamespaceUseCase @Inject constructor(private val podRepository: PodRepository) {
    suspend operator fun invoke(
        clusterId: String?,
        namespace: String,
    ): Result<Unit> {
        return podRepository.deleteNamespace(clusterId, namespace)
    }
}
