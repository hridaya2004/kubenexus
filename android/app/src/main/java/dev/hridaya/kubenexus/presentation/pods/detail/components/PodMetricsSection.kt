package dev.hridaya.kubenexus.presentation.pods.detail.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Speed

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hridaya.kubenexus.domain.model.PodMetricSample
import dev.hridaya.kubenexus.presentation.pods.detail.MetricsRange
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private val CPU_COLOR = Color(0xFF6C9EFF)
private val MEMORY_COLOR = Color(0xFF63D8A6)

@Composable
fun PodMetricsSection(
    samples: List<PodMetricSample>,
    selectedRange: MetricsRange,
    isLoading: Boolean,
    onSelectRange: (MetricsRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Resource Usage",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                IntervalDropdown(selectedRange, onSelectRange)
            }

            Spacer(Modifier.height(10.dp))

            val windowed = remember(samples, selectedRange) {
                val cutoff = System.currentTimeMillis() - selectedRange.durationMs
                samples.filter { it.timestampMillis >= cutoff }
            }
            val latest = windowed.lastOrNull()

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendDot(CPU_COLOR, "CPU", latest?.cpuCores?.let(::formatCores) ?: "—")
                LegendDot(MEMORY_COLOR, "Mem", latest?.memoryBytes?.let(::formatBytes) ?: "—")
            }

            Spacer(Modifier.height(8.dp))

            if (windowed.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isLoading || windowed.isEmpty()) {
                            "Collecting usage samples…"
                        } else {
                            "Not enough samples for this range yet"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                UsageChart(windowed)
            }
        }
    }
}

@Composable
private fun IntervalDropdown(
    selected: MetricsRange,
    onSelect: (MetricsRange) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = selected.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MetricsRange.entries.forEach { range ->
                DropdownMenuItem(
                    text = { Text(range.label) },
                    onClick = {
                        expanded = false
                        onSelect(range)
                    },
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = "$label $value",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun UsageChart(samples: List<PodMetricSample>) {
    val gridColor = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()

    val spanMs = (samples.last().timestampMillis - samples.first().timestampMillis)
        .coerceAtLeast(1L)

    val cpuValues = samples.map { it.cpuCores }
    val memValues = samples.map { it.memoryBytes.toDouble() }
    val cpuMax = (cpuValues.maxOrNull() ?: 0.0).coerceAtLeast(1e-9)
    val memMax = (memValues.maxOrNull() ?: 0.0).coerceAtLeast(1.0)

    val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
    ) {
        val padLeft = 44f
        val padRight = 40f
        val padTop = 6f
        val padBottom = 18f
        val width = size.width - padLeft - padRight
        val height = size.height - padTop - padBottom

        fun xAt(index: Int): Float =
            padLeft + width * index / (samples.size - 1).coerceAtLeast(1)

        fun fractionAt(sample: PodMetricSample): Float =
            ((sample.timestampMillis - samples.first().timestampMillis).toFloat() / spanMs)
                .coerceIn(0f, 1f)

        // Gridlines at 0%, 50%, 100% of the plot area.
        for (i in 0..2) {
            val y = padTop + height * i / 2f
            drawLine(gridColor, Offset(padLeft, y), Offset(padLeft + width, y), strokeWidth = 1f)
            val frac = 1f - i / 2f
            drawText(
                textMeasurer = textMeasurer,
                text = formatBytes((memMax * frac).roundToLong()),
                topLeft = Offset(0f, y - 14f),
                style = labelStyle,
            )
            drawText(
                textMeasurer = textMeasurer,
                text = formatCores(cpuMax * frac),
                topLeft = Offset(padLeft + width + 4f, y - 14f),
                style = labelStyle,
            )
        }

        fun seriesPath(values: List<Double>, max: Double): Path {
            val path = Path()
            values.forEachIndexed { index, value ->
                val x = xAt(index)
                val y = padTop + height * (1f - (value / max).toFloat().coerceIn(0f, 1f))
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    val prevX = xAt(index - 1)
                    val prevY = padTop + height * (1f - (values[index - 1] / max).toFloat().coerceIn(0f, 1f))
                    val midX = (prevX + x) / 2f
                    path.cubicTo(midX, prevY, midX, y, x, y)
                }
            }
            return path
        }

        val memPath = seriesPath(memValues, memMax)
        val cpuPath = seriesPath(cpuValues, cpuMax)
        val bottom = padTop + height

        drawPath(
            brush = Brush.verticalGradient(
                colors = listOf(CPU_COLOR.copy(alpha = 0.22f), Color.Transparent),
                startY = padTop,
                endY = bottom,
            ),
            path = Path().apply {
                addPath(cpuPath)
                lineTo(xAt(samples.size - 1), bottom)
                lineTo(xAt(0), bottom)
                close()
            },
        )

        drawPath(cpuPath, CPU_COLOR, style = Stroke(width = 4f, cap = StrokeCap.Round))
        drawPath(memPath, MEMORY_COLOR, style = Stroke(width = 4f, cap = StrokeCap.Round))

        // Time markers.
        drawText(
            textMeasurer = textMeasurer,
            text = "-${selectedRangeLabel(samples)}",
            topLeft = Offset(padLeft, padTop + height + 4f),
            style = labelStyle,
        )
        drawText(
            textMeasurer = textMeasurer,
            text = "now",
            topLeft = Offset(padLeft + width - 20f, padTop + height + 4f),
            style = labelStyle,
        )
    }
}

private fun selectedRangeLabel(samples: List<PodMetricSample>): String {
    val spanMin = (samples.last().timestampMillis - samples.first().timestampMillis) / 60_000f
    return if (spanMin >= 1f) "${spanMin.roundToInt()}m" else "${((spanMin * 60f).roundToInt())}s"
}

private fun formatCores(cores: Double): String =
    if (cores >= 1.0) {
        "%.2f core".format(cores)
    } else {
        "${(cores * 1000).roundToInt()}m"
    }

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> "%.1f GiB".format(bytes / (1024.0 * 1024 * 1024))
    bytes >= 1024L * 1024 -> "${(bytes / (1024.0 * 1024)).roundToInt()} MiB"
    else -> "${(bytes / 1024.0).roundToInt()} KiB"
}
