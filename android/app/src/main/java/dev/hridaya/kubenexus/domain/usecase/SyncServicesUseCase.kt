package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.repository.ServiceRepository
import javax.inject.Inject

/**
 * Pulls the live Service list into the Room cache. Screens pair this with
 * `getServicesStream` so reads stay offline-first.
 */
class SyncServicesUseCase @Inject constructor(
    private val serviceRepository: ServiceRepository,
) {

    suspend operator fun invoke(clusterId: String?, namespace: String?): Result<Unit> {
        return serviceRepository.syncServices(clusterId, namespace)
    }
}
