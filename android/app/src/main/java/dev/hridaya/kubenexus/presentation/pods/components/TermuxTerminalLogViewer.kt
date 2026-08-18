package dev.hridaya.kubenexus.presentation.pods.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.WrapText
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val TermuxBg = Color(0xFF0D1117)
private val TermuxSurface = Color(0xFF161B22)
private val TermuxText = Color(0xFFC9D1D9)
private val TermuxGutter = Color(0xFF484F58)
private val TermuxGreen = Color(0xFF3FB950)
private val TermuxYellow = Color(0xFFD29922)
private val TermuxRed = Color(0xFFF85149)
private val TermuxCyan = Color(0xFF58A6FF)
private val TermuxPurple = Color(0xFFBC8CFF)

@Composable
fun TermuxTerminalLogViewer(logs: List<String>, isStreaming: Boolean, onClearLogs: () -> Unit, modifier: Modifier = Modifier) {
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var autoScroll by remember { mutableStateOf(true) }
    var wrapLines by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val filteredLogs = remember(logs, searchQuery) {
        if (searchQuery.isBlank()) {
            logs
        } else {
            logs.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    LaunchedEffect(filteredLogs.size, autoScroll) {
        if (autoScroll && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cursor_alpha",
    )

    Surface(
        color = TermuxBg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp)),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TermuxSurface)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (isStreaming) TermuxGreen else TermuxYellow,
                                shape = RoundedCornerShape(5.dp),
                            ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isStreaming) "termux (live stream)" else "termux (log output)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TermuxText,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${filteredLogs.size} lines)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TermuxGutter,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showSearch = !showSearch },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = if (showSearch) TermuxCyan else TermuxText,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    IconButton(
                        onClick = { wrapLines = !wrapLines },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.WrapText,
                            contentDescription = "Wrap lines",
                            tint = if (wrapLines) TermuxGreen else TermuxGutter,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    IconButton(
                        onClick = {
                            val textToCopy = logs.joinToString("\n")
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Pod Logs", textToCopy))
                            Toast.makeText(
                                context,
                                "Copied ${logs.size} log lines to clipboard",
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy all",
                            tint = TermuxText,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    IconButton(
                        onClick = onClearLogs,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = "Clear",
                            tint = TermuxText,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            if (showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Filter logs...",
                            color = TermuxGutter,
                            fontSize = 12.sp,
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TermuxText,
                        unfocusedTextColor = TermuxText,
                        focusedBorderColor = TermuxCyan,
                        unfocusedBorderColor = TermuxGutter,
                        focusedContainerColor = TermuxSurface,
                        unfocusedContainerColor = TermuxSurface,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .height(48.dp),
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (isStreaming) "Waiting for container logs..." else "No log output available",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = TermuxGutter,
                        )
                    }
                } else {
                    val horizontalScrollState = rememberScrollState()
                    val lineModifier =
                        if (wrapLines) {
                            Modifier.fillMaxWidth()
                        } else {
                            Modifier.horizontalScroll(
                                horizontalScrollState,
                            )
                        }

                    LazyColumn(
                        state = listState,
                        modifier = lineModifier.fillMaxSize(),
                    ) {
                        itemsIndexed(filteredLogs) { index, line ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                            ) {
                                Text(
                                    text = (index + 1).toString().padStart(4, ' '),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = TermuxGutter,
                                    modifier = Modifier.width(36.dp),
                                )

                                val parsedLine = remember(line, searchQuery) {
                                    parseAnsiToAnnotatedString(line, searchQuery)
                                }

                                Text(
                                    text = parsedLine,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = TermuxText,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        if (isStreaming) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                ) {
                                    Text(
                                        text = "$ ",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TermuxGreen,
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(width = 8.dp, height = 14.dp)
                                            .alpha(cursorAlpha)
                                            .background(TermuxGreen),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TermuxSurface)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Auto-scroll",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TermuxText,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = autoScroll,
                        onCheckedChange = { autoScroll = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TermuxGreen,
                            checkedTrackColor = TermuxSurface,
                        ),
                        modifier = Modifier.size(width = 36.dp, height = 20.dp),
                    )
                }

                if (filteredLogs.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                listState.animateScrollToItem(filteredLogs.size - 1)
                            }
                        },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowDownward,
                            contentDescription = "Scroll to bottom",
                            tint = TermuxText,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun parseAnsiToAnnotatedString(rawText: String, highlightQuery: String = ""): AnnotatedString {
    val cleanText = rawText.replace("\r", "")
    val builder = AnnotatedString.Builder()

    // ANSI parser regex: \u001B\[[0-9;]*m
    val ansiRegex = Regex("""\u001B\[([0-9;]*)m""")
    var lastIndex = 0
    var currentColor = TermuxText
    var isBold = false

    val matches = ansiRegex.findAll(cleanText).toList()

    if (matches.isEmpty()) {
        appendWithHighlight(builder, cleanText, currentColor, isBold, highlightQuery)
        return builder.toAnnotatedString()
    }

    for (match in matches) {
        val plainChunk = cleanText.substring(lastIndex, match.range.first)
        if (plainChunk.isNotEmpty()) {
            appendWithHighlight(builder, plainChunk, currentColor, isBold, highlightQuery)
        }

        val codeStr = match.groupValues.getOrNull(1).orEmpty()
        val codes = codeStr.split(";").mapNotNull { it.toIntOrNull() }

        if (codes.isEmpty() || codes.contains(0)) {
            currentColor = TermuxText
            isBold = false
        }
        if (codes.contains(1)) isBold = true

        for (c in codes) {
            when (c) {
                30 -> currentColor = TermuxGutter
                31 -> currentColor = TermuxRed
                32 -> currentColor = TermuxGreen
                33 -> currentColor = TermuxYellow
                34 -> currentColor = TermuxCyan
                35 -> currentColor = TermuxPurple
                36 -> currentColor = TermuxCyan
                37, 39 -> currentColor = TermuxText
                90 -> currentColor = TermuxGutter
                91 -> currentColor = TermuxRed
                92 -> currentColor = TermuxGreen
                93 -> currentColor = TermuxYellow
                94 -> currentColor = TermuxCyan
                95 -> currentColor = TermuxPurple
                96 -> currentColor = TermuxCyan
                97 -> currentColor = Color.White
            }
        }

        lastIndex = match.range.last + 1
    }

    if (lastIndex < cleanText.length) {
        val remaining = cleanText.substring(lastIndex)
        appendWithHighlight(builder, remaining, currentColor, isBold, highlightQuery)
    }

    return builder.toAnnotatedString()
}

private fun appendWithHighlight(builder: AnnotatedString.Builder, text: String, baseColor: Color, isBold: Boolean, query: String) {
    if (query.isBlank() || !text.contains(query, ignoreCase = true)) {
        builder.withStyle(
            SpanStyle(
                color = baseColor,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            ),
        ) {
            append(text)
        }
        return
    }

    var start = 0
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()

    while (start < text.length) {
        val index = lowerText.indexOf(lowerQuery, start)
        if (index == -1) {
            builder.withStyle(
                SpanStyle(
                    color = baseColor,
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                ),
            ) {
                append(text.substring(start))
            }
            break
        }

        if (index > start) {
            builder.withStyle(
                SpanStyle(
                    color = baseColor,
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                ),
            ) {
                append(text.substring(start, index))
            }
        }

        val matchEnd = index + query.length
        builder.withStyle(
            SpanStyle(
                color = Color.Black,
                background = TermuxYellow,
                fontWeight = FontWeight.Bold,
            ),
        ) {
            append(text.substring(index, matchEnd))
        }

        start = matchEnd
    }
}
