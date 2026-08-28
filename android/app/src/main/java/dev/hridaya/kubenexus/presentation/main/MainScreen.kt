package dev.hridaya.kubenexus.presentation.main

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hridaya.kubenexus.presentation.deployments.DeploymentsRoute
import dev.hridaya.kubenexus.presentation.deployments.DeploymentsViewModel
import dev.hridaya.kubenexus.presentation.deployments.detail.DeploymentDetailRoute
import dev.hridaya.kubenexus.presentation.deployments.detail.DeploymentDetailViewModel
import dev.hridaya.kubenexus.presentation.home.HomeUiEffect
import dev.hridaya.kubenexus.presentation.home.HomeViewModel
import dev.hridaya.kubenexus.presentation.home.ManageClustersScreen
import dev.hridaya.kubenexus.presentation.logcat.LogcatRoute
import dev.hridaya.kubenexus.presentation.logcat.LogcatViewModel
import dev.hridaya.kubenexus.presentation.navigation.Destination
import dev.hridaya.kubenexus.presentation.pods.PodsScreen
import dev.hridaya.kubenexus.presentation.pods.detail.PodDetailRoute
import dev.hridaya.kubenexus.presentation.pods.detail.PodDetailViewModel
import dev.hridaya.kubenexus.presentation.portforward.sessions.PortForwardSessionsViewModel
import dev.hridaya.kubenexus.presentation.portforward.sessions.rememberPortForwardSessionsState
import dev.hridaya.kubenexus.presentation.services.ServicesRoute
import dev.hridaya.kubenexus.presentation.services.ServicesViewModel
import dev.hridaya.kubenexus.presentation.services.detail.ServiceDetailRoute
import dev.hridaya.kubenexus.presentation.services.detail.ServiceDetailViewModel

@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    var currentDestination by rememberSaveable { mutableStateOf(Destination.Home) }
    var isManagingClusters by rememberSaveable { mutableStateOf(false) }
    var isViewingPods by rememberSaveable { mutableStateOf(false) }
    var isViewingDeployments by rememberSaveable { mutableStateOf(false) }
    var isViewingServices by rememberSaveable { mutableStateOf(false) }
    var isViewingLogcat by rememberSaveable { mutableStateOf(false) }
    var isCreatingDeployment by rememberSaveable { mutableStateOf(false) }
    var isCreatingPod by rememberSaveable { mutableStateOf(false) }
    var isCreatingService by rememberSaveable { mutableStateOf(false) }
    var selectedPodName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPodNamespace by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDeploymentName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDeploymentNamespace by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedServiceName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedServiceNamespace by rememberSaveable { mutableStateOf<String?>(null) }
    val portForwardSessionsViewModel: PortForwardSessionsViewModel = hiltViewModel()
    val portForwardSessionsState by rememberPortForwardSessionsState(portForwardSessionsViewModel)
    var showPortForwardSessions by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(homeViewModel.effects) {
        homeViewModel.effects.collect { effect ->
            if (effect is HomeUiEffect.NavigateToHome) {
                isManagingClusters = false
                isViewingPods = false
                isViewingDeployments = false
                isViewingServices = false
                isViewingLogcat = false
                isCreatingDeployment = false
                isCreatingPod = false
                isCreatingService = false
                selectedPodName = null
                selectedPodNamespace = null
                selectedDeploymentName = null
                selectedDeploymentNamespace = null
                selectedServiceName = null
                selectedServiceNamespace = null
                showPortForwardSessions = false
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

        selectedDeploymentName != null && selectedDeploymentNamespace != null -> {
            val deploymentName = selectedDeploymentName!!
            val deploymentNamespace = selectedDeploymentNamespace!!
            val deploymentDetailViewModel: DeploymentDetailViewModel = hiltViewModel(
                key = "deployment_detail_${deploymentNamespace}_$deploymentName",
                creationCallback = { factory: DeploymentDetailViewModel.Factory ->
                    factory.create(
                        deploymentName = deploymentName,
                        namespace = deploymentNamespace,
                    )
                },
            )
            BackHandler {
                selectedDeploymentName = null
                selectedDeploymentNamespace = null
            }
            DeploymentDetailRoute(
                viewModel = deploymentDetailViewModel,
                onNavigateBack = {
                    selectedDeploymentName = null
                    selectedDeploymentNamespace = null
                },
                modifier = modifier,
            )
        }

        selectedServiceName != null && selectedServiceNamespace != null -> {
            val serviceName = selectedServiceName!!
            val serviceNamespace = selectedServiceNamespace!!
            val serviceDetailViewModel: ServiceDetailViewModel = hiltViewModel(
                key = "service_detail_${serviceNamespace}_$serviceName",
                creationCallback = { factory: ServiceDetailViewModel.Factory ->
                    factory.create(
                        serviceName = serviceName,
                        namespace = serviceNamespace,
                    )
                },
            )
            BackHandler {
                selectedServiceName = null
                selectedServiceNamespace = null
            }
            ServiceDetailRoute(
                viewModel = serviceDetailViewModel,
                onNavigateBack = {
                    selectedServiceName = null
                    selectedServiceNamespace = null
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
            val clusterId = homeUiState.activeCluster?.id
            val deploymentsViewModel: DeploymentsViewModel = hiltViewModel(
                key = "deployments_list_${clusterId.orEmpty()}",
                creationCallback = { factory: DeploymentsViewModel.Factory ->
                    factory.create(
                        clusterId = clusterId,
                        namespace = homeUiState.selectedNamespace
                            .takeIf { it.isNotBlank() && it != "All Namespaces" },
                    )
                },
            )
            BackHandler { isViewingDeployments = false }
            DeploymentsRoute(
                viewModel = deploymentsViewModel,
                onNavigateBack = { isViewingDeployments = false },
                onDeploymentClick = { deployment ->
                    selectedDeploymentName = deployment.name
                    selectedDeploymentNamespace = deployment.namespace
                },
                modifier = modifier,
            )
        }

        isViewingServices -> {
            val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
            val clusterId = homeUiState.activeCluster?.id
            val servicesViewModel: ServicesViewModel = hiltViewModel(
                key = "services_list_${clusterId.orEmpty()}",
                creationCallback = { factory: ServicesViewModel.Factory ->
                    factory.create(
                        clusterId = clusterId,
                        namespace = homeUiState.selectedNamespace
                            .takeIf { it.isNotBlank() && it != "All Namespaces" },
                    )
                },
            )
            BackHandler { isViewingServices = false }
            ServicesRoute(
                viewModel = servicesViewModel,
                onNavigateBack = { isViewingServices = false },
                onServiceClick = { service ->
                    selectedServiceName = service.name
                    selectedServiceNamespace = service.namespace
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

        isCreatingDeployment -> CreateDeploymentOverlay(
            homeViewModel = homeViewModel,
            onDismiss = { isCreatingDeployment = false },
            modifier = modifier,
        )

        isCreatingPod -> CreatePodOverlay(
            homeViewModel = homeViewModel,
            onDismiss = { isCreatingPod = false },
            modifier = modifier,
        )

        isCreatingService -> CreateServiceOverlay(
            homeViewModel = homeViewModel,
            onDismiss = { isCreatingService = false },
            modifier = modifier,
        )

        else -> {
            MainTopLevelScaffold(
                homeViewModel = homeViewModel,
                currentDestination = currentDestination,
                onSelectDestination = { currentDestination = it },
                onNavigateToManageClusters = { isManagingClusters = true },
                onNavigateToPods = { isViewingPods = true },
                onNavigateToDeployments = { isViewingDeployments = true },
                onNavigateToServices = { isViewingServices = true },
                onNavigateToCreatePod = { isCreatingPod = true },
                onNavigateToCreateDeployment = { isCreatingDeployment = true },
                onNavigateToCreateService = { isCreatingService = true },
                onNavigateToLogcat = { isViewingLogcat = true },
                activeForwardCount = portForwardSessionsState.activeCount,
                onOpenPortForwardSessions = { showPortForwardSessions = true },
                modifier = modifier,
            )
        }
    }

    if (showPortForwardSessions) {
        MainPortForwardSessionsEntry(
            onDismiss = { showPortForwardSessions = false },
        )
    }
}

