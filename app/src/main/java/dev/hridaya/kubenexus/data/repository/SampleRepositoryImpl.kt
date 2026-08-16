package dev.hridaya.kubenexus.data.repository

import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.data.mapper.toDomain
import dev.hridaya.kubenexus.data.mapper.toDomainList
import dev.hridaya.kubenexus.data.source.local.SampleLocalDataSource
import dev.hridaya.kubenexus.data.source.remote.SampleRemoteDataSource
import dev.hridaya.kubenexus.domain.model.SampleItem
import dev.hridaya.kubenexus.domain.repository.SampleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SampleRepositoryImpl(
    private val localDataSource: SampleLocalDataSource,
    private val remoteDataSource: SampleRemoteDataSource,
    private val dispatcherProvider: DispatcherProvider
) : SampleRepository {

    override fun getSampleItemsStream(): Flow<List<SampleItem>> {
        return localDataSource.getItemsStream().map { it.toDomainList() }
    }

    override suspend fun getSampleItemById(id: String): Result<SampleItem> = withContext(dispatcherProvider.io) {
        try {
            val local = localDataSource.getItemById(id)
            if (local != null) {
                Result.Success(local.toDomain())
            } else {
                Result.Error(AppError.NotFound("Item with id '$id' not found."))
            }
        } catch (e: Exception) {
            Result.Error(AppError.Unknown("Failed to load item", e))
        }
    }

    override suspend fun addSampleItem(title: String, description: String): Result<SampleItem> = withContext(dispatcherProvider.io) {
        try {
            val createdDto = remoteDataSource.createItem(title, description)
            localDataSource.saveItem(createdDto)
            Result.Success(createdDto.toDomain())
        } catch (e: Exception) {
            Result.Error(AppError.Network("Failed to add item: ${e.message}"))
        }
    }

    override suspend fun refreshSampleItems(): Result<Unit> = withContext(dispatcherProvider.io) {
        try {
            val remoteItems = remoteDataSource.fetchItems()
            localDataSource.saveAll(remoteItems)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.Network("Failed to refresh items: ${e.message}"))
        }
    }

    override suspend fun deleteSampleItem(id: String): Result<Unit> = withContext(dispatcherProvider.io) {
        try {
            remoteDataSource.deleteItem(id)
            localDataSource.deleteItem(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.Network("Failed to delete item: ${e.message}"))
        }
    }
}
