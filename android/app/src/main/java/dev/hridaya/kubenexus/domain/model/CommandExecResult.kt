package dev.hridaya.kubenexus.domain.model

data class CommandExecResult(val stdout: String = "", val stderr: String = "")

interface TerminalSession {
    fun write(input: String)
    fun writeBytes(bytes: ByteArray)
    fun close()
}
