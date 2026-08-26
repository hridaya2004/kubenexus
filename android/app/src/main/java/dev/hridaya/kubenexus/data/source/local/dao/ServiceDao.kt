package dev.hridaya.kubenexus.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.hridaya.kubenexus.data.source.local.entity.ServiceEntity
import dev.hridaya.kubenexus.data.source.local.entity.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceDao {

    @Query("SELECT * FROM services WHERE clusterId = :clusterId ORDER BY namespace ASC, name ASC")
    fun getServicesStream(clusterId: String): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services WHERE clusterId = :clusterId AND namespace = :namespace ORDER BY name ASC")
    fun getServicesByNamespaceStream(clusterId: String, namespace: String): Flow<List<ServiceEntity>>

    @Query("SELECT id FROM services WHERE clusterId = :clusterId")
    suspend fun getServiceIdsForCluster(clusterId: String): List<String>

    @Query("SELECT id FROM services WHERE clusterId = :clusterId AND namespace = :namespace")
    suspend fun getServiceIdsForNamespace(clusterId: String, namespace: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServices(services: List<ServiceEntity>)

    @Query("DELETE FROM services WHERE id IN (:ids)")
    suspend fun deleteServicesByIds(ids: List<String>)

    /**
     * Replaces the cached set for [namespace] (or the whole cluster when null)
     * in one transaction: rows the live list no longer contains are deleted,
     * everything else upserted, and the sync timestamp recorded — all or
     * nothing, so an interrupted sync cannot leave a half-stale cache.
     */
    @Transaction
    suspend fun syncServices(
        clusterId: String,
        namespace: String?,
        services: List<ServiceEntity>,
        timestamp: Long,
        chunkSize: Int = 250,
    ) {
        val isAll = namespace.isNullOrBlank() ||
                namespace == "All Namespaces" ||
                namespace.equals("all", ignoreCase = true)

        val existingIds = if (isAll) {
            getServiceIdsForCluster(clusterId)
        } else {
            getServiceIdsForNamespace(clusterId, namespace!!)
        }

        val incomingIds = services.map { it.id }.toSet()
        val idsToDelete = existingIds.filter { it !in incomingIds }

        // Delete removed services in chunks
        if (idsToDelete.isNotEmpty()) {
            idsToDelete.chunked(chunkSize).forEach { chunk ->
                deleteServicesByIds(chunk)
            }
        }

        // Upsert new or updated services in chunks
        if (services.isNotEmpty()) {
            services.chunked(chunkSize).forEach { chunk ->
                insertServices(chunk)
            }
        }

        // Record sync metadata timestamp
        insertSyncMetadata(
            SyncMetadataEntity(
                key = "${clusterId}_services",
                clusterId = clusterId,
                resourceType = "services",
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
