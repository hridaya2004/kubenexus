package dev.hridaya.kubenexus.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogSanitizerTest {

    @Test
    fun `sanitize redacts bearer tokens and jwt tokens`() {
        val input = "Error: token: eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJrdWJlcm5ldGVzIn0 failed"
        val output = LogSanitizer.sanitize(input)

        assertFalse(output.contains("eyJhbGciOiJSUzI1Ni"))
        assertTrue(output.contains("token: [REDACTED]"))
    }

    @Test
    fun `sanitize redacts authorization bearer header`() {
        val input = "Request with Bearer ya29.a0AfH6SMD_123456 failed with 401"
        val output = LogSanitizer.sanitize(input)

        assertFalse(output.contains("ya29.a0AfH6SMD_123456"))
        assertTrue(output.contains("Bearer [REDACTED]"))
    }

    @Test
    fun `sanitize redacts client-certificate-data and client-key-data`() {
        val input = """
            client-certificate-data: LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCg==
            client-key-data: LS0tLS1CRUdJTiBSU0EgUFJJVkFURSBLRVktLS0tLQo=
            certificate-authority-data: LS0tLS1CRUdJTiBDQVRFLS0tLS0=
        """.trimIndent()

        val output = LogSanitizer.sanitize(input)

        assertFalse(output.contains("LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCg=="))
        assertFalse(output.contains("LS0tLS1CRUdJTiBSU0EgUFJJVkFURSBLRVktLS0tLQo="))
        assertFalse(output.contains("LS0tLS1CRUdJTiBDQVRFLS0tLS0="))

        assertTrue(output.contains("client-certificate-data: [REDACTED]"))
        assertTrue(output.contains("client-key-data: [REDACTED]"))
        assertTrue(output.contains("certificate-authority-data: [REDACTED]"))
    }

    @Test
    fun `sanitize redacts raw PEM blocks`() {
        val input = """
            Failed with key:
            -----BEGIN RSA PRIVATE KEY-----
            MIIEowIBAAKCAQEA0m4w8hZ8x...
            -----END RSA PRIVATE KEY-----
            and cert:
            -----BEGIN CERTIFICATE-----
            MIIDIDCCAigCCQDF...
            -----END CERTIFICATE-----
        """.trimIndent()

        val output = LogSanitizer.sanitize(input)

        assertFalse(output.contains("BEGIN RSA PRIVATE KEY"))
        assertFalse(output.contains("BEGIN CERTIFICATE"))
        assertTrue(output.contains("[REDACTED PEM BLOCK]"))
    }

    @Test
    fun `sanitize redacts passwords and basic auth`() {
        val input = "Connecting with password: secretPass123! and Authorization: Basic dXNlcjpwYXNz"
        val output = LogSanitizer.sanitize(input)

        assertFalse(output.contains("secretPass123!"))
        assertFalse(output.contains("dXNlcjpwYXNz"))
        assertTrue(output.contains("password: [REDACTED]"))
        assertTrue(output.contains("Basic [REDACTED]"))
    }

    @Test
    fun `sanitize redacts embedded url userinfo credentials`() {
        val input = "Connecting to https://admin:superSecret@k8s.example.com:6443/version"
        val output = LogSanitizer.sanitize(input)

        assertFalse(output.contains("superSecret"))
        assertTrue(output.contains("https://admin:[REDACTED]@k8s.example.com:6443/version"))
    }

    @Test
    fun `sanitize preserves harmless diagnostic messages`() {
        val input = "Pod nginx-7854ff88-abc is in Running state on node worker-1"
        val output = LogSanitizer.sanitize(input)

        assertEquals(input, output)
    }

    @Test
    fun `sanitize handles null and empty inputs safely`() {
        assertEquals("", LogSanitizer.sanitize(null))
        assertEquals("", LogSanitizer.sanitize(""))
    }
}
