package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.ServiceDetails
import dev.hridaya.kubenexus.domain.repository.ServiceRepository
import javax.inject.Inject

/** Describes one Service, including ports and best-effort events. */
class GetServiceDetailsUseCase @Inject constructor(
    private val serviceRepository: ServiceRepository,
) {

    suspend operator fun invoke(
        clusterId: String?,
        namespace: String,
        name: String,
    ): Result<ServiceDetails> {
        return serviceRepository.getServiceDetails(clusterId, namespace, name)
    }
}
