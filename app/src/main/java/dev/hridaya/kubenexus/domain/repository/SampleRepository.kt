package dev.hridaya.kubenexus.domain.repository

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.SampleItem
import kotlinx.coroutines.flow.Flow

interface SampleRepository {
    fun getSampleItemsStream(): Flow<List<SampleItem>>
    suspend fun getSampleItemById(id: String): Result<SampleItem>
    suspend fun addSampleItem(title: String, description: String): Result<SampleItem>
    suspend fun refreshSampleItems(): Result<Unit>
    suspend fun deleteSampleItem(id: String): Result<Unit>
}
