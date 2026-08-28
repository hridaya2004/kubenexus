package dev.hridaya.kubenexus.presentation.logcat.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.hridaya.kubenexus.domain.model.LogcatEntry

@Composable
fun LogcatLogList(
    listState: LazyListState,
    logs: List<LogcatEntry>,
    searchQuery: String,
    wrapLines: Boolean,
    onCopyEntry: (LogcatEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalScrollState = rememberScrollState()

    SelectionContainer(modifier = modifier.fillMaxSize()) {
        val logBoxModifier = if (wrapLines) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .fillMaxSize()
                .horizontalScroll(horizontalScrollState)
        }

        Box(modifier = logBoxModifier) {
            LazyColumn(
                state = listState,
                modifier = if (wrapLines) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxHeight()
                        .wrapContentWidth(align = Alignment.Start, unbounded = true)
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(
                    items = logs,
                    key = { _, logEntry -> logEntry.id },
                ) { entryIndex, entry ->
                    LogcatEntryRow(
                        index = entryIndex + 1,
                        entry = entry,
                        searchQuery = searchQuery,
                        wrapLines = wrapLines,
                        onCopy = { onCopyEntry(entry) },
                    )
                }
            }
        }
    }
}
