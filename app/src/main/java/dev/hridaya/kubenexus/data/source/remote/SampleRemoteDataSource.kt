package dev.hridaya.kubenexus.data.source.remote

import dev.hridaya.kubenexus.data.dto.SampleItemDto
import kotlinx.coroutines.delay
import java.util.UUID

interface SampleRemoteDataSource {
    suspend fun fetchItems(): List<SampleItemDto>
    suspend fun createItem(title: String, description: String): SampleItemDto
    suspend fun deleteItem(id: String)
}

class SimulatedSampleRemoteDataSource : SampleRemoteDataSource {

    private val remoteItems = mutableListOf(
        SampleItemDto(
            id = "item-1",
            title = "Clean Architecture Foundation",
            description = "Separation of concerns between Domain, Data, and Presentation layers.",
            timestamp = System.currentTimeMillis() - 3600000
        ),
        SampleItemDto(
            id = "item-2",
            title = "Jetpack Compose UI",
            description = "Declarative, reactive UI built entirely with modern Android Jetpack Compose.",
            timestamp = System.currentTimeMillis() - 1800000
        ),
        SampleItemDto(
            id = "item-3",
            title = "Unidirectional Data Flow",
            description = "UiState, UiAction, and UiEffect driven state management.",
            timestamp = System.currentTimeMillis()
        )
    )

    override suspend fun fetchItems(): List<SampleItemDto> {
        delay(300)
        return remoteItems.toList()
    }

    override suspend fun createItem(title: String, description: String): SampleItemDto {
        delay(400)
        val newItem = SampleItemDto(
            id = "item-${UUID.randomUUID().toString().take(6)}",
            title = title,
            description = description,
            timestamp = System.currentTimeMillis()
        )
        remoteItems.add(newItem)
        return newItem
    }

    override suspend fun deleteItem(id: String) {
        delay(200)
        remoteItems.removeAll { it.id == id }
    }
}
