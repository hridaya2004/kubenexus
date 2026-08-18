package dev.hridaya.kubenexus.data.repository

import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.core.nativebridge.KubeNexusNativeBridge
import dev.hridaya.kubenexus.data.source.local.dao.APIResourceDao
import dev.hridaya.kubenexus.data.source.local.dao.ClusterDao
import dev.hridaya.kubenexus.data.source.local.dao.ExplainedResourceDao
import dev.hridaya.kubenexus.data.source.local.entity.APIResourceEntity
import dev.hridaya.kubenexus.data.source.local.entity.ClusterEntity
import dev.hridaya.kubenexus.data.source.local.entity.ExplainedResourceEntity
import dev.hridaya.kubenexus.data.source.local.entity.SyncMetadataEntity
import dev.hridaya.kubenexus.domain.model.APIResource
import dev.hridaya.kubenexus.domain.model.ResourceExplain
import dev.hridaya.kubenexus.domain.model.ResourceField
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExploreRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private lateinit var fakeClusterDao: FakeClusterDao
    private lateinit var fakeAPIResourceDao: FakeAPIResourceDao
    private lateinit var fakeExplainedResourceDao: FakeExplainedResourceDao
    private lateinit var fakeNativeBridge: FakeNativeBridge
    private lateinit var repository: ExploreRepositoryImpl

    @Before
    fun setUp() {
        fakeClusterDao = FakeClusterDao()
        fakeAPIResourceDao = FakeAPIResourceDao()
        fakeExplainedResourceDao = FakeExplainedResourceDao()
        fakeNativeBridge = FakeNativeBridge()

        repository = ExploreRepositoryImpl(
            clusterDao = fakeClusterDao,
            apiResourceDao = fakeAPIResourceDao,
            explainedResourceDao = fakeExplainedResourceDao,
            nativeBridge = fakeNativeBridge,
            dispatcherProvider = testDispatcherProvider,
        )
    }

    @Test
    fun `fetchAPIResources saves items to room and deletes removed items on subsequent fetch`() = runTest(testDispatcher) {
        val clusterId = "cluster-1"
        fakeClusterDao.insertCluster(
            ClusterEntity(
                id = clusterId,
                name = "test-cluster",
                serverUrl = "https://127.0.0.1:6443",
                rawKubeconfig = "raw",
                contextName = "ctx",
                userName = "user",
                namespace = "default",
                isActive = true,
                createdAt = 1000L,
                lastConnectedAt = 1000L,
                status = "Connected",
            ),
        )

        // First fetch with 3 resources
        fakeNativeBridge.mockResources = listOf(
            APIResource(name = "pods", kind = "Pod", groupVersion = "v1"),
            APIResource(name = "services", kind = "Service", groupVersion = "v1"),
            APIResource(name = "deployments", kind = "Deployment", groupVersion = "apps/v1"),
        )

        val result1 = repository.fetchAPIResources(clusterId)
        assertTrue(result1 is Result.Success)

        val streamed1 = repository.getAPIResourcesStream(clusterId).first()
        assertEquals(3, streamed1.size)
        assertEquals(listOf("pods", "services", "deployments"), streamed1.map { it.name })

        val lastRefreshed1 = repository.getLastRefreshedStream(clusterId).first()
        assertNotNull(lastRefreshed1)

        // Also explain "services" and "pods"
        val explainResult = repository.explainResource(clusterId, "services", "v1")
        assertTrue(explainResult is Result.Success)
        assertNotNull(fakeExplainedResourceDao.getExplainedResource(clusterId, "services", "v1"))

        // Second fetch where "services" is removed
        fakeNativeBridge.mockResources = listOf(
            APIResource(name = "pods", kind = "Pod", groupVersion = "v1"),
            APIResource(name = "deployments", kind = "Deployment", groupVersion = "apps/v1"),
        )

        val result2 = repository.fetchAPIResources(clusterId)
        assertTrue(result2 is Result.Success)

        val streamed2 = repository.getAPIResourcesStream(clusterId).first()
        assertEquals(2, streamed2.size)
        assertEquals(listOf("pods", "deployments"), streamed2.map { it.name })

        // "services" explained resource should now be deleted from Room DB
        assertNull(fakeExplainedResourceDao.getExplainedResource(clusterId, "services", "v1"))
    }

    @Test
    fun `explainResource caches in room and falls back to room on bridge failure`() = runTest(testDispatcher) {
        val clusterId = "cluster-1"
        fakeClusterDao.insertCluster(
            ClusterEntity(
                id = clusterId,
                name = "test-cluster",
                serverUrl = "https://127.0.0.1:6443",
                rawKubeconfig = "raw",
                contextName = "ctx",
                userName = "user",
                namespace = "default",
                isActive = true,
                createdAt = 1000L,
                lastConnectedAt = 1000L,
                status = "Connected",
            ),
        )

        fakeNativeBridge.mockExplain = ResourceExplain(
            kind = "Pod",
            groupVersion = "v1",
            description = "Pod is a collection of containers",
            fields = listOf(
                ResourceField(name = "spec", type = "PodSpec", description = "Specification", required = true),
            ),
        )

        val result = repository.explainResource(clusterId, "pods", "v1")
        assertTrue(result is Result.Success)
        val explainData = (result as Result.Success).data
        assertEquals("Pod", explainData.kind)
        assertEquals(1, explainData.fields.size)

        // Verify stored in Room
        val cached = fakeExplainedResourceDao.getExplainedResource(clusterId, "pods", "v1")
        assertNotNull(cached)
        assertEquals("Pod", cached!!.kind)

        // Now simulate native bridge failure
        fakeNativeBridge.shouldFailExplain = true
        val cachedResult = repository.explainResource(clusterId, "pods", "v1")
        assertTrue(cachedResult is Result.Success)
        val fallbackData = (cachedResult as Result.Success).data
        assertEquals("Pod", fallbackData.kind)
        assertEquals("Pod is a collection of containers", fallbackData.description)
        assertEquals("spec", fallbackData.fields.first().name)

        // Test direct getCachedExplainedResource
        val directCached = repository.getCachedExplainedResource(clusterId, "pods", "v1")
        assertNotNull(directCached)
        assertEquals("Pod", directCached!!.kind)
    }

    private class FakeExplainedResourceDao : ExplainedResourceDao {
        private val storage = mutableMapOf<String, ExplainedResourceEntity>()

        override suspend fun getExplainedResource(
            clusterId: String,
            resourceOrKind: String,
            groupVersion: String,
        ): ExplainedResourceEntity? {
            val rk = resourceOrKind.lowercase()
            return storage.values.firstOrNull { entity ->
                entity.clusterId == clusterId &&
                    (entity.resourceOrKind == rk || entity.kind.lowercase() == rk) &&
                    (groupVersion.isEmpty() || entity.groupVersion == groupVersion)
            }
        }

        override fun getExplainedResourceStream(
            clusterId: String,
            resourceOrKind: String,
            groupVersion: String,
        ): Flow<ExplainedResourceEntity?> {
            val rk = resourceOrKind.lowercase()
            val match = storage.values.firstOrNull { entity ->
                entity.clusterId == clusterId &&
                    (entity.resourceOrKind == rk || entity.kind.lowercase() == rk) &&
                    (groupVersion.isEmpty() || entity.groupVersion == groupVersion)
            }
            return MutableStateFlow(match).asStateFlow()
        }

        override suspend fun insertExplainedResource(explained: ExplainedResourceEntity) {
            storage[explained.id] = explained
        }

        override suspend fun deleteExplainedResourcesForCluster(clusterId: String) {
            storage.entries.removeIf { it.value.clusterId == clusterId }
        }

        override suspend fun deleteOrphanedExplainedResources(
            clusterId: String,
            activeResources: List<String>,
        ) {
            val activeSet = activeResources.map { it.lowercase() }.toSet()
            storage.entries.removeIf { entry ->
                entry.value.clusterId == clusterId &&
                    entry.value.resourceOrKind !in activeSet &&
                    entry.value.kind.lowercase() !in activeSet
            }
        }
    }

    private class FakeAPIResourceDao : APIResourceDao {
        private val resourcesMap = mutableMapOf<String, List<APIResourceEntity>>()
        private val resourcesFlow = MutableStateFlow<Map<String, List<APIResourceEntity>>>(emptyMap())
        private val metadataMap = mutableMapOf<String, Long>()
        private val metadataFlow = MutableStateFlow<Map<String, Long>>(emptyMap())

        override fun getAPIResourcesStream(clusterId: String): Flow<List<APIResourceEntity>> {
            return resourcesFlow.map { it[clusterId] ?: emptyList() }
        }

        override suspend fun getAPIResourcesList(clusterId: String): List<APIResourceEntity> {
            return resourcesMap[clusterId] ?: emptyList()
        }

        override suspend fun insertAPIResources(resources: List<APIResourceEntity>) {
            val clusterId = resources.firstOrNull()?.clusterId ?: return
            val existing = resourcesMap[clusterId]?.toMutableList() ?: mutableListOf()
            existing.addAll(resources)
            resourcesMap[clusterId] = existing
            resourcesFlow.value = resourcesMap.toMap()
        }

        override suspend fun deleteAPIResourcesForCluster(clusterId: String) {
            resourcesMap.remove(clusterId)
            resourcesFlow.value = resourcesMap.toMap()
        }

        override suspend fun syncAPIResources(
            clusterId: String,
            resources: List<APIResourceEntity>,
            timestamp: Long,
        ) {
            resourcesMap[clusterId] = resources
            resourcesFlow.value = resourcesMap.toMap()

            metadataMap["${clusterId}_api_resources"] = timestamp
            metadataFlow.value = metadataMap.toMap()
        }

        override fun getSyncMetadataStream(key: String): Flow<Long?> {
            return metadataFlow.map { it[key] }
        }

        override suspend fun getLastRefreshedTime(key: String): Long? = metadataMap[key]

        override suspend fun insertSyncMetadata(metadata: SyncMetadataEntity) {
            metadataMap[metadata.key] = metadata.lastRefreshedAt
            metadataFlow.value = metadataMap.toMap()
        }
    }

    private class FakeClusterDao : ClusterDao() {
        private val storage = mutableMapOf<String, ClusterEntity>()
        private val clustersFlow = MutableStateFlow<List<ClusterEntity>>(emptyList())

        private fun notifyChanges() {
            clustersFlow.value = storage.values.toList()
        }

        override fun observeClusters(): Flow<List<ClusterEntity>> = clustersFlow.asStateFlow()

        override fun observeActiveCluster(): Flow<ClusterEntity?> =
            clustersFlow.map { list -> list.firstOrNull { it.isActive } }

        override suspend fun getClusterById(id: String): ClusterEntity? = storage[id]

        override suspend fun getAllClusters(): List<ClusterEntity> = storage.values.toList()

        override suspend fun insertCluster(cluster: ClusterEntity) {
            storage[cluster.id] = cluster
            notifyChanges()
        }

        override suspend fun updateCluster(cluster: ClusterEntity) {
            storage[cluster.id] = cluster
            notifyChanges()
        }

        override suspend fun deleteCluster(id: String) {
            storage.remove(id)
            notifyChanges()
        }

        override suspend fun deactivateAllClusters() {
            storage.replaceAll { _, v -> v.copy(isActive = false) }
            notifyChanges()
        }

        override suspend fun activateCluster(id: String) {
            storage[id]?.let {
                storage[id] = it.copy(isActive = true)
            }
            notifyChanges()
        }

        override suspend fun updateClusterName(id: String, name: String) {
            storage[id]?.let {
                storage[id] = it.copy(name = name)
            }
            notifyChanges()
        }

        override suspend fun updateStatus(id: String, status: String, lastConnectedAt: Long?) {
            storage[id]?.let {
                storage[id] = it.copy(status = status, lastConnectedAt = lastConnectedAt)
            }
            notifyChanges()
        }
    }

    private class FakeNativeBridge : KubeNexusNativeBridge {
        var mockResources: List<APIResource> = emptyList()
        var mockExplain: ResourceExplain? = null
        var shouldFailExplain: Boolean = false

        override fun initialize() {}
        override fun isAvailable(): Boolean = true
        override fun touch(): Boolean = true
        override fun createClient(rawKubeconfig: String): kotlin.Result<client.Client_> =
            kotlin.Result.failure(UnsupportedOperationException())

        override fun createClientWithOptions(
            rawKubeconfig: String,
            timeoutSec: Long,
            insecure: Boolean,
        ): kotlin.Result<client.Client_> = kotlin.Result.failure(UnsupportedOperationException())

        override fun listPods(rawKubeconfig: String, namespace: String?): kotlin.Result<List<String>> =
            kotlin.Result.success(emptyList())

        override fun listPodsWide(rawKubeconfig: String, namespace: String?): kotlin.Result<List<client.Pod>> =
            kotlin.Result.success(emptyList())

        override fun describePod(rawKubeconfig: String, namespace: String, podName: String): kotlin.Result<client.PodDetails> =
            kotlin.Result.failure(UnsupportedOperationException())

        override fun getPodLogs(rawKubeconfig: String, namespace: String, podName: String, container: String?): kotlin.Result<String> =
            kotlin.Result.success("")

        override fun streamPodLogs(rawKubeconfig: String, namespace: String, podName: String, container: String?, callback: client.LogCallback): kotlin.Result<Unit> =
            kotlin.Result.success(Unit)

        override fun listNamespaces(rawKubeconfig: String): kotlin.Result<List<client.Namespace>> =
            kotlin.Result.success(emptyList())

        override fun deletePod(rawKubeconfig: String, namespace: String, podName: String): kotlin.Result<Unit> =
            kotlin.Result.success(Unit)

        override fun deleteNamespace(rawKubeconfig: String, namespace: String): kotlin.Result<Unit> =
            kotlin.Result.success(Unit)

        override fun exec(rawKubeconfig: String, namespace: String, podName: String, container: String, command: String, stdin: String): kotlin.Result<client.ExecResult> =
            kotlin.Result.failure(UnsupportedOperationException())

        override fun startTerminal(rawKubeconfig: String, namespace: String, podName: String, container: String, callback: client.ExecCallback): kotlin.Result<client.ExecSession> =
            kotlin.Result.failure(UnsupportedOperationException())

        override fun startExecSession(rawKubeconfig: String, namespace: String, podName: String, container: String, command: String, tty: Boolean, callback: client.ExecCallback): kotlin.Result<client.ExecSession> =
            kotlin.Result.failure(UnsupportedOperationException())

        override fun listAPIResources(rawKubeconfig: String): kotlin.Result<List<dev.hridaya.kubenexus.domain.model.APIResource>> =
            kotlin.Result.success(mockResources)

        override fun explainResource(
            rawKubeconfig: String,
            resourceOrKind: String,
            groupVersion: String,
        ): kotlin.Result<dev.hridaya.kubenexus.domain.model.ResourceExplain> {
            if (shouldFailExplain) {
                return kotlin.Result.failure(RuntimeException("Network error"))
            }
            val exp = mockExplain ?: dev.hridaya.kubenexus.domain.model.ResourceExplain(
                kind = resourceOrKind,
                groupVersion = groupVersion,
                description = "desc",
            )
            return kotlin.Result.success(exp)
        }
    }
}
