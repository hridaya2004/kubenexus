package dev.hridaya.kubenexus.data.repository

import android.util.Log
import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.core.nativebridge.KubeNexusNativeBridge
import dev.hridaya.kubenexus.data.mapper.toDomain
import dev.hridaya.kubenexus.data.mapper.toDomainName
import dev.hridaya.kubenexus.data.mapper.toEntity
import dev.hridaya.kubenexus.data.source.local.dao.ClusterDao
import dev.hridaya.kubenexus.data.source.local.dao.NamespaceDao
import dev.hridaya.kubenexus.data.source.local.dao.PodDao
import dev.hridaya.kubenexus.data.source.local.entity.NamespaceEntity
import dev.hridaya.kubenexus.data.source.remote.KubernetesApiClient
import dev.hridaya.kubenexus.domain.model.Pod
import dev.hridaya.kubenexus.domain.repository.PodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PodRepositoryImpl(
    private val clusterDao: ClusterDao,
    private val podDao: PodDao,
    private val namespaceDao: NamespaceDao,
    private val apiClient: KubernetesApiClient,
    private val nativeBridge: KubeNexusNativeBridge,
    private val dispatcherProvider: DispatcherProvider
) : PodRepository {

    companion object {
        private const val TAG = "PodRepositoryImpl"
    }

    override fun getPodsStream(clusterId: String?, namespace: String?): Flow<List<Pod>> {
        if (clusterId == null) return flowOf(emptyList())

        val stream = if (namespace.isNullOrBlank() || namespace == "All Namespaces") {
            podDao.getPodsStream(clusterId)
        } else {
            podDao.getPodsByNamespaceStream(clusterId, namespace)
        }

        return stream.map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(dispatcherProvider.io)
    }

    override fun getNamespacesStream(clusterId: String?): Flow<List<String>> {
        if (clusterId == null) return flowOf(listOf("All Namespaces"))

        return namespaceDao.getNamespacesStream(clusterId).map { entities ->
            val nsList = entities.map { it.toDomainName() }.filter { it.isNotBlank() }.distinct().sorted()
            if (nsList.isNotEmpty()) {
                listOf("All Namespaces") + nsList
            } else {
                listOf("All Namespaces", "default", "kube-system")
            }
        }.flowOn(dispatcherProvider.io)
    }

    override fun getLastRefreshedStream(clusterId: String?): Flow<Long?> {
        if (clusterId == null) return flowOf(null)
        return podDao.getSyncMetadataStream("${clusterId}_pods").flowOn(dispatcherProvider.io)
    }

    override suspend fun refreshWorkloads(clusterId: String?, namespace: String?): Result<Unit> {
        return withContext(dispatcherProvider.io) {
            if (clusterId == null) {
                return@withContext Result.Error(AppError.NotFound("No active cluster specified"))
            }

            val cluster = clusterDao.getClusterById(clusterId)
                ?: return@withContext Result.Error(AppError.NotFound("Cluster with ID '$clusterId' not found"))

            try {
                // 1. Fetch live Pods (try native or REST API)
                val livePods = try {
                    val nativeResult = nativeBridge.listPodsWide(namespace).getOrNull()
                    if (!nativeResult.isNullOrEmpty()) {
                        nativeResult.map { it.toDomain() }
                    } else {
                        apiClient.fetchPods(
                            serverUrl = cluster.serverUrl,
                            rawKubeconfig = cluster.rawKubeconfig,
                            namespace = namespace
                        )
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to fetch live pods: ${t.message}", t)
                    throw t
                }

                // 2. Fetch live Namespaces
                val liveNamespaces = try {
                    val nativeNsResult = nativeBridge.listNamespaces().getOrNull()
                    if (!nativeNsResult.isNullOrEmpty()) {
                        nativeNsResult.map { it.toDomainName() }
                    } else {
                        apiClient.fetchNamespaces(
                            serverUrl = cluster.serverUrl,
                            rawKubeconfig = cluster.rawKubeconfig
                        )
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "Failed to fetch live namespaces: ${t.message}")
                    emptyList()
                }

                // 3. Persist to Room local database
                val podEntities = livePods.map { it.toEntity(clusterId) }
                podDao.syncPods(
                    clusterId = clusterId,
                    namespace = namespace,
                    pods = podEntities,
                    timestamp = System.currentTimeMillis()
                )

                if (liveNamespaces.isNotEmpty()) {
                    val namespaceEntities = liveNamespaces
                        .filter { it != "All Namespaces" }
                        .map { name ->
                            NamespaceEntity(
                                id = "${clusterId}_$name",
                                clusterId = clusterId,
                                name = name
                            )
                        }
                    namespaceDao.syncNamespaces(clusterId, namespaceEntities)
                }

                Result.Success(Unit)
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to refresh workloads for cluster '$clusterId': ${t.message}", t)
                Result.Error(AppError.Network(t.message ?: "Failed to connect to cluster API"))
            }
        }
    }
}
