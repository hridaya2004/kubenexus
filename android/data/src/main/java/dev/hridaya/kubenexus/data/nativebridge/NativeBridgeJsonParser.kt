package dev.hridaya.kubenexus.data.nativebridge

import dev.hridaya.kubenexus.domain.model.APIResource
import dev.hridaya.kubenexus.domain.model.ClusterHealth
import dev.hridaya.kubenexus.domain.model.ResourceExplain
import dev.hridaya.kubenexus.domain.model.ResourceField
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Transforms raw JSON payloads from the native Go runtime into domain models.
 */
@Singleton
class NativeBridgeJsonParser @Inject constructor() {

    /**
     * Parses the discovery payload of `Client.listAPIResourcesJSON()`: an array
     * of `{groupVersion, resources:[...]}` listings, into [APIResource]s sorted
     * by name then group version.
     */
    fun parseAPIResources(payload: String): List<APIResource> {
        if (payload.isBlank()) return emptyList()
        val seen = HashSet<String>()
        val result = ArrayList<APIResource>()
        val listings = JSONArray(payload)
        for (i in 0 until listings.length()) {
            val listing = listings.getJSONObject(i)
            val groupVersion = listing.optString("groupVersion", "")
            val slash = groupVersion.indexOf('/')
            val group = if (slash >= 0) groupVersion.take(slash) else ""
            val version = if (slash >= 0) groupVersion.substring(slash + 1) else groupVersion
            val resources = listing.optJSONArray("resources") ?: continue
            for (j in 0 until resources.length()) {
                val r = resources.getJSONObject(j)
                val name = r.optString("name", "")
                if (!seen.add("$groupVersion/$name")) continue
                result.add(
                    APIResource(
                        name = name,
                        singularName = r.optString("singularName", ""),
                        namespaced = r.optBoolean("namespaced", true),
                        kind = r.optString("kind", ""),
                        group = group,
                        version = version,
                        groupVersion = groupVersion,
                        verbs = r.optJSONArray("verbs")?.toStringList() ?: emptyList(),
                        shortNames = r.optJSONArray("shortNames")?.toStringList() ?: emptyList(),
                        categories = r.optJSONArray("categories")?.toStringList() ?: emptyList(),
                    )
                )
            }
        }
        result.sortWith(compareBy({ it.name }, { it.groupVersion }))
        return result
    }

    /**
     * Resolves [ResourceExplain] for a resource or kind against an OpenAPI v2
     * schema document. Returns a best-effort fallback when the schema is
     * unavailable or nothing matches.
     */
    fun resolveResourceExplain(
        schemaJson: String,
        resourceOrKind: String,
        groupVersion: String
    ): ResourceExplain {
        return findDefinition(schemaJson, resourceOrKind, groupVersion)
            ?: buildFallbackExplain(resourceOrKind, groupVersion)
    }

    /**
     * Parses the `definitions` map out of an OpenAPI document.
     *
     * Exposed separately because the document is multi-megabyte and a caller
     * attempting several lookups would otherwise re-parse it for each one.
     */
    fun parseDefinitions(schemaJson: String): JSONObject? {
        if (schemaJson.isBlank()) return null
        return JSONObject(schemaJson).optJSONObject("definitions")
    }

    /**
     * Returns null when the schema has no definition matching the resource or
     * kind, so callers can decide whether a fallback is worth persisting.
     */
    fun findDefinition(
        schemaJson: String,
        resourceOrKind: String,
        groupVersion: String
    ): ResourceExplain? =
        parseDefinitions(schemaJson)?.let { findDefinition(it, resourceOrKind, groupVersion) }

    fun findDefinition(
        definitions: JSONObject,
        resourceOrKind: String,
        groupVersion: String,
    ): ResourceExplain? {
        val target = resourceOrKind.lowercase()

        // Sorted for deterministic match order across JVMs.
        for (key in definitions.sortedKeys()) {
            val definition = definitions.optJSONObject(key) ?: continue
            val gvks = definition.optJSONArray("x-kubernetes-group-version-kind") ?: continue
            for (i in 0 until gvks.length()) {
                val gvk = gvks.getJSONObject(i)
                val kind = gvk.optString("kind", "")
                val group = gvk.optString("group", "")
                val version = gvk.optString("version", "")

                val kindMatches =
                    kind.equals(target, ignoreCase = true) || kind.plus("s")
                        .equals(target, ignoreCase = true)
                val versionMatches = groupVersion.isBlank() ||
                        version.equals(groupVersion, ignoreCase = true) ||
                        "$group/$version".equals(groupVersion, ignoreCase = true)
                if (!kindMatches || !versionMatches) continue

                return buildExplainFromDefinition(definition, kind, group, version)
            }
        }
        return null
    }

    /**
     * Exact GVK lookup — the kubectl approach. Discovery maps a resource name,
     * singular name or kind to group+version+kind; this finds the schema
     * definition carrying that GVK. Handles irregular plurals ("policies" ->
     * Policy) and every custom resource installed on the cluster.
     */
    fun findDefinitionByGVK(
        schemaJson: String,
        group: String,
        version: String,
        kind: String,
    ): ResourceExplain? =
        parseDefinitions(schemaJson)?.let { findDefinitionByGVK(it, group, version, kind) }

    fun findDefinitionByGVK(
        definitions: JSONObject,
        group: String,
        version: String,
        kind: String,
    ): ResourceExplain? {
        if (kind.isBlank()) return null

        for (key in definitions.sortedKeys()) {
            val definition = definitions.optJSONObject(key) ?: continue
            val gvks = definition.optJSONArray("x-kubernetes-group-version-kind") ?: continue
            for (i in 0 until gvks.length()) {
                val gvk = gvks.getJSONObject(i)
                if (gvk.optString("kind").equals(kind, ignoreCase = true) &&
                    gvk.optString("group").equals(group, ignoreCase = true) &&
                    gvk.optString("version").equals(version, ignoreCase = true)
                ) {
                    return buildExplainFromDefinition(
                        definition,
                        gvk.optString("kind"),
                        gvk.optString("group"),
                        gvk.optString("version"),
                    )
                }
            }
        }
        return null
    }

    private fun buildExplainFromDefinition(
        definition: JSONObject,
        kind: String,
        group: String,
        version: String,
    ): ResourceExplain {
        val required = mutableSetOf<String>()
        definition.optJSONArray("required")?.let { arr ->
            for (r in 0 until arr.length()) required.add(arr.getString(r))
        }

        val fields = ArrayList<ResourceField>()
        val props = definition.optJSONObject("properties")
        for (name in props?.sortedKeys() ?: emptyList()) {
            val prop = props!!.getJSONObject(name)
            var type = prop.optString("type", "object").ifBlank { "object" }
            if (!prop.isNull("\$ref") && prop.optString("\$ref").isNotEmpty()) {
                type = prop.optString("\$ref").substringAfterLast('.')
            }
            prop.optString("format", "").takeIf { it.isNotEmpty() }?.let { type += " ($it)" }
            fields.add(
                ResourceField(
                    name = name,
                    type = type,
                    description = prop.optString("description", ""),
                    required = name in required,
                )
            )
        }

        return ResourceExplain(
            kind = kind,
            group = group,
            version = version,
            groupVersion = if (group.isEmpty()) version else "$group/$version",
            description = definition.optString("description", ""),
            fields = fields,
        )
    }

    fun buildFallbackExplain(resourceOrKind: String, groupVersion: String): ResourceExplain {
        val kind = if (groupVersion.isBlank() && resourceOrKind.isNotEmpty()) {
            resourceOrKind.replaceFirstChar { it.uppercase() }
        } else {
            resourceOrKind
        }
        return ResourceExplain(
            kind = kind,
            groupVersion = groupVersion,
            description =
                "Kubernetes resource %s (%s). Documentation schema unavailable from active cluster discovery."
                    .format(resourceOrKind, groupVersion),
            fields = listOf(
                ResourceField(
                    "apiVersion", "string",
                    "APIVersion defines the versioned schema of this representation of an object.",
                    required = true,
                ),
                ResourceField(
                    "kind", "string",
                    "Kind is a string value representing the REST resource this object represents.",
                    required = true,
                ),
                ResourceField(
                    "metadata", "ObjectMeta",
                    "Standard object's metadata. More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#metadata",
                ),
                ResourceField(
                    "spec",
                    "object",
                    "Specification of the desired behavior of the resource."
                ),
                ResourceField(
                    "status", "object",
                    "Most recently observed status of the resource. More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#spec-and-status",
                ),
            ),
        )
    }

    /**
     * Parses the JSON payload of `Client.checkHealthJSON()` into [ClusterHealth].
     */
    fun parseClusterHealth(clusterHealth: String): ClusterHealth {
        if (clusterHealth.isBlank()) return ClusterHealth()
        val healthObject = JSONObject(clusterHealth)
        return ClusterHealth(
            livez = healthObject.optBoolean("livez", false),
            readyz = healthObject.optBoolean("readyz", false),
            healthz = healthObject.optBoolean("healthz", false),
            serverVersion = healthObject.optString("serverVersion", ""),
            statusMessage = healthObject.optString("statusMessage", ""),
        )
    }

    private fun JSONObject.sortedKeys(): List<String> {
        val names = ArrayList<String>()
        val iter = keys()
        while (iter.hasNext()) names.add(iter.next())
        return names.sorted()
    }

    private fun JSONArray.toStringList(): List<String> {
        val stringList = ArrayList<String>(length())
        for (index in 0 until length()) {
            stringList.add(getString(index))
        }
        return stringList
    }
}
