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
    /**
     * Pod creation time in epoch milliseconds, replacing the pre-rendered `age`
     * string that used to be formatted in Go. A cached string froze at sync time
     * and drifted until the next refresh; age is now derived on read.
     *
     * Null when the API server omitted a creation timestamp.
     */
    val creationTimestampMillis: Long?,
    val ip: String?,
    val node: String?,
    val image: String?,
    val lastUpdated: Long = System.currentTimeMillis(),
)
