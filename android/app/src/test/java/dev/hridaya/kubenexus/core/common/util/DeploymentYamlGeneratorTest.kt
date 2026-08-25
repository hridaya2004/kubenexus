package dev.hridaya.kubenexus.core.common.util

import dev.hridaya.kubenexus.domain.model.DeploymentDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeploymentYamlGeneratorTest {

    @Test
    fun `renders the full manifest deterministically`() {
        val yaml = DeploymentYamlGenerator.generate(
            DeploymentDraft(
                name = "nginx",
                namespace = "web",
                image = "nginx:1.27",
                replicas = 3,
                containerPort = 8080,
            ),
        )

        val expected = """
            apiVersion: apps/v1
            kind: Deployment
            metadata:
              name: nginx
              namespace: web
              labels:
                app: nginx
            spec:
              replicas: 3
              selector:
                matchLabels:
                  app: nginx
              template:
                metadata:
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

    // Issue #5 acceptance: the Deployment's labels and selector must match so a
    // Service added later can target the workload.
    @Test
    fun `pod labels match the deployment selector`() {
        val yaml = DeploymentYamlGenerator.generate(
            DeploymentDraft(name = "api", namespace = "default", image = "api:v1"),
        )

        // One app label at each level: deployment metadata, selector matchLabels,
        // and the pod template.
        assertEquals(1, Regex("^    app: api$", RegexOption.MULTILINE).findAll(yaml).count())
        assertEquals(1, Regex("^      app: api$", RegexOption.MULTILINE).findAll(yaml).count())
        assertEquals(1, Regex("^        app: api$", RegexOption.MULTILINE).findAll(yaml).count())
    }

    @Test
    fun `values are interpolated verbatim`() {
        val yaml = DeploymentYamlGenerator.generate(
            DeploymentDraft(
                name = "cache",
                namespace = "team-42",
                image = "registry.example.com/redis:7-alpine",
                replicas = 5,
                containerPort = 6379,
            ),
        )

        assertEquals("cache", Regex("name: (\\S+)").find(yaml)!!.groupValues[1])
        assertTrue(yaml.contains("image: registry.example.com/redis:7-alpine"))
        assertTrue(yaml.contains("replicas: 5"))
        assertTrue(yaml.contains("containerPort: 6379"))
    }
}
