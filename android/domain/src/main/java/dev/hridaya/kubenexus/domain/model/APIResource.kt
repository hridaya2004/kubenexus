package dev.hridaya.kubenexus.domain.model

data class APIResource(
    val name: String,
    val singularName: String = "",
    val namespaced: Boolean = true,
    val kind: String,
    val group: String = "",
    val version: String = "",
    val groupVersion: String = "",
    val verbs: List<String> = emptyList(),
    val shortNames: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
)

data class ResourceField(
    val name: String,
    val type: String,
    val description: String,
    val required: Boolean = false,
)

data class ResourceExplain(
    val kind: String,
    val group: String = "",
    val version: String = "",
    val groupVersion: String = "",
    val description: String,
    val fields: List<ResourceField> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis(),
)
