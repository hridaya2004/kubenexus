package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.SampleItem
import dev.hridaya.kubenexus.domain.repository.SampleRepository
import kotlinx.coroutines.withContext

class AddSampleItemUseCase(
    private val repository: SampleRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(title: String, description: String): Result<SampleItem> {
        val trimmedTitle = title.trim()
        val trimmedDesc = description.trim()

        if (trimmedTitle.isBlank()) {
            return Result.Error(AppError.Validation("Title cannot be blank."))
        }

        return withContext(dispatcherProvider.io) {
            repository.addSampleItem(trimmedTitle, trimmedDesc)
        }
    }
}
