package dev.hridaya.kubenexus.presentation.pods.components.terminal

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.ViewConfiguration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hridaya.kubenexus.core.terminal.GhosttyBridge
import dev.hridaya.kubenexus.core.terminal.TerminalSnapshot
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.round

@Composable
fun TerminalCanvas(
    snapshot: TerminalSnapshot?,
    modifier: Modifier = Modifier,
    fontSizeSp: Float = 13f,
    cursorColor: Color = Color.White.copy(alpha = 0.4f),
    selectionBackgroundColor: Color = Color(0x663B82F6),
    selection: TerminalSelection? = null,
    onSelectionChange: (TerminalSelection?) -> Unit = {},
    terminalHandle: Long = 0,
    onResize: (cols: Int, rows: Int, cellWidth: Int, cellHeight: Int, widthPx: Int, heightPx: Int) -> Unit =
        { _, _, _, _, _, _ -> },
    onTap: () -> Unit = {},
    onScroll: (delta: Int, x: Float, y: Float) -> Unit = { _, _, _ -> },
    onSelectionChanged: (TerminalSelectionState?) -> Unit = {},
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val fontSizePx = with(density) { fontSizeSp.sp.toPx() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var lastResizedGrid by remember { mutableStateOf(Pair(0, 0)) }
    val androidViewConfiguration = remember(context) { ViewConfiguration.get(context) }
    val touchSlopPx = remember(androidViewConfiguration) { androidViewConfiguration.scaledTouchSlop.toFloat() }

    val primaryTypeface = remember { Typeface.MONOSPACE }
    val boldTypeface = remember { Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) }
    val italicTypeface = remember { Typeface.create(Typeface.MONOSPACE, Typeface.ITALIC) }
    val boldItalicTypeface = remember { Typeface.create(Typeface.MONOSPACE, Typeface.BOLD_ITALIC) }

    val textPaint = remember(primaryTypeface, fontSizePx) {
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = primaryTypeface
            textSize = fontSizePx
        }
    }

    val cellWidth = remember(textPaint) {
        max(1f, textPaint.measureText("W"))
    }
    val cellHeight = remember(textPaint) {
        val metrics = textPaint.fontMetrics
        max(1f, ceil(metrics.descent - metrics.ascent + metrics.leading))
    }
    val fontBaseline = remember(textPaint) {
        val metrics = textPaint.fontMetrics
        -metrics.ascent
    }

    LaunchedEffect(canvasSize, cellWidth, cellHeight) {
        if (canvasSize.width > 0 && canvasSize.height > 0 && cellWidth > 0f && cellHeight > 0f) {
            val cols = max(1, floor(canvasSize.width / cellWidth).toInt())
            val rows = max(1, floor(canvasSize.height / cellHeight).toInt())
            if (lastResizedGrid != Pair(cols, rows)) {
                lastResizedGrid = Pair(cols, rows)
                onResize(cols, rows, round(cellWidth).toInt(), round(cellHeight).toInt(), canvasSize.width, canvasSize.height)
            }
        }
    }

    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnScroll by rememberUpdatedState(onScroll)
    val currentSnapshot by rememberUpdatedState(snapshot)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var accumulatedScrollY = 0f
                    var isDrag = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (change.pressed) {
                            val dragDistance = change.position - down.position
                            if (!isDrag && dragDistance.getDistance() > touchSlopPx) {
                                isDrag = true
                            }
                            if (isDrag) {
                                val previousPos = change.previousPosition
                                val deltaY = change.position.y - previousPos.y
                                accumulatedScrollY += deltaY

                                if (cellHeight > 0f && abs(accumulatedScrollY) >= cellHeight) {
                                    val rowsToScroll = (accumulatedScrollY / cellHeight).toInt()
                                    currentOnScroll(-rowsToScroll, change.position.x, change.position.y)
                                    accumulatedScrollY -= rowsToScroll * cellHeight
                                }
                                change.consume()
                            }
                        } else {
                            if (!isDrag) {
                                currentOnTap()
                            }
                            break
                        }
                    }
                }
            },
    ) {
        val snap = currentSnapshot ?: return@Canvas
        val totalCells = snap.cols * snap.rows
        if (totalCells <= 0 || snap.codepoints.size < totalCells) return@Canvas

        val normalizedSelection = selection?.normalized(totalCells)
        val selBgArgb = selectionBackgroundColor.toArgb()
        val bgRect = RectF()

        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas

            // Fill default background
            native.drawColor(snap.defaultBgArgb)

            for (row in 0 until snap.rows) {
                val rowY = row * cellHeight
                val baselineY = rowY + fontBaseline
                val rowStart = row * snap.cols

                for (col in 0 until snap.cols) {
                    val idx = rowStart + col
                    val codepoint = snap.codepoints[idx]
                    val cellFlag = snap.flags[idx].toInt() and 0xFF

                    if ((cellFlag and TerminalSnapshot.CELL_FLAG_SPACER) != 0) continue

                    val cellX = col * cellWidth
                    val fgColor = snap.fgArgb[idx]
                    val bgColor = snap.bgArgb[idx]
                    val isSelected = normalizedSelection != null && idx in normalizedSelection

                    // Draw cell background if different from default background
                    val effectiveBg = if (isSelected) selBgArgb else bgColor
                    if (effectiveBg != snap.defaultBgArgb) {
                        bgRect.set(cellX, rowY, cellX + cellWidth, rowY + cellHeight)
                        textPaint.color = effectiveBg
                        native.drawRect(bgRect, textPaint)
                    }

                    // Draw cursor
                    if (snap.cursorVisible && col == snap.cursorX && row == snap.cursorY) {
                        bgRect.set(cellX, rowY, cellX + cellWidth, rowY + cellHeight)
                        textPaint.color = cursorColor.toArgb()
                        native.drawRect(bgRect, textPaint)
                    }

                    // Draw text character
                    if (codepoint != 0 && codepoint != 32) {
                        val isBold = (cellFlag and TerminalSnapshot.CELL_FLAG_BOLD) != 0
                        val isItalic = (cellFlag and TerminalSnapshot.CELL_FLAG_ITALIC) != 0

                        textPaint.typeface = when {
                            isBold && isItalic -> boldItalicTypeface
                            isBold -> boldTypeface
                            isItalic -> italicTypeface
                            else -> primaryTypeface
                        }

                        textPaint.color = fgColor
                        textPaint.isUnderlineText = (cellFlag and TerminalSnapshot.CELL_FLAG_UNDERLINE) != 0

                        val glyph = snap.glyphAt(idx)
                        native.drawText(glyph, cellX, baselineY, textPaint)
                    }
                }
            }
        }
    }
}
