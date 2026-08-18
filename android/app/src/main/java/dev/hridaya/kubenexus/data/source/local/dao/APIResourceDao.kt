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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAPIResources(resources: List<APIResourceEntity>)

    @Query("DELETE FROM api_resources WHERE clusterId = :clusterId")
    suspend fun deleteAPIResourcesForCluster(clusterId: String)

    @Transaction
    suspend fun syncAPIResources(
        clusterId: String,
        resources: List<APIResourceEntity>,
        timestamp: Long,
    ) {
        deleteAPIResourcesForCluster(clusterId)
        if (resources.isNotEmpty()) {
            insertAPIResources(resources)
        }
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
