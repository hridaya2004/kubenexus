package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.SampleItem
import dev.hridaya.kubenexus.domain.repository.SampleRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddSampleItemUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private lateinit var fakeRepository: FakeAddRepository
    private lateinit var useCase: AddSampleItemUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeAddRepository()
        useCase = AddSampleItemUseCase(fakeRepository, testDispatcherProvider)
    }

    @Test
    fun `invoke with blank title returns validation error`() = runTest(testDispatcher) {
        val result = useCase("   ", "Some description")
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.Validation)
        assertEquals("Title cannot be blank.", error.message)
    }

    @Test
    fun `invoke with valid title returns created item`() = runTest(testDispatcher) {
        val result = useCase("Valid Title", "Valid description")
        assertTrue(result is Result.Success)
        val item = (result as Result.Success).data
        assertEquals("Valid Title", item.title)
        assertEquals("Valid description", item.description)
    }

    private class FakeAddRepository : SampleRepository {
        override fun getSampleItemsStream(): Flow<List<SampleItem>> = emptyFlow()
        override suspend fun getSampleItemById(id: String): Result<SampleItem> = Result.Error(AppError.NotFound())
        override suspend fun addSampleItem(title: String, description: String): Result<SampleItem> {
            return Result.Success(
                SampleItem(
                    id = "new-id",
                    title = title,
                    description = description,
                    timestamp = 0L
                )
            )
        }
        override suspend fun refreshSampleItems(): Result<Unit> = Result.Success(Unit)
        override suspend fun deleteSampleItem(id: String): Result<Unit> = Result.Success(Unit)
    }
}
