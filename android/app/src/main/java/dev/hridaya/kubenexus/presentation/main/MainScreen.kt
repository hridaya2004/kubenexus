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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hridaya.kubenexus.core.di.AppContainer
import dev.hridaya.kubenexus.presentation.explore.ExploreRoute
import dev.hridaya.kubenexus.presentation.explore.ExploreViewModel
import dev.hridaya.kubenexus.presentation.home.HomeRoute
import dev.hridaya.kubenexus.presentation.home.HomeUiEffect
import dev.hridaya.kubenexus.presentation.home.HomeViewModel
import dev.hridaya.kubenexus.presentation.home.ManageClustersScreen
import dev.hridaya.kubenexus.presentation.logcat.LogcatRoute
import dev.hridaya.kubenexus.presentation.logcat.LogcatViewModel
import dev.hridaya.kubenexus.presentation.navigation.AppNavigationBar
import dev.hridaya.kubenexus.presentation.navigation.Destination
import dev.hridaya.kubenexus.presentation.pods.PodsScreen
import dev.hridaya.kubenexus.presentation.pods.detail.PodDetailRoute
import dev.hridaya.kubenexus.presentation.pods.detail.PodDetailViewModel
import dev.hridaya.kubenexus.presentation.settings.SettingsScreen

@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    appContainer: AppContainer? = null,
) {
    var currentDestination by rememberSaveable { mutableStateOf(Destination.Home) }
    var isManagingClusters by rememberSaveable { mutableStateOf(false) }
    var isViewingPods by rememberSaveable { mutableStateOf(false) }
    var isViewingLogcat by rememberSaveable { mutableStateOf(false) }
    var selectedPodName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPodNamespace by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(homeViewModel.effects) {
        homeViewModel.effects.collect { effect ->
            if (effect is HomeUiEffect.NavigateToHome) {
                isManagingClusters = false
                isViewingPods = false
                isViewingLogcat = false
                selectedPodName = null
                selectedPodNamespace = null
                currentDestination = Destination.Home
            }
        }
    }

    when {
        selectedPodName != null && selectedPodNamespace != null -> {
            val podName = selectedPodName!!
            val podNamespace = selectedPodNamespace!!
            val podDetailViewModel: PodDetailViewModel = hiltViewModel(
                key = "pod_detail_${podNamespace}_$podName",
                creationCallback = { factory: PodDetailViewModel.Factory ->
                    factory.create(
                        podName = podName,
                        namespace = podNamespace,
                    )
                },
            )
            BackHandler {
                selectedPodName = null
                selectedPodNamespace = null
            }
            PodDetailRoute(
                viewModel = podDetailViewModel,
                onNavigateBack = {
                    selectedPodName = null
                    selectedPodNamespace = null
                },
                modifier = modifier,
            )
        }

        isManagingClusters -> {
            val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
            BackHandler { isManagingClusters = false }
            ManageClustersScreen(
                uiState = uiState,
                onAction = homeViewModel::onAction,
                onNavigateBack = { isManagingClusters = false },
                modifier = modifier,
            )
        }

        isViewingPods -> {
            val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
            BackHandler { isViewingPods = false }
            PodsScreen(
                uiState = uiState,
                onAction = homeViewModel::onAction,
                onNavigateBack = { isViewingPods = false },
                onNavigateToPodDetail = { pod ->
                    selectedPodName = pod.name
                    selectedPodNamespace = pod.namespace
                },
                modifier = modifier,
            )
        }

        isViewingLogcat -> {
            val logcatViewModel: LogcatViewModel = hiltViewModel()
            BackHandler { isViewingLogcat = false }
            LogcatRoute(
                viewModel = logcatViewModel,
                onNavigateBack = { isViewingLogcat = false },
                modifier = modifier,
            )
        }

        else -> {
            Scaffold(
                modifier = modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                bottomBar = {
                    AppNavigationBar(
                        destinations = Destination.topLevelDestinations,
                        currentDestination = currentDestination,
                        onDestinationSelected = { destination ->
                            currentDestination = destination
                        },
                    )
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding()),
                ) {
                    when (currentDestination) {
                        Destination.Home -> {
                            HomeRoute(
                                viewModel = homeViewModel,
                                onNavigateToManageClusters = { isManagingClusters = true },
                                onNavigateToPods = { isViewingPods = true },
                            )
                        }

                        Destination.Explore -> {
                            val exploreViewModel: ExploreViewModel = hiltViewModel()
                            ExploreRoute(
                                viewModel = exploreViewModel,
                            )
                        }


                        Destination.Settings -> {
                            SettingsScreen(
                                onNavigateToLogcat = { isViewingLogcat = true },
                            )
                        }
                    }
                }
            }
        }
    }
}
