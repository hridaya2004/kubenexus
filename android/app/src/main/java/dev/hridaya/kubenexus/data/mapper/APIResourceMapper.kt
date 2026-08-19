package dev.hridaya.kubenexus.data.mapper

import dev.hridaya.kubenexus.data.source.local.entity.APIResourceEntity
import dev.hridaya.kubenexus.data.source.local.entity.ExplainedResourceEntity
import dev.hridaya.kubenexus.domain.model.APIResource
import dev.hridaya.kubenexus.domain.model.ResourceExplain
import dev.hridaya.kubenexus.domain.model.ResourceField
import org.json.JSONArray
import org.json.JSONObject

fun APIResourceEntity.toDomain(): APIResource {
    return APIResource(
        name = name,
        singularName = singularName,
        namespaced = namespaced,
        kind = kind,
        group = group,
        version = version,
        groupVersion = groupVersion,
        verbs = if (verbs.isBlank()) emptyList() else verbs.split(",").map { it.trim() }
            .filter { it.isNotEmpty() },
        shortNames = if (shortNames.isBlank()) emptyList() else shortNames.split(",")
            .map { it.trim() }.filter { it.isNotEmpty() },
        categories = if (categories.isBlank()) emptyList() else categories.split(",")
            .map { it.trim() }.filter { it.isNotEmpty() },
    )
}

fun APIResource.toEntity(clusterId: String): APIResourceEntity {
    return APIResourceEntity(
        id = "${clusterId}_${groupVersion}_${name}",
        clusterId = clusterId,
        name = name,
        singularName = singularName,
        namespaced = namespaced,
        kind = kind,
        group = group,
        version = version,
        groupVersion = groupVersion,
        verbs = verbs.joinToString(","),
        shortNames = shortNames.joinToString(","),
        categories = categories.joinToString(","),
    )
}

fun ExplainedResourceEntity.toDomain(): ResourceExplain {
    return ResourceExplain(
        kind = kind,
        group = group,
        version = version,
        groupVersion = groupVersion,
        description = description,
        fields = deserializeFields(fieldsJson),
        lastUpdated = lastUpdated,
    )
}

fun ResourceExplain.toEntity(clusterId: String, resourceOrKind: String): ExplainedResourceEntity {
    val normalizedResource = resourceOrKind.trim().lowercase()
    return ExplainedResourceEntity(
        id = "${clusterId}_${normalizedResource}_${groupVersion}",
        clusterId = clusterId,
        resourceOrKind = normalizedResource,
        kind = kind,
        group = group,
        version = version,
        groupVersion = groupVersion,
        description = description,
        fieldsJson = serializeFields(fields),
        lastUpdated = lastUpdated,
    )
}

private fun serializeFields(fields: List<ResourceField>): String {
    val array = JSONArray()
    for (field in fields) {
        val obj = JSONObject()
        obj.put("name", field.name)
        obj.put("type", field.type)
        obj.put("description", field.description)
        obj.put("required", field.required)
        array.put(obj)
    }
    return array.toString()
}

private fun deserializeFields(json: String): List<ResourceField> {
    if (json.isBlank()) return emptyList()
    return try {
        val array = JSONArray(json)
        val list = ArrayList<ResourceField>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                ResourceField(
                    name = obj.optString("name", ""),
                    type = obj.optString("type", ""),
                    description = obj.optString("description", ""),
                    required = obj.optBoolean("required", false),
                ),
            )
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}
