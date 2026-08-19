package dev.hridaya.kubenexus.presentation.explore.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import dev.hridaya.kubenexus.ui.theme.KubeNexusTheme

@Composable
fun ExplainDescriptionText(
    text: String,
    modifier: Modifier = Modifier,
) {
    if (text.isEmpty()) return

    val formattedText = remember(text) {
        val regex = Regex("""(?<!\n)(\s*)(More info\s*:\s*https?://\S+)""", RegexOption.IGNORE_CASE)
        text.replace(regex) { matchResult ->
            "\n\n${matchResult.groupValues[2]}"
        }.trim()
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val urlRegex = remember { Regex("""https?://[^\s)\]>,"']+""") }

    val annotatedString = remember(formattedText, primaryColor) {
        buildAnnotatedString {
            var currentIndex = 0
            val matches = urlRegex.findAll(formattedText)

            for (match in matches) {
                if (match.range.first > currentIndex) {
                    append(formattedText.substring(currentIndex, match.range.first))
                }
                val url = match.value
                withLink(
                    LinkAnnotation.Url(
                        url = url,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = primaryColor,
                                textDecoration = TextDecoration.Underline,
                            ),
                        ),
                    ),
                ) {
                    append(url)
                }
                currentIndex = match.range.last + 1
            }

            if (currentIndex < formattedText.length) {
                append(formattedText.substring(currentIndex))
            }
        }
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun ExplainDescriptionTextPreview() {
    KubeNexusTheme {
        ExplainDescriptionText(
            text = "Pod is a collection of containers that can run on a host. More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-overview/",
        )
    }
}
