package dev.hridaya.kubenexus.presentation.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hridaya.kubenexus.presentation.explore.ExploreScreen
import dev.hridaya.kubenexus.presentation.home.HomeRoute
import dev.hridaya.kubenexus.presentation.home.HomeUiEffect
import dev.hridaya.kubenexus.presentation.home.HomeViewModel
import dev.hridaya.kubenexus.presentation.home.ManageClustersScreen
import dev.hridaya.kubenexus.presentation.navigation.AppNavigationBar
import dev.hridaya.kubenexus.presentation.navigation.Destination
import dev.hridaya.kubenexus.presentation.settings.SettingsScreen

@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    var currentDestination by rememberSaveable { mutableStateOf(Destination.Home) }
    var isManagingClusters by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(homeViewModel.effects) {
        homeViewModel.effects.collect { effect ->
            if (effect is HomeUiEffect.NavigateToHome) {
                isManagingClusters = false
                currentDestination = Destination.Home
            }
        }
    }

    if (isManagingClusters) {
        val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
        BackHandler { isManagingClusters = false }
        ManageClustersScreen(
            uiState = uiState,
            onAction = homeViewModel::onAction,
            onNavigateBack = { isManagingClusters = false },
            modifier = modifier
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                AppNavigationBar(
                    destinations = Destination.topLevelDestinations,
                    currentDestination = currentDestination,
                    onDestinationSelected = { destination ->
                        currentDestination = destination
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                when (currentDestination) {
                    Destination.Home -> {
                        HomeRoute(
                            viewModel = homeViewModel,
                            onNavigateToManageClusters = { isManagingClusters = true }
                        )
                    }

                    Destination.Explore -> {
                        ExploreScreen()
                    }

                    Destination.Settings -> {
                        SettingsScreen()
                    }
                }
            }
        }
    }
}
