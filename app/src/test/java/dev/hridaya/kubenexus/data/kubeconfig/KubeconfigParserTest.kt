package dev.hridaya.kubenexus.data.kubeconfig

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KubeconfigParserTest {

    private val sampleKubeconfig = """
        apiVersion: v1
        clusters:
        - cluster:
            certificate-authority-data: LS0tLS1CRUdJTi...
            server: https://192.168.49.2:8443
          name: minikube
        contexts:
        - context:
            cluster: minikube
            namespace: default
            user: minikube
          name: minikube
        current-context: minikube
        kind: Config
        preferences: {}
        users:
        - name: minikube
          user:
            client-certificate-data: LS0tLS1CRUdJTi...
            client-key-data: LS0tLS1CRUdJTi...
    """.trimIndent()

    @Test
    fun `parse valid kubeconfig extracts correct metadata`() {
        val parsed = KubeconfigParser.parse(sampleKubeconfig)

        assertEquals("minikube", parsed.clusterName)
        assertEquals("https://192.168.49.2:8443", parsed.serverUrl)
        assertEquals("minikube", parsed.contextName)
        assertEquals("minikube", parsed.userName)
        assertEquals("default", parsed.namespace)
    }

    @Test
    fun `parse with custom name overrides extracted name`() {
        val parsed = KubeconfigParser.parse(sampleKubeconfig, customName = "My Minikube Lab")

        assertEquals("My Minikube Lab", parsed.clusterName)
        assertEquals("https://192.168.49.2:8443", parsed.serverUrl)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parse empty string throws exception`() {
        KubeconfigParser.parse("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parse kubeconfig without server throws exception`() {
        val invalidKubeconfig = """
            apiVersion: v1
            kind: Config
            current-context: dev-cluster
        """.trimIndent()
        KubeconfigParser.parse(invalidKubeconfig)
    }
}
