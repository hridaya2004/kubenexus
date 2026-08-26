package dev.hridaya.kubenexus.presentation.portforward.sessions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The one seam between a host surface and the sessions state: hosts obtain
 * their (shared, MainScreen-scoped) [PortForwardSessionsViewModel] via
 * hiltViewModel() and read state through this function. If the collection
 * strategy ever changes, this is the only file hosts need to revisit.
 */
@Composable
internal fun rememberPortForwardSessionsState(
    viewModel: PortForwardSessionsViewModel,
): State<PortForwardSessionsUiState> = viewModel.uiState.collectAsStateWithLifecycle()
