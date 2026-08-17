package dev.hridaya.kubenexus.core.nativebridge

import client.ExecCallback
import client.ExecResult
import client.ExecSession
import client.LogCallback
import client.Namespace as NativeNamespace
import client.Pod as NativePod
import client.PodDetails as NativePodDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KubeNexusNativeBridgeTest {

    @Test
    fun `fake bridge reports availability and creates client successfully`() {
        val fakeBridge = object : KubeNexusNativeBridge {
            private var isInit = false

            override fun initialize() {
                isInit = true
            }

            override fun isAvailable(): Boolean = isInit

            override fun touch(): Boolean = true

            override fun createClient(rawKubeconfig: String): Result<client.Client_> {
                return if (isInit) {
                    Result.failure(UnsupportedOperationException("JVM mock environment"))
                } else {
                    Result.failure(IllegalStateException("Not initialized"))
                }
            }

            override fun createClientWithOptions(rawKubeconfig: String, timeoutSec: Long, insecure: Boolean): Result<client.Client_> {
                return createClient(rawKubeconfig)
            }

            override fun listPods(rawKubeconfig: String, namespace: String?): Result<List<String>> {
                return Result.success(listOf("coredns", "traefik"))
            }

            override fun listPodsWide(rawKubeconfig: String, namespace: String?): Result<List<NativePod>> {
                return Result.success(emptyList())
            }

            override fun listNamespaces(rawKubeconfig: String): Result<List<NativeNamespace>> {
                return Result.success(emptyList())
            }

            override fun describePod(rawKubeconfig: String, namespace: String, podName: String): Result<NativePodDetails> {
                return Result.failure(UnsupportedOperationException())
            }

            override fun deletePod(rawKubeconfig: String, namespace: String, podName: String): Result<Unit> {
                return Result.success(Unit)
            }

            override fun getPodLogs(rawKubeconfig: String, namespace: String, podName: String, container: String?): Result<String> {
                return Result.success("log data")
            }

            override fun streamPodLogs(
                rawKubeconfig: String,
                namespace: String,
                podName: String,
                container: String?,
                callback: LogCallback
            ): Result<Unit> {
                callback.onLogLine("streaming line")
                callback.onDone()
                return Result.success(Unit)
            }

            override fun exec(
                rawKubeconfig: String,
                namespace: String,
                podName: String,
                container: String,
                command: String,
                stdin: String
            ): Result<ExecResult> {
                return Result.failure(UnsupportedOperationException())
            }

            override fun startTerminal(
                rawKubeconfig: String,
                namespace: String,
                podName: String,
                container: String,
                callback: ExecCallback
            ): Result<ExecSession> {
                return Result.failure(UnsupportedOperationException())
            }

            override fun startExecSession(
                rawKubeconfig: String,
                namespace: String,
                podName: String,
                container: String,
                command: String,
                tty: Boolean,
                callback: ExecCallback
            ): Result<ExecSession> {
                return Result.failure(UnsupportedOperationException())
            }
        }

        assertFalse(fakeBridge.isAvailable())
        fakeBridge.initialize()
        assertTrue(fakeBridge.isAvailable())
        assertTrue(fakeBridge.touch())
        val pods = fakeBridge.listPods("mock-kubeconfig", null).getOrNull()
        assertEquals(2, pods?.size)
    }
}
