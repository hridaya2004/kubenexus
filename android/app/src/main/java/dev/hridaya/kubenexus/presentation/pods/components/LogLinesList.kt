package dev.hridaya.kubenexus.presentation.pods.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
internal fun LogLinesList(
    lines: List<String>,
    wrapLines: Boolean,
    highlightQuery: String,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
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
            modifier = modifier.fillMaxSize(),
        ) {
            itemsIndexed(lines) { index, line ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp),
                ) {
                    Text(
                        text = (index + 1).toString().padStart(4, ' '),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = TerminalGutter,
                        modifier = Modifier.width(36.dp),
                    )

                    val parsedLine = remember(line, highlightQuery) {
                        parseAnsiToAnnotatedString(line, highlightQuery)
                    }

                    Text(
                        text = parsedLine,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = TerminalText,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
