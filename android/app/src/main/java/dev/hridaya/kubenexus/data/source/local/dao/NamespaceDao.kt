package dev.hridaya.kubenexus.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.hridaya.kubenexus.data.source.local.entity.NamespaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NamespaceDao {

    @Query("SELECT * FROM namespaces WHERE clusterId = :clusterId ORDER BY name ASC")
    fun getNamespacesStream(clusterId: String): Flow<List<NamespaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNamespaces(namespaces: List<NamespaceEntity>)

    @Query("DELETE FROM namespaces WHERE clusterId = :clusterId")
    suspend fun deleteNamespacesForCluster(clusterId: String)

    @Transaction
    suspend fun syncNamespaces(clusterId: String, namespaces: List<NamespaceEntity>) {
        deleteNamespacesForCluster(clusterId)
        if (namespaces.isNotEmpty()) {
            insertNamespaces(namespaces)
        }
    }
}
