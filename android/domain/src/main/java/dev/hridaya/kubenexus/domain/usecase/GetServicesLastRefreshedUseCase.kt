package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.domain.repository.ServiceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetServicesLastRefreshedUseCase @Inject constructor(
    private val serviceRepository: ServiceRepository,
) {
    operator fun invoke(clusterId: String?): Flow<Long?> {
        return serviceRepository.getLastRefreshedStream(clusterId)
    }
}
