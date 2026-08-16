package dev.hridaya.kubenexus.presentation.home

import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.Cluster
import dev.hridaya.kubenexus.domain.model.ClusterStatus
import dev.hridaya.kubenexus.domain.repository.ClusterRepository
import dev.hridaya.kubenexus.domain.usecase.AddClusterUseCase
import dev.hridaya.kubenexus.domain.usecase.DeleteClusterUseCase
import dev.hridaya.kubenexus.domain.usecase.GetActiveClusterUseCase
import dev.hridaya.kubenexus.domain.usecase.GetClustersUseCase
import dev.hridaya.kubenexus.domain.usecase.SetActiveClusterUseCase
import dev.hridaya.kubenexus.domain.usecase.TestClusterConnectionUseCase
import dev.hridaya.kubenexus.domain.usecase.UpdateClusterNameUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private lateinit var fakeRepository: FakeClusterRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeClusterRepository()
        viewModel = HomeViewModel(
            getClustersUseCase = GetClustersUseCase(fakeRepository, testDispatcherProvider),
            getActiveClusterUseCase = GetActiveClusterUseCase(fakeRepository, testDispatcherProvider),
            addClusterUseCase = AddClusterUseCase(fakeRepository, testDispatcherProvider),
            setActiveClusterUseCase = SetActiveClusterUseCase(fakeRepository, testDispatcherProvider),
            deleteClusterUseCase = DeleteClusterUseCase(fakeRepository, testDispatcherProvider),
            updateClusterNameUseCase = UpdateClusterNameUseCase(fakeRepository),
            testClusterConnectionUseCase = TestClusterConnectionUseCase(fakeRepository, testDispatcherProvider),
            dispatcherProvider = testDispatcherProvider
        )
    }

    @Test
    fun `initial state shows empty clusters and isLoading false after observation`() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.clusters.isEmpty())
    }

    @Test
    fun `OpenClusterDrawer and DismissClusterDrawer toggle showClusterDrawer`() = runTest(testDispatcher) {
        viewModel.onAction(HomeUiAction.OpenClusterDrawer)
        assertTrue(viewModel.uiState.value.showClusterDrawer)

        viewModel.onAction(HomeUiAction.DismissClusterDrawer)
        assertFalse(viewModel.uiState.value.showClusterDrawer)
    }

    @Test
    fun `OpenFabActionSheet and DismissFabActionSheet toggle showFabActionSheet`() = runTest(testDispatcher) {
        viewModel.onAction(HomeUiAction.OpenFabActionSheet)
        assertTrue(viewModel.uiState.value.showFabActionSheet)

        viewModel.onAction(HomeUiAction.DismissFabActionSheet)
        assertFalse(viewModel.uiState.value.showFabActionSheet)
    }

    @Test
    fun `ConnectAndSaveSubmitted with blank input sets error`() = runTest(testDispatcher) {
        viewModel.onAction(HomeUiAction.OpenAddClusterSheet)
        viewModel.onAction(HomeUiAction.ConnectAndSaveSubmitted)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.kubeconfigError)
    }

    @Test
    fun `ConnectAndSaveSubmitted with valid kubeconfig saves and sets active`() = runTest(testDispatcher) {
        val validYaml = """
            apiVersion: v1
            kind: Config
            clusters:
            - cluster:
                server: https://127.0.0.1:6443
              name: test-cluster
            current-context: test-cluster
        """.trimIndent()

        viewModel.onAction(HomeUiAction.KubeconfigInputChanged(validYaml))
        viewModel.onAction(HomeUiAction.ConnectAndSaveSubmitted)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showAddClusterSheet)
        assertEquals(1, state.clusters.size)
        assertEquals("test-cluster", state.activeCluster?.name)
    }

    @Test
    fun `SaveClusterName updates cluster alias`() = runTest(testDispatcher) {
        val validYaml = """
            apiVersion: v1
            kind: Config
            clusters:
            - cluster:
                server: https://127.0.0.1:6443
              name: test-cluster
            current-context: test-cluster
        """.trimIndent()

        viewModel.onAction(HomeUiAction.KubeconfigInputChanged(validYaml))
        viewModel.onAction(HomeUiAction.ConnectAndSaveSubmitted)
        advanceUntilIdle()

        val clusterId = viewModel.uiState.value.clusters.first().id
        viewModel.onAction(HomeUiAction.SaveClusterName(clusterId, "production-k8s"))
        advanceUntilIdle()

        assertEquals("production-k8s", viewModel.uiState.value.clusters.first().name)
    }

    @Test
    fun `ConfirmDeleteCluster removes cluster from state`() = runTest(testDispatcher) {
        val validYaml = """
            apiVersion: v1
            kind: Config
            clusters:
            - cluster:
                server: https://127.0.0.1:6443
              name: test-cluster
            current-context: test-cluster
        """.trimIndent()

        viewModel.onAction(HomeUiAction.KubeconfigInputChanged(validYaml))
        viewModel.onAction(HomeUiAction.ConnectAndSaveSubmitted)
        advanceUntilIdle()

        val clusterId = viewModel.uiState.value.clusters.first().id
        viewModel.onAction(HomeUiAction.ConfirmDeleteCluster(clusterId))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.clusters.isEmpty())
    }

    private class FakeClusterRepository : ClusterRepository {
        private val clustersFlow = MutableStateFlow<List<Cluster>>(emptyList())

        override fun getClustersStream(): Flow<List<Cluster>> = clustersFlow.asStateFlow()

        override fun getActiveClusterStream(): Flow<Cluster?> =
            clustersFlow.map { list -> list.firstOrNull { it.isActive } }

        override suspend fun getClusterById(id: String): Cluster? =
            clustersFlow.value.firstOrNull { it.id == id }

        override suspend fun addCluster(
            kubeconfigRaw: String,
            customName: String?,
            setAsActive: Boolean
        ): Result<Cluster> {
            val cluster = Cluster(
                id = "cluster-${System.currentTimeMillis()}",
                name = customName ?: "test-cluster",
                serverUrl = "https://127.0.0.1:6443",
                rawKubeconfig = kubeconfigRaw,
                contextName = "test-cluster",
                isActive = setAsActive,
                status = ClusterStatus.CONNECTED
            )
            val current = clustersFlow.value.map { if (setAsActive) it.copy(isActive = false) else it }
            clustersFlow.value = current + cluster
            return Result.Success(cluster)
        }

        override suspend fun setActiveCluster(id: String): Result<Unit> {
            clustersFlow.value = clustersFlow.value.map { it.copy(isActive = it.id == id) }
            return Result.Success(Unit)
        }

        override suspend fun updateClusterName(id: String, newName: String): Result<Unit> {
            clustersFlow.value = clustersFlow.value.map {
                if (it.id == id) it.copy(name = newName) else it
            }
            return Result.Success(Unit)
        }

        override suspend fun deleteCluster(id: String): Result<Unit> {
            clustersFlow.value = clustersFlow.value.filterNot { it.id == id }
            return Result.Success(Unit)
        }

        override suspend fun testConnection(kubeconfigRaw: String): Result<String> =
            Result.Success("Reachable")

        override suspend fun testClusterById(id: String): Result<String> =
            Result.Success("Reachable")

        override suspend fun updateClusterStatus(
            id: String,
            status: ClusterStatus,
            lastConnectedAt: Long?
        ): Result<Unit> {
            clustersFlow.value = clustersFlow.value.map {
                if (it.id == id) it.copy(status = status, lastConnectedAt = lastConnectedAt) else it
            }
            return Result.Success(Unit)
        }
    }
}
