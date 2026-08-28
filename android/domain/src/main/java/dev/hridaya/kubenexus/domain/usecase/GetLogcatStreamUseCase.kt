package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.domain.model.LogcatEntry
import dev.hridaya.kubenexus.domain.repository.LogcatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLogcatStreamUseCase @Inject constructor(private val repository: LogcatRepository) {
    operator fun invoke(maxBufferSize: Int = 2000): Flow<List<LogcatEntry>> {
        return repository.streamLogs(maxBufferSize)
    }
}
