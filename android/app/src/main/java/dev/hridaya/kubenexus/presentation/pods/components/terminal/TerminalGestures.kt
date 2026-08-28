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
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val downTime = System.currentTimeMillis()
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

        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break

            if (change.pressed) {
                val elapsed = System.currentTimeMillis() - downTime
                val dragDistance = change.position - down.position

                if (!isSelecting && !isDrag && elapsed >= longPressTimeoutMs && initialAnchor >= 0) {
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
