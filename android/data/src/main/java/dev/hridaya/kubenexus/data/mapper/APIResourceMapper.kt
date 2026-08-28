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
    val fieldsJsonArray = JSONArray()
    for (field in fields) {
        val fieldJsonObject = JSONObject()
        fieldJsonObject.put("name", field.name)
        fieldJsonObject.put("type", field.type)
        fieldJsonObject.put("description", field.description)
        fieldJsonObject.put("required", field.required)
        fieldsJsonArray.put(fieldJsonObject)
    }
    return fieldsJsonArray.toString()
}

private fun deserializeFields(json: String): List<ResourceField> {
    if (json.isBlank()) return emptyList()
    return try {
        val fieldsJsonArray = JSONArray(json)
        val fieldList = ArrayList<ResourceField>(fieldsJsonArray.length())
        for (index in 0 until fieldsJsonArray.length()) {
            val fieldJsonObject = fieldsJsonArray.getJSONObject(index)
            fieldList.add(
                ResourceField(
                    name = fieldJsonObject.optString("name", ""),
                    type = fieldJsonObject.optString("type", ""),
                    description = fieldJsonObject.optString("description", ""),
                    required = fieldJsonObject.optBoolean("required", false),
                ),
            )
        }
        fieldList
    } catch (_: Exception) {
        emptyList()
    }
}
