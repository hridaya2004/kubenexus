package dev.hridaya.kubenexus.core.nativebridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NativeBridgeJsonParserTest {

    private lateinit var parser: NativeBridgeJsonParser

    @Before
    fun setUp() {
        parser = NativeBridgeJsonParser()
    }

    private fun loadFixture(name: String): String =
        javaClass.classLoader.getResourceAsStream(name)!!.bufferedReader().readText()

    @Test
    fun `parseAPIResources returns empty list for blank input`() {
        assertTrue(parser.parseAPIResources("").isEmpty())
        assertTrue(parser.parseAPIResources("   ").isEmpty())
    }

    @Test
    fun `parseAPIResources splits groupVersion, dedupes and sorts by name`() {
        val resources = parser.parseAPIResources(loadFixture("api-resources.json"))

        assertEquals(4, resources.size)

        // Sorted by name, then group version: apps/v1 pods before core v1 pods.
        assertEquals("deployments", resources[0].name)
        assertEquals("apps/v1", resources[0].groupVersion)
        assertEquals("apps", resources[0].group)
        assertEquals("v1", resources[0].version)

        val corePods = resources.last { it.name == "pods" }
        assertEquals("", corePods.group)
        assertEquals("v1", corePods.version)
        assertEquals("pod", corePods.singularName)
        assertTrue(corePods.namespaced)
        assertEquals(listOf("po"), corePods.shortNames)
        assertEquals(listOf("all"), corePods.categories)

        val nodes = resources.first { it.name == "nodes" }
        assertFalse(nodes.namespaced)
        assertEquals("Node", nodes.kind)
    }

    @Test
    fun `parseAPIResources drops repeated entries within a group version`() {
        val payload = loadFixture("api-resources-duplicates.json")

        assertEquals(1, parser.parseAPIResources(payload).size)
    }

    @Test
    fun `resolveResourceExplain falls back to title-cased stub without schema`() {
        val explain = parser.resolveResourceExplain("", "pod", "")

        assertEquals("Pod", explain.kind)
        assertEquals("", explain.groupVersion)
        assertTrue(explain.description.contains("schema unavailable"))
        assertTrue(explain.fields.isNotEmpty())
        assertTrue(explain.fields.any { it.name == "spec" && it.type == "object" })
    }

    @Test
    fun `resolveResourceExplain leaves versioned custom resources untouched`() {
        val explain = parser.resolveResourceExplain("", "mycustomresource", "custom.io/v1alpha1")

        assertEquals("mycustomresource", explain.kind)
        assertEquals("custom.io/v1alpha1", explain.groupVersion)
    }

    @Test
    fun `resolveResourceExplain matches singular and plural against schema GVKs`() {
        val schema = loadFixture("openapi-pod-schema.json")

        val pod = parser.resolveResourceExplain(schema, "pod", "")
        assertEquals("Pod", pod.kind)
        assertEquals("", pod.group)
        assertEquals("v1", pod.version)
        assertEquals("v1", pod.groupVersion)
        assertEquals("Pod is a collection of containers.", pod.description)

        // Fields come out sorted by name; $ref maps to its last path segment;
        // format rides along in parentheses; required comes from the schema.
        assertEquals(
            listOf("apiVersion", "restartPolicy", "status", "terminationGracePeriodSeconds"),
            pod.fields.map { it.name },
        )
        val status = pod.fields.first { it.name == "status" }
        assertEquals("PodStatus", status.type)
        assertFalse(status.required)
        assertTrue(pod.fields.first { it.name == "apiVersion" }.required)
        assertEquals("integer (int64)", pod.fields.first { it.name == "terminationGracePeriodSeconds" }.type)

        val podsPlural = parser.resolveResourceExplain(schema, "pods", "v1")
        assertEquals("Pod", podsPlural.kind)
    }

    @Test
    fun `resolveResourceExplain uses groupVersion to disambiguate`() {
        val schema = loadFixture("openapi-pod-multiversion.json")

        assertEquals(
            "Something pod.",
            parser.resolveResourceExplain(schema, "pod", "something/v2").description,
        )
        assertEquals(
            "Core pod.",
            parser.resolveResourceExplain(schema, "pod", "v1").description,
        )
    }
}
