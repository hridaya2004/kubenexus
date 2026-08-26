package dev.hridaya.kubenexus.presentation.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dev.hridaya.kubenexus.presentation.portforward.sessions.PortForwardSessionsSheet
import dev.hridaya.kubenexus.presentation.portforward.sessions.PortForwardSessionsViewModel
import dev.hridaya.kubenexus.presentation.portforward.sessions.rememberPortForwardSessionsState

/**
 * Host-side entry point for the global port-forward sessions sheet. The
 * ViewModel is resolved with hiltViewModel() against MainScreen's store, so
 * the instance (and the manager flow collection) is shared with the badge in
 * [MainTopLevelScaffold]. Sheet visibility stays hoisted in MainScreen.
 */
@Composable
internal fun MainPortForwardSessionsEntry(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: PortForwardSessionsViewModel = hiltViewModel()
    val uiState = rememberPortForwardSessionsState(viewModel)
    PortForwardSessionsSheet(
        uiState = uiState.value,
        onAction = viewModel::onAction,
        onDismiss = onDismiss,
        modifier = modifier,
    )
}
