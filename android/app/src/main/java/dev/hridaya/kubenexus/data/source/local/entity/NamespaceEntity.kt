package dev.hridaya.kubenexus.data.source.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "namespaces",
    indices = [
        Index(value = ["clusterId", "name"]),
    ],
)
data class NamespaceEntity(
    @PrimaryKey
    val id: String,
    val clusterId: String,
    val name: String,
    val status: String = "Active",
    val lastUpdated: Long = System.currentTimeMillis(),
)
