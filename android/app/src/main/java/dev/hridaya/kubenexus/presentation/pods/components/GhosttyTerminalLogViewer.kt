package dev.hridaya.kubenexus.presentation.pods.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.border
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

private val TerminalBg = Color(0xFF000000)
private val TerminalHeaderBg = Color(0xFF000000)
private val TerminalBorder = Color(0xFF222222)
private val TerminalText = Color(0xFFFFFFFF)
private val TerminalGutter = Color(0xFF777777)
private val TerminalFabBg = Color(0xFF141414)
private val TerminalGreen = Color(0xFF3FB950)
private val TerminalYellow = Color(0xFFD29922)
private val TerminalRed = Color(0xFFF85149)
private val TerminalCyan = Color(0xFF58A6FF)
private val TerminalPurple = Color(0xFFBC8CFF)

@Composable
fun GhosttyTerminalLogViewer(
    logs: List<String>,
    isStreaming: Boolean,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
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

    // Content-aware bottom detection
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty() || layoutInfo.totalItemsCount == 0) {
                true
            } else {
                val lastVisibleItem = visibleItems.last()
                lastVisibleItem.index >= layoutInfo.totalItemsCount - 2
            }
        }
    }

    // Content-aware auto-scrolling: only stick to bottom if user is already at the end
    LaunchedEffect(filteredLogs.size) {
        if (isAtBottom && filteredLogs.isNotEmpty()) {
            listState.scrollToItem(filteredLogs.size - 1)
        }
    }

    Surface(
        color = TerminalBg,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxSize()
            .border(1.dp, TerminalBorder, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp)),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Minimal header bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalHeaderBg)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (isStreaming) TerminalGreen else TerminalYellow,
                                shape = CircleShape,
                            ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isStreaming) "LIVE LOGS" else "LOGS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerminalText,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${filteredLogs.size})",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TerminalGutter,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showSearch = !showSearch },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = if (showSearch) TerminalCyan else TerminalText,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    IconButton(
                        onClick = { wrapLines = !wrapLines },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.WrapText,
                            contentDescription = "Wrap lines",
                            tint = if (wrapLines) TerminalGreen else TerminalGutter,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

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
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy all",
                            tint = TerminalText,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    IconButton(
                        onClick = onClearLogs,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = "Clear",
                            tint = TerminalText,
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
                            color = TerminalGutter,
                            fontSize = 12.sp,
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TerminalText,
                        unfocusedTextColor = TerminalText,
                        focusedBorderColor = TerminalCyan,
                        unfocusedBorderColor = TerminalGutter,
                        focusedContainerColor = TerminalHeaderBg,
                        unfocusedContainerColor = TerminalHeaderBg,
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
                            color = TerminalGutter,
                        )
                    }
                } else {
                    val horizontalScrollState = rememberScrollState()
                    val lineModifier =
                        if (wrapLines) {
                            Modifier.fillMaxWidth()
                        } else {
                            Modifier.horizontalScroll(horizontalScrollState)
                        }

                    SelectionContainer(modifier = lineModifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
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
                                        color = TerminalGutter,
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
                                        color = TerminalText,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }

                // Down arrow button to jump straight to latest log line
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isAtBottom && filteredLogs.isNotEmpty(),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            scope.launch {
                                listState.animateScrollToItem(filteredLogs.size - 1)
                            }
                        },
                        containerColor = TerminalFabBg,
                        contentColor = TerminalText,
                        shape = CircleShape,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowDownward,
                            contentDescription = "Go to latest log",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun parseAnsiToAnnotatedString(
    rawText: String,
    highlightQuery: String = ""
): AnnotatedString {
    val cleanText = rawText.replace("\r", "")
    val builder = AnnotatedString.Builder()

    // ANSI parser regex: \u001B\[[0-9;]*m
    val ansiRegex = Regex("""\u001B\[([0-9;]*)m""")
    var lastIndex = 0
    var currentColor = TerminalText
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
            currentColor = TerminalText
            isBold = false
        }
        if (codes.contains(1)) isBold = true

        for (c in codes) {
            when (c) {
                30 -> currentColor = TerminalGutter
                31 -> currentColor = TerminalRed
                32 -> currentColor = TerminalGreen
                33 -> currentColor = TerminalYellow
                34 -> currentColor = TerminalCyan
                35 -> currentColor = TerminalPurple
                36 -> currentColor = TerminalCyan
                37, 39 -> currentColor = TerminalText
                90 -> currentColor = TerminalGutter
                91 -> currentColor = TerminalRed
                92 -> currentColor = TerminalGreen
                93 -> currentColor = TerminalYellow
                94 -> currentColor = TerminalCyan
                95 -> currentColor = TerminalPurple
                96 -> currentColor = TerminalCyan
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

private fun appendWithHighlight(
    builder: AnnotatedString.Builder,
    text: String,
    baseColor: Color,
    isBold: Boolean,
    query: String
) {
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
                background = TerminalYellow,
                fontWeight = FontWeight.Bold,
            ),
        ) {
            append(text.substring(index, matchEnd))
        }

        start = matchEnd
    }
}
