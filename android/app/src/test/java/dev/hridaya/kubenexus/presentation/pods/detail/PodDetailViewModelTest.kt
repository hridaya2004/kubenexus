package dev.hridaya.kubenexus.presentation.pods.detail

import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.network.NetworkMonitor
import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.core.nativebridge.ClusterHealth
import dev.hridaya.kubenexus.domain.model.Cluster
import dev.hridaya.kubenexus.domain.model.ClusterConnectionStatus
import dev.hridaya.kubenexus.domain.model.ClusterStatus
import dev.hridaya.kubenexus.domain.model.CommandExecResult
import dev.hridaya.kubenexus.domain.model.ContainerDetail
import dev.hridaya.kubenexus.domain.model.Pod
import dev.hridaya.kubenexus.domain.model.PodConditionDetail
import dev.hridaya.kubenexus.domain.model.PodDetails
import dev.hridaya.kubenexus.domain.model.PodEventDetail
import dev.hridaya.kubenexus.domain.model.PodMetricSample
import dev.hridaya.kubenexus.domain.model.PodStatus
import dev.hridaya.kubenexus.domain.model.TerminalSession
import dev.hridaya.kubenexus.domain.repository.ClusterRepository
import dev.hridaya.kubenexus.domain.repository.PodRepository
import dev.hridaya.kubenexus.domain.usecase.DeletePodUseCase
import dev.hridaya.kubenexus.domain.usecase.DescribePodUseCase
import dev.hridaya.kubenexus.domain.usecase.ExecPodCommandUseCase
import dev.hridaya.kubenexus.domain.usecase.GetActiveClusterUseCase
import dev.hridaya.kubenexus.domain.usecase.GetPodLogsUseCase
import dev.hridaya.kubenexus.domain.usecase.GetPodMetricsUseCase
import dev.hridaya.kubenexus.domain.usecase.StartExecSessionUseCase
import dev.hridaya.kubenexus.domain.usecase.StartPodTerminalUseCase
import dev.hridaya.kubenexus.domain.usecase.StreamPodLogsUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PodDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private lateinit var fakeClusterRepository: FakeClusterRepository
    private lateinit var fakePodRepository: FakePodRepository
    private val onlineFlow = MutableStateFlow(true)
    private val fakeNetworkMonitor =
        object : NetworkMonitor {
            override val isOnline: Flow<Boolean> = onlineFlow
        }
    private lateinit var viewModel: PodDetailViewModel

    @Before
    fun setUp() {
        fakeClusterRepository = FakeClusterRepository()
        fakePodRepository = FakePodRepository()
        onlineFlow.value = true

        viewModel = PodDetailViewModel(
            podName = "test-pod-1",
            namespace = "default",
            getActiveClusterUseCase = GetActiveClusterUseCase(
                fakeClusterRepository,
                testDispatcherProvider,
            ),
            describePodUseCase = DescribePodUseCase(fakePodRepository),
            getPodMetricsUseCase = GetPodMetricsUseCase(fakePodRepository),
            getPodLogsUseCase = GetPodLogsUseCase(fakePodRepository),
            streamPodLogsUseCase = StreamPodLogsUseCase(fakePodRepository),
            deletePodUseCase = DeletePodUseCase(fakePodRepository),
            execPodCommandUseCase = ExecPodCommandUseCase(fakePodRepository),
            startPodTerminalUseCase = StartPodTerminalUseCase(fakePodRepository),
            startExecSessionUseCase = StartExecSessionUseCase(fakePodRepository),
            checkClusterHealthUseCase = dev.hridaya.kubenexus.domain.usecase.CheckClusterHealthUseCase(
                fakeClusterRepository,
                testDispatcherProvider,
            ),
            networkMonitor = fakeNetworkMonitor,
            dispatcherProvider = testDispatcherProvider,
        )
    }

    @Test
    fun `initial load fetches pod describe details and selects default container`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNotNull(state.podDetails)
            assertNotNull(state.lastRefreshedAt)
            assertEquals("test-pod-1", state.podDetails?.name)
            assertEquals("container-app", state.selectedContainer)
            assertEquals(1, state.podDetails?.containers?.size)
            assertEquals(1, state.podDetails?.conditions?.size)
            assertEquals(1, state.podDetails?.events?.size)
        }

    @Test
    fun `switching tab to LOGS triggers log fetching`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onAction(PodDetailUiAction.SelectTab(PodDetailTab.LOGS))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PodDetailTab.LOGS, state.selectedTab)
        assertFalse(state.logs.isEmpty())
        assertTrue(state.logs.first().contains("Starting service"))
    }

    @Test
    fun `switching tab to TERMINAL updates selected tab`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onAction(PodDetailUiAction.SelectTab(PodDetailTab.TERMINAL))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PodDetailTab.TERMINAL, state.selectedTab)
    }

    @Test
    fun `executing command appends input line and stdout`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onAction(PodDetailUiAction.ExecuteCommand("uname -a"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.terminalLines.any { it.text == "$ uname -a" })
        assertTrue(state.terminalLines.any { it.text == "Linux k8s-node 5.15.0" })
    }

    @Test
    fun `interactive terminal session connects and handles input`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onAction(PodDetailUiAction.StartInteractiveTerminal())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isTerminalActive)

        viewModel.onAction(PodDetailUiAction.SendTerminalInput("whoami"))
        advanceUntilIdle()

        assertTrue(
            viewModel.uiState.value.terminalLines.any {
                it.text == "echo: whoami\n" ||
                        it.text == "echo: whoami" ||
                        it.text == "whoami"
            },
        )

        viewModel.onAction(PodDetailUiAction.StopInteractiveTerminal)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isTerminalActive)
    }

    @Test
    fun `streaming logs clears existing logs first and then streams new lines`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            // Fetch logs first to populate old logs
            viewModel.onAction(PodDetailUiAction.FetchLogs)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.logs.any { it.contains("Starting service") })

            // Now start streaming logs
            viewModel.onAction(PodDetailUiAction.StartStreamingLogs)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isStreamingLogs)
            assertFalse(state.logs.any { it.contains("Starting service") })
            assertTrue(state.logs.any { it.contains("Log line 1") || it.contains("Streaming logs initiated") })

            viewModel.onAction(PodDetailUiAction.StopStreamingLogs)
            assertFalse(viewModel.uiState.value.isStreamingLogs)
        }

    @Test
    fun `setting tail lines updates state and refetches logs with the specified tail count`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            viewModel.onAction(PodDetailUiAction.SelectTab(PodDetailTab.LOGS))
            advanceUntilIdle()
            assertEquals(250L, fakePodRepository.lastGetPodLogsTail)

            viewModel.onAction(PodDetailUiAction.SetTailLines(500L))
            advanceUntilIdle()
            assertEquals(500L, viewModel.uiState.value.tailLines)
            assertEquals(500L, fakePodRepository.lastGetPodLogsTail)

            viewModel.onAction(PodDetailUiAction.SetTailLines(null))
            advanceUntilIdle()
            assertNull(viewModel.uiState.value.tailLines)
            assertNull(fakePodRepository.lastGetPodLogsTail)
        }

    @Test
    fun `setting tail lines while streaming restarts stream with new tail limit`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            viewModel.onAction(PodDetailUiAction.StartStreamingLogs)
            advanceUntilIdle()
            assertEquals(250L, fakePodRepository.lastStreamPodLogsTail)

            viewModel.onAction(PodDetailUiAction.SetTailLines(500L))
            advanceUntilIdle()
            assertEquals(500L, viewModel.uiState.value.tailLines)
            assertEquals(500L, fakePodRepository.lastStreamPodLogsTail)
            assertTrue(viewModel.uiState.value.isStreamingLogs)

            viewModel.onAction(PodDetailUiAction.StopStreamingLogs)
        }

    @Test
    fun `fetching all logs clears existing logs and requests un-tailed logs from repository`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            // Switch to logs tab and set tail lines
            viewModel.onAction(PodDetailUiAction.SelectTab(PodDetailTab.LOGS))
            viewModel.onAction(PodDetailUiAction.SetTailLines(50L))
            advanceUntilIdle()
            assertEquals(50L, fakePodRepository.lastGetPodLogsTail)

            // Trigger FetchAllLogs (long-press action)
            viewModel.onAction(PodDetailUiAction.FetchAllLogs)
            advanceUntilIdle()

            // Verifies un-tailed request was dispatched to repository
            assertNull(fakePodRepository.lastGetPodLogsTail)
            assertTrue(viewModel.uiState.value.logs.isNotEmpty())
            assertFalse(viewModel.uiState.value.isLoadingLogs)
        }

    @Test
    fun `network offline updates isOnline and isContainerAttachable and closes active terminal`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isOnline)
            assertTrue(viewModel.uiState.value.isContainerAttachable)

            viewModel.onAction(PodDetailUiAction.StartInteractiveTerminal())
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isTerminalActive)

            // Network drops offline
            onlineFlow.value = false
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isOnline)
            assertFalse(viewModel.uiState.value.isContainerAttachable)
            assertFalse(viewModel.uiState.value.isTerminalActive)
            assertTrue(viewModel.uiState.value.terminalLines.any { it.text.contains("Network disconnected") })
        }

    @Test
    fun `network offline stops streaming logs and reconnection refetches pod describe`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            viewModel.onAction(PodDetailUiAction.SelectTab(PodDetailTab.LOGS))
            viewModel.onAction(PodDetailUiAction.StartStreamingLogs)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isStreamingLogs)

            // Drop offline
            onlineFlow.value = false
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isStreamingLogs)
            assertTrue(viewModel.uiState.value.logs.any { it.contains("Network disconnected") })

            // Reconnect online
            onlineFlow.value = true
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isOnline)
            assertNotNull(viewModel.uiState.value.podDetails)
        }

    @Test
    fun `refreshDescribe when describe fails sets error and resets isRefreshing`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            fakePodRepository.describeError = "Connection refused: cannot reach cluster"
            viewModel.onAction(PodDetailUiAction.RefreshDescribe)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isRefreshing)
            assertFalse(state.isLoading)
            assertEquals("Connection refused: cannot reach cluster", state.errorMessage)
        }

    @Test
    fun `unhealthy cluster health check disables container attachability`() =
        runTest(testDispatcher) {
            fakeClusterRepository.healthResult = Result.Success(
                ClusterHealth(
                    livez = false,
                    readyz = false,
                    serverVersion = "",
                    statusMessage = "Not Ready"
                )
            )

            viewModel.onAction(PodDetailUiAction.RefreshDescribe)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(ClusterConnectionStatus.DISCONNECTED, state.clusterConnectionStatus)
            assertFalse(state.isContainerAttachable)
        }

    private class FakeClusterRepository : ClusterRepository {
        var healthResult: Result<ClusterHealth> = Result.Success(
            ClusterHealth(
                livez = true,
                readyz = true,
                serverVersion = "v1.30.0",
                statusMessage = "Ready"
            )
        )

        private val activeCluster = Cluster(
            id = "c-1",
            name = "prod-cluster",
            serverUrl = "https://127.0.0.1:6443",
            rawKubeconfig = "...",
            contextName = "prod",
            isActive = true,
            status = ClusterStatus.CONNECTED,
        )

        override fun getClustersStream(): Flow<List<Cluster>> = flowOf(listOf(activeCluster))
        override fun getActiveClusterStream(): Flow<Cluster?> = flowOf(activeCluster)
        override suspend fun getClusterById(id: String): Cluster = activeCluster
        override suspend fun addCluster(
            kubeconfigRaw: String,
            customName: String?,
            setAsActive: Boolean
        ): Result<Cluster> = Result.Success(activeCluster)

        override suspend fun setActiveCluster(id: String): Result<Unit> = Result.Success(Unit)
        override suspend fun updateClusterName(id: String, newName: String): Result<Unit> =
            Result.Success(Unit)

        override suspend fun testConnection(kubeconfigRaw: String): Result<String> =
            Result.Success("OK")

        override suspend fun testClusterById(id: String): Result<String> = Result.Success("OK")
        override suspend fun checkClusterHealth(id: String): Result<ClusterHealth> = healthResult

        override suspend fun checkClusterHealthByKubeconfig(kubeconfigRaw: String): Result<ClusterHealth> =
            healthResult

        override suspend fun updateClusterStatus(
            id: String,
            status: ClusterStatus,
            lastConnectedAt: Long?
        ): Result<Unit> = Result.Success(Unit)

        override suspend fun deleteCluster(id: String): Result<Unit> = Result.Success(Unit)

        override suspend fun migratePlaintextClusters(): Result<Int> = Result.Success(0)
    }

    private class FakePodRepository : PodRepository {
        var describeError: String? = null

        override fun getPodsStream(
            clusterId: String?,
            namespace: String?
        ): Flow<List<Pod>> = flowOf(emptyList())

        override fun getNamespacesStream(clusterId: String?): Flow<List<String>> =
            flowOf(listOf("default"))

        override fun getLastRefreshedStream(clusterId: String?): Flow<Long?> = flowOf(null)
        override suspend fun refreshWorkloads(
            clusterId: String?,
            namespace: String?
        ): Result<Unit> = Result.Success(Unit)

        override suspend fun getPodMetrics(
            clusterId: String?,
            namespace: String?,
        ): Result<List<PodMetricSample>> = Result.Success(emptyList())

        override suspend fun describePod(
            clusterId: String?,
            namespace: String,
            podName: String
        ): Result<PodDetails> {
            describeError?.let {
                return Result.Error(AppError.Network(it))
            }
            val details = PodDetails(
                name = podName,
                namespace = namespace,
                status = PodStatus.RUNNING,
                node = "k8s-node-1",
                ip = "10.244.0.15",
                containers = listOf(
                    ContainerDetail(
                        name = "container-app",
                        image = "kubenexus/api:v1",
                        ready = true,
                        restartCount = 0,
                    ),
                ),
                conditions = listOf(
                    PodConditionDetail(type = "Ready", status = "True"),
                ),
                events = listOf(
                    PodEventDetail(
                        type = "Normal",
                        reason = "Started",
                        message = "Started container",
                        age = "5m",
                    ),
                ),
            )
            return Result.Success(details)
        }

        override suspend fun deletePod(
            clusterId: String?,
            namespace: String,
            podName: String
        ): Result<Unit> {
            return Result.Success(Unit)
        }

        override suspend fun deleteNamespace(
            clusterId: String?,
            namespace: String
        ): Result<Unit> {
            return Result.Success(Unit)
        }


        var lastGetPodLogsTail: Long? = null
        var lastStreamPodLogsTail: Long? = null

        override suspend fun getPodLogs(
            clusterId: String?,
            namespace: String,
            podName: String,
            containerName: String?,
            tailLines: Long?,
        ): Result<String> {
            lastGetPodLogsTail = tailLines
            return Result.Success("Starting service...\nListening on port 8080\nReady to accept connections.")
        }

        override fun streamPodLogs(
            clusterId: String?,
            namespace: String,
            podName: String,
            containerName: String?,
            tailLines: Long?,
        ): Flow<String> {
            lastStreamPodLogsTail = tailLines
            return flowOf("Log line 1", "Log line 2", "Log line 3")
        }

        override suspend fun execCommand(
            clusterId: String?,
            namespace: String,
            podName: String,
            containerName: String,
            command: String,
            stdin: String,
        ): Result<CommandExecResult> {
            return Result.Success(CommandExecResult(stdout = "Linux k8s-node 5.15.0", stderr = ""))
        }

        override suspend fun startTerminalSession(
            clusterId: String?,
            namespace: String,
            podName: String,
            containerName: String,
            onStdout: (String) -> Unit,
            onStderr: (String) -> Unit,
            onError: (String) -> Unit,
            onDone: () -> Unit,
        ): Result<TerminalSession> {
            val session = object : TerminalSession {
                override fun write(input: String) {
                    onStdout("echo: $input")
                }

                override fun writeBytes(bytes: ByteArray) {}
                override fun close() {
                    onDone()
                }
            }
            return Result.Success(session)
        }

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
        ): Result<TerminalSession> {
            val session = object : TerminalSession {
                override fun write(input: String) {
                    onStdout("echo: $input")
                }

                override fun writeBytes(bytes: ByteArray) {}
                override fun close() {
                    onDone()
                }
            }
            return Result.Success(session)
        }
    }
}
