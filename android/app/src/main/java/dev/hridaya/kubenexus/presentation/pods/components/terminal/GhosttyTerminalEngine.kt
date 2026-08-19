package dev.hridaya.kubenexus.presentation.pods.components.terminal

import dev.hridaya.kubenexus.core.terminal.GhosttyBridge
import dev.hridaya.kubenexus.core.terminal.GhosttyKeyAction
import dev.hridaya.kubenexus.core.terminal.KeyMapper
import dev.hridaya.kubenexus.core.terminal.TerminalSnapshot
import dev.hridaya.kubenexus.domain.model.TerminalSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer

class GhosttyTerminalEngine(
    private val maxScrollback: Int = 5000,
) {
    val bridge = GhosttyBridge()
    private var handle: Long = 0L

    private val _snapshot = MutableStateFlow<TerminalSnapshot?>(null)
    val snapshot: StateFlow<TerminalSnapshot?> = _snapshot.asStateFlow()

    private var terminalSession: TerminalSession? = null
    private var cols: Int = 80
    private var rows: Int = 24

    val isNativeLoaded: Boolean get() = bridge.isLoaded()
    val terminalHandle: Long get() = handle

    fun initialize(initialCols: Int = 80, initialRows: Int = 24) {
        if (!bridge.isLoaded()) return
        if (handle != 0L) {
            bridge.nativeDestroy(handle)
        }
        cols = initialCols.coerceAtLeast(10)
        rows = initialRows.coerceAtLeast(4)
        handle = bridge.nativeCreate(cols, rows, maxScrollback)
        updateSnapshot()
    }

    fun attachSession(session: TerminalSession) {
        this.terminalSession = session
    }

    fun detachSession() {
        this.terminalSession = null
    }

    fun feedRemoteOutput(data: ByteArray) {
        if (handle == 0L || data.isEmpty()) return
        bridge.nativeWriteRemote(handle, data)
        updateSnapshot()
    }

    fun feedRemoteOutput(text: String) {
        feedRemoteOutput(text.toByteArray(Charsets.UTF_8))
    }

    fun resize(newCols: Int, newRows: Int, cellW: Int, cellH: Int) {
        if (handle == 0L) return
        if (newCols <= 0 || newRows <= 0) return
        cols = newCols
        rows = newRows
        bridge.nativeResize(handle, cols, rows, cellW, cellH)
        updateSnapshot()
    }

    fun scroll(delta: Int, x: Float, y: Float) {
        if (handle == 0L) return
        bridge.nativeScroll(handle, delta, x, y)
        updateSnapshot()
    }

    fun scrollToActive() {
        if (handle == 0L) return
        bridge.nativeScrollToActive(handle)
        updateSnapshot()
    }

    fun sendText(text: String) {
        if (text.isEmpty()) return
        val session = terminalSession
        if (session != null) {
            session.write(text)
        }
    }

    fun sendKey(
        keyCode: Int,
        codepoint: Int = 0,
        metaState: Int = 0,
        action: Int = GhosttyKeyAction.Press
    ): Boolean {
        if (handle == 0L) return false
        val mapped = KeyMapper.map(keyCode, codepoint, metaState) ?: return false
        val utf8 = if (mapped.charCode != 0) String(Character.toChars(mapped.charCode)) else null
        val encoded =
            bridge.nativeEncodeKey(handle, mapped.key, mapped.codepoint, mapped.mods, action, utf8)
        if (encoded != null && encoded.isNotEmpty()) {
            terminalSession?.writeBytes(encoded)
            return true
        }
        return false
    }

    fun sendPaste(text: String) {
        if (handle == 0L || text.isEmpty()) return
        val encoded = bridge.nativeEncodePaste(handle, text)
        if (encoded != null && encoded.isNotEmpty()) {
            terminalSession?.writeBytes(encoded)
        } else {
            terminalSession?.write(text)
        }
    }

    fun updateSnapshot() {
        if (handle == 0L) return
        try {
            val buf: ByteBuffer = bridge.nativeSnapshot(handle)
            val snap = TerminalSnapshot.fromByteBuffer(buf)
            _snapshot.value = snap
        } catch (e: Exception) {
            // ignore
        }
    }

    fun destroy() {
        if (handle != 0L) {
            bridge.nativeDestroy(handle)
            handle = 0L
        }
        _snapshot.value = null
        terminalSession = null
    }
}
