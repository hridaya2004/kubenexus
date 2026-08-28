package dev.hridaya.kubenexus.data.source.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "explained_resources",
    indices = [
        Index(value = ["clusterId"]),
        Index(value = ["clusterId", "resourceOrKind"]),
        Index(value = ["clusterId", "resourceOrKind", "groupVersion"]),
    ],
)
data class ExplainedResourceEntity(
    @PrimaryKey
    val id: String,
    val clusterId: String,
    val resourceOrKind: String,
    val kind: String,
    val group: String,
    val version: String,
    val groupVersion: String,
    val description: String,
    val fieldsJson: String,
    val lastUpdated: Long = System.currentTimeMillis(),
)
