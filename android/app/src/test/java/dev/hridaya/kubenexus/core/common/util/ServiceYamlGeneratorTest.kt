package dev.hridaya.kubenexus.core.common.util

import dev.hridaya.kubenexus.domain.model.ServiceDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceYamlGeneratorTest {

    @Test
    fun `renders the full manifest deterministically`() {
        val yaml = ServiceYamlGenerator.generate(
            ServiceDraft(
                name = "nginx",
                namespace = "web",
                selectorApp = "nginx",
                port = 80,
                targetPort = 8080,
                serviceType = "NodePort",
            ),
        )

        val expected = """
            apiVersion: v1
            kind: Service
            metadata:
              name: nginx
              namespace: web
              labels:
                app: nginx
            spec:
              selector:
                app: nginx
              type: NodePort
              ports:
                - port: 80
                  targetPort: 8080
        """.trimIndent() + "\n"

        assertEquals(expected, yaml)
    }

    // Issue #5 acceptance: the selector intentionally targets the app label the
    // Deployment generator stamps onto pods, independent of the Service's name.
    @Test
    fun `labels carry the service name while the selector carries the target app`() {
        val yaml = ServiceYamlGenerator.generate(
            ServiceDraft(
                name = "web-svc",
                namespace = "default",
                selectorApp = "web",
                port = 80,
                targetPort = 8080,
            ),
        )

        val appValues = Regex("^    app: (.+)$", RegexOption.MULTILINE)
            .findAll(yaml)
            .map { it.groupValues[1] }
            .toList()

        assertEquals(listOf("web-svc", "web"), appValues)
    }

    @Test
    fun `default type is emitted verbatim`() {
        val yaml = ServiceYamlGenerator.generate(
            ServiceDraft(
                name = "cache",
                namespace = "team-42",
                selectorApp = "cache",
                port = 6379,
                targetPort = 6379,
            ),
        )

        assertTrue(yaml.contains("  type: ${ServiceDraft.DEFAULT_TYPE}"))
        assertTrue(Regex("^    - port: 6379$", RegexOption.MULTILINE).containsMatchIn(yaml))
        assertTrue(yaml.contains("targetPort: 6379"))
    }
}
