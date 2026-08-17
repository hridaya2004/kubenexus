package dev.hridaya.kubenexus.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.hridaya.kubenexus.data.source.local.entity.PodEntity
import dev.hridaya.kubenexus.data.source.local.entity.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PodDao {

    @Query("SELECT * FROM pods WHERE clusterId = :clusterId ORDER BY namespace ASC, name ASC")
    fun getPodsStream(clusterId: String): Flow<List<PodEntity>>

    @Query("SELECT * FROM pods WHERE clusterId = :clusterId AND namespace = :namespace ORDER BY name ASC")
    fun getPodsByNamespaceStream(clusterId: String, namespace: String): Flow<List<PodEntity>>

    @Query("SELECT * FROM pods WHERE clusterId = :clusterId")
    suspend fun getPodsList(clusterId: String): List<PodEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPods(pods: List<PodEntity>)

    @Query("DELETE FROM pods WHERE clusterId = :clusterId")
    suspend fun deletePodsForCluster(clusterId: String)

    @Query("DELETE FROM pods WHERE clusterId = :clusterId AND namespace = :namespace")
    suspend fun deletePodsForNamespace(clusterId: String, namespace: String)

    @Query("DELETE FROM pods WHERE id = :podId")
    suspend fun deletePod(podId: String)

    @Transaction
    suspend fun syncPods(
        clusterId: String,
        namespace: String?,
        pods: List<PodEntity>,
        timestamp: Long
    ) {
        if (namespace.isNullOrBlank() || namespace == "All Namespaces" || namespace.equals(
                "all",
                ignoreCase = true
            )
        ) {
            deletePodsForCluster(clusterId)
        } else {
            deletePodsForNamespace(clusterId, namespace)
        }
        if (pods.isNotEmpty()) {
            insertPods(pods)
        }
        insertSyncMetadata(
            SyncMetadataEntity(
                key = "${clusterId}_pods",
                clusterId = clusterId,
                resourceType = "pods",
                lastRefreshedAt = timestamp
            )
        )
    }

    @Query("SELECT lastRefreshedAt FROM sync_metadata WHERE `key` = :key LIMIT 1")
    fun getSyncMetadataStream(key: String): Flow<Long?>

    @Query("SELECT lastRefreshedAt FROM sync_metadata WHERE `key` = :key LIMIT 1")
    suspend fun getLastRefreshedTime(key: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncMetadata(metadata: SyncMetadataEntity)
}
