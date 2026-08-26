package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.core.nativebridge.ClusterHealth
import dev.hridaya.kubenexus.core.nativebridge.KubeNexusNativeBridge
import dev.hridaya.kubenexus.core.nativebridge.PortForwardListener
import dev.hridaya.kubenexus.domain.model.APIResource
import dev.hridaya.kubenexus.domain.model.DeploymentDetails
import dev.hridaya.kubenexus.domain.model.DeploymentSummary
import dev.hridaya.kubenexus.domain.model.Namespace
import dev.hridaya.kubenexus.domain.model.Pod
import dev.hridaya.kubenexus.domain.model.PodDetails
import dev.hridaya.kubenexus.domain.model.PodMetricSample
import dev.hridaya.kubenexus.domain.model.PodStatus
import dev.hridaya.kubenexus.domain.model.ServiceDetails
import dev.hridaya.kubenexus.domain.model.ServicePortDetail
import dev.hridaya.kubenexus.domain.model.ServiceSummary
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ResolveServiceForwardTargetUseCaseTest {

    private lateinit var fakeBridge: FakeNativeBridge
    private lateinit var useCase: ResolveServiceForwardTargetUseCase

    @Before
    fun setUp() {
        fakeBridge = FakeNativeBridge()
        useCase = ResolveServiceForwardTargetUseCase(fakeBridge)
    }

    @Test
    fun `resolves target pod and port matching service selector and port definition`() = runTest {
        val service = ServiceDetails(
            name = "web-service",
            namespace = "default",
            creationTimestampMillis = 1000L,
            type = "ClusterIP",
            clusterIP = "10.96.0.1",
            clusterIPs = listOf("10.96.0.1"),
            externalIPs = emptyList(),
            selector = mapOf("app" to "web"),
            ports = listOf(
                ServicePortDetail(
                    port = 80,
                    targetPort = 8080,
                    nodePort = null,
                    protocol = "TCP",
                    name = "http",
                ),
            ),
            labels = emptyMap(),
            annotations = emptyMap(),
            events = emptyList(),
        )

        fakeBridge.podsToReturn = listOf(
            Pod(
                id = "1",
                name = "web-pod-abc",
                namespace = "default",
                status = PodStatus.RUNNING,
                readyContainers = "1/1",
            ),
        )

        val result = useCase("cfg", service, 80)
        assertTrue(result is Result.Success)
        val target = (result as Result.Success).data
        assertEquals("web-pod-abc", target.podName)
        assertEquals(8080, target.podPort)
    }

    @Test
    fun `returns validation error when service has empty selector`() = runTest {
        val service = ServiceDetails(
            name = "external-service",
            namespace = "default",
            creationTimestampMillis = 1000L,
            type = "ClusterIP",
            clusterIP = "None",
            clusterIPs = emptyList(),
            externalIPs = emptyList(),
            selector = emptyMap(),
            ports = emptyList(),
            labels = emptyMap(),
            annotations = emptyMap(),
            events = emptyList(),
        )

        val result = useCase("cfg", service, 80)
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Validation)
    }

    private class FakeNativeBridge : KubeNexusNativeBridge {
        var podsToReturn: List<Pod> = emptyList()

        override fun listPods(
            rawKubeconfig: String,
            namespace: String?,
            labelSelector: String,
            limit: Long,
        ): Result<List<Pod>> = Result.Success(podsToReturn)

        override fun initialize() = Unit
        override fun isAvailable(): Boolean = true
        override fun touch(): Boolean = true
        override fun listNamespaces(rawKubeconfig: String): Result<List<Namespace>> = Result.Success(emptyList())
        override fun createNamespace(rawKubeconfig: String, name: String): Result<Unit> = Result.Success(Unit)
        override fun deleteNamespace(rawKubeconfig: String, namespace: String): Result<Unit> = Result.Success(Unit)
        override fun listAPIResources(rawKubeconfig: String): Result<List<APIResource>> = Result.Success(emptyList())
        override fun topPods(rawKubeconfig: String, namespace: String?): Result<List<PodMetricSample>> = Result.Success(emptyList())
        override fun topPod(rawKubeconfig: String, namespace: String, podName: String): Result<PodMetricSample?> = Result.Success(null)
        override fun openAPISchemaJSON(rawKubeconfig: String): Result<String> = Result.Success("")
        override fun describePod(rawKubeconfig: String, namespace: String, podName: String): Result<PodDetails> = Result.Error(AppError.NotFound())
        override fun deletePod(rawKubeconfig: String, namespace: String, podName: String): Result<Unit> = Result.Success(Unit)
        override fun listDeployments(rawKubeconfig: String, namespace: String?): Result<List<DeploymentSummary>> = Result.Success(emptyList())
        override fun describeDeployment(rawKubeconfig: String, namespace: String, name: String): Result<DeploymentDetails> = Result.Error(AppError.NotFound())
        override fun listServices(rawKubeconfig: String, namespace: String?): Result<List<ServiceSummary>> = Result.Success(emptyList())
        override fun describeService(rawKubeconfig: String, namespace: String, name: String): Result<ServiceDetails> = Result.Error(AppError.NotFound())
        override fun createDeployment(rawKubeconfig: String, namespace: String, manifestYaml: String): Result<String> = Result.Success("")
        override fun createPod(rawKubeconfig: String, namespace: String, manifestYaml: String): Result<String> = Result.Success("")
        override fun createService(rawKubeconfig: String, namespace: String, manifestYaml: String): Result<String> = Result.Success("")
        override fun getPodLogs(rawKubeconfig: String, namespace: String, podName: String, container: String?, tailLines: Long?): Result<String> = Result.Success("")
        override fun streamPodLogs(rawKubeconfig: String, namespace: String, podName: String, container: String?, tailLines: Long?, callback: client.LogCallback): Result<Unit> = Result.Success(Unit)
        override fun exec(rawKubeconfig: String, namespace: String, podName: String, container: String, command: String, stdin: String): Result<client.ExecResult> = Result.Error(AppError.NotFound())
        override fun startTerminal(rawKubeconfig: String, namespace: String, podName: String, container: String, callback: client.ExecCallback): Result<client.ExecSession> = Result.Error(AppError.NotFound())
        override fun startExecSession(rawKubeconfig: String, namespace: String, podName: String, container: String, command: String, tty: Boolean, callback: client.ExecCallback): Result<client.ExecSession> = Result.Error(AppError.NotFound())
        override fun startPortForward(rawKubeconfig: String, namespace: String, podName: String, localPort: Int, remotePort: Int, listener: PortForwardListener): Result<String> = Result.Success("pf-1")
        override fun stopPortForward(handleId: String): Result<Unit> = Result.Success(Unit)
        override fun ping(rawKubeconfig: String): Result<String> = Result.Success("pong")
        override fun checkLivez(rawKubeconfig: String): Result<Boolean> = Result.Success(true)
        override fun checkReadyz(rawKubeconfig: String): Result<Boolean> = Result.Success(true)
        override fun checkHealthz(rawKubeconfig: String): Result<Boolean> = Result.Success(true)
        override fun serverVersion(rawKubeconfig: String): Result<String> = Result.Success("1.30.0")
        override fun checkHealth(rawKubeconfig: String): Result<ClusterHealth> =
            Result.Success(ClusterHealth(livez = true, readyz = true, healthz = true, serverVersion = "1.30.0", statusMessage = "Ready"))
    }
}
