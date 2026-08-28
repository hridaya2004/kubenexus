package dev.hridaya.kubenexus.data.source.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * OpenAPI v2 schema document for a cluster, gzip-compressed. The document is
 * multi-megabyte, so it is fetched once and reused offline instead of being
 * held in memory.
 *
 * Compression is not just a size optimisation: Android reads query results
 * through a CursorWindow capped at 2 MB per row, and an uncompressed schema
 * already exceeds that on a stock cluster.
 */
@Entity(tableName = "open_api_schemas")
data class OpenApiSchemaEntity(
    @PrimaryKey
    val clusterId: String,
    val schemaGzip: ByteArray,
    val fetchedAt: Long,
) {
    // A generated data class equals/hashCode would compare the ByteArray by
    // reference, so two rows with identical content would not be equal. That
    // silently breaks any Set membership or distinctUntilChanged over this type.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OpenApiSchemaEntity) return false
        return clusterId == other.clusterId &&
                fetchedAt == other.fetchedAt &&
                schemaGzip.contentEquals(other.schemaGzip)
    }

    override fun hashCode(): Int {
        var result = clusterId.hashCode()
        result = 31 * result + schemaGzip.contentHashCode()
        result = 31 * result + fetchedAt.hashCode()
        return result
    }
}
