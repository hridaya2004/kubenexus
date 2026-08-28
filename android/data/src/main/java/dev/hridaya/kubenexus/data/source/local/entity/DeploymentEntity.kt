package dev.hridaya.kubenexus.data.source.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cached apps/v1 Deployment row for offline-first workload lists.
 *
 * Mirrors [PodEntity]: a clusterId-qualified primary key so two clusters can
 * cache same-named Deployments without colliding, composite indices matching
 * the two list queries (all namespaces / one namespace), and multi-valued data
 * kept in flat TEXT columns rather than child tables or type converters —
 * images are comma-joined because commas cannot occur inside an image
 * reference.
 */
@Entity(
    tableName = "deployments",
    indices = [
        Index(value = ["clusterId", "namespace"]),
        Index(value = ["clusterId", "name"]),
    ],
)
data class DeploymentEntity(
    @PrimaryKey
    val id: String,
    val clusterId: String,
    val name: String,
    val namespace: String,
    val desiredReplicas: Int,
    val readyReplicas: Int,
    val availableReplicas: Int,
    val updatedReplicas: Int,
    /**
     * Creation time in epoch milliseconds rather than a pre-rendered age
     * string, so cached rows do not freeze their age at sync time.
     * Null when the API server omitted the timestamp.
     */
    val creationTimestampMillis: Long?,
    /** Comma-joined container image references; empty when none resolved. */
    val images: String,
    val lastUpdated: Long = System.currentTimeMillis(),
)
