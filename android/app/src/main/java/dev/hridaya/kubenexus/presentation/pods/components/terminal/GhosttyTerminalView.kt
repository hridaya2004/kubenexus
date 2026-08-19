package dev.hridaya.kubenexus.presentation.pods.components.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hridaya.kubenexus.presentation.pods.detail.PodDetailUiAction
import dev.hridaya.kubenexus.presentation.pods.detail.PodDetailUiState
import dev.hridaya.kubenexus.ui.theme.KubeNexusTheme

private val GhosttyBg = Color(0xFF000000)
private val GhosttySurface = Color(0xFF000000)
private val GhosttyBorder = Color(0xFF222222)
private val GhosttyText = Color(0xFFFFFFFF)
private val GhosttyGutter = Color(0xFF777777)
private val GhosttyKeyBg = Color(0xFF141414)
private val GhosttyKeyBorder = Color(0xFF2E2E2E)
private val GhosttyGreen = Color(0xFF3FB950)
private val GhosttyRed = Color(0xFFF85149)

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun GhosttyTerminalView(
    uiState: PodDetailUiState,
    engine: GhosttyTerminalEngine,
    onAction: (PodDetailUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val snapshot by engine.snapshot.collectAsState()
    val isImeVisible = WindowInsets.isImeVisible
    val containers = (uiState.podDetails?.initContainers.orEmpty() + uiState.podDetails?.containers.orEmpty()).distinctBy { it.name }

    var terminalSelection by remember { mutableStateOf<TerminalSelection?>(null) }
    var localInput by remember { mutableStateOf("") }
    var commandHistory by remember { mutableStateOf(listOf<String>()) }
    var historyIndex by remember { mutableIntStateOf(-1) }
    var ctrlActive by remember { mutableStateOf(false) }
    var altActive by remember { mutableStateOf(false) }

    // When soft keyboard is visible and user presses back, forcefully clear focus and hide keyboard so layout expands to full screen
    BackHandler(enabled = isImeVisible) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    // Force layout expansion when IME visibility changes to hidden (e.g. system back key / IME close)
    LaunchedEffect(isImeVisible) {
        if (!isImeVisible) {
            focusManager.clearFocus(force = true)
        }
    }

    LaunchedEffect(Unit) {
        if (!engine.isNativeLoaded) {
            engine.initialize(80, 24)
        }
    }

    LaunchedEffect(uiState.isTerminalActive) {
        if (!uiState.isTerminalActive) {
            localInput = ""
            terminalSelection = null
            focusManager.clearFocus(force = true)
        }
    }

    fun executeLocalCommand() {
        if (localInput.isNotEmpty()) {
            val cmd = localInput
            if (commandHistory.isEmpty() || commandHistory.last() != cmd) {
                commandHistory = commandHistory + cmd
            }
            historyIndex = -1
            engine.sendText(cmd + "\n")
            localInput = ""
        } else {
            engine.sendText("\n")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .imePadding(),
    ) {
        // Target container selector and attach/disconnect button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false),
            ) {
                Text(
                    text = "Target:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(6.dp))
                if (containers.size > 1) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(containers) { c ->
                            FilterChip(
                                selected = c.name == uiState.selectedContainer,
                                onClick = { onAction(PodDetailUiAction.SelectContainer(c.name)) },
                                label = { Text(c.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            )
                        }
                    }
                } else {
                    Text(
                        text = uiState.selectedContainer ?: "default",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (uiState.isTerminalActive) {
                Button(
                    onClick = { onAction(PodDetailUiAction.StopInteractiveTerminal) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Disconnect", fontSize = 12.sp)
                }
            } else {
                Button(
                    onClick = { onAction(PodDetailUiAction.StartInteractiveTerminal()) },
                    enabled = uiState.isContainerAttachable && !uiState.isExecutingCommand,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Attach", fontSize = 12.sp)
                }
            }
        }

        // Pure pitch black terminal box
        Surface(
            color = GhosttyBg,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, GhosttyBorder, MaterialTheme.shapes.small)
                .clip(MaterialTheme.shapes.small),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GhosttySurface)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val dotColor = when {
                            !uiState.isOnline -> GhosttyRed
                            uiState.isTerminalActive -> GhosttyGreen
                            else -> GhosttyGutter
                        }
                        val statusTitle = when {
                            !uiState.isOnline -> "OFFLINE"
                            uiState.isTerminalActive -> "ATTACHED"
                            else -> "DETACHED"
                        }
                        val statusSubtitle = when {
                            !uiState.isOnline -> "(disconnected)"
                            uiState.isTerminalActive -> "(${uiState.activeShellCommand ?: "sh"})"
                            else -> "(inactive)"
                        }

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(dotColor, CircleShape),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusTitle,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isTerminalActive) GhosttyGreen else if (!uiState.isOnline) GhosttyRed else GhosttyGutter,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusSubtitle,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = GhosttyGutter,
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Toggle Keyboard button
                        IconButton(
                            onClick = {
                                if (isImeVisible) {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                } else {
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                            },
                            modifier = Modifier.size(26.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Keyboard,
                                contentDescription = "Toggle Keyboard",
                                tint = if (isImeVisible) GhosttyGreen else GhosttyText,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))

                        // Select All button
                        IconButton(
                            onClick = {
                                val snap = snapshot
                                if (snap != null && snap.cols > 0 && snap.rows > 0) {
                                    terminalSelection = TerminalSelection(0, (snap.cols * snap.rows) - 1)
                                }
                            },
                            modifier = Modifier.size(26.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SelectAll,
                                contentDescription = "Select All",
                                tint = GhosttyText,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))

                        // Copy button
                        IconButton(
                            onClick = {
                                val snap = snapshot
                                if (snap != null) {
                                    val sel = terminalSelection?.normalized(snap.cols * snap.rows)
                                    val text = if (sel != null) {
                                        extractSelectionText(snap, sel)
                                    } else {
                                        extractSelectionText(snap, 0 until (snap.cols * snap.rows))
                                    }
                                    if (!text.isNullOrBlank()) {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Terminal Output", text))
                                        Toast.makeText(context, "Terminal output copied", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.size(26.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy",
                                tint = GhosttyText,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))

                        // Paste button
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val pasteText = clip.getItemAt(0).text?.toString().orEmpty()
                                    if (pasteText.isNotEmpty()) {
                                        if (uiState.isTerminalActive) {
                                            localInput += pasteText
                                            focusRequester.requestFocus()
                                        } else {
                                            engine.sendPaste(pasteText)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.size(26.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentPaste,
                                contentDescription = "Paste",
                                tint = GhosttyText,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))

                        // Clear button
                        IconButton(
                            onClick = {
                                onAction(PodDetailUiAction.ClearTerminal)
                                engine.initialize()
                                terminalSelection = null
                            },
                            modifier = Modifier.size(26.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = "Clear",
                                tint = GhosttyText,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }

                // Render TerminalCanvas & selection controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(GhosttyBg),
                ) {
                    val hasContent = snapshot?.let { snap ->
                        snap.codepoints.any { it != 0 && it != 32 }
                    } ?: false

                    if (!uiState.isTerminalActive && !hasContent) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Terminal,
                                    contentDescription = null,
                                    tint = GhosttyGutter,
                                    modifier = Modifier.size(40.dp),
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Terminal Detached",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GhosttyText,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (uiState.isOnline) {
                                        "Container '${uiState.selectedContainer ?: "default"}' is ready.\nTap 'Attach' to connect terminal."
                                    } else {
                                        "Network offline. Connect to network to attach terminal."
                                    },
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = GhosttyGutter,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    } else {
                        TerminalCanvas(
                            snapshot = snapshot,
                            terminalHandle = engine.terminalHandle,
                            selection = terminalSelection,
                            onSelectionChange = { terminalSelection = it },
                            onResize = { cols, rows, cellW, cellH, _, _ ->
                                engine.resize(cols, rows, cellW, cellH)
                            },
                            onScroll = { delta, x, y ->
                                engine.scroll(delta, x, y)
                            },
                            onTap = {
                                if (terminalSelection != null) {
                                    terminalSelection = null
                                } else if (isImeVisible) {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )

                        // Floating Selection Action Bar
                        if (terminalSelection != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(8.dp),
                            ) {
                                Surface(
                                    color = GhosttyKeyBg,
                                    shape = MaterialTheme.shapes.small,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    ) {
                                        Text(
                                            text = "Text Selected",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = Color.White,
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = {
                                                val snap = snapshot
                                                if (snap != null) {
                                                    val sel = terminalSelection?.normalized(snap.cols * snap.rows)
                                                    if (sel != null) {
                                                        val text = extractSelectionText(snap, sel)
                                                        if (!text.isNullOrBlank()) {
                                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                            clipboard.setPrimaryClip(ClipData.newPlainText("Terminal Selection", text))
                                                            Toast.makeText(context, "Selection copied", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                                terminalSelection = null
                                            },
                                            modifier = Modifier.size(24.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.ContentCopy,
                                                contentDescription = "Copy Selection",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { terminalSelection = null },
                                            modifier = Modifier.size(24.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Close,
                                                contentDescription = "Close",
                                                tint = GhosttyGutter,
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (!uiState.isTerminalActive) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.85f),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(GhosttyGutter, CircleShape),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Terminal detached. Tap Attach to reconnect.",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = GhosttyGutter,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dedicated local command input line with 0ms local latency
        if (uiState.isTerminalActive) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GhosttyBg, MaterialTheme.shapes.small)
                    .border(1.dp, GhosttyBorder, MaterialTheme.shapes.small)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "❯ ",
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
                BasicTextField(
                    value = localInput,
                    onValueChange = { localInput = it },
                    textStyle = TextStyle(
                        color = GhosttyText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                    ),
                    cursorBrush = SolidColor(Color.White),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { executeLocalCommand() },
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                )
                IconButton(
                    onClick = { executeLocalCommand() },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Send Command",
                        tint = if (localInput.isNotEmpty()) Color.White else GhosttyGutter,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        // Accessory keys bar (Virtual keys for ESC, TAB, CTRL, ALT, UP, DOWN, etc.)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TerminalKeyButton(
                label = "ESC",
                onClick = {
                    engine.sendKey(KeyEvent.KEYCODE_ESCAPE)
                    focusRequester.requestFocus()
                },
            )
            TerminalKeyButton(
                label = "TAB",
                onClick = {
                    if (uiState.isTerminalActive) {
                        localInput += "\t"
                    } else {
                        engine.sendKey(KeyEvent.KEYCODE_TAB)
                    }
                    focusRequester.requestFocus()
                },
            )
            TerminalKeyButton(
                label = "CTRL",
                isActive = ctrlActive,
                onClick = {
                    ctrlActive = !ctrlActive
                    focusRequester.requestFocus()
                },
            )
            TerminalKeyButton(
                label = "ALT",
                isActive = altActive,
                onClick = {
                    altActive = !altActive
                    focusRequester.requestFocus()
                },
            )
            TerminalKeyButton(
                label = "^C",
                onClick = {
                    engine.sendText("\u0003")
                    localInput = ""
                    focusRequester.requestFocus()
                },
            )
            TerminalKeyButton(
                label = "^D",
                onClick = {
                    engine.sendText("\u0004")
                    focusRequester.requestFocus()
                },
            )
            TerminalKeyButton(
                label = "^Z",
                onClick = {
                    engine.sendText("\u001A")
                    focusRequester.requestFocus()
                },
            )
            TerminalKeyButton(
                label = "↑",
                onClick = {
                    if (commandHistory.isNotEmpty()) {
                        val nextIdx = if (historyIndex == -1) {
                            commandHistory.lastIndex
                        } else {
                            (historyIndex - 1).coerceAtLeast(0)
                        }
                        historyIndex = nextIdx
                        localInput = commandHistory[nextIdx]
                    } else {
                        engine.sendKey(KeyEvent.KEYCODE_DPAD_UP)
                    }
                    focusRequester.requestFocus()
                },
            )
            TerminalKeyButton(
                label = "↓",
                onClick = {
                    if (commandHistory.isNotEmpty() && historyIndex != -1) {
                        val nextIdx = historyIndex + 1
                        if (nextIdx <= commandHistory.lastIndex) {
                            historyIndex = nextIdx
                            localInput = commandHistory[nextIdx]
                        } else {
                            historyIndex = -1
                            localInput = ""
                        }
                    } else {
                        engine.sendKey(KeyEvent.KEYCODE_DPAD_DOWN)
                    }
                    focusRequester.requestFocus()
                },
            )
            TerminalKeyButton(
                label = "←",
                onClick = {
                    engine.sendKey(KeyEvent.KEYCODE_DPAD_LEFT)
                    focusRequester.requestFocus()
                },
            )
            TerminalKeyButton(
                label = "→",
                onClick = {
                    engine.sendKey(KeyEvent.KEYCODE_DPAD_RIGHT)
                    focusRequester.requestFocus()
                },
            )
        }
    }
}

@Composable
private fun TerminalKeyButton(
    label: String,
    onClick: () -> Unit,
    isActive: Boolean = false,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraSmall,
        color = if (isActive) Color.White else GhosttyKeyBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) Color.White else GhosttyKeyBorder),
        modifier = Modifier.height(32.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 10.dp),
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) Color.Black else GhosttyText,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TerminalKeyButtonPreview() {
    KubeNexusTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            TerminalKeyButton(label = "ESC", onClick = {})
            TerminalKeyButton(label = "TAB", onClick = {})
            TerminalKeyButton(label = "CTRL", onClick = {}, isActive = true)
            TerminalKeyButton(label = "ALT", onClick = {})
            TerminalKeyButton(label = "↑", onClick = {})
        }
    }
}
