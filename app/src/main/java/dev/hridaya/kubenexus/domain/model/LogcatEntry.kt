package dev.hridaya.kubenexus.domain.model

enum class LogLevel(val code: String, val label: String, val priority: Int) {
    VERBOSE("V", "Verbose", 2),
    DEBUG("D", "Debug", 3),
    INFO("I", "Info", 4),
    WARN("W", "Warn", 5),
    ERROR("E", "Error", 6),
    FATAL("F", "Fatal", 7),
    UNKNOWN("?", "Unknown", 0);

    companion object {
        fun fromCode(code: String): LogLevel = when (code.trim().uppercase()) {
            "V" -> VERBOSE
            "D" -> DEBUG
            "I" -> INFO
            "W" -> WARN
            "E" -> ERROR
            "F", "A" -> FATAL
            else -> UNKNOWN
        }
    }
}

data class LogcatEntry(
    val id: Long,
    val timestamp: String,
    val pid: String,
    val tid: String,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val raw: String
)
