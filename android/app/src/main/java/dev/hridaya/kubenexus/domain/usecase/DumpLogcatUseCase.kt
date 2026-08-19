package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.LogcatEntry
import dev.hridaya.kubenexus.domain.repository.LogcatRepository
import javax.inject.Inject

class DumpLogcatUseCase @Inject constructor(private val repository: LogcatRepository) {
    suspend operator fun invoke(maxLines: Int = 1000): Result<List<LogcatEntry>> {
        return repository.dumpLogs(maxLines)
    }
}
