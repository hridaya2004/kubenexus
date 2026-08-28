package dev.hridaya.kubenexus.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.hridaya.kubenexus.data.source.local.entity.DeploymentEntity
import dev.hridaya.kubenexus.data.source.local.entity.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeploymentDao {

    @Query("SELECT * FROM deployments WHERE clusterId = :clusterId ORDER BY namespace ASC, name ASC")
    fun getDeploymentsStream(clusterId: String): Flow<List<DeploymentEntity>>

    @Query("SELECT * FROM deployments WHERE clusterId = :clusterId AND namespace = :namespace ORDER BY name ASC")
    fun getDeploymentsByNamespaceStream(
        clusterId: String,
        namespace: String
    ): Flow<List<DeploymentEntity>>

    @Query("SELECT id FROM deployments WHERE clusterId = :clusterId")
    suspend fun getDeploymentIdsForCluster(clusterId: String): List<String>

    @Query("SELECT id FROM deployments WHERE clusterId = :clusterId AND namespace = :namespace")
    suspend fun getDeploymentIdsForNamespace(clusterId: String, namespace: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeployments(deployments: List<DeploymentEntity>)

    @Query("DELETE FROM deployments WHERE id IN (:ids)")
    suspend fun deleteDeploymentsByIds(ids: List<String>)

    /**
     * Replaces the cached set for [namespace] (or the whole cluster when null)
     * in one transaction: rows the live list no longer contains are deleted,
     * everything else upserted, and the sync timestamp recorded — all or
     * nothing, so an interrupted sync cannot leave a half-stale cache.
     */
    @Transaction
    suspend fun syncDeployments(
        clusterId: String,
        namespace: String?,
        deployments: List<DeploymentEntity>,
        timestamp: Long,
        chunkSize: Int = 250,
    ) {
        val isAll = namespace.isNullOrBlank() ||
                namespace == "All Namespaces" ||
                namespace.equals("all", ignoreCase = true)

        val existingIds = if (isAll) {
            getDeploymentIdsForCluster(clusterId)
        } else {
            getDeploymentIdsForNamespace(clusterId, namespace!!)
        }

        val incomingIds = deployments.map { it.id }.toSet()
        val idsToDelete = existingIds.filter { it !in incomingIds }

        // Delete removed deployments in chunks
        if (idsToDelete.isNotEmpty()) {
            idsToDelete.chunked(chunkSize).forEach { chunk ->
                deleteDeploymentsByIds(chunk)
            }
        }

        // Upsert new or updated deployments in chunks
        if (deployments.isNotEmpty()) {
            deployments.chunked(chunkSize).forEach { chunk ->
                insertDeployments(chunk)
            }
        }

        // Record sync metadata timestamp
        insertSyncMetadata(
            SyncMetadataEntity(
                key = "${clusterId}_deployments",
                clusterId = clusterId,
                resourceType = "deployments",
                lastRefreshedAt = timestamp,
            ),
        )
    }

    @Query("SELECT lastRefreshedAt FROM sync_metadata WHERE `key` = :key LIMIT 1")
    fun getSyncMetadataStream(key: String): Flow<Long?>

    @Query("SELECT lastRefreshedAt FROM sync_metadata WHERE `key` = :key LIMIT 1")
    suspend fun getLastRefreshedTime(key: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncMetadata(metadata: SyncMetadataEntity)
}
