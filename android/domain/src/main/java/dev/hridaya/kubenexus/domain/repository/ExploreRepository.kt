package dev.hridaya.kubenexus.domain.repository

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.APIResource
import dev.hridaya.kubenexus.domain.model.ResourceExplain
import kotlinx.coroutines.flow.Flow

interface ExploreRepository {
    fun getAPIResourcesStream(clusterId: String?): Flow<List<APIResource>>
    fun getLastRefreshedStream(clusterId: String?): Flow<Long?>
    suspend fun fetchAPIResources(clusterId: String?): Result<List<APIResource>>
    fun getExplainedResourceStream(
        clusterId: String?,
        resourceOrKind: String,
        groupVersion: String = "",
    ): Flow<ResourceExplain?>

    suspend fun getCachedExplainedResource(
        clusterId: String?,
        resourceOrKind: String,
        groupVersion: String = "",
    ): ResourceExplain?

    suspend fun explainResource(
        clusterId: String?,
        resourceOrKind: String,
        groupVersion: String = "",
        forceRefresh: Boolean = false,
    ): Result<ResourceExplain>
}
