package dev.hridaya.kubenexus.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.hridaya.kubenexus.data.source.local.entity.APIResourceEntity
import dev.hridaya.kubenexus.data.source.local.entity.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface APIResourceDao {

    @Query("SELECT * FROM api_resources WHERE clusterId = :clusterId ORDER BY name ASC")
    fun getAPIResourcesStream(clusterId: String): Flow<List<APIResourceEntity>>

    @Query("SELECT * FROM api_resources WHERE clusterId = :clusterId ORDER BY name ASC")
    suspend fun getAPIResourcesList(clusterId: String): List<APIResourceEntity>

    @Query("SELECT id FROM api_resources WHERE clusterId = :clusterId")
    suspend fun getResourceIdsForCluster(clusterId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAPIResources(resources: List<APIResourceEntity>)

    @Query("DELETE FROM api_resources WHERE clusterId = :clusterId")
    suspend fun deleteAPIResourcesForCluster(clusterId: String)

    @Query("DELETE FROM api_resources WHERE id IN (:ids)")
    suspend fun deleteAPIResourcesByIds(ids: List<String>)

    @Transaction
    suspend fun syncAPIResources(
        clusterId: String,
        resources: List<APIResourceEntity>,
        timestamp: Long,
        chunkSize: Int = 250,
    ) {
        val existingIds = getResourceIdsForCluster(clusterId)
        val incomingIds = resources.map { it.id }.toSet()
        val idsToDelete = existingIds.filter { it !in incomingIds }

        // Delete removed API resources in chunks
        if (idsToDelete.isNotEmpty()) {
            idsToDelete.chunked(chunkSize).forEach { chunk ->
                deleteAPIResourcesByIds(chunk)
            }
        }

        // Upsert new or updated API resources in chunks
        if (resources.isNotEmpty()) {
            resources.chunked(chunkSize).forEach { chunk ->
                insertAPIResources(chunk)
            }
        }

        // Record sync metadata timestamp
        insertSyncMetadata(
            SyncMetadataEntity(
                key = "${clusterId}_api_resources",
                clusterId = clusterId,
                resourceType = "api_resources",
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
