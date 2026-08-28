package dev.hridaya.kubenexus.data.nativebridge

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.APIResource
import dev.hridaya.kubenexus.domain.model.Namespace
import dev.hridaya.kubenexus.domain.model.Pod
import dev.hridaya.kubenexus.domain.model.PodStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KubeNexusNativeBridgeTest {

    @Test
    fun `fake bridge reports availability only after initialization`() {
        val fakeBridge = FakeKubeNexusNativeBridge()

        assertFalse(fakeBridge.isAvailable())
        fakeBridge.initialize()
        assertTrue(fakeBridge.isAvailable())
        assertTrue(fakeBridge.touch())
    }

    @Test
    fun `bridge surfaces pods as domain models`() {
        val fakeBridge = object : FakeKubeNexusNativeBridge() {
            override fun listPods(
                rawKubeconfig: String,
                namespace: String?,
                labelSelector: String,
                limit: Long,
            ): Result<List<Pod>> = Result.Success(
                listOf(
                    Pod(id = "kube-system_coredns", name = "coredns", namespace = "kube-system"),
                    Pod(
                        id = "kube-system_traefik",
                        name = "traefik",
                        namespace = "kube-system",
                        status = PodStatus.CRASH_LOOP,
                    ),
                ),
            )
        }

        val pods = fakeBridge.listPods("mock-kubeconfig", null).getOrThrow()

        assertEquals(2, pods.size)
        assertEquals("coredns", pods[0].name)
        assertEquals(PodStatus.CRASH_LOOP, pods[1].status)
    }

    @Test
    fun `bridge carries namespace phase through instead of assuming Active`() {
        val fakeBridge = object : FakeKubeNexusNativeBridge() {
            override fun listNamespaces(rawKubeconfig: String): Result<List<Namespace>> =
                Result.Success(
                    listOf(
                        Namespace(name = "default", status = "Active"),
                        Namespace(name = "doomed", status = "Terminating"),
                    ),
                )
        }

        val namespaces = fakeBridge.listNamespaces("mock-kubeconfig").getOrThrow()

        assertEquals(2, namespaces.size)
        assertEquals("Terminating", namespaces[1].status)
    }

    @Test
    fun `health and version probes report success`() {
        val fakeBridge = FakeKubeNexusNativeBridge()

        assertTrue(fakeBridge.checkLivez("mock-kubeconfig").getOrThrow())
        assertTrue(fakeBridge.checkReadyz("mock-kubeconfig").getOrThrow())
        assertTrue(fakeBridge.checkHealthz("mock-kubeconfig").getOrThrow())
        assertEquals("v1.30.0", fakeBridge.serverVersion("mock-kubeconfig").getOrThrow())
        assertEquals(
            "Ready",
            fakeBridge.checkHealth("mock-kubeconfig").getOrThrow().statusMessage,
        )
    }

    @Test
    fun `api resources are returned as domain models`() {
        val fakeBridge = object : FakeKubeNexusNativeBridge() {
            override fun listAPIResources(rawKubeconfig: String): Result<List<APIResource>> =
                Result.Success(
                    listOf(APIResource(name = "pods", kind = "Pod", groupVersion = "v1")),
                )
        }

        val resources = fakeBridge.listAPIResources("mock-kubeconfig").getOrThrow()

        assertEquals(1, resources.size)
        assertEquals("Pod", resources[0].kind)
    }
}
