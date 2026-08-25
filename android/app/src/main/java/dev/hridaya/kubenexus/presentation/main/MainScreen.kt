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
import dev.hridaya.kubenexus.presentation.deployments.DeploymentsRoute
import dev.hridaya.kubenexus.presentation.deployments.DeploymentsViewModel
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
import dev.hridaya.kubenexus.presentation.pods.create.CreatePodRoute
import dev.hridaya.kubenexus.presentation.pods.create.CreatePodUiEffect
import dev.hridaya.kubenexus.presentation.pods.create.CreatePodViewModel
import dev.hridaya.kubenexus.presentation.pods.detail.PodDetailRoute
import dev.hridaya.kubenexus.presentation.pods.detail.PodDetailViewModel
import dev.hridaya.kubenexus.presentation.services.create.CreateServiceRoute
import dev.hridaya.kubenexus.presentation.services.create.CreateServiceUiEffect
import dev.hridaya.kubenexus.presentation.services.create.CreateServiceViewModel
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
    var isViewingDeployments by rememberSaveable { mutableStateOf(false) }
    var isViewingLogcat by rememberSaveable { mutableStateOf(false) }
    var isCreatingDeployment by rememberSaveable { mutableStateOf(false) }
    var isCreatingPod by rememberSaveable { mutableStateOf(false) }
    var isCreatingService by rememberSaveable { mutableStateOf(false) }
    var selectedPodName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPodNamespace by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(homeViewModel.effects) {
        homeViewModel.effects.collect { effect ->
            if (effect is HomeUiEffect.NavigateToHome) {
                isManagingClusters = false
                isViewingPods = false
                isViewingDeployments = false
                isViewingLogcat = false
                isCreatingDeployment = false
                isCreatingPod = false
                isCreatingService = false
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

        isViewingDeployments -> {
            val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
            val deploymentsViewModel: DeploymentsViewModel = hiltViewModel(
                key = "deployments_list",
                creationCallback = { factory: DeploymentsViewModel.Factory ->
                    factory.create(
                        clusterId = homeUiState.activeCluster?.id,
                        namespace = homeUiState.selectedNamespace
                            .takeIf { it.isNotBlank() && it != "All Namespaces" },
                    )
                },
            )
            BackHandler { isViewingDeployments = false }
            DeploymentsRoute(
                viewModel = deploymentsViewModel,
                onNavigateBack = { isViewingDeployments = false },
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
                        availableNamespaces = homeUiState.availableNamespaces,
                    )
                },
            )
            LaunchedEffect(createDeploymentViewModel.effects) {
                createDeploymentViewModel.effects.collect { effect ->
                    when (effect) {
                        is CreateDeploymentUiEffect.Created -> {
                            isCreatingDeployment = false
                            Toast.makeText(
                                context,
                                "Deployment '${effect.deploymentName}' created successfully",
                                Toast.LENGTH_SHORT,
                            ).show()
                            homeViewModel.onAction(HomeUiAction.RefreshWorkloads)
                        }

                        is CreateDeploymentUiEffect.NamespaceCreated -> {
                            Toast.makeText(context, "Namespace created", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            BackHandler { isCreatingDeployment = false }
            CreateDeploymentRoute(
                viewModel = createDeploymentViewModel,
                onNavigateBack = { isCreatingDeployment = false },
                modifier = modifier,
            )
        }

        isCreatingPod -> {
            val context = LocalContext.current
            val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
            val createPodViewModel: CreatePodViewModel = hiltViewModel(
                key = "create_pod",
                creationCallback = { factory: CreatePodViewModel.Factory ->
                    factory.create(
                        clusterId = homeUiState.activeCluster?.id,
                        namespace = homeUiState.selectedNamespace
                            .takeIf { it.isNotBlank() && it != "All Namespaces" }
                            ?: "default",
                        availableNamespaces = homeUiState.availableNamespaces,
                    )
                },
            )
            LaunchedEffect(createPodViewModel.effects) {
                createPodViewModel.effects.collect { effect ->
                    when (effect) {
                        is CreatePodUiEffect.Created -> {
                            isCreatingPod = false
                            Toast.makeText(
                                context,
                                "Pod '${effect.podName}' created successfully",
                                Toast.LENGTH_SHORT,
                            ).show()
                            homeViewModel.onAction(HomeUiAction.RefreshWorkloads)
                        }

                        is CreatePodUiEffect.NamespaceCreated -> {
                            Toast.makeText(context, "Namespace created", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            BackHandler { isCreatingPod = false }
            CreatePodRoute(
                viewModel = createPodViewModel,
                onNavigateBack = { isCreatingPod = false },
                modifier = modifier,
            )
        }

        isCreatingService -> {
            val context = LocalContext.current
            val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
            val createServiceViewModel: CreateServiceViewModel = hiltViewModel(
                key = "create_service",
                creationCallback = { factory: CreateServiceViewModel.Factory ->
                    factory.create(
                        clusterId = homeUiState.activeCluster?.id,
                        namespace = homeUiState.selectedNamespace
                            .takeIf { it.isNotBlank() && it != "All Namespaces" }
                            ?: "default",
                        availableNamespaces = homeUiState.availableNamespaces,
                    )
                },
            )
            LaunchedEffect(createServiceViewModel.effects) {
                createServiceViewModel.effects.collect { effect ->
                    when (effect) {
                        is CreateServiceUiEffect.Created -> {
                            isCreatingService = false
                            Toast.makeText(
                                context,
                                "Service '${effect.serviceName}' created successfully",
                                Toast.LENGTH_SHORT,
                            ).show()
                            homeViewModel.onAction(HomeUiAction.RefreshWorkloads)
                        }

                        is CreateServiceUiEffect.NamespaceCreated -> {
                            Toast.makeText(context, "Namespace created", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            BackHandler { isCreatingService = false }
            CreateServiceRoute(
                viewModel = createServiceViewModel,
                onNavigateBack = { isCreatingService = false },
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
                                onNavigateToDeployments = { isViewingDeployments = true },
                                onNavigateToCreatePod = { isCreatingPod = true },
                                onNavigateToCreateDeployment = { isCreatingDeployment = true },
                                onNavigateToCreateService = { isCreatingService = true },
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
