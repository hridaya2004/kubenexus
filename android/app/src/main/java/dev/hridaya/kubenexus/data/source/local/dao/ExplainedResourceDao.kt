package dev.hridaya.kubenexus.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.hridaya.kubenexus.data.source.local.entity.ExplainedResourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExplainedResourceDao {

    @Query("SELECT * FROM explained_resources WHERE clusterId = :clusterId AND (resourceOrKind = :resourceOrKind OR LOWER(kind) = :resourceOrKind) AND (groupVersion = :groupVersion OR :groupVersion = '' OR groupVersion = '') LIMIT 1")
    suspend fun getExplainedResource(clusterId: String, resourceOrKind: String, groupVersion: String): ExplainedResourceEntity?

    @Query("SELECT * FROM explained_resources WHERE clusterId = :clusterId AND (resourceOrKind = :resourceOrKind OR LOWER(kind) = :resourceOrKind) AND (groupVersion = :groupVersion OR :groupVersion = '' OR groupVersion = '') LIMIT 1")
    fun getExplainedResourceStream(clusterId: String, resourceOrKind: String, groupVersion: String): Flow<ExplainedResourceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExplainedResource(explained: ExplainedResourceEntity)

    @Query("DELETE FROM explained_resources WHERE clusterId = :clusterId")
    suspend fun deleteExplainedResourcesForCluster(clusterId: String)

    @Query("DELETE FROM explained_resources WHERE clusterId = :clusterId AND resourceOrKind NOT IN (:activeResources) AND LOWER(kind) NOT IN (:activeResources)")
    suspend fun deleteOrphanedExplainedResources(clusterId: String, activeResources: List<String>)
}
