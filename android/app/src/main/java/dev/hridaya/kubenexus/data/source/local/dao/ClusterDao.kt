package dev.hridaya.kubenexus.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.hridaya.kubenexus.data.source.local.entity.ClusterEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ClusterDao {

    @Query("SELECT * FROM clusters ORDER BY isActive DESC, createdAt DESC")
    abstract fun observeClusters(): Flow<List<ClusterEntity>>

    @Query("SELECT * FROM clusters WHERE isActive = 1 LIMIT 1")
    abstract fun observeActiveCluster(): Flow<ClusterEntity?>

    @Query("SELECT * FROM clusters WHERE id = :id LIMIT 1")
    abstract suspend fun getClusterById(id: String): ClusterEntity?

    @Query("SELECT * FROM clusters")
    abstract suspend fun getAllClusters(): List<ClusterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCluster(cluster: ClusterEntity)

    @Update
    abstract suspend fun updateCluster(cluster: ClusterEntity)

    @Query("DELETE FROM clusters WHERE id = :id")
    abstract suspend fun deleteCluster(id: String)

    @Query("UPDATE clusters SET isActive = 0")
    abstract suspend fun deactivateAllClusters()

    @Query("UPDATE clusters SET isActive = 1 WHERE id = :id")
    abstract suspend fun activateCluster(id: String)

    @Transaction
    open suspend fun setActiveCluster(id: String) {
        deactivateAllClusters()
        activateCluster(id)
    }

    @Query("UPDATE clusters SET name = :name WHERE id = :id")
    abstract suspend fun updateClusterName(id: String, name: String)

    @Query("UPDATE clusters SET status = :status, lastConnectedAt = :lastConnectedAt WHERE id = :id")
    abstract suspend fun updateStatus(id: String, status: String, lastConnectedAt: Long?)
}
