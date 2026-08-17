package dev.hridaya.kubenexus.presentation.pods.detail

import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.Cluster
import dev.hridaya.kubenexus.domain.model.ClusterStatus
import dev.hridaya.kubenexus.domain.model.CommandExecResult
import dev.hridaya.kubenexus.domain.model.ContainerDetail
import dev.hridaya.kubenexus.domain.model.PodConditionDetail
import dev.hridaya.kubenexus.domain.model.PodDetails
import dev.hridaya.kubenexus.domain.model.PodEventDetail
import dev.hridaya.kubenexus.domain.model.PodStatus
import dev.hridaya.kubenexus.domain.model.TerminalSession
import dev.hridaya.kubenexus.domain.repository.ClusterRepository
import dev.hridaya.kubenexus.domain.repository.PodRepository
import dev.hridaya.kubenexus.domain.usecase.DeletePodUseCase
import dev.hridaya.kubenexus.domain.usecase.DescribePodUseCase
import dev.hridaya.kubenexus.domain.usecase.ExecPodCommandUseCase
import dev.hridaya.kubenexus.domain.usecase.GetActiveClusterUseCase
import dev.hridaya.kubenexus.domain.usecase.GetPodLogsUseCase
import dev.hridaya.kubenexus.domain.usecase.StartExecSessionUseCase
import dev.hridaya.kubenexus.domain.usecase.StartPodTerminalUseCase
import dev.hridaya.kubenexus.domain.usecase.StreamPodLogsUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
    private val onlineFlow = kotlinx.coroutines.flow.MutableStateFlow(true)
    private val fakeNetworkMonitor = object : dev.hridaya.kubenexus.core.common.network.NetworkMonitor {
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
            getActiveClusterUseCase = GetActiveClusterUseCase(fakeClusterRepository, testDispatcherProvider),
            describePodUseCase = DescribePodUseCase(fakePodRepository),
            getPodLogsUseCase = GetPodLogsUseCase(fakePodRepository),
            streamPodLogsUseCase = StreamPodLogsUseCase(fakePodRepository),
            deletePodUseCase = DeletePodUseCase(fakePodRepository),
            execPodCommandUseCase = ExecPodCommandUseCase(fakePodRepository),
            startPodTerminalUseCase = StartPodTerminalUseCase(fakePodRepository),
            startExecSessionUseCase = StartExecSessionUseCase(fakePodRepository),
            networkMonitor = fakeNetworkMonitor,
            dispatcherProvider = testDispatcherProvider
        )
    }

    @Test
    fun `initial load fetches pod describe details and selects default container`() = runTest(testDispatcher) {
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

        assertTrue(viewModel.uiState.value.terminalLines.any { it.text == "echo: whoami\n" || it.text == "echo: whoami" || it.text == "whoami" })

        viewModel.onAction(PodDetailUiAction.StopInteractiveTerminal)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isTerminalActive)
    }

    @Test
    fun `streaming logs clears existing logs first and then streams new lines`() = runTest(testDispatcher) {
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
    fun `network offline updates isOnline and isContainerAttachable and closes active terminal`() = runTest(testDispatcher) {
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
    fun `network offline stops streaming logs and reconnection refetches pod describe`() = runTest(testDispatcher) {
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

    private class FakeClusterRepository : ClusterRepository {
        private val activeCluster = Cluster(
            id = "c-1",
            name = "prod-cluster",
            serverUrl = "https://127.0.0.1:6443",
            rawKubeconfig = "...",
            contextName = "prod",
            isActive = true,
            status = ClusterStatus.CONNECTED
        )

        override fun getClustersStream(): Flow<List<Cluster>> = flowOf(listOf(activeCluster))
        override fun getActiveClusterStream(): Flow<Cluster?> = flowOf(activeCluster)
        override suspend fun getClusterById(id: String): Cluster? = activeCluster
        override suspend fun addCluster(kubeconfigRaw: String, customName: String?, setAsActive: Boolean): Result<Cluster> = Result.Success(activeCluster)
        override suspend fun setActiveCluster(id: String): Result<Unit> = Result.Success(Unit)
        override suspend fun updateClusterName(id: String, newName: String): Result<Unit> = Result.Success(Unit)
        override suspend fun deleteCluster(id: String): Result<Unit> = Result.Success(Unit)
        override suspend fun testConnection(kubeconfigRaw: String): Result<String> = Result.Success("OK")
        override suspend fun testClusterById(id: String): Result<String> = Result.Success("OK")
        override suspend fun updateClusterStatus(id: String, status: ClusterStatus, lastConnectedAt: Long?): Result<Unit> = Result.Success(Unit)
        override suspend fun migratePlaintextClusters(): Result<Int> = Result.Success(0)
    }

    private class FakePodRepository : PodRepository {
        override fun getPodsStream(clusterId: String?, namespace: String?): Flow<List<dev.hridaya.kubenexus.domain.model.Pod>> = flowOf(emptyList())
        override fun getNamespacesStream(clusterId: String?): Flow<List<String>> = flowOf(listOf("default"))
        override fun getLastRefreshedStream(clusterId: String?): Flow<Long?> = flowOf(null)
        override suspend fun refreshWorkloads(clusterId: String?, namespace: String?): Result<Unit> = Result.Success(Unit)

        override suspend fun describePod(clusterId: String?, namespace: String, podName: String): Result<PodDetails> {
            val details = PodDetails(
                name = podName,
                namespace = namespace,
                status = PodStatus.RUNNING,
                node = "k8s-node-1",
                ip = "10.244.0.15",
                containers = listOf(
                    ContainerDetail(name = "container-app", image = "kubenexus/api:v1", ready = true, restartCount = 0)
                ),
                conditions = listOf(
                    PodConditionDetail(type = "Ready", status = "True")
                ),
                events = listOf(
                    PodEventDetail(type = "Normal", reason = "Started", message = "Started container", age = "5m")
                )
            )
            return Result.Success(details)
        }

        override suspend fun deletePod(clusterId: String?, namespace: String, podName: String): Result<Unit> {
            return Result.Success(Unit)
        }

        override suspend fun getPodLogs(clusterId: String?, namespace: String, podName: String, containerName: String?): Result<String> {
            return Result.Success("Starting service...\nListening on port 8080\nReady to accept connections.")
        }

        override fun streamPodLogs(clusterId: String?, namespace: String, podName: String, containerName: String?): Flow<String> {
            return flowOf("Log line 1", "Log line 2", "Log line 3")
        }

        override suspend fun execCommand(
            clusterId: String?,
            namespace: String,
            podName: String,
            containerName: String,
            command: String,
            stdin: String
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
            onDone: () -> Unit
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
            onDone: () -> Unit
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
