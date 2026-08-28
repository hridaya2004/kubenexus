package dev.hridaya.kubenexus.presentation.deployments

import androidx.lifecycle.viewModelScope
import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.DeploymentSummary
import dev.hridaya.kubenexus.domain.model.Pod
import dev.hridaya.kubenexus.domain.model.PodDetails
import dev.hridaya.kubenexus.domain.model.PodMetricSample
import dev.hridaya.kubenexus.domain.repository.DeploymentRepository
import dev.hridaya.kubenexus.domain.repository.PodRepository
import dev.hridaya.kubenexus.domain.usecase.GetDeploymentsStreamUseCase
import dev.hridaya.kubenexus.domain.usecase.GetNamespacesUseCase
import dev.hridaya.kubenexus.domain.usecase.SyncDeploymentsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeploymentsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeRepository: FakeDeploymentsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeDeploymentsRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // The Route fires Refresh on lifecycle start, so tests drive syncs through
    // the same action the real screen uses.
    private fun vmTest(block: suspend TestScope.(DeploymentsViewModel) -> Unit) =
        runTest(testDispatcher) {
            val viewModel = DeploymentsViewModel(
                clusterId = "c-1",
                initialNamespace = "team-a",
                getNamespacesUseCase = GetNamespacesUseCase(InertPodRepository),
                syncDeploymentsUseCase = SyncDeploymentsUseCase(fakeRepository),
                getDeploymentsStreamUseCase = GetDeploymentsStreamUseCase(fakeRepository),
            )
            try {
                advanceUntilIdle()
                viewModel.onAction(DeploymentsUiAction.Refresh)
                advanceUntilIdle()
                block(viewModel)
            } finally {
                viewModel.viewModelScope.cancel()
            }
        }

    private fun summary(name: String, ready: Int = 1) = DeploymentSummary(
        id = "team-a/$name",
        name = name,
        namespace = "team-a",
        desiredReplicas = 1,
        readyReplicas = ready,
        availableReplicas = ready,
        images = listOf("nginx:1.27"),
        creationTimestampMillis = 0L,
    )

    @Test
    fun `sync writes through Room stream which clears loading state`() = vmTest { viewModel ->
        fakeRepository.cachedRows.value = listOf(summary("web"), summary("api"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isSyncing)
        assertNull(state.errorMessage)
        assertEquals(listOf("web", "api"), state.deployments.map { it.name })
    }

    @Test
    fun `sync failure with an empty cache surfaces friendly blocking error`() = vmTest { viewModel ->
        fakeRepository.syncResult = Result.Error(AppError.Network(message = "dial tcp refused"))
        advanceUntilIdle()

        viewModel.onAction(DeploymentsUiAction.Refresh)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.deployments.isEmpty())
        assertEquals(
            "Couldn't reach the cluster to sync deployments. Check your connection and try again.",
            state.errorMessage,
        )
    }

    @Test
    fun `retry after failure recovers the list`() = vmTest { viewModel ->
        fakeRepository.syncResult = Result.Error(AppError.Unknown(message = "boom"))
        advanceUntilIdle()
        viewModel.onAction(DeploymentsUiAction.Refresh)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.errorMessage != null)

        fakeRepository.cachedRows.value = listOf(summary("web"))
        fakeRepository.syncResult = Result.Success(Unit)
        viewModel.onAction(DeploymentsUiAction.Refresh)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertFalse(state.isSyncing)
        assertEquals(1, state.deployments.size)
    }

    @Test
    fun `refresh failure keeps cached rows visible without blocking error`() = vmTest { viewModel ->
        fakeRepository.cachedRows.value = listOf(summary("web"))
        advanceUntilIdle()

        fakeRepository.syncResult = Result.Error(AppError.Network(message = "timeout"))
        viewModel.onAction(DeploymentsUiAction.Refresh)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSyncing)
        // Cached rows stay visible offline; no blocking copy over live data.
        assertNull(state.errorMessage)
        assertEquals(1, state.deployments.size)
    }

    @Test
    fun `refresh passes cluster and namespace through to sync`() = vmTest { viewModel ->
        viewModel.onAction(DeploymentsUiAction.Refresh)
        advanceUntilIdle()
        viewModel.onAction(DeploymentsUiAction.Refresh)
        advanceUntilIdle()

        // One sync from the helper's lifecycle-start equivalent plus two from
        // the explicit actions below.
        assertEquals(
            listOf(
                Pair<String?, String?>("c-1", "team-a"),
                Pair<String?, String?>("c-1", "team-a"),
                Pair<String?, String?>("c-1", "team-a"),
            ),
            fakeRepository.syncCalls,
        )
    }

    @Test
    fun `selecting a namespace re-scopes the stream without syncing`() = vmTest { viewModel ->
        val allNamespaces = MutableStateFlow(
            listOf(
                summary("web").copy(namespace = "web", id = "web_web"),
                summary("api"),
            ),
        )
        fakeRepository.cachedRows = allNamespaces

        advanceUntilIdle()
        viewModel.onAction(DeploymentsUiAction.SelectNamespace("All Namespaces"))
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.deployments.size)

        val beforeSyncCalls = fakeRepository.syncCalls.size
        viewModel.onAction(DeploymentsUiAction.SelectNamespace("team-a"))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.deployments.size)
        assertEquals(beforeSyncCalls, fakeRepository.syncCalls.size)
    }
}

private class FakeDeploymentsRepository : DeploymentRepository {
    var cachedRows: MutableStateFlow<List<DeploymentSummary>> = MutableStateFlow(emptyList())
    var syncResult: Result<Unit> = Result.Success(Unit)
    val syncCalls = mutableListOf<Pair<String?, String?>>()

    override fun getDeploymentsStream(
        clusterId: String?,
        namespace: String?,
    ): Flow<List<DeploymentSummary>> {
        return if (namespace.isNullOrBlank()) {
            cachedRows
        } else {
            cachedRows.map { rows ->
                rows.filter { it.namespace == namespace }
            }
        }
    }

    override suspend fun syncDeployments(
        clusterId: String?,
        namespace: String?,
    ): Result<Unit> {
        syncCalls += clusterId to namespace
        return syncResult
    }

    override suspend fun getDeployments(
        clusterId: String?,
        namespace: String?,
    ): Result<List<DeploymentSummary>> = Result.Success(cachedRows.value)

    override suspend fun getDeploymentDetails(
        clusterId: String?,
        namespace: String,
        name: String,
    ): Result<dev.hridaya.kubenexus.domain.model.DeploymentDetails> =
        Result.Error(AppError.NotFound("Not exercised in this test"))

    override suspend fun createFromManifest(clusterId: String?, manifestYaml: String): Result<Unit> {
        throw UnsupportedOperationException("not used in this test")
    }
}

/**
 * The view model only consumes namespace options from the pods repository;
 * every other member stays inert so accidental use fails loudly.
 */
private object InertPodRepository : PodRepository {
    override fun getPodsStream(clusterId: String?, namespace: String?): Flow<List<Pod>> = emptyFlow()
    override fun getNamespacesStream(clusterId: String?): Flow<List<String>> = emptyFlow()
    override fun getLastRefreshedStream(clusterId: String?): Flow<Long?> = emptyFlow()
    override suspend fun refreshWorkloads(clusterId: String?, namespace: String?): Result<Unit> =
        Result.Success(Unit)

    override suspend fun listPodsBySelector(
        rawKubeconfig: String,
        namespace: String?,
        labelSelector: String,
    ): Result<List<Pod>> = Result.Success(emptyList())

    override suspend fun describePod(
        clusterId: String?,
        namespace: String,
        podName: String,
    ): Result<PodDetails> = Result.Error(AppError.NotFound("inert"))

    override suspend fun getPodMetrics(
        clusterId: String?,
        namespace: String?,
    ): Result<List<PodMetricSample>> = Result.Success(emptyList())

    override suspend fun getSinglePodMetrics(
        clusterId: String?,
        namespace: String,
        podName: String,
    ): Result<PodMetricSample?> = Result.Error(AppError.NotFound("inert"))

    override suspend fun deletePod(clusterId: String?, namespace: String, podName: String): Result<Unit> =
        Result.Success(Unit)

    override suspend fun deleteNamespace(clusterId: String?, namespace: String): Result<Unit> =
        Result.Success(Unit)

    override suspend fun createNamespace(clusterId: String?, name: String): Result<Unit> =
        Result.Success(Unit)

    override suspend fun createPodFromManifest(clusterId: String?, manifestYaml: String): Result<Unit> =
        Result.Success(Unit)

    override suspend fun getPodLogs(
        clusterId: String?,
        namespace: String,
        podName: String,
        containerName: String?,
        tailLines: Long?,
    ): Result<String> = Result.Success("")

    override fun streamPodLogs(
        clusterId: String?,
        namespace: String,
        podName: String,
        containerName: String?,
        tailLines: Long?,
    ): Flow<String> = emptyFlow()

    override suspend fun execCommand(
        clusterId: String?,
        namespace: String,
        podName: String,
        containerName: String,
        command: String,
        stdin: String,
    ): Result<dev.hridaya.kubenexus.domain.model.CommandExecResult> =
        Result.Error(AppError.NotFound("inert"))

    override suspend fun startTerminalSession(
        clusterId: String?,
        namespace: String,
        podName: String,
        containerName: String,
        onStdout: (String) -> Unit,
        onStderr: (String) -> Unit,
        onError: (String) -> Unit,
        onDone: () -> Unit,
    ): Result<dev.hridaya.kubenexus.domain.model.TerminalSession> =
        Result.Error(AppError.NotFound("inert"))

    override suspend fun startExecSession(
        clusterId: String?,
        namespace: String,
        podName: String,
        containerName: String,
        command: String,
        tty: Boolean,
        onStdout: (String) -> Unit,
        onStderr: (String) -> Unit,
        onError: (String) -> Unit,
        onDone: () -> Unit,
    ): Result<dev.hridaya.kubenexus.domain.model.TerminalSession> =
        Result.Error(AppError.NotFound("inert"))
}
