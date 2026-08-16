package dev.hridaya.kubenexus.data.source.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    val key: String,
    val clusterId: String,
    val resourceType: String,
    val lastRefreshedAt: Long
)
