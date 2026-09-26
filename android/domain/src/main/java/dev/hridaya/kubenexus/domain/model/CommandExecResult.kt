package dev.hridaya.kubenexus.domain.model

data class CommandExecResult(
    val stdout: String = "",
    val stderr: String = "",
    val exitCode: Int = 0,
)

interface TerminalSession {
    fun write(input: String)
    fun writeBytes(bytes: ByteArray)

    /**
     * Tells the remote TTY its window size in character cells. Sessions without a TTY
     * ignore it.
     */
    fun resize(columns: Int, rows: Int) = Unit

    fun close()
}
