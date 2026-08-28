package dev.hridaya.kubenexus.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeploymentDraftTest {

    private fun validDraft() = DeploymentDraft(
        name = "nginx",
        namespace = "default",
        image = "nginx:1.27",
        replicas = 2,
        containerPort = 8080,
    )

    @Test
    fun `valid draft has no errors`() {
        assertTrue(validDraft().validate().isEmpty())
    }

    @Test
    fun `blank required fields are reported individually`() {
        val errors = DeploymentDraft(image = " ").validate()

        assertEquals(setOf("name", "namespace", "image"), errors.keys)
        assertTrue(errors["image"]!!.contains("required"))
    }

    @Test
    fun `name must be a dns1035 label`() {
        val draft = validDraft().copy(name = "Web_Server")

        assertEquals(1, draft.validate().size)
        assertTrue(draft.validate().containsKey("name"))
    }

    @Test
    fun `name cannot start or end with a hyphen`() {
        assertTrue(validDraft().copy(name = "-nginx").validate().containsKey("name"))
        assertTrue(validDraft().copy(name = "nginx-").validate().containsKey("name"))
    }

    @Test
    fun `name longer than 63 characters is rejected`() {
        val longName = "a".repeat(64)

        val errors = validDraft().copy(name = longName).validate()

        assertTrue(errors["name"]!!.contains("63"))
    }

    @Test
    fun `single character names are valid`() {
        assertTrue(validDraft().copy(name = "a").validate().isEmpty())
        assertTrue(validDraft().copy(name = "a-b-c").validate().isEmpty())
    }

    @Test
    fun `namespace follows dns1123 labels and may start with a digit`() {
        assertTrue(validDraft().copy(namespace = "team-42").validate().isEmpty())
        assertTrue(validDraft().copy(namespace = "-team").validate().containsKey("namespace"))
        assertTrue(validDraft().copy(namespace = "Team").validate().containsKey("namespace"))
    }

    @Test
    fun `image cannot contain whitespace`() {
        val errors = validDraft().copy(image = "my registry/nginx").validate()

        assertTrue(errors.containsKey("image"))
    }

    @Test
    fun `replicas must stay within range`() {
        assertTrue(validDraft().replicas >= 1)

        val zero = validDraft().copy(replicas = 0).validate()
        val over = validDraft().copy(replicas = DeploymentDraft.MAX_REPLICAS + 1).validate()
        val min = validDraft().copy(replicas = DeploymentDraft.MIN_REPLICAS).validate()
        val max = validDraft().copy(replicas = DeploymentDraft.MAX_REPLICAS).validate()

        assertTrue(zero.containsKey("replicas"))
        assertTrue(over.containsKey("replicas"))
        assertTrue(min.isEmpty())
        assertTrue(max.isEmpty())
    }

    @Test
    fun `containerPort must be a valid port`() {
        assertTrue(validDraft().copy(containerPort = 0).validate().containsKey("containerPort"))
        assertTrue(validDraft().copy(containerPort = 65536).validate().containsKey("containerPort"))
        assertEquals(
            emptyMap<String, String>(),
            validDraft().copy(containerPort = 65535).validate(),
        )
    }
}
