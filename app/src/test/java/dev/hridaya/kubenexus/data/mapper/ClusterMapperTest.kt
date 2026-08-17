package dev.hridaya.kubenexus.data.mapper

import dev.hridaya.kubenexus.core.security.AesGcmKubeconfigEncryptor
import dev.hridaya.kubenexus.data.source.local.entity.ClusterEntity
import dev.hridaya.kubenexus.domain.model.Cluster
import dev.hridaya.kubenexus.domain.model.ClusterStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClusterMapperTest {

    @Test
    fun `toDomain converts entity to domain model correctly`() {
        val entity = ClusterEntity(
            id = "c-1",
            name = "Production",
            serverUrl = "https://k8s.example.com:6443",
            rawKubeconfig = "apiVersion: v1...",
            contextName = "prod-ctx",
            userName = "admin",
            namespace = "default",
            isActive = true,
            createdAt = 1000L,
            lastConnectedAt = 2000L,
            status = "CONNECTED"
        )

        val domain = entity.toDomain()

        assertEquals("c-1", domain.id)
        assertEquals("Production", domain.name)
        assertEquals("https://k8s.example.com:6443", domain.serverUrl)
        assertEquals(true, domain.isActive)
        assertEquals(ClusterStatus.CONNECTED, domain.status)
        assertEquals("apiVersion: v1...", domain.rawKubeconfig)
    }

    @Test
    fun `toEntity converts domain to entity model correctly`() {
        val domain = Cluster(
            id = "c-2",
            name = "Staging",
            serverUrl = "https://staging.k8s.example.com",
            rawKubeconfig = "apiVersion: v1...",
            contextName = "staging-ctx",
            userName = "developer",
            namespace = "kube-system",
            isActive = false,
            createdAt = 3000L,
            lastConnectedAt = null,
            status = ClusterStatus.DISCONNECTED
        )

        val entity = domain.toEntity()

        assertEquals("c-2", entity.id)
        assertEquals("Staging", entity.name)
        assertEquals(false, entity.isActive)
        assertEquals("DISCONNECTED", entity.status)
        assertEquals("apiVersion: v1...", entity.rawKubeconfig)
    }

    @Test
    fun `toEntity with encryptor encrypts rawKubeconfig and toDomain decrypts it`() {
        val encryptor = AesGcmKubeconfigEncryptor(AesGcmKubeconfigEncryptor.generateKey())
        val plainKubeconfig = "apiVersion: v1\nkind: Config\nusers:\n- name: admin\n  user:\n    token: test-token"

        val domain = Cluster(
            id = "c-3",
            name = "Secure Cluster",
            serverUrl = "https://secure.k8s.example.com",
            rawKubeconfig = plainKubeconfig,
            contextName = "secure-ctx"
        )

        val entity = domain.toEntity(encryptor)
        assertTrue(entity.rawKubeconfig.startsWith("enc:v1:"))

        val restoredDomain = entity.toDomain(encryptor)
        assertEquals(plainKubeconfig, restoredDomain.rawKubeconfig)
    }
}
