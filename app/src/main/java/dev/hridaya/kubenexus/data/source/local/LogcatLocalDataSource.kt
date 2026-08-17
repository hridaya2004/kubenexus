package dev.hridaya.kubenexus.data.source.local

import android.os.Process
import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.domain.model.LogLevel
import dev.hridaya.kubenexus.domain.model.LogcatEntry
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Pattern

interface LogcatLocalDataSource {
    fun streamLogs(maxBufferSize: Int): Flow<List<LogcatEntry>>
    suspend fun dumpLogs(maxLines: Int): List<LogcatEntry>
    suspend fun clearLogs()
}

class DefaultLogcatLocalDataSource(
    private val dispatcherProvider: DispatcherProvider
) : LogcatLocalDataSource {

    private val entryIdSequence = AtomicLong(1L)
    private val threadTimePattern = Pattern.compile(
        """^(\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d{3})\s+(\d+)\s+(\d+)\s+([VDIWEFA])\s+([^:]+):\s*(.*)$"""
    )

    override fun streamLogs(maxBufferSize: Int): Flow<List<LogcatEntry>> = callbackFlow {
        val pid = Process.myPid().toString()
        val buffer = ArrayDeque<LogcatEntry>(maxBufferSize)

        val processBuilder = try {
            ProcessBuilder("logcat", "-v", "threadtime", "--pid=$pid")
        } catch (_: Exception) {
            ProcessBuilder("logcat", "-v", "threadtime")
        }

        var process: java.lang.Process? = null
        try {
            process = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            var lastEmitTime = System.currentTimeMillis()
            var pendingChanges = false

            while (isActive) {
                val line = reader.readLine() ?: break
                val entry = parseLogLine(line)

                if (entry.pid.isNotEmpty() && entry.pid != pid) {
                    continue
                }
                if (entry.level == LogLevel.UNKNOWN && entry.tag == "System" && !entry.message.contains(pid)) {
                    continue
                }

                if (buffer.size >= maxBufferSize) {
                    buffer.removeFirst()
                }
                buffer.addLast(entry)
                pendingChanges = true

                val now = System.currentTimeMillis()
                if (now - lastEmitTime >= 100L || buffer.size < 10) {
                    trySend(buffer.toList())
                    lastEmitTime = now
                    pendingChanges = false
                }
            }

            if (pendingChanges) {
                trySend(buffer.toList())
            }
        } catch (e: Exception) {
            if (isActive) {
                trySend(buffer.toList())
            }
        } finally {
            process?.destroy()
        }

        awaitClose {
            process?.destroy()
        }
    }.flowOn(dispatcherProvider.io)

    override suspend fun dumpLogs(maxLines: Int): List<LogcatEntry> = withContext(dispatcherProvider.io) {
        val pid = Process.myPid().toString()
        val entries = mutableListOf<LogcatEntry>()

        try {
            val process = ProcessBuilder("logcat", "-d", "-v", "threadtime", "-t", maxLines.toString(), "--pid=$pid").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let {
                    val entry = parseLogLine(it)
                    if (entry.pid.isEmpty() || entry.pid == pid) {
                        entries.add(entry)
                    }
                }
            }
            process.waitFor()
            process.destroy()
        } catch (_: Exception) {
            try {
                val process = ProcessBuilder("logcat", "-d", "-v", "threadtime", "-t", maxLines.toString()).start()
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let {
                        val entry = parseLogLine(it)
                        if (entry.pid == pid) {
                            entries.add(entry)
                        }
                    }
                }
                process.waitFor()
                process.destroy()
            } catch (_: Exception) {}
        }

        entries
    }

    override suspend fun clearLogs(): Unit = withContext(dispatcherProvider.io) {
        try {
            val process = ProcessBuilder("logcat", "-c").start()
            process.waitFor()
            process.destroy()
        } catch (_: Exception) {}
    }

    private fun parseLogLine(line: String): LogcatEntry {
        val matcher = threadTimePattern.matcher(line)
        return if (matcher.matches()) {
            val timestamp = matcher.group(1).orEmpty()
            val pid = matcher.group(2).orEmpty()
            val tid = matcher.group(3).orEmpty()
            val levelCode = matcher.group(4).orEmpty()
            val tag = matcher.group(5).orEmpty().trim()
            val message = matcher.group(6).orEmpty()
            LogcatEntry(
                id = entryIdSequence.getAndIncrement(),
                timestamp = timestamp,
                pid = pid,
                tid = tid,
                level = LogLevel.fromCode(levelCode),
                tag = tag,
                message = message,
                raw = line
            )
        } else {
            LogcatEntry(
                id = entryIdSequence.getAndIncrement(),
                timestamp = "",
                pid = "",
                tid = "",
                level = LogLevel.UNKNOWN,
                tag = "System",
                message = line,
                raw = line
            )
        }
    }
}
