package dev.hridaya.kubenexus.data.source.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pods",
    indices = [
        Index(value = ["clusterId", "namespace"]),
        Index(value = ["clusterId", "name"]),
    ],
)
data class PodEntity(
    @PrimaryKey
    val id: String,
    val clusterId: String,
    val name: String,
    val namespace: String,
    val status: String,
    val readyContainers: String,
    val restarts: Int,
    val age: String,
    val ip: String?,
    val node: String?,
    val image: String?,
    val lastUpdated: Long = System.currentTimeMillis(),
)
