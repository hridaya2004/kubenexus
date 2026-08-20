package dev.hridaya.kubenexus.core.nativebridge

import dev.hridaya.kubenexus.domain.model.APIResource
import dev.hridaya.kubenexus.domain.model.ResourceExplain
import dev.hridaya.kubenexus.domain.model.ResourceField
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parser responsible for transforming raw JSON payloads returned by the native Go runtime
 * into typed domain models.
 */
@Singleton
class NativeBridgeJsonParser @Inject constructor() {

    /**
     * Parses the JSON payload returned by `Client.listAPIResourcesJSON()` into a list of [APIResource].
     */
    fun parseAPIResources(apiResourceList: String): List<APIResource> {
        if (apiResourceList.isBlank()) return emptyList()
        val resourcesArray = JSONArray(apiResourceList)
        val resourceList = ArrayList<APIResource>(resourcesArray.length())
        for (index in 0 until resourcesArray.length()) {
            val resourceObject = resourcesArray.getJSONObject(index)
            val verbs = resourceObject.optJSONArray("verbs")?.toStringList() ?: emptyList()
            val shortNames = resourceObject.optJSONArray("shortNames")?.toStringList() ?: emptyList()
            val categories = resourceObject.optJSONArray("categories")?.toStringList() ?: emptyList()

            resourceList.add(
                APIResource(
                    name = resourceObject.optString("name", ""),
                    singularName = resourceObject.optString("singularName", ""),
                    namespaced = resourceObject.optBoolean("namespaced", true),
                    kind = resourceObject.optString("kind", ""),
                    group = resourceObject.optString("group", ""),
                    version = resourceObject.optString("version", ""),
                    groupVersion = resourceObject.optString("groupVersion", ""),
                    verbs = verbs,
                    shortNames = shortNames,
                    categories = categories,
                )
            )
        }
        return resourceList
    }

    /**
     * Parses the JSON payload returned by `Client.explainResourceJSON()` into a [ResourceExplain] model.
     */
    fun parseResourceExplain(
        resourceExplain: String,
        fallbackKind: String = "",
        fallbackGroupVersion: String = "",
    ): ResourceExplain {
        if (resourceExplain.isBlank()) {
            return ResourceExplain(
                kind = fallbackKind,
                groupVersion = fallbackGroupVersion,
                description = "",
                fields = emptyList(),
            )
        }

        val explainObject = JSONObject(resourceExplain)
        val fieldsArray = explainObject.optJSONArray("fields")
        val fieldsList = if (fieldsArray != null) {
            val resourceFields = ArrayList<ResourceField>(fieldsArray.length())
            for (index in 0 until fieldsArray.length()) {
                val fieldObject = fieldsArray.getJSONObject(index)
                resourceFields.add(
                    ResourceField(
                        name = fieldObject.optString("name", ""),
                        type = fieldObject.optString("type", ""),
                        description = fieldObject.optString("description", ""),
                        required = fieldObject.optBoolean("required", false),
                    )
                )
            }
            resourceFields
        } else {
            emptyList()
        }

        return ResourceExplain(
            kind = explainObject.optString("kind", fallbackKind),
            group = explainObject.optString("group", ""),
            version = explainObject.optString("version", ""),
            groupVersion = explainObject.optString("groupVersion", fallbackGroupVersion),
            description = explainObject.optString("description", ""),
            fields = fieldsList,
        )
    }

    /**
     * Parses the JSON payload returned by `Client.checkHealthJSON()` into a [ClusterHealth] model.
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

    private fun JSONArray.toStringList(): List<String> {
        val stringList = ArrayList<String>(length())
        for (index in 0 until length()) {
            stringList.add(getString(index))
        }
        return stringList
    }
}
