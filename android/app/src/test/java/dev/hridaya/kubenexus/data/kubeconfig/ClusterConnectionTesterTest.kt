package dev.hridaya.kubenexus.data.kubeconfig

import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.core.nativebridge.ClusterHealth
import dev.hridaya.kubenexus.core.nativebridge.KubeNexusNativeBridge
import dev.hridaya.kubenexus.domain.model.APIResource
import dev.hridaya.kubenexus.domain.model.ParsedKubeconfig
import dev.hridaya.kubenexus.domain.model.ResourceExplain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ClusterConnectionTesterTest {

    private lateinit var fakeNativeBridge: FakeNativeBridge
    private lateinit var tester: ClusterConnectionTester

    private class FakeNativeBridge : KubeNexusNativeBridge {
        var pingResult: Result<String> =
            Result.Success("Cluster ready & healthy (Kubernetes v1.30.0)")

        override fun initialize() {}
        override fun isAvailable(): Boolean = true
        override fun touch(): Boolean = true
        override fun createClient(rawKubeconfig: String): Result<client.Client_> =
            Result.Error(AppError.Unknown("mock"))

        override fun createClientWithOptions(
            rawKubeconfig: String,
            timeoutSec: Long,
            insecure: Boolean
        ): Result<client.Client_> =
            Result.Error(AppError.Unknown("mock"))

        override fun listPods(rawKubeconfig: String, namespace: String?): Result<List<String>> =
            Result.Success(emptyList())

        override fun listPodsWide(
            rawKubeconfig: String,
            namespace: String?
        ): Result<List<client.Pod>> = Result.Success(emptyList())

        override fun listNamespaces(rawKubeconfig: String): Result<List<client.Namespace>> =
            Result.Success(emptyList())

        override fun deleteNamespace(rawKubeconfig: String, namespace: String): Result<Unit> =
            Result.Success(Unit)

        override fun listAPIResources(rawKubeconfig: String): Result<List<APIResource>> =
            Result.Success(emptyList())

        override fun explainResource(
            rawKubeconfig: String,
            resourceOrKind: String,
            groupVersion: String
        ): Result<ResourceExplain> =
            Result.Success(ResourceExplain(kind = resourceOrKind, description = "mock"))

        override fun describePod(
            rawKubeconfig: String,
            namespace: String,
            podName: String
        ): Result<client.PodDetails> =
            Result.Error(AppError.NotFound("Pod"))

        override fun deletePod(
            rawKubeconfig: String,
            namespace: String,
            podName: String
        ): Result<Unit> = Result.Success(Unit)

        override fun getPodLogs(
            rawKubeconfig: String,
            namespace: String,
            podName: String,
            container: String?
        ): Result<String> = Result.Success("")

        override fun streamPodLogs(
            rawKubeconfig: String,
            namespace: String,
            podName: String,
            container: String?,
            callback: client.LogCallback
        ): Result<Unit> = Result.Success(Unit)

        override fun exec(
            rawKubeconfig: String,
            namespace: String,
            podName: String,
            container: String,
            command: String,
            stdin: String
        ): Result<client.ExecResult> =
            Result.Error(AppError.Unknown("mock"))

        override fun startTerminal(
            rawKubeconfig: String,
            namespace: String,
            podName: String,
            container: String,
            callback: client.ExecCallback
        ): Result<client.ExecSession> =
            Result.Error(AppError.Unknown("mock"))

        override fun startExecSession(
            rawKubeconfig: String,
            namespace: String,
            podName: String,
            container: String,
            command: String,
            tty: Boolean,
            callback: client.ExecCallback
        ): Result<client.ExecSession> =
            Result.Error(AppError.Unknown("mock"))

        override fun ping(rawKubeconfig: String): Result<String> = pingResult
        override fun checkLivez(rawKubeconfig: String): Result<Boolean> = Result.Success(true)
        override fun checkReadyz(rawKubeconfig: String): Result<Boolean> = Result.Success(true)
        override fun checkHealthz(rawKubeconfig: String): Result<Boolean> = Result.Success(true)
        override fun serverVersion(rawKubeconfig: String): Result<String> =
            Result.Success("v1.30.0")

        override fun checkHealth(rawKubeconfig: String): Result<ClusterHealth> =
            Result.Success(
                ClusterHealth(
                    livez = true,
                    readyz = true,
                    serverVersion = "v1.30.0",
                    statusMessage = "Ready"
                )
            )
    }

    @Before
    fun setUp() {
        fakeNativeBridge = FakeNativeBridge()
        tester = ClusterConnectionTester(fakeNativeBridge)
    }

    @Test
    fun `testConnection returns success message when native ping succeeds`() {
        val parsed = ParsedKubeconfig(
            clusterName = "test-cluster",
            serverUrl = "https://127.0.0.1:6443",
            contextName = "test-ctx",
            userName = "test-user",
            namespace = "default",
            rawKubeconfig = "clusters: []",
        )

        val result = tester.testConnection(parsed)
        assertEquals("Cluster ready & healthy (Kubernetes v1.30.0)", result)
    }

    @Test
    fun `testConnection throws exception with diagnostic information when native ping fails`() {
        fakeNativeBridge.pingResult =
            Result.Error(AppError.Network("TLS handshake failed: certificate untrusted"))

        val parsed = ParsedKubeconfig(
            clusterName = "test-cluster",
            serverUrl = "https://127.0.0.1:6443",
            contextName = "test-ctx",
            userName = "test-user",
            namespace = "default",
            rawKubeconfig = "clusters: []",
        )

        val exception = assertThrows(Exception::class.java) {
            tester.testConnection(parsed)
        }

        assertTrue(exception.message!!.contains("Failed to connect to Kubernetes Cluster: 'test-cluster'"))
        assertTrue(exception.message!!.contains("TLS handshake failed: certificate untrusted"))
    }
}
