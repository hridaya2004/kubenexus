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

    @Query("SELECT id FROM pods WHERE clusterId = :clusterId")
    suspend fun getPodIdsForCluster(clusterId: String): List<String>

    @Query("SELECT id FROM pods WHERE clusterId = :clusterId AND namespace = :namespace")
    suspend fun getPodIdsForNamespace(clusterId: String, namespace: String): List<String>

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

    @Query("DELETE FROM pods WHERE id IN (:ids)")
    suspend fun deletePodsByIds(ids: List<String>)

    @Transaction
    suspend fun syncPods(
        clusterId: String,
        namespace: String?,
        pods: List<PodEntity>,
        timestamp: Long,
        chunkSize: Int = 250,
    ) {
        val isAll = namespace.isNullOrBlank() ||
                namespace == "All Namespaces" ||
                namespace.equals("all", ignoreCase = true)

        val existingIds = if (isAll) {
            getPodIdsForCluster(clusterId)
        } else {
            getPodIdsForNamespace(clusterId, namespace!!)
        }

        val incomingIds = pods.map { it.id }.toSet()
        val idsToDelete = existingIds.filter { it !in incomingIds }

        // Delete removed pods in chunks
        if (idsToDelete.isNotEmpty()) {
            idsToDelete.chunked(chunkSize).forEach { chunk ->
                deletePodsByIds(chunk)
            }
        }

        // Upsert new or updated pods in chunks
        if (pods.isNotEmpty()) {
            pods.chunked(chunkSize).forEach { chunk ->
                insertPods(chunk)
            }
        }

        // Record sync metadata timestamp
        insertSyncMetadata(
            SyncMetadataEntity(
                key = "${clusterId}_pods",
                clusterId = clusterId,
                resourceType = "pods",
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
