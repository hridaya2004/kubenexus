package dev.hridaya.kubenexus.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.hridaya.kubenexus.data.source.local.entity.OpenApiSchemaEntity

@Dao
interface OpenApiSchemaDao {

    @Query("SELECT * FROM open_api_schemas WHERE clusterId = :clusterId LIMIT 1")
    suspend fun getForCluster(clusterId: String): OpenApiSchemaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(schema: OpenApiSchemaEntity)

    @Query("DELETE FROM open_api_schemas WHERE clusterId = :clusterId")
    suspend fun deleteForCluster(clusterId: String)
}
