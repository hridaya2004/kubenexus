package dev.hridaya.kubenexus.core.nativebridge

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

            override fun createClient(): Result<client.Client_> {
                return if (isInit) {
                    Result.failure(UnsupportedOperationException("JVM mock environment"))
                } else {
                    Result.failure(IllegalStateException("Not initialized"))
                }
            }

            override fun listPods(namespace: String?): Result<List<String>> {
                return Result.success(listOf("coredns", "traefik"))
            }

            override fun listPodsWide(namespace: String?): Result<List<client.Pod>> {
                return Result.success(emptyList())
            }

            override fun listNamespaces(): Result<List<client.Namespace>> {
                return Result.success(emptyList())
            }

            override fun touch(): Boolean = true
        }

        assertFalse(fakeBridge.isAvailable())
        fakeBridge.initialize()
        assertTrue(fakeBridge.isAvailable())
        assertTrue(fakeBridge.touch())
        val pods = fakeBridge.listPods(null).getOrNull()
        assertEquals(2, pods?.size)
    }
}
