package dev.hridaya.kubenexus.data.portforward

import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.PortForwardListener
import dev.hridaya.kubenexus.domain.model.PortForwardSessionStatus
import dev.hridaya.kubenexus.domain.model.PortForwardTargetKind
import dev.hridaya.kubenexus.domain.repository.PortForwardRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PortForwardSessionManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private lateinit var fakeRepository: FakePortForwardRepository
    private lateinit var manager: PortForwardSessionManager

    @Before
    fun setUp() {
        fakeRepository = FakePortForwardRepository()
        manager = PortForwardSessionManager(
            repository = fakeRepository,
            externalScope = TestScope(testDispatcher),
            dispatcherProvider = testDispatcherProvider,
        )
    }

    @Test
    fun `startPodForward registers active session with STARTING status and transitions to READY`() =
        runTest(testDispatcher) {
            val result = manager.startPodForward(
                rawKubeconfig = "test-kubeconfig",
                namespace = "default",
                podName = "nginx",
                localPort = 8080,
                remotePort = 80,
            )

            assertTrue(result is Result.Success)
            val handleId = (result as Result.Success).data
            assertEquals("pf-1", handleId)

            val sessions = manager.sessions.value
            assertEquals(1, sessions.size)
            val session = sessions.first()
            assertEquals("pf-1", session.handleId)
            assertEquals(PortForwardTargetKind.Pod, session.kind)
            assertEquals("default", session.namespace)
            assertEquals("nginx", session.targetName)
            assertEquals("nginx", session.podName)
            assertEquals(8080, session.localPort)
            assertEquals(80, session.remotePort)
            assertEquals(PortForwardSessionStatus.STARTING, session.status)

            // Trigger listener callback
            fakeRepository.lastListener?.onPortForwardReady(handleId, 8080)
            assertEquals(PortForwardSessionStatus.READY, manager.sessions.value.first().status)
        }

    @Test
    fun `startServiceForward registers active service session`() = runTest(testDispatcher) {
        val result = manager.startServiceForward(
            rawKubeconfig = "test-kubeconfig",
            namespace = "prod",
            serviceName = "web-svc",
            localPort = 3000,
            servicePort = 80,
            targetPodName = "web-pod-xyz",
            targetPodPort = 8080,
        )

        assertTrue(result is Result.Success)
        val session = manager.sessions.value.first()
        assertEquals(PortForwardTargetKind.Service, session.kind)
        assertEquals("prod", session.namespace)
        assertEquals("web-svc", session.targetName)
        assertEquals("web-pod-xyz", session.podName)
        assertEquals(3000, session.localPort)
        assertEquals(80, session.remotePort)
    }

    @Test
    fun `stop updates session status to STOPPED`() = runTest(testDispatcher) {
        manager.startPodForward(
            rawKubeconfig = "cfg",
            namespace = "default",
            podName = "nginx",
            localPort = 8080,
            remotePort = 80,
        )

        val stopResult = manager.stop("pf-1")
        assertTrue(stopResult is Result.Success)
        assertEquals(PortForwardSessionStatus.STOPPED, manager.sessions.value.first().status)
    }

    private class FakePortForwardRepository : PortForwardRepository {
        var lastListener: PortForwardListener? = null
        var nextHandleId = "pf-1"

        override fun start(
            rawKubeconfig: String,
            namespace: String,
            podName: String,
            localPort: Int,
            remotePort: Int,
            listener: PortForwardListener,
        ): Result<String> {
            lastListener = listener
            return Result.Success(nextHandleId)
        }

        override fun stop(handleId: String): Result<Unit> = Result.Success(Unit)
    }
}
