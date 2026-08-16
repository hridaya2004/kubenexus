package dev.hridaya.kubenexus.data.repository

import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.nativebridge.KubeNexusNativeBridge
import dev.hridaya.kubenexus.data.mapper.toDomain
import dev.hridaya.kubenexus.data.mapper.toDomainName
import dev.hridaya.kubenexus.data.source.local.dao.ClusterDao
import dev.hridaya.kubenexus.data.source.remote.KubernetesApiClient
import dev.hridaya.kubenexus.domain.model.Pod
import dev.hridaya.kubenexus.domain.repository.PodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class PodRepositoryImpl(
    private val clusterDao: ClusterDao,
    private val apiClient: KubernetesApiClient,
    private val nativeBridge: KubeNexusNativeBridge,
    private val dispatcherProvider: DispatcherProvider
) : PodRepository {

    override fun getPodsStream(clusterId: String?, namespace: String?): Flow<List<Pod>> = flow {
        if (clusterId == null) {
            emit(emptyList())
            return@flow
        }

        val cluster = clusterDao.getClusterById(clusterId)
        if (cluster == null) {
            emit(emptyList())
            return@flow
        }

        // 1. Check native library result first
        val nativePodsResult = nativeBridge.listPodsWide(namespace)
        val nativePods = nativePodsResult.getOrNull()
        if (!nativePods.isNullOrEmpty()) {
            emit(nativePods.map { it.toDomain() })
            return@flow
        }

        // 2. Fetch live pods directly from active Kubernetes cluster
        val livePods = apiClient.fetchPods(
            serverUrl = cluster.serverUrl,
            rawKubeconfig = cluster.rawKubeconfig,
            namespace = namespace
        )

        emit(livePods)
    }.flowOn(dispatcherProvider.io)

    override fun getNamespacesStream(clusterId: String?): Flow<List<String>> = flow {
        if (clusterId == null) {
            emit(listOf("All Namespaces"))
            return@flow
        }

        val cluster = clusterDao.getClusterById(clusterId)
        if (cluster == null) {
            emit(listOf("All Namespaces"))
            return@flow
        }

        // 1. Check native library result first
        val nativeNamespacesResult = nativeBridge.listNamespaces()
        val nativeNamespaces = nativeNamespacesResult.getOrNull()
        if (!nativeNamespaces.isNullOrEmpty()) {
            emit(listOf("All Namespaces") + nativeNamespaces.map { it.toDomainName() }.distinct())
            return@flow
        }

        // 2. Fetch live namespaces directly from active Kubernetes cluster
        val liveNamespaces = apiClient.fetchNamespaces(
            serverUrl = cluster.serverUrl,
            rawKubeconfig = cluster.rawKubeconfig
        )

        emit(liveNamespaces)
    }.flowOn(dispatcherProvider.io)
}
