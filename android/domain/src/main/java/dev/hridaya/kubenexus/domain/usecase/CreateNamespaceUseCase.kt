package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.repository.PodRepository
import javax.inject.Inject

/** Creates an empty namespace on the active cluster. */
class CreateNamespaceUseCase @Inject constructor(
    private val podRepository: PodRepository,
) {

    suspend operator fun invoke(clusterId: String?, name: String): Result<Unit> {
        return podRepository.createNamespace(clusterId, name)
    }
}
