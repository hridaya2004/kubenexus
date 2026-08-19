package dev.hridaya.kubenexus.presentation.pods.components.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.PlayArrow
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hridaya.kubenexus.presentation.pods.detail.PodDetailUiAction
import dev.hridaya.kubenexus.presentation.pods.detail.PodDetailUiState

private val GhosttyBg = Color(0xFF0D1117)
private val GhosttySurface = Color(0xFF161B22)
private val GhosttyText = Color(0xFFC9D1D9)
private val GhosttyGutter = Color(0xFF6E7681)
private val GhosttyGreen = Color(0xFF3FB950)
private val GhosttyYellow = Color(0xFFD29922)
private val GhosttyRed = Color(0xFFF85149)
private val GhosttyKeyBg = Color(0xFF21262D)

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

    var ctrlActive by remember { mutableStateOf(false) }
    var altActive by remember { mutableStateOf(false) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue("  ", TextRange(2))) }

    // When soft keyboard is visible and user presses back, clear focus and hide keyboard so layout expands to full screen
    BackHandler(enabled = isImeVisible) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    LaunchedEffect(Unit) {
        if (!engine.isNativeLoaded) {
            engine.initialize(80, 24)
        }
    }

    LaunchedEffect(uiState.isTerminalActive) {
        if (uiState.isTerminalActive) {
            focusRequester.requestFocus()
            keyboardController?.show()
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
                                ),
                            )
                        }
                    }
                } else {
                    Text(
                        text = uiState.selectedContainer ?: "default",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (uiState.isTerminalActive) {
                Button(
                    onClick = { onAction(PodDetailUiAction.StopInteractiveTerminal) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
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

        // Ghostty Canvas terminal box
        Surface(
            color = GhosttyBg,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header bar with clear session status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GhosttySurface)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
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
                            !uiState.isOnline -> "(network disconnected)"
                            uiState.isTerminalActive -> "(${uiState.activeShellCommand ?: "sh"})"
                            else -> "(session inactive)"
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
                        // Keyboard toggle button
                        IconButton(
                            onClick = {
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Keyboard,
                                contentDescription = "Show Keyboard",
                                tint = GhosttyText,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))

                        // Paste button
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val pasteText = clip.getItemAt(0).text?.toString().orEmpty()
                                    if (pasteText.isNotEmpty()) {
                                        engine.sendPaste(pasteText)
                                    }
                                }
                            },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentPaste,
                                contentDescription = "Paste",
                                tint = GhosttyText,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))

                        // Copy button
                        IconButton(
                            onClick = {
                                val snap = snapshot
                                if (snap != null) {
                                    val text = extractSelectionText(snap, 0 until (snap.cols * snap.rows))
                                    if (!text.isNullOrBlank()) {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Terminal Output", text))
                                        Toast.makeText(context, "Terminal output copied", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy",
                                tint = GhosttyText,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))

                        // Clear button
                        IconButton(
                            onClick = {
                                onAction(PodDetailUiAction.ClearTerminal)
                                engine.initialize()
                            },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = "Clear",
                                tint = GhosttyText,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }

                // Render TerminalCanvas & Integrated Hidden Key Interceptor
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (uiState.isTerminalActive) {
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            } else if (uiState.isContainerAttachable && !uiState.isExecutingCommand) {
                                onAction(PodDetailUiAction.StartInteractiveTerminal())
                            }
                        },
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
                                        "Container '${uiState.selectedContainer ?: "default"}' is ready.\nTap 'Attach' above or tap here to connect."
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
                            onResize = { cols, rows, cellW, cellH, _, _ ->
                                engine.resize(cols, rows, cellW, cellH)
                            },
                            onScroll = { delta, x, y ->
                                engine.scroll(delta, x, y)
                            },
                            onTap = {
                                if (uiState.isTerminalActive) {
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                } else if (uiState.isContainerAttachable && !uiState.isExecutingCommand) {
                                    onAction(PodDetailUiAction.StartInteractiveTerminal())
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )

                        if (!uiState.isTerminalActive) {
                            Surface(
                                color = GhosttySurface.copy(alpha = 0.92f),
                                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
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

                    // Integrated hidden keyboard event listener
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            val oldText = textFieldValue.text
                            val newText = newValue.text

                            if (newText.length > oldText.length) {
                                val inserted = newText.substring(oldText.length)
                                if (ctrlActive && inserted.length == 1) {
                                    val c = inserted[0]
                                    if (c in 'a'..'z' || c in 'A'..'Z') {
                                        val ctrlChar = (c.uppercaseChar().code - 64).toChar()
                                        engine.sendText(ctrlChar.toString())
                                    } else {
                                        engine.sendText(inserted)
                                    }
                                    ctrlActive = false
                                } else if (altActive && inserted.length == 1) {
                                    engine.sendText("\u001B" + inserted)
                                    altActive = false
                                } else {
                                    engine.sendText(inserted)
                                }
                            } else if (newText.length < oldText.length) {
                                val count = oldText.length - newText.length
                                repeat(count) {
                                    engine.sendText("\u007F")
                                }
                            }
                            textFieldValue = TextFieldValue("  ", TextRange(2))
                        },
                        keyboardOptions = KeyboardOptions(
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.None,
                        ),
                        modifier = Modifier
                            .size(1.dp)
                            .alpha(0.01f)
                            .focusRequester(focusRequester)
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown) {
                                    val meta = (if (ctrlActive) KeyEvent.META_CTRL_ON else 0) or
                                        (if (altActive) KeyEvent.META_ALT_ON else 0) or
                                        keyEvent.nativeKeyEvent.metaState

                                    when (keyEvent.key) {
                                        Key.Enter -> {
                                            engine.sendText("\r")
                                            true
                                        }
                                        Key.Tab -> {
                                            engine.sendKey(KeyEvent.KEYCODE_TAB, 0, meta)
                                            true
                                        }
                                        Key.Escape -> {
                                            engine.sendKey(KeyEvent.KEYCODE_ESCAPE, 0, meta)
                                            true
                                        }
                                        Key.DirectionUp -> {
                                            engine.sendKey(KeyEvent.KEYCODE_DPAD_UP, 0, meta)
                                            true
                                        }
                                        Key.DirectionDown -> {
                                            engine.sendKey(KeyEvent.KEYCODE_DPAD_DOWN, 0, meta)
                                            true
                                        }
                                        Key.DirectionLeft -> {
                                            engine.sendKey(KeyEvent.KEYCODE_DPAD_LEFT, 0, meta)
                                            true
                                        }
                                        Key.DirectionRight -> {
                                            engine.sendKey(KeyEvent.KEYCODE_DPAD_RIGHT, 0, meta)
                                            true
                                        }
                                        Key.Backspace -> {
                                            engine.sendText("\u007F")
                                            true
                                        }
                                        else -> false
                                    }
                                } else {
                                    false
                                }
                            },
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
                    engine.sendKey(KeyEvent.KEYCODE_TAB)
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
                    engine.sendKey(
                        keyCode = KeyEvent.KEYCODE_C,
                        codepoint = 0,
                        metaState = KeyEvent.META_CTRL_ON,
                    )
                    focusRequester.requestFocus()
                },
            )
            TerminalKeyButton(
                label = "^D",
                onClick = {
                    engine.sendKey(
                        keyCode = KeyEvent.KEYCODE_D,
                        codepoint = 0,
                        metaState = KeyEvent.META_CTRL_ON,
                    )
                    focusRequester.requestFocus()
                },
            )
            TerminalKeyButton(
                label = "^Z",
                onClick = {
                    engine.sendKey(
                        keyCode = KeyEvent.KEYCODE_Z,
                        codepoint = 0,
                        metaState = KeyEvent.META_CTRL_ON,
                    )
                    focusRequester.requestFocus()
                },
            )
            TerminalKeyButton(
                label = "↑",
                onClick = {
                    engine.sendKey(KeyEvent.KEYCODE_DPAD_UP)
                    focusRequester.requestFocus()
                },
            )
            TerminalKeyButton(
                label = "↓",
                onClick = {
                    engine.sendKey(KeyEvent.KEYCODE_DPAD_DOWN)
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
        shape = RoundedCornerShape(6.dp),
        color = if (isActive) MaterialTheme.colorScheme.primary else GhosttyKeyBg,
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
                color = if (isActive) MaterialTheme.colorScheme.onPrimary else GhosttyText,
            )
        }
    }
}
