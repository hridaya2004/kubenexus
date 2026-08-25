package dev.hridaya.kubenexus.presentation.main

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hridaya.kubenexus.presentation.deployments.CreateDeploymentRoute
import dev.hridaya.kubenexus.presentation.deployments.CreateDeploymentUiEffect
import dev.hridaya.kubenexus.presentation.deployments.CreateDeploymentViewModel
import dev.hridaya.kubenexus.presentation.explore.ExploreRoute
import dev.hridaya.kubenexus.presentation.explore.ExploreViewModel
import dev.hridaya.kubenexus.presentation.home.HomeRoute
import dev.hridaya.kubenexus.presentation.home.HomeUiAction
import dev.hridaya.kubenexus.presentation.home.HomeUiEffect
import dev.hridaya.kubenexus.presentation.home.HomeViewModel
import dev.hridaya.kubenexus.presentation.home.ManageClustersScreen
import dev.hridaya.kubenexus.presentation.logcat.LogcatRoute
import dev.hridaya.kubenexus.presentation.logcat.LogcatViewModel
import dev.hridaya.kubenexus.presentation.navigation.Destination
import dev.hridaya.kubenexus.presentation.pods.PodsScreen
import dev.hridaya.kubenexus.presentation.pods.detail.PodDetailRoute
import dev.hridaya.kubenexus.presentation.pods.detail.PodDetailViewModel
import dev.hridaya.kubenexus.presentation.settings.SettingsScreen
import dev.hridaya.kubenexus.ui.theme.KubeNexusTheme

@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    var currentDestination by rememberSaveable { mutableStateOf(Destination.Home) }
    var isManagingClusters by rememberSaveable { mutableStateOf(false) }
    var isViewingPods by rememberSaveable { mutableStateOf(false) }
    var isViewingLogcat by rememberSaveable { mutableStateOf(false) }
    var isCreatingDeployment by rememberSaveable { mutableStateOf(false) }
    var selectedPodName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPodNamespace by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(homeViewModel.effects) {
        homeViewModel.effects.collect { effect ->
            if (effect is HomeUiEffect.NavigateToHome) {
                isManagingClusters = false
                isViewingPods = false
                isViewingLogcat = false
                isCreatingDeployment = false
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

        isCreatingDeployment -> {
            val context = LocalContext.current
            val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
            val createDeploymentViewModel: CreateDeploymentViewModel = hiltViewModel(
                key = "create_deployment",
                creationCallback = { factory: CreateDeploymentViewModel.Factory ->
                    factory.create(
                        clusterId = homeUiState.activeCluster?.id,
                        namespace = homeUiState.selectedNamespace
                            .takeIf { it.isNotBlank() && it != "All Namespaces" }
                            ?: "default",
                    )
                },
            )
            LaunchedEffect(createDeploymentViewModel.effects) {
                createDeploymentViewModel.effects.collect { effect ->
                    if (effect is CreateDeploymentUiEffect.Created) {
                        isCreatingDeployment = false
                        Toast.makeText(
                            context,
                            "Deployment '${effect.deploymentName}' created successfully",
                            Toast.LENGTH_SHORT,
                        ).show()
                        homeViewModel.onAction(HomeUiAction.RefreshWorkloads)
                    }
                }
            }
            BackHandler { isCreatingDeployment = false }
            CreateDeploymentRoute(
                viewModel = createDeploymentViewModel,
                availableNamespaces = homeUiState.availableNamespaces.filterNot { it == "All Namespaces" },
                onNavigateBack = { isCreatingDeployment = false },
                modifier = modifier,
            )
        }

        else -> {
            NavigationSuiteScaffold(
                navigationSuiteItems = {
                    Destination.topLevelDestinations.forEach { destination ->
                        item(
                            selected = currentDestination == destination,
                            onClick = { currentDestination = destination },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        destination.badgeCount?.let { count ->
                                            Badge {
                                                Text(text = "$count")
                                            }
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = if (currentDestination == destination) destination.selectedIcon else destination.unselectedIcon,
                                        contentDescription = destination.title,
                                    )
                                }
                            },
                            label = {
                                Text(text = destination.title)
                            },
                        )
                    }
                },
                modifier = modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when (currentDestination) {
                        Destination.Home -> {
                            HomeRoute(
                                viewModel = homeViewModel,
                                onNavigateToManageClusters = { isManagingClusters = true },
                                onNavigateToPods = { isViewingPods = true },
                                onNavigateToCreateDeployment = { isCreatingDeployment = true },
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

@Preview(showBackground = true)
@Composable
private fun MainScreenNavigationPreview() {
    KubeNexusTheme {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                Destination.topLevelDestinations.forEach { destination ->
                    item(
                        selected = destination == Destination.Home,
                        onClick = {},
                        icon = {
                            Icon(
                                imageVector = destination.selectedIcon,
                                contentDescription = destination.title,
                            )
                        },
                        label = {
                            Text(text = destination.title)
                        },
                    )
                }
            },
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}
