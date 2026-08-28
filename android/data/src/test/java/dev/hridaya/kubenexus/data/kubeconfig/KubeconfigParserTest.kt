package dev.hridaya.kubenexus.data.kubeconfig

import org.junit.Assert.assertEquals
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
        assertEquals("LS0tLS1CRUdJTi...", parsed.certificateAuthorityData)
        assertEquals(false, parsed.insecureSkipTlsVerify)
    }

    @Test
    fun `parse kubeconfig with insecure-skip-tls-verify extracts flag`() {
        val insecureKubeconfig = """
            apiVersion: v1
            clusters:
            - cluster:
                insecure-skip-tls-verify: true
                server: https://10.0.0.1:6443
              name: insecure-cluster
            contexts:
            - context:
                cluster: insecure-cluster
                user: admin
              name: insecure-ctx
            current-context: insecure-ctx
            kind: Config
        """.trimIndent()

        val parsed = KubeconfigParser.parse(insecureKubeconfig)
        assertEquals(true, parsed.insecureSkipTlsVerify)
        assertEquals(null, parsed.certificateAuthorityData)
    }

    @Test
    fun `parse multi-cluster kubeconfig extracts targeted cluster TLS config`() {
        val multiClusterKubeconfig = """
            apiVersion: v1
            clusters:
            - name: dev-cluster
              cluster:
                insecure-skip-tls-verify: true
                server: https://10.0.0.1:6443
            - name: prod-cluster
              cluster:
                certificate-authority-data: cHJvZC1jYS1kYXRh
                insecure-skip-tls-verify: false
                server: https://10.0.0.2:6443
            contexts:
            - name: dev-ctx
              context:
                cluster: dev-cluster
            - name: prod-ctx
              context:
                cluster: prod-cluster
            current-context: prod-ctx
            kind: Config
        """.trimIndent()

        val parsed = KubeconfigParser.parse(multiClusterKubeconfig)
        assertEquals("prod-cluster", parsed.clusterName)
        assertEquals("https://10.0.0.2:6443", parsed.serverUrl)
        assertEquals("cHJvZC1jYS1kYXRh", parsed.certificateAuthorityData)
        assertEquals(false, parsed.insecureSkipTlsVerify)
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
