package dev.hridaya.kubenexus.presentation.pods.components.terminal

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import dev.hridaya.kubenexus.core.terminal.TerminalSnapshot
import kotlin.math.abs

/**
 * Long-press word selection, drag selection, drag-to-scroll and tap handling
 * for [TerminalCanvas]. Provider lambdas are re-read on every event so the
 * latest composition values are observed, matching rememberUpdatedState
 * semantics.
 */
internal fun Modifier.terminalGestures(
    cellWidth: Float,
    cellHeight: Float,
    touchSlopPx: Float,
    longPressTimeoutMs: Long,
    snapshot: () -> TerminalSnapshot?,
    selection: () -> TerminalSelection?,
    onSelectionChange: (TerminalSelection?) -> Unit,
    onTap: () -> Unit,
    onScroll: (delta: Int, x: Float, y: Float) -> Unit,
): Modifier = pointerInput(cellWidth, cellHeight, touchSlopPx, longPressTimeoutMs) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var lastEventUptime = down.uptimeMillis
        var accumulatedScrollY = 0f
        var isDrag = false
        var isSelecting = false
        var initialAnchor = -1

        val gestureStartSnapshot = snapshot()
        if (gestureStartSnapshot != null && cellWidth > 0f && cellHeight > 0f &&
            gestureStartSnapshot.cols > 0 && gestureStartSnapshot.rows > 0
        ) {
            val col =
                (down.position.x / cellWidth).toInt().coerceIn(0, gestureStartSnapshot.cols - 1)
            val row =
                (down.position.y / cellHeight).toInt().coerceIn(0, gestureStartSnapshot.rows - 1)
            initialAnchor =
                (row * gestureStartSnapshot.cols + col).coerceIn(
                    0,
                    (gestureStartSnapshot.cols * gestureStartSnapshot.rows) - 1
                )
        }

        fun beginSelection() {
            isSelecting = true
            val wordRange = snapshot()?.wordAt(initialAnchor)
            if (wordRange != null) {
                onSelectionChange(
                    TerminalSelection(
                        wordRange.first,
                        wordRange.last
                    )
                )
            } else {
                onSelectionChange(
                    TerminalSelection(
                        initialAnchor,
                        initialAnchor
                    )
                )
            }
        }

        while (true) {
            val awaitingLongPress = !isSelecting && !isDrag && initialAnchor >= 0
            val event = if (awaitingLongPress) {
                // A finger held still produces no events, so the long press is timed here rather
                // than noticed on the next move; otherwise holding and lifting counted as a tap.
                val remaining = longPressTimeoutMs - (lastEventUptime - down.uptimeMillis)
                withTimeoutOrNull(remaining.coerceAtLeast(0L)) { awaitPointerEvent() }
            } else {
                awaitPointerEvent()
            }
            if (event == null) {
                beginSelection()
                continue
            }
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            lastEventUptime = change.uptimeMillis

            if (change.pressed) {
                val elapsed = change.uptimeMillis - down.uptimeMillis
                val dragDistance = change.position - down.position

                if (!isSelecting && !isDrag && elapsed >= longPressTimeoutMs && initialAnchor >= 0) {
                    beginSelection()
                }

                if (isSelecting) {
                    val terminalSnapshot = snapshot()
                    if (terminalSnapshot != null && cellWidth > 0f && cellHeight > 0f &&
                        terminalSnapshot.cols > 0 && terminalSnapshot.rows > 0
                    ) {
                        val col = (change.position.x / cellWidth).toInt()
                            .coerceIn(0, terminalSnapshot.cols - 1)
                        val row = (change.position.y / cellHeight).toInt()
                            .coerceIn(0, terminalSnapshot.rows - 1)
                        val currentCell =
                            (row * terminalSnapshot.cols + col).coerceIn(
                                0,
                                (terminalSnapshot.cols * terminalSnapshot.rows) - 1
                            )
                        val currentAnchor =
                            selection()?.anchorIndex ?: initialAnchor
                        onSelectionChange(
                            TerminalSelection(
                                currentAnchor,
                                currentCell
                            )
                        )
                    }
                    change.consume()
                } else {
                    if (!isDrag && dragDistance.getDistance() > touchSlopPx) {
                        isDrag = true
                    }
                    if (isDrag) {
                        val previousPos = change.previousPosition
                        val deltaY = change.position.y - previousPos.y
                        accumulatedScrollY += deltaY

                        if (cellHeight > 0f && abs(accumulatedScrollY) >= cellHeight) {
                            val rowsToScroll = (accumulatedScrollY / cellHeight).toInt()
                            onScroll(
                                -rowsToScroll,
                                change.position.x,
                                change.position.y
                            )
                            accumulatedScrollY -= rowsToScroll * cellHeight
                        }
                        change.consume()
                    }
                }
            } else {
                if (!isDrag && !isSelecting) {
                    if (selection() != null) {
                        onSelectionChange(null)
                    } else {
                        onTap()
                    }
                }
                break
            }
        }
    }
}
