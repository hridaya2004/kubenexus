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
    fun parseAPIResources(jsonStr: String): List<APIResource> {
        if (jsonStr.isBlank()) return emptyList()
        val jsonArray = JSONArray(jsonStr)
        val list = ArrayList<APIResource>(jsonArray.length())
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val verbs = obj.optJSONArray("verbs")?.toStringList() ?: emptyList()
            val shortNames = obj.optJSONArray("shortNames")?.toStringList() ?: emptyList()
            val categories = obj.optJSONArray("categories")?.toStringList() ?: emptyList()

            list.add(
                APIResource(
                    name = obj.optString("name", ""),
                    singularName = obj.optString("singularName", ""),
                    namespaced = obj.optBoolean("namespaced", true),
                    kind = obj.optString("kind", ""),
                    group = obj.optString("group", ""),
                    version = obj.optString("version", ""),
                    groupVersion = obj.optString("groupVersion", ""),
                    verbs = verbs,
                    shortNames = shortNames,
                    categories = categories,
                )
            )
        }
        return list
    }

    /**
     * Parses the JSON payload returned by `Client.explainResourceJSON()` into a [ResourceExplain] model.
     */
    fun parseResourceExplain(
        jsonStr: String,
        fallbackKind: String = "",
        fallbackGroupVersion: String = "",
    ): ResourceExplain {
        if (jsonStr.isBlank()) {
            return ResourceExplain(
                kind = fallbackKind,
                groupVersion = fallbackGroupVersion,
                description = "",
                fields = emptyList(),
            )
        }

        val obj = JSONObject(jsonStr)
        val fieldsArray = obj.optJSONArray("fields")
        val fieldsList = if (fieldsArray != null) {
            val fields = ArrayList<ResourceField>(fieldsArray.length())
            for (i in 0 until fieldsArray.length()) {
                val fObj = fieldsArray.getJSONObject(i)
                fields.add(
                    ResourceField(
                        name = fObj.optString("name", ""),
                        type = fObj.optString("type", ""),
                        description = fObj.optString("description", ""),
                        required = fObj.optBoolean("required", false),
                    )
                )
            }
            fields
        } else {
            emptyList()
        }

        return ResourceExplain(
            kind = obj.optString("kind", fallbackKind),
            group = obj.optString("group", ""),
            version = obj.optString("version", ""),
            groupVersion = obj.optString("groupVersion", fallbackGroupVersion),
            description = obj.optString("description", ""),
            fields = fieldsList,
        )
    }

    /**
     * Parses the JSON payload returned by `Client.checkHealthJSON()` into a [ClusterHealth] model.
     */
    fun parseClusterHealth(jsonStr: String): ClusterHealth {
        if (jsonStr.isBlank()) return ClusterHealth()
        val obj = JSONObject(jsonStr)
        return ClusterHealth(
            livez = obj.optBoolean("livez", false),
            readyz = obj.optBoolean("readyz", false),
            healthz = obj.optBoolean("healthz", false),
            serverVersion = obj.optString("serverVersion", ""),
            statusMessage = obj.optString("statusMessage", ""),
        )
    }

    private fun JSONArray.toStringList(): List<String> {
        val result = ArrayList<String>(length())
        for (i in 0 until length()) {
            result.add(getString(i))
        }
        return result
    }
}
