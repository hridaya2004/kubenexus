package dev.hridaya.kubenexus.data.source.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * OpenAPI v2 schema document for a cluster, gzip-compressed. The document is
 * multi-megabyte, so it is fetched once and reused offline instead of being
 * held in memory.
 */
@Entity(tableName = "open_api_schemas")
data class OpenApiSchemaEntity(
    @PrimaryKey
    val clusterId: String,
    val schemaGzip: ByteArray,
    val fetchedAt: Long,
)
