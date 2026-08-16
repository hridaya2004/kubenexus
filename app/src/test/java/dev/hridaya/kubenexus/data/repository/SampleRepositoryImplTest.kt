package dev.hridaya.kubenexus.data.repository

import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.data.dto.SampleItemDto
import dev.hridaya.kubenexus.data.source.local.InMemorySampleLocalDataSource
import dev.hridaya.kubenexus.data.source.remote.SampleRemoteDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SampleRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private lateinit var localDataSource: InMemorySampleLocalDataSource
    private lateinit var fakeRemoteDataSource: FakeRemoteDataSource
    private lateinit var repository: SampleRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = InMemorySampleLocalDataSource()
        fakeRemoteDataSource = FakeRemoteDataSource()
        repository = SampleRepositoryImpl(
            localDataSource = localDataSource,
            remoteDataSource = fakeRemoteDataSource,
            dispatcherProvider = testDispatcherProvider
        )
    }

    @Test
    fun `refreshSampleItems fetches from remote and caches locally`() = runTest(testDispatcher) {
        val result = repository.refreshSampleItems()
        assertTrue(result is Result.Success)

        val items = repository.getSampleItemsStream().first()
        assertEquals(2, items.size)
        assertEquals("remote-1", items[0].id)
    }

    @Test
    fun `addSampleItem saves to remote and updates local cache`() = runTest(testDispatcher) {
        val result = repository.addSampleItem("New Title", "New Desc")
        assertTrue(result is Result.Success)

        val items = repository.getSampleItemsStream().first()
        assertEquals(1, items.size)
        assertEquals("New Title", items[0].title)
    }

    private class FakeRemoteDataSource : SampleRemoteDataSource {
        private val list = mutableListOf(
            SampleItemDto("remote-1", "Remote 1", "Desc 1", 100L),
            SampleItemDto("remote-2", "Remote 2", "Desc 2", 200L)
        )

        override suspend fun fetchItems(): List<SampleItemDto> = list.toList()

        override suspend fun createItem(title: String, description: String): SampleItemDto {
            val dto = SampleItemDto("new-id", title, description, 300L)
            list.add(dto)
            return dto
        }

        override suspend fun deleteItem(id: String) {
            list.removeAll { it.id == id }
        }
    }
}
