package dev.hridaya.kubenexus.presentation.services

import androidx.lifecycle.viewModelScope
import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.network.NetworkMonitor
import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.Pod
import dev.hridaya.kubenexus.domain.model.PodDetails
import dev.hridaya.kubenexus.domain.model.PodMetricSample
import dev.hridaya.kubenexus.domain.model.ServicePortDetail
import dev.hridaya.kubenexus.domain.model.ServiceSummary
import dev.hridaya.kubenexus.domain.repository.PodRepository
import dev.hridaya.kubenexus.domain.repository.ServiceRepository
import dev.hridaya.kubenexus.domain.usecase.GetNamespacesUseCase
import dev.hridaya.kubenexus.domain.usecase.GetServicesLastRefreshedUseCase
import dev.hridaya.kubenexus.domain.usecase.GetServicesStreamUseCase
import dev.hridaya.kubenexus.domain.usecase.SyncServicesUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
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
class ServicesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeServicesRepository
    private lateinit var fakeNetworkMonitor: FakeNetworkMonitor

    private val testDispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeServicesRepository()
        fakeNetworkMonitor = FakeNetworkMonitor()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vmTest(
        initialNamespace: String? = "team-a",
        block: suspend TestScope.(ServicesViewModel) -> Unit,
    ) = runTest(testDispatcher) {
        val viewModel = ServicesViewModel(
            clusterId = "c-1",
            initialNamespace = initialNamespace,
            getServicesStreamUseCase = GetServicesStreamUseCase(fakeRepository),
            syncServicesUseCase = SyncServicesUseCase(fakeRepository),
            getNamespacesUseCase = GetNamespacesUseCase(InertPodRepository),
            getServicesLastRefreshedUseCase = GetServicesLastRefreshedUseCase(fakeRepository),
            networkMonitor = fakeNetworkMonitor,
            dispatcherProvider = testDispatcherProvider,
        )
        try {
            advanceUntilIdle()
            viewModel.onAction(ServicesUiAction.Refresh)
            advanceUntilIdle()
            block(viewModel)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    private fun serviceSummary(name: String, namespace: String = "team-a") = ServiceSummary(
        id = "$namespace/$name",
        name = name,
        namespace = namespace,
        type = "ClusterIP",
        clusterIP = "10.96.0.1",
        ports = listOf(
            ServicePortDetail(
                port = 80,
                targetPort = 8080,
                nodePort = null,
                protocol = "TCP",
                name = "http"
            )
        ),
        creationTimestampMillis = 0L,
    )

    @Test
    fun `emits cached rows immediately without waiting for sync`() = vmTest { viewModel ->
        fakeRepository.cachedRows.value = listOf(serviceSummary("svc-1"), serviceSummary("svc-2"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.services.size)
        assertEquals("svc-1", state.services[0].name)
        assertFalse(state.isLoading)
    }

    @Test
    fun `switching namespace filters cached rows reactively`() = vmTest { viewModel ->
        fakeRepository.cachedRows.value = listOf(
            serviceSummary("svc-a", namespace = "team-a"),
            serviceSummary("svc-b", namespace = "team-b"),
        )
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.services.size)
        assertEquals("svc-a", viewModel.uiState.value.services[0].name)

        viewModel.onAction(ServicesUiAction.SelectNamespace("team-b"))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.services.size)
        assertEquals("svc-b", viewModel.uiState.value.services[0].name)
    }

    @Test
    fun `sync failure preserves cached rows and does not show blocking error`() =
        vmTest { viewModel ->
            fakeRepository.cachedRows.value = listOf(serviceSummary("svc-cached"))
            fakeRepository.syncResult = Result.Error(AppError.Network("Offline"))
            advanceUntilIdle()

            viewModel.onAction(ServicesUiAction.Refresh)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.services.size)
            assertNull(state.errorMessage)
            assertFalse(state.isRefreshing)
        }

    @Test
    fun `sync failure with empty cache surfaces friendly error`() = vmTest { viewModel ->
        fakeRepository.cachedRows.value = emptyList()
        fakeRepository.syncResult = Result.Error(AppError.Network("Offline"))
        advanceUntilIdle()

        viewModel.onAction(ServicesUiAction.Refresh)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.services.isEmpty())
        assertEquals(
            "Couldn't load your services right now. Check that the cluster is reachable and try again.",
            state.errorMessage,
        )
    }

    @Test
    fun `reconnecting to network triggers auto-sync`() = vmTest { viewModel ->
        fakeNetworkMonitor.setOnline(false)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isOnline)

        fakeRepository.syncCalls.clear()
        fakeNetworkMonitor.setOnline(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isOnline)
        assertTrue(fakeRepository.syncCalls.isNotEmpty())
    }
}

private class FakeServicesRepository : ServiceRepository {
    val cachedRows = MutableStateFlow<List<ServiceSummary>>(emptyList())
    var syncResult: Result<Unit> = Result.Success(Unit)
    val syncCalls = mutableListOf<Pair<String?, String?>>()

    override suspend fun createFromManifest(
        clusterId: String?,
        manifestYaml: String
    ): Result<Unit> =
        Result.Success(Unit)

    override fun getServicesStream(
        clusterId: String?,
        namespace: String?,
    ): Flow<List<ServiceSummary>> {
        return if (namespace.isNullOrBlank() || namespace == "All Namespaces") {
            cachedRows
        } else {
            cachedRows.map { list -> list.filter { it.namespace == namespace } }
        }
    }

    override fun getLastRefreshedStream(clusterId: String?): Flow<Long?> = flowOf(null)

    override suspend fun syncServices(clusterId: String?, namespace: String?): Result<Unit> {
        syncCalls += clusterId to namespace
        return syncResult
    }

    override suspend fun getServiceDetails(
        clusterId: String?,
        namespace: String,
        name: String,
    ): Result<dev.hridaya.kubenexus.domain.model.ServiceDetails> =
        Result.Error(AppError.NotFound("inert"))
}

private class FakeNetworkMonitor : NetworkMonitor {
    private val _isOnline = MutableStateFlow(true)
    override val isOnline: Flow<Boolean> = _isOnline

    fun setOnline(online: Boolean) {
        _isOnline.value = online
    }
}

private object InertPodRepository : PodRepository {
    override fun getPodsStream(clusterId: String?, namespace: String?): Flow<List<Pod>> =
        emptyFlow()

    override fun getNamespacesStream(clusterId: String?): Flow<List<String>> =
        flowOf(listOf("team-a", "team-b"))

    override fun getLastRefreshedStream(clusterId: String?): Flow<Long?> = flowOf(null)
    override suspend fun refreshWorkloads(clusterId: String?, namespace: String?): Result<Unit> =
        Result.Success(Unit)

    override suspend fun listPodsBySelector(
        rawKubeconfig: String,
        namespace: String?,
        labelSelector: String
    ): Result<List<Pod>> =
        Result.Success(emptyList())

    override suspend fun describePod(
        clusterId: String?,
        namespace: String,
        podName: String
    ): Result<PodDetails> =
        Result.Error(AppError.NotFound("inert"))

    override suspend fun getPodMetrics(
        clusterId: String?,
        namespace: String?
    ): Result<List<PodMetricSample>> =
        Result.Success(emptyList())

    override suspend fun getSinglePodMetrics(
        clusterId: String?,
        namespace: String,
        podName: String
    ): Result<PodMetricSample?> =
        Result.Error(AppError.NotFound("inert"))

    override suspend fun deletePod(
        clusterId: String?,
        namespace: String,
        podName: String
    ): Result<Unit> = Result.Success(Unit)

    override suspend fun deleteNamespace(clusterId: String?, namespace: String): Result<Unit> =
        Result.Success(Unit)

    override suspend fun createNamespace(clusterId: String?, name: String): Result<Unit> =
        Result.Success(Unit)

    override suspend fun createPodFromManifest(
        clusterId: String?,
        manifestYaml: String
    ): Result<Unit> = Result.Success(Unit)

    override suspend fun getPodLogs(
        clusterId: String?,
        namespace: String,
        podName: String,
        containerName: String?,
        tailLines: Long?
    ): Result<String> =
        Result.Success("")

    override fun streamPodLogs(
        clusterId: String?,
        namespace: String,
        podName: String,
        containerName: String?,
        tailLines: Long?
    ): Flow<String> =
        emptyFlow()

    override suspend fun execCommand(
        clusterId: String?,
        namespace: String,
        podName: String,
        containerName: String,
        command: String,
        stdin: String
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
        onDone: () -> Unit
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
        onDone: () -> Unit
    ): Result<dev.hridaya.kubenexus.domain.model.TerminalSession> =
        Result.Error(AppError.NotFound("inert"))
}
