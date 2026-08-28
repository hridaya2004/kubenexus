package dev.hridaya.kubenexus.domain.util

import dev.hridaya.kubenexus.domain.model.PodDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PodYamlGeneratorTest {

    // yamlkt owns two cosmetics this suite deliberately does not pin: the
    // trailing space it emits after a key introducing a nested block, and the
    // single quotes it wraps around values containing YAML-special characters.
    // Normalizing them keeps expectations focused on manifest structure.
    private fun normalizeRenderedManifest(renderedManifest: String): String =
        renderedManifest.trimEnd('\n').lines().joinToString("\n") { it.trimEnd().replace("'", "") }

    @Test
    fun `renders the full manifest deterministically`() {
        val renderedManifest = PodYamlGenerator.generate(
            PodDraft(
                name = "nginx",
                namespace = "web",
                image = "nginx:1.27",
                containerPort = 8080,
            ),
        )

        val expectedManifest = """
            apiVersion: v1
            kind: Pod
            metadata:
              name: nginx
              namespace: web
              labels:
                app: nginx
            spec:
              containers:
                - name: nginx
                  image: nginx:1.27
                  ports:
                    - containerPort: 8080
        """.trimIndent()

        assertEquals(expectedManifest, normalizeRenderedManifest(renderedManifest))
        assertTrue(renderedManifest.endsWith("\n"))
        assertFalse(renderedManifest.endsWith("\n\n"))
    }

    // Issue #5 follow-up acceptance: a zero port means "no ports block at all",
    // not port zero, so the block must disappear from the reviewed text.
    @Test
    fun `port is omitted from the manifest when zero`() {
        val renderedManifest = PodYamlGenerator.generate(
            PodDraft(name = "api", namespace = "default", image = "api:v1"),
        )

        assertFalse(renderedManifest.contains("ports"))
        assertFalse(renderedManifest.contains("containerPort"))
    }

    @Test
    fun `port is emitted in the manifest when positive`() {
        val renderedManifest = PodYamlGenerator.generate(
            PodDraft(
                name = "cache",
                namespace = "team-42",
                image = "redis:7-alpine",
                containerPort = 6379,
            ),
        )

        assertEquals(1, Regex("^\\s*ports:\\s*$", RegexOption.MULTILINE).findAll(renderedManifest).count())
        assertTrue(renderedManifest.contains("containerPort: 6379"))
    }
}
