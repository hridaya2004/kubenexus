package dev.hridaya.kubenexus.data.source.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "api_resources",
    indices = [
        Index(value = ["clusterId"]),
        Index(value = ["clusterId", "name"]),
        Index(value = ["clusterId", "groupVersion", "name"]),
    ],
)
data class APIResourceEntity(
    @PrimaryKey
    val id: String,
    val clusterId: String,
    val name: String,
    val singularName: String,
    val namespaced: Boolean,
    val kind: String,
    val group: String,
    val version: String,
    val groupVersion: String,
    val verbs: String,
    val shortNames: String,
    val categories: String,
    val lastUpdated: Long = System.currentTimeMillis(),
)
