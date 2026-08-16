package dev.hridaya.kubenexus.data.source.local

import dev.hridaya.kubenexus.data.dto.SampleItemDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface SampleLocalDataSource {
    fun getItemsStream(): Flow<List<SampleItemDto>>
    suspend fun getItemById(id: String): SampleItemDto?
    suspend fun saveItem(item: SampleItemDto)
    suspend fun saveAll(items: List<SampleItemDto>)
    suspend fun deleteItem(id: String)
}

class InMemorySampleLocalDataSource(
    initialItems: List<SampleItemDto> = emptyList()
) : SampleLocalDataSource {

    private val _itemsFlow = MutableStateFlow(initialItems)

    override fun getItemsStream(): Flow<List<SampleItemDto>> = _itemsFlow.asStateFlow()

    override suspend fun getItemById(id: String): SampleItemDto? {
        return _itemsFlow.value.find { it.id == id }
    }

    override suspend fun saveItem(item: SampleItemDto) {
        _itemsFlow.update { currentList ->
            val index = currentList.indexOfFirst { it.id == item.id }
            if (index >= 0) {
                currentList.toMutableList().apply { set(index, item) }
            } else {
                currentList + item
            }
        }
    }

    override suspend fun saveAll(items: List<SampleItemDto>) {
        _itemsFlow.value = items
    }

    override suspend fun deleteItem(id: String) {
        _itemsFlow.update { currentList ->
            currentList.filterNot { it.id == id }
        }
    }
}
