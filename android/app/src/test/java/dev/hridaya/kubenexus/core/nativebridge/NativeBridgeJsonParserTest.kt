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

    @Test
    fun `parseAPIResources returns empty list for empty or blank json`() {
        assertTrue(parser.parseAPIResources("").isEmpty())
        assertTrue(parser.parseAPIResources("   ").isEmpty())
    }

    @Test
    fun `parseAPIResources correctly parses json array`() {
        val apiResourceList = """
            [
                {
                    "name": "pods",
                    "singularName": "pod",
                    "namespaced": true,
                    "kind": "Pod",
                    "group": "",
                    "version": "v1",
                    "groupVersion": "v1",
                    "verbs": ["get", "list", "watch", "create", "delete"],
                    "shortNames": ["po"],
                    "categories": ["all"]
                },
                {
                    "name": "nodes",
                    "singularName": "node",
                    "namespaced": false,
                    "kind": "Node",
                    "group": "",
                    "version": "v1",
                    "groupVersion": "v1",
                    "verbs": ["get", "list"],
                    "shortNames": ["no"],
                    "categories": []
                }
            ]
        """.trimIndent()

        val resources = parser.parseAPIResources(apiResourceList)
        assertEquals(2, resources.size)

        val pod = resources[0]
        assertEquals("pods", pod.name)
        assertEquals("pod", pod.singularName)
        assertTrue(pod.namespaced)
        assertEquals("Pod", pod.kind)
        assertEquals("v1", pod.groupVersion)
        assertEquals(listOf("get", "list", "watch", "create", "delete"), pod.verbs)
        assertEquals(listOf("po"), pod.shortNames)
        assertEquals(listOf("all"), pod.categories)

        val node = resources[1]
        assertEquals("nodes", node.name)
        assertFalse(node.namespaced)
        assertEquals(listOf("no"), node.shortNames)
    }

    @Test
    fun `parseResourceExplain returns fallback on blank json`() {
        val result = parser.parseResourceExplain("", fallbackKind = "Deployment", fallbackGroupVersion = "apps/v1")
        assertEquals("Deployment", result.kind)
        assertEquals("apps/v1", result.groupVersion)
        assertTrue(result.fields.isEmpty())
    }

    @Test
    fun `parseResourceExplain correctly parses full explain json`() {
        val resourceExplain = """
            {
                "kind": "Pod",
                "group": "",
                "version": "v1",
                "groupVersion": "v1",
                "description": "Pod is a collection of containers that can run on a host.",
                "fields": [
                    {
                        "name": "apiVersion",
                        "type": "string",
                        "description": "APIVersion defines the versioned schema.",
                        "required": false
                    },
                    {
                        "name": "spec",
                        "type": "PodSpec",
                        "description": "Specification of the desired behavior of the pod.",
                        "required": true
                    }
                ]
            }
        """.trimIndent()

        val explain = parser.parseResourceExplain(resourceExplain, "Pod", "v1")
        assertEquals("Pod", explain.kind)
        assertEquals("v1", explain.groupVersion)
        assertEquals("Pod is a collection of containers that can run on a host.", explain.description)
        assertEquals(2, explain.fields.size)

        val apiVersionField = explain.fields[0]
        assertEquals("apiVersion", apiVersionField.name)
        assertEquals("string", apiVersionField.type)
        assertFalse(apiVersionField.required)

        val specField = explain.fields[1]
        assertEquals("spec", specField.name)
        assertEquals("PodSpec", specField.type)
        assertTrue(specField.required)
    }

    @Test
    fun `parseClusterHealth returns default on blank json`() {
        val health = parser.parseClusterHealth("")
        assertFalse(health.livez)
        assertFalse(health.readyz)
        assertFalse(health.healthz)
        assertEquals("", health.serverVersion)
    }

    @Test
    fun `parseClusterHealth parses health json correctly`() {
        val clusterHealth = """
            {
                "livez": true,
                "readyz": true,
                "healthz": true,
                "serverVersion": "v1.31.1",
                "statusMessage": "Cluster is operational"
            }
        """.trimIndent()

        val health = parser.parseClusterHealth(clusterHealth)
        assertTrue(health.livez)
        assertTrue(health.readyz)
        assertTrue(health.healthz)
        assertEquals("v1.31.1", health.serverVersion)
        assertEquals("Cluster is operational", health.statusMessage)
    }
}
