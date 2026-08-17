package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.repository.LogcatRepository

class ClearLogcatUseCase(
    private val repository: LogcatRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.clearLogs()
    }
}
