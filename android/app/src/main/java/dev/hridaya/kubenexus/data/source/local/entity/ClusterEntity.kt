package dev.hridaya.kubenexus.data.source.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clusters")
data class ClusterEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val serverUrl: String,
    val rawKubeconfig: String,
    val contextName: String,
    val userName: String,
    val namespace: String,
    val isActive: Boolean,
    val createdAt: Long,
    val lastConnectedAt: Long?,
    val status: String,
)
