package dev.hridaya.kubenexus.core.common.util

import dev.hridaya.kubenexus.domain.model.PodDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PodYamlGeneratorTest {

    @Test
    fun `renders the full manifest deterministically`() {
        val yaml = PodYamlGenerator.generate(
            PodDraft(
                name = "nginx",
                namespace = "web",
                image = "nginx:1.27",
                containerPort = 8080,
            ),
        )

        val expected = """
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
        """.trimIndent() + "\n"

        assertEquals(expected, yaml)
    }

    // Issue #5 follow-up acceptance: a zero port means "no ports block at all",
    // not port zero, so the block must disappear from the reviewed text.
    @Test
    fun `port is omitted from the manifest when zero`() {
        val yaml = PodYamlGenerator.generate(
            PodDraft(name = "api", namespace = "default", image = "api:v1"),
        )

        assertFalse(yaml.contains("ports"))
        assertFalse(yaml.contains("containerPort"))
    }

    @Test
    fun `port is emitted in the manifest when positive`() {
        val yaml = PodYamlGenerator.generate(
            PodDraft(
                name = "cache",
                namespace = "team-42",
                image = "redis:7-alpine",
                containerPort = 6379,
            ),
        )

        assertEquals(1, Regex("^      ports:$", RegexOption.MULTILINE).findAll(yaml).count())
        assertTrue(yaml.contains("containerPort: 6379"))
    }
}
