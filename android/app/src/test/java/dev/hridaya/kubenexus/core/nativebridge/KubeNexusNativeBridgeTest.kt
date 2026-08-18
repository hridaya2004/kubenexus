package dev.hridaya.kubenexus.core.nativebridge

import client.ExecCallback
import client.ExecResult
import client.ExecSession
import client.LogCallback
import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.APIResource
import dev.hridaya.kubenexus.domain.model.ResourceExplain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import client.Namespace as NativeNamespace
import client.Pod as NativePod
import client.PodDetails as NativePodDetails

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
                    Result.Error(AppError.Unknown("JVM mock environment"))
                } else {
                    Result.Error(AppError.Unknown("Not initialized"))
                }
            }

            override fun createClientWithOptions(
                rawKubeconfig: String,
                timeoutSec: Long,
                insecure: Boolean
            ): Result<client.Client_> {
                return createClient(rawKubeconfig)
            }

            override fun listPods(rawKubeconfig: String, namespace: String?): Result<List<String>> {
                return Result.Success(listOf("coredns", "traefik"))
            }

            override fun listPodsWide(
                rawKubeconfig: String,
                namespace: String?
            ): Result<List<NativePod>> {
                return Result.Success(emptyList())
            }

            override fun listNamespaces(rawKubeconfig: String): Result<List<NativeNamespace>> {
                return Result.Success(emptyList())
            }

            override fun deleteNamespace(rawKubeconfig: String, namespace: String): Result<Unit> {
                return Result.Success(Unit)
            }

            override fun listAPIResources(rawKubeconfig: String): Result<List<APIResource>> {
                return Result.Success(listOf(APIResource(name = "pods", kind = "Pod", groupVersion = "v1")))
            }

            override fun explainResource(
                rawKubeconfig: String,
                resourceOrKind: String,
                groupVersion: String,
            ): Result<ResourceExplain> {
                return Result.Success(
                    ResourceExplain(
                        kind = resourceOrKind,
                        groupVersion = groupVersion,
                        description = "Test explain description",
                    ),
                )
            }


            override fun describePod(
                rawKubeconfig: String,
                namespace: String,
                podName: String
            ): Result<NativePodDetails> {
                return Result.Error(AppError.Unknown())
            }

            override fun deletePod(
                rawKubeconfig: String,
                namespace: String,
                podName: String
            ): Result<Unit> {
                return Result.Success(Unit)
            }

            override fun getPodLogs(
                rawKubeconfig: String,
                namespace: String,
                podName: String,
                container: String?
            ): Result<String> {
                return Result.Success("log data")
            }

            override fun streamPodLogs(
                rawKubeconfig: String,
                namespace: String,
                podName: String,
                container: String?,
                callback: LogCallback,
            ): Result<Unit> {
                callback.onLogLine("streaming line")
                callback.onDone()
                return Result.Success(Unit)
            }

            override fun exec(
                rawKubeconfig: String,
                namespace: String,
                podName: String,
                container: String,
                command: String,
                stdin: String,
            ): Result<ExecResult> {
                return Result.Error(AppError.Unknown())
            }

            override fun startTerminal(
                rawKubeconfig: String,
                namespace: String,
                podName: String,
                container: String,
                callback: ExecCallback,
            ): Result<ExecSession> {
                return Result.Error(AppError.Unknown())
            }

            override fun startExecSession(
                rawKubeconfig: String,
                namespace: String,
                podName: String,
                container: String,
                command: String,
                tty: Boolean,
                callback: ExecCallback,
            ): Result<ExecSession> {
                return Result.Error(AppError.Unknown())
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
