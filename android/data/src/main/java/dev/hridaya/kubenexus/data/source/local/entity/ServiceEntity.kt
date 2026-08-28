package dev.hridaya.kubenexus.data.source.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cached v1 Service row for offline-first service lists.
 *
 * Mirrors [PodEntity]: clusterId-qualified primary key, composite indices for
 * the all-namespaces and per-namespace queries, and structured port rows kept
 * in one flat TEXT column (`name|port|targetPort|nodePort|protocol`, joined by
 * commas) instead of child tables or type converters — see ServiceMapper.
 */
@Entity(
    tableName = "services",
    indices = [
        Index(value = ["clusterId", "namespace"]),
        Index(value = ["clusterId", "name"]),
    ],
)
data class ServiceEntity(
    @PrimaryKey
    val id: String,
    val clusterId: String,
    val name: String,
    val namespace: String,
    val type: String,
    /** Virtual cluster IP verbatim ("None" for headless); column is clusterIp per camelCase convention. */
    val clusterIp: String,
    val ports: String,
    /**
     * Creation time in epoch milliseconds rather than a pre-rendered age
     * string, so cached rows do not freeze their age at sync time.
     * Null when the API server omitted the timestamp.
     */
    val creationTimestampMillis: Long?,
    val lastUpdated: Long = System.currentTimeMillis(),
)
