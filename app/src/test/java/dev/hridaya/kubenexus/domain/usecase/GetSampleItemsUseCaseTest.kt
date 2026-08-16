package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.SampleItem
import dev.hridaya.kubenexus.domain.repository.SampleRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetSampleItemsUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private val sampleList = listOf(
        SampleItem("1", "Title 1", "Desc 1", 100L),
        SampleItem("2", "Title 2", "Desc 2", 200L)
    )

    private lateinit var fakeRepository: FakeSampleRepository
    private lateinit var useCase: GetSampleItemsUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeSampleRepository(sampleList)
        useCase = GetSampleItemsUseCase(fakeRepository, testDispatcherProvider)
    }

    @Test
    fun `invoke returns success with items stream`() = runTest(testDispatcher) {
        val result = useCase().first()
        assertTrue(result is Result.Success)
        assertEquals(2, (result as Result.Success).data.size)
        assertEquals("Title 1", result.data[0].title)
    }

    private class FakeSampleRepository(private val items: List<SampleItem>) : SampleRepository {
        override fun getSampleItemsStream(): Flow<List<SampleItem>> = flowOf(items)
        override suspend fun getSampleItemById(id: String): Result<SampleItem> = Result.Success(items.first())
        override suspend fun addSampleItem(title: String, description: String): Result<SampleItem> = Result.Success(items.first())
        override suspend fun refreshSampleItems(): Result<Unit> = Result.Success(Unit)
        override suspend fun deleteSampleItem(id: String): Result<Unit> = Result.Success(Unit)
    }
}
