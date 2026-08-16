package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.repository.SampleRepository
import kotlinx.coroutines.withContext

class RefreshSampleItemsUseCase(
    private val repository: SampleRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(): Result<Unit> {
        return withContext(dispatcherProvider.io) {
            repository.refreshSampleItems()
        }
    }
}
