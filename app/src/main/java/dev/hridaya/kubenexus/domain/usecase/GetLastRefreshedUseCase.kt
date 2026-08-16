package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.domain.repository.PodRepository
import kotlinx.coroutines.flow.Flow

class GetLastRefreshedUseCase(
    private val podRepository: PodRepository
) {
    operator fun invoke(clusterId: String?): Flow<Long?> {
        return podRepository.getLastRefreshedStream(clusterId)
    }
}
