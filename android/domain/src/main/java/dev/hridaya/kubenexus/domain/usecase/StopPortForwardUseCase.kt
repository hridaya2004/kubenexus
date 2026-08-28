package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.repository.PortForwardRepository
import javax.inject.Inject

/** Terminates the port-forward session identified by [handleId]. */
class StopPortForwardUseCase @Inject constructor(private val repository: PortForwardRepository) {
    suspend operator fun invoke(handleId: String): Result<Unit> {
        return repository.stop(handleId)
    }
}
