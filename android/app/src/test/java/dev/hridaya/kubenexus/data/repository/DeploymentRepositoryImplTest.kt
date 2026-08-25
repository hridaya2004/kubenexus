package dev.hridaya.kubenexus.data.repository

import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.core.nativebridge.FakeKubeNexusNativeBridge
import dev.hridaya.kubenexus.core.security.AesGcmKubeconfigEncryptor
import dev.hridaya.kubenexus.data.source.local.dao.ClusterDao
import dev.hridaya.kubenexus.data.source.local.entity.ClusterEntity
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
     * Records every [createDeployment] argument so tests can assert exactly what
     * crossed the bridge, then returns the preconfigured outcome.
     */
    private class RecordingBridge : FakeKubeNexusNativeBridge() {
        var capturedKubeconfig: String? = null
        var capturedNamespace: String? = null
        var capturedManifest: String? = null

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
