package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.domain.model.ServiceSummary
import dev.hridaya.kubenexus.domain.repository.ServiceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observes the offline-first Service list for the active cluster. Emits from
 * the Room cache immediately and re-emits on every [ServiceRepository.syncServices]
 * write, so callers never block on the network to render.
 */
class GetServicesStreamUseCase @Inject constructor(
    private val serviceRepository: ServiceRepository,
) {
    operator fun invoke(
        clusterId: String?,
        namespace: String? = null,
    ): Flow<List<ServiceSummary>> {
        return serviceRepository.getServicesStream(clusterId, namespace)
    }
}
