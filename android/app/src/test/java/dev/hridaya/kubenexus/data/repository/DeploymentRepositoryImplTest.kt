package dev.hridaya.kubenexus.data.repository

import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.core.nativebridge.FakeKubeNexusNativeBridge
import dev.hridaya.kubenexus.core.security.AesGcmKubeconfigEncryptor
import dev.hridaya.kubenexus.data.source.local.dao.ClusterDao
import dev.hridaya.kubenexus.data.source.local.dao.NamespaceDao
import dev.hridaya.kubenexus.data.source.local.dao.PodDao
import dev.hridaya.kubenexus.data.source.local.entity.ClusterEntity
import dev.hridaya.kubenexus.data.source.local.entity.NamespaceEntity
import dev.hridaya.kubenexus.data.source.local.entity.PodEntity
import dev.hridaya.kubenexus.data.source.local.entity.SyncMetadataEntity
import dev.hridaya.kubenexus.domain.model.DeploymentSummary
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeploymentRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private lateinit var fakeDao: FakeClusterDao
    private lateinit var encryptor: AesGcmKubeconfigEncryptor
    private lateinit var recordingBridge: RecordingBridge
    private lateinit var repository: DeploymentRepositoryImpl
    private lateinit var podRepository: PodRepositoryImpl

    private val sampleKubeconfig = """
        apiVersion: v1
        clusters:
        - cluster:
            certificate-authority-data: LS0tLS1CRUdJTi...
            server: https://192.168.49.2:8443
          name: minikube
        contexts:
        - context:
            cluster: minikube
            namespace: default
            user: minikube
          name: minikube
        current-context: minikube
        kind: Config
        preferences: {}
        users:
        - name: minikube
          user:
            client-certificate-data: LS0tLS1CRUdJTi...
            client-key-data: LS0tLS1CRUdJTi...
            token: secret-token-12345
    """.trimIndent()

    private val sampleManifest = """
        apiVersion: apps/v1
        kind: Deployment
        metadata:
          name: nginx
          namespace: web
        spec:
          replicas: 2
    """.trimIndent()

    @Before
    fun setUp() {
        fakeDao = FakeClusterDao()
        val secretKey = AesGcmKubeconfigEncryptor.generateKey()
        encryptor = AesGcmKubeconfigEncryptor(secretKey)
        recordingBridge = RecordingBridge()

        repository = DeploymentRepositoryImpl(
            clusterDao = fakeDao,
            nativeBridge = recordingBridge,
            encryptor = encryptor,
            dispatcherProvider = testDispatcherProvider,
        )

        podRepository = PodRepositoryImpl(
            clusterDao = fakeDao,
            podDao = NoOpPodDao(),
            namespaceDao = NoOpNamespaceDao(),
            nativeBridge = recordingBridge,
            encryptor = encryptor,
            dispatcherProvider = testDispatcherProvider,
        )
    }

    @Test
    fun `createFromManifest fails when no cluster is selected`() = runTest(testDispatcher) {
        val result = repository.createFromManifest(clusterId = null, manifestYaml = sampleManifest)

        assertTrue(result is Result.Error)
        assertEquals(
            "No cluster selected",
            (result as Result.Error).error.message,
        )
    }

    @Test
    fun `createFromManifest fails when cluster id is unknown`() = runTest(testDispatcher) {
        val result = repository.createFromManifest(
            clusterId = "c-missing",
            manifestYaml = sampleManifest,
        )

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.NotFound)
        assertEquals(
            "Cluster 'c-missing' not found",
            result.error.message,
        )
    }

    @Test
    fun `createFromManifest decrypts kubeconfig and applies reviewed manifest verbatim`() =
        runTest(testDispatcher) {
            seedCluster(id = "c-1", rawKubeconfig = encryptor.encrypt(sampleKubeconfig))

            val result = repository.createFromManifest(
                clusterId = "c-1",
                manifestYaml = sampleManifest,
            )

            assertTrue(result is Result.Success)
            assertEquals(Unit, (result as Result.Success).data)
            // The bridge must receive the decrypted plaintext, never the stored ciphertext.
            assertEquals(sampleKubeconfig, recordingBridge.capturedKubeconfig)
            // The manifest declares its own namespace, so none is passed on top of it.
            assertEquals("", recordingBridge.capturedNamespace)
            assertEquals(sampleManifest, recordingBridge.capturedManifest)
        }

    @Test
    fun `createFromManifest surfaces sanitized bridge error messages`() = runTest(testDispatcher) {
        seedCluster(id = "c-1", rawKubeconfig = encryptor.encrypt(sampleKubeconfig))
        recordingBridge.resultToReturn = Result.Error(
            AppError.Unknown("deployment.apps denied: token: secret-token-12345 rejected"),
        )

        val result = repository.createFromManifest(
            clusterId = "c-1",
            manifestYaml = sampleManifest,
        )

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.Network)
        assertEquals(
            "deployment.apps denied: token: [REDACTED] rejected",
            error.message,
        )
    }

    @Test
    fun `createFromManifest maps bridge exceptions to network errors`() = runTest(testDispatcher) {
        seedCluster(id = "c-1", rawKubeconfig = encryptor.encrypt(sampleKubeconfig))
        recordingBridge.errorToThrow = RuntimeException("connection reset by peer")

        val result = repository.createFromManifest(
            clusterId = "c-1",
            manifestYaml = sampleManifest,
        )

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.Network)
        assertEquals("connection reset by peer", error.message)
    }

    @Test
    fun `getDeployments fails when no cluster is selected`() = runTest(testDispatcher) {
        val result = repository.getDeployments(clusterId = null, namespace = "default")

        assertTrue(result is Result.Error)
        assertEquals(
            "No cluster selected",
            (result as Result.Error).error.message,
        )
    }

    @Test
    fun `getDeployments fails when cluster id is unknown`() = runTest(testDispatcher) {
        val result = repository.getDeployments(clusterId = "c-missing", namespace = "default")

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.NotFound)
        assertEquals(
            "Cluster 'c-missing' not found",
            result.error.message,
        )
    }

    @Test
    fun `getDeployments maps summaries and passes namespace verbatim`() = runTest(testDispatcher) {
        seedCluster(id = "c-1", rawKubeconfig = encryptor.encrypt(sampleKubeconfig))
        val summary = DeploymentSummary(
            id = "web_nginx",
            name = "nginx",
            namespace = "web",
            desiredReplicas = 3,
            readyReplicas = 3,
            availableReplicas = 3,
            images = listOf("nginx:1.25"),
            creationTimestampMillis = 1000L,
        )
        recordingBridge.listResultToReturn = Result.Success(listOf(summary))

        // A blank namespace means all namespaces and must reach the bridge as-is.
        val result = repository.getDeployments(clusterId = "c-1", namespace = "")

        assertTrue(result is Result.Success)
        assertEquals(listOf(summary), (result as Result.Success).data)
        assertEquals(sampleKubeconfig, recordingBridge.capturedKubeconfig)
        assertEquals("", recordingBridge.capturedListNamespace)
    }

    @Test
    fun `getDeployments surfaces sanitized bridge error messages`() = runTest(testDispatcher) {
        seedCluster(id = "c-1", rawKubeconfig = encryptor.encrypt(sampleKubeconfig))
        recordingBridge.listResultToReturn = Result.Error(
            AppError.Unknown("deployments.apps denied: token: secret-token-12345 rejected"),
        )

        val result = repository.getDeployments(clusterId = "c-1", namespace = "web")

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.Network)
        assertEquals(
            "deployments.apps denied: token: [REDACTED] rejected",
            error.message,
        )
    }

    @Test
    fun `createNamespace fails when no cluster is selected`() = runTest(testDispatcher) {
        val result = podRepository.createNamespace(clusterId = null, name = "team-a")

        assertTrue(result is Result.Error)
        assertEquals(
            "No cluster selected",
            (result as Result.Error).error.message,
        )
    }

    @Test
    fun `createNamespace passes decrypted kubeconfig and name to the bridge`() =
        runTest(testDispatcher) {
            seedCluster(id = "c-1", rawKubeconfig = encryptor.encrypt(sampleKubeconfig))

            val result = podRepository.createNamespace(clusterId = "c-1", name = "team-a")

            assertTrue(result is Result.Success)
            assertEquals(Unit, (result as Result.Success).data)
            // The bridge must receive the decrypted plaintext, never the stored ciphertext.
            assertEquals(sampleKubeconfig, recordingBridge.capturedCreateNamespaceKubeconfig)
            assertEquals("team-a", recordingBridge.capturedCreateNamespaceName)
        }

    @Test
    fun `createNamespace surfaces sanitized bridge error messages`() = runTest(testDispatcher) {
        seedCluster(id = "c-1", rawKubeconfig = encryptor.encrypt(sampleKubeconfig))
        recordingBridge.createNamespaceResultToReturn = Result.Error(
            AppError.Unknown("namespaces denied: token: secret-token-12345 rejected"),
        )

        val result = podRepository.createNamespace(clusterId = "c-1", name = "team-a")

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.Network)
        assertEquals(
            "namespaces denied: token: [REDACTED] rejected",
            error.message,
        )
    }

    private fun seedCluster(id: String, rawKubeconfig: String) {
        fakeDao.clusters[id] = ClusterEntity(
            id = id,
            name = "Test Cluster",
            serverUrl = "https://k8s.example.com",
            rawKubeconfig = rawKubeconfig,
            contextName = "ctx",
            userName = "admin",
            namespace = "default",
            isActive = true,
            createdAt = 1000L,
            lastConnectedAt = null,
            status = "CONNECTED",
        )
    }

    /**
     * Records every [createDeployment], [listDeployments] and [createNamespace]
     * argument so tests can assert exactly what crossed the bridge, then returns
     * the preconfigured outcome.
     */
    private class RecordingBridge : FakeKubeNexusNativeBridge() {
        var capturedKubeconfig: String? = null
        var capturedNamespace: String? = null
        var capturedManifest: String? = null

        var capturedListNamespace: String? = null
        var listResultToReturn: Result<List<DeploymentSummary>>? = null

        var capturedCreateNamespaceKubeconfig: String? = null
        var capturedCreateNamespaceName: String? = null
        var createNamespaceResultToReturn: Result<Unit>? = null

        var resultToReturn: Result<String>? = null
        var errorToThrow: Throwable? = null

        override fun createDeployment(
            rawKubeconfig: String,
            namespace: String,
            manifestYaml: String,
        ): Result<String> {
            capturedKubeconfig = rawKubeconfig
            capturedNamespace = namespace
            capturedManifest = manifestYaml
            errorToThrow?.let { throw it }
            return resultToReturn ?: Result.Success(manifestYaml)
        }

        override fun listDeployments(
            rawKubeconfig: String,
            namespace: String?,
        ): Result<List<DeploymentSummary>> {
            capturedKubeconfig = rawKubeconfig
            capturedListNamespace = namespace
            errorToThrow?.let { throw it }
            return listResultToReturn ?: Result.Success(emptyList())
        }

        override fun createNamespace(rawKubeconfig: String, name: String): Result<Unit> {
            capturedCreateNamespaceKubeconfig = rawKubeconfig
            capturedCreateNamespaceName = name
            errorToThrow?.let { throw it }
            return createNamespaceResultToReturn ?: Result.Success(Unit)
        }
    }

    private class NoOpPodDao : PodDao {
        override fun getPodsStream(clusterId: String): Flow<List<PodEntity>> =
            MutableStateFlow(emptyList())

        override fun getPodsByNamespaceStream(clusterId: String, namespace: String): Flow<List<PodEntity>> =
            MutableStateFlow(emptyList())

        override suspend fun getPodIdsForCluster(clusterId: String): List<String> = emptyList()

        override suspend fun getPodIdsForNamespace(clusterId: String, namespace: String): List<String> =
            emptyList()

        override suspend fun getPodsList(clusterId: String): List<PodEntity> = emptyList()

        override suspend fun insertPods(pods: List<PodEntity>) = Unit

        override suspend fun deletePodsForCluster(clusterId: String) = Unit

        override suspend fun deletePodsForNamespace(clusterId: String, namespace: String) = Unit

        override suspend fun deletePod(podId: String) = Unit

        override suspend fun deletePodsByIds(ids: List<String>) = Unit

        override fun getSyncMetadataStream(key: String): Flow<Long?> = MutableStateFlow(null)

        override suspend fun getLastRefreshedTime(key: String): Long? = null

        override suspend fun insertSyncMetadata(metadata: SyncMetadataEntity) = Unit
    }

    private class NoOpNamespaceDao : NamespaceDao {
        override fun getNamespacesStream(clusterId: String): Flow<List<NamespaceEntity>> =
            MutableStateFlow(emptyList())

        override suspend fun insertNamespaces(namespaces: List<NamespaceEntity>) = Unit

        override suspend fun deleteNamespacesForCluster(clusterId: String) = Unit

        override suspend fun deleteNamespace(clusterId: String, name: String) = Unit
    }

    private class FakeClusterDao : ClusterDao() {
        val clusters = mutableMapOf<String, ClusterEntity>()

        override fun observeClusters(): Flow<List<ClusterEntity>> =
            MutableStateFlow(clusters.values.toList())

        override fun observeActiveCluster(): Flow<ClusterEntity?> =
            MutableStateFlow(clusters.values.firstOrNull { it.isActive })

        override suspend fun getClusterById(id: String): ClusterEntity? = clusters[id]

        override suspend fun getAllClusters(): List<ClusterEntity> = clusters.values.toList()

        override suspend fun insertCluster(cluster: ClusterEntity) {
            clusters[cluster.id] = cluster
        }

        override suspend fun updateCluster(cluster: ClusterEntity) {
            clusters[cluster.id] = cluster
        }

        override suspend fun deleteCluster(id: String) {
            clusters.remove(id)
        }

        override suspend fun deletePodsForCluster(id: String) = Unit
        override suspend fun deleteNamespacesForCluster(id: String) = Unit
        override suspend fun deleteAPIResourcesForCluster(id: String) = Unit
        override suspend fun deleteExplainedResourcesForCluster(id: String) = Unit
        override suspend fun deleteOpenApiSchemaForCluster(id: String) = Unit
        override suspend fun deleteSyncMetadataForCluster(id: String) = Unit

        override suspend fun deactivateAllClusters() {
            clusters.replaceAll { _, v -> v.copy(isActive = false) }
        }

        override suspend fun activateCluster(id: String) {
            clusters[id]?.let { clusters[id] = it.copy(isActive = true) }
        }

        override suspend fun updateClusterName(id: String, name: String) {
            clusters[id]?.let { clusters[id] = it.copy(name = name) }
        }

        override suspend fun updateStatus(id: String, status: String, lastConnectedAt: Long?) {
            clusters[id]?.let { clusters[id] = it.copy(status = status, lastConnectedAt = lastConnectedAt) }
        }
    }
}
