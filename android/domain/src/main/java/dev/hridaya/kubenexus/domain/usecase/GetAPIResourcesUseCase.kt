package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.APIResource
import dev.hridaya.kubenexus.domain.repository.ExploreRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAPIResourcesUseCase @Inject constructor(
    private val exploreRepository: ExploreRepository,
) {
    fun getStream(clusterId: String?): Flow<List<APIResource>> {
        return exploreRepository.getAPIResourcesStream(clusterId)
    }

    fun getLastRefreshedStream(clusterId: String?): Flow<Long?> {
        return exploreRepository.getLastRefreshedStream(clusterId)
    }

    suspend fun refresh(clusterId: String?): Result<List<APIResource>> {
        return exploreRepository.fetchAPIResources(clusterId)
    }
}
