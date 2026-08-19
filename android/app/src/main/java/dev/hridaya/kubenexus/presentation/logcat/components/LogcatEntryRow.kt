package dev.hridaya.kubenexus.presentation.logcat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hridaya.kubenexus.domain.model.LogLevel
import dev.hridaya.kubenexus.domain.model.LogcatEntry
import dev.hridaya.kubenexus.ui.theme.KubeNexusTheme

@Composable
fun LogcatEntryRow(
    index: Int,
    entry: LogcatEntry,
    searchQuery: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val levelColor = getLogLevelColor(entry.level)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCopy)
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = index.toString().padStart(4, ' '),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = GhosttyGutter,
            modifier = Modifier.width(32.dp),
        )

        if (entry.timestamp.isNotBlank()) {
            Text(
                text = entry.timestamp.takeLast(12),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = GhosttyGutter,
                maxLines = 1,
                modifier = Modifier.padding(end = 4.dp),
            )
        }

        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .background(levelColor.copy(alpha = 0.2f))
                .padding(horizontal = 3.dp, vertical = 0.5.dp),
        ) {
            Text(
                text = entry.level.code,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = levelColor,
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        if (entry.tag.isNotBlank()) {
            Text(
                text = "${entry.tag}:",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = LogTagColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 4.dp),
            )
        }

        val annotatedMessage = remember(entry.message, searchQuery) {
            buildAnnotatedMessage(entry.message, searchQuery)
        }

        Text(
            text = annotatedMessage,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = GhosttyText,
            lineHeight = 15.sp,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

private fun buildAnnotatedMessage(message: String, query: String): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(message)
    val builder = AnnotatedString.Builder()
    var lastIndex = 0
    var index = message.indexOf(query, 0, ignoreCase = true)
    while (index >= 0) {
        builder.append(message.substring(lastIndex, index))
        builder.pushStyle(
            SpanStyle(
                background = GhosttyYellow.copy(alpha = 0.35f),
                color = Color.White,
                fontWeight = FontWeight.Bold,
            ),
        )
        builder.append(message.substring(index, index + query.length))
        builder.pop()
        lastIndex = index + query.length
        index = message.indexOf(query, lastIndex, ignoreCase = true)
    }
    if (lastIndex < message.length) {
        builder.append(message.substring(lastIndex))
    }
    return builder.toAnnotatedString()
}

@Preview(showBackground = true)
@Composable
private fun LogcatEntryRowPreview() {
    KubeNexusTheme {
        Box(modifier = Modifier
            .background(GhosttyBg)
            .padding(8.dp)) {
            LogcatEntryRow(
                index = 1,
                entry = LogcatEntry(
                    id = 1L,
                    timestamp = "16:42:05.123",
                    pid = "1234",
                    tid = "5678",
                    level = LogLevel.INFO,
                    tag = "KubeNexusNative",
                    message = "Connected to Kubernetes API",
                    raw = "08-19 16:42:05.123  1234  5678 I KubeNexusNative: Connected to Kubernetes API",
                ),
                searchQuery = "Kubernetes",
                onCopy = {},
            )
        }
    }
}
