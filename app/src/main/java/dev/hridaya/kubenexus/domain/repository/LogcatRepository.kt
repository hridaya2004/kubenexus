package dev.hridaya.kubenexus.domain.repository

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.LogcatEntry
import kotlinx.coroutines.flow.Flow

interface LogcatRepository {
    fun streamLogs(maxBufferSize: Int = 2000): Flow<List<LogcatEntry>>
    suspend fun dumpLogs(maxLines: Int = 1000): Result<List<LogcatEntry>>
    suspend fun clearLogs(): Result<Unit>
}
