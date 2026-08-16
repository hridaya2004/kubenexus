package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.SampleItem
import dev.hridaya.kubenexus.domain.repository.SampleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class GetSampleItemsUseCase(
    private val repository: SampleRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    operator fun invoke(): Flow<Result<List<SampleItem>>> {
        return repository.getSampleItemsStream()
            .map { items ->
                Result.Success(items) as Result<List<SampleItem>>
            }
            .flowOn(dispatcherProvider.default)
    }
}
