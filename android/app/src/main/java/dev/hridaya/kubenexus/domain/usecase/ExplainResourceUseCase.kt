package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.ResourceExplain
import dev.hridaya.kubenexus.domain.repository.ExploreRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ExplainResourceUseCase @Inject constructor(
    private val exploreRepository: ExploreRepository,
) {
    suspend operator fun invoke(
        clusterId: String?,
        resourceOrKind: String,
        groupVersion: String = "",
    ): Result<ResourceExplain> {
        return exploreRepository.explainResource(clusterId, resourceOrKind, groupVersion)
    }

    suspend fun getCached(
        clusterId: String?,
        resourceOrKind: String,
        groupVersion: String = "",
    ): ResourceExplain? {
        return exploreRepository.getCachedExplainedResource(clusterId, resourceOrKind, groupVersion)
    }

    fun getStream(
        clusterId: String?,
        resourceOrKind: String,
        groupVersion: String = "",
    ): Flow<ResourceExplain?> {
        return exploreRepository.getExplainedResourceStream(clusterId, resourceOrKind, groupVersion)
    }
}
