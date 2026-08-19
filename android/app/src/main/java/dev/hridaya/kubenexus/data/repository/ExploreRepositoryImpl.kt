package dev.hridaya.kubenexus.data.repository

import android.util.Log
import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.core.nativebridge.KubeNexusNativeBridge
import dev.hridaya.kubenexus.core.security.KubeconfigEncryptor
import dev.hridaya.kubenexus.core.security.LogSanitizer
import dev.hridaya.kubenexus.core.security.NoOpKubeconfigEncryptor
import dev.hridaya.kubenexus.data.mapper.toDomain
import dev.hridaya.kubenexus.data.mapper.toEntity
import dev.hridaya.kubenexus.data.source.local.dao.APIResourceDao
import dev.hridaya.kubenexus.data.source.local.dao.ClusterDao
import dev.hridaya.kubenexus.data.source.local.dao.ExplainedResourceDao
import dev.hridaya.kubenexus.domain.model.APIResource
import dev.hridaya.kubenexus.domain.model.ResourceExplain
import dev.hridaya.kubenexus.domain.repository.ExploreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ExploreRepositoryImpl @Inject constructor(
    private val clusterDao: ClusterDao,
    private val apiResourceDao: APIResourceDao,
    private val explainedResourceDao: ExplainedResourceDao,
    private val nativeBridge: KubeNexusNativeBridge,
    private val encryptor: KubeconfigEncryptor = NoOpKubeconfigEncryptor,
    private val dispatcherProvider: DispatcherProvider,
) : ExploreRepository {

    companion object {
        private const val TAG = "ExploreRepositoryImpl"
        private const val OFFLINE_CLUSTER_ID = "offline"
    }

    override fun getAPIResourcesStream(clusterId: String?): Flow<List<APIResource>> {
        val resolvedClusterId = clusterId ?: OFFLINE_CLUSTER_ID
        return apiResourceDao.getAPIResourcesStream(resolvedClusterId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getLastRefreshedStream(clusterId: String?): Flow<Long?> {
        val resolvedClusterId = clusterId ?: OFFLINE_CLUSTER_ID
        val key = "${resolvedClusterId}_api_resources"
        return apiResourceDao.getSyncMetadataStream(key)
    }

    override suspend fun fetchAPIResources(clusterId: String?): Result<List<APIResource>> =
        withContext(dispatcherProvider.io) {
            val resolvedClusterId = clusterId ?: OFFLINE_CLUSTER_ID
            val decryptedKubeconfig = if (clusterId != null) {
                val cluster = clusterDao.getClusterById(clusterId)
                if (cluster != null) encryptor.decrypt(cluster.rawKubeconfig) else ""
            } else ""

            try {
                val nativeResult = nativeBridge.listAPIResources(decryptedKubeconfig)
                if (nativeResult.isSuccess) {
                    val resources = nativeResult.getOrNull() ?: emptyList()
                    val entities = resources.map { it.toEntity(resolvedClusterId) }
                    apiResourceDao.syncAPIResources(
                        clusterId = resolvedClusterId,
                        resources = entities,
                        timestamp = System.currentTimeMillis(),
                    )

                    val activeIdentifiers = resources.flatMap {
                        listOf(
                            it.name.lowercase(),
                            it.kind.lowercase(),
                            it.singularName.lowercase()
                        )
                    }.filter { it.isNotBlank() }.distinct()

                    if (activeIdentifiers.isEmpty()) {
                        explainedResourceDao.deleteExplainedResourcesForCluster(resolvedClusterId)
                    } else {
                        explainedResourceDao.deleteOrphanedExplainedResources(
                            resolvedClusterId,
                            activeIdentifiers
                        )
                    }

                    Result.Success(resources)
                } else {
                    val error = nativeResult.exceptionOrNull()
                    val sanitizedMsg = LogSanitizer.sanitize(error?.message)
                    Result.Error(AppError.Network(sanitizedMsg.ifEmpty { "Failed to discover API resources" }))
                }
            } catch (t: Throwable) {
                val sanitizedMsg = LogSanitizer.sanitize(t.message)
                Log.e(TAG, "Failed to fetch API resources: $sanitizedMsg", t)
                Result.Error(AppError.Network(sanitizedMsg.ifEmpty { "Failed to discover API resources" }))
            }
        }

    override fun getExplainedResourceStream(
        clusterId: String?,
        resourceOrKind: String,
        groupVersion: String,
    ): Flow<ResourceExplain?> {
        val resolvedClusterId = clusterId ?: OFFLINE_CLUSTER_ID
        val normalizedResourceOrKind = resourceOrKind.trim().lowercase()
        return explainedResourceDao.getExplainedResourceStream(
            clusterId = resolvedClusterId,
            resourceOrKind = normalizedResourceOrKind,
            groupVersion = groupVersion,
        ).map { entity -> entity?.toDomain() }
    }

    override suspend fun getCachedExplainedResource(
        clusterId: String?,
        resourceOrKind: String,
        groupVersion: String,
    ): ResourceExplain? = withContext(dispatcherProvider.io) {
        val resolvedClusterId = clusterId ?: OFFLINE_CLUSTER_ID
        val normalizedResourceOrKind = resourceOrKind.trim().lowercase()
        explainedResourceDao.getExplainedResource(
            clusterId = resolvedClusterId,
            resourceOrKind = normalizedResourceOrKind,
            groupVersion = groupVersion,
        )?.toDomain()
    }

    override suspend fun explainResource(
        clusterId: String?,
        resourceOrKind: String,
        groupVersion: String,
    ): Result<ResourceExplain> = withContext(dispatcherProvider.io) {
        val resolvedClusterId = clusterId ?: OFFLINE_CLUSTER_ID
        val normalizedResourceOrKind = resourceOrKind.trim().lowercase()

        val decryptedKubeconfig = if (clusterId != null) {
            val cluster = clusterDao.getClusterById(clusterId)
            if (cluster != null) encryptor.decrypt(cluster.rawKubeconfig) else ""
        } else ""

        try {
            val nativeResult =
                nativeBridge.explainResource(decryptedKubeconfig, resourceOrKind, groupVersion)
            if (nativeResult.isSuccess) {
                val explain = nativeResult.getOrNull()
                if (explain != null) {
                    explainedResourceDao.insertExplainedResource(
                        explain.toEntity(resolvedClusterId, normalizedResourceOrKind),
                    )
                    Result.Success(explain)
                } else {
                    val cached = explainedResourceDao.getExplainedResource(
                        resolvedClusterId,
                        normalizedResourceOrKind,
                        groupVersion
                    )
                    if (cached != null) {
                        Result.Success(cached.toDomain())
                    } else {
                        Result.Error(AppError.NotFound("Documentation for $resourceOrKind not found"))
                    }
                }
            } else {
                val error = nativeResult.exceptionOrNull()
                val sanitizedMsg = LogSanitizer.sanitize(error?.message)
                Result.Error(AppError.Network(sanitizedMsg.ifEmpty { "Failed to explain resource $resourceOrKind" }))
            }
        } catch (t: Throwable) {
            val sanitizedMsg = LogSanitizer.sanitize(t.message)
            Log.e(TAG, "Failed to explain resource '$resourceOrKind': $sanitizedMsg", t)
            Result.Error(AppError.Network(sanitizedMsg.ifEmpty { "Failed to explain resource" }))
        }
    }
}
