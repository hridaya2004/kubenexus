package dev.hridaya.kubenexus.core.di

import android.content.Context
import dev.hridaya.kubenexus.core.common.dispatcher.DefaultDispatcherProvider
import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.network.ConnectivityNetworkMonitor
import dev.hridaya.kubenexus.core.common.network.NetworkMonitor
import dev.hridaya.kubenexus.core.nativebridge.KubeNexusNativeBridge
import dev.hridaya.kubenexus.core.nativebridge.KubeNexusNativeBridgeImpl
import dev.hridaya.kubenexus.core.security.AndroidKeystoreKubeconfigEncryptor
import dev.hridaya.kubenexus.core.security.KubeconfigEncryptor
import dev.hridaya.kubenexus.data.kubeconfig.ClusterConnectionTester
import dev.hridaya.kubenexus.data.repository.ClusterRepositoryImpl
import dev.hridaya.kubenexus.data.repository.PodRepositoryImpl
import dev.hridaya.kubenexus.data.repository.ThemePreferencesRepositoryImpl
import dev.hridaya.kubenexus.data.source.local.DefaultLogcatLocalDataSource
import dev.hridaya.kubenexus.data.source.local.KubeNexusDatabase
import dev.hridaya.kubenexus.data.source.local.LogcatLocalDataSource
import dev.hridaya.kubenexus.data.source.local.SharedPrefsThemePreferencesDataSource
import dev.hridaya.kubenexus.data.source.local.ThemePreferencesLocalDataSource
import dev.hridaya.kubenexus.domain.repository.ClusterRepository
import dev.hridaya.kubenexus.domain.repository.LogcatRepository
import dev.hridaya.kubenexus.domain.repository.PodRepository
import dev.hridaya.kubenexus.domain.repository.ThemePreferencesRepository
import dev.hridaya.kubenexus.domain.usecase.AddClusterUseCase
import dev.hridaya.kubenexus.domain.usecase.ClearLogcatUseCase
import dev.hridaya.kubenexus.domain.usecase.DeleteClusterUseCase
import dev.hridaya.kubenexus.domain.usecase.DeleteNamespaceUseCase
import dev.hridaya.kubenexus.domain.usecase.DeletePodUseCase
import dev.hridaya.kubenexus.domain.usecase.DescribePodUseCase
import dev.hridaya.kubenexus.domain.usecase.DumpLogcatUseCase
import dev.hridaya.kubenexus.domain.usecase.ExecPodCommandUseCase
import dev.hridaya.kubenexus.domain.usecase.GetActiveClusterUseCase
import dev.hridaya.kubenexus.domain.usecase.GetClustersUseCase
import dev.hridaya.kubenexus.domain.usecase.GetLastRefreshedUseCase
import dev.hridaya.kubenexus.domain.usecase.GetLogcatStreamUseCase
import dev.hridaya.kubenexus.domain.usecase.GetNamespacesUseCase
import dev.hridaya.kubenexus.domain.usecase.GetPodLogsUseCase
import dev.hridaya.kubenexus.domain.usecase.GetPodsUseCase
import dev.hridaya.kubenexus.domain.usecase.RefreshWorkloadsUseCase
import dev.hridaya.kubenexus.domain.usecase.SetActiveClusterUseCase
import dev.hridaya.kubenexus.domain.usecase.StartExecSessionUseCase
import dev.hridaya.kubenexus.domain.usecase.StartPodTerminalUseCase
import dev.hridaya.kubenexus.domain.usecase.StreamPodLogsUseCase
import dev.hridaya.kubenexus.domain.usecase.TestClusterConnectionUseCase
import dev.hridaya.kubenexus.domain.usecase.UpdateClusterNameUseCase

interface AppContainer {
    val dispatcherProvider: DispatcherProvider
    val nativeBridge: KubeNexusNativeBridge
    val clusterRepository: ClusterRepository
    val podRepository: PodRepository
    val themePreferencesRepository: ThemePreferencesRepository
    val getClustersUseCase: GetClustersUseCase
    val getActiveClusterUseCase: GetActiveClusterUseCase
    val getPodsUseCase: GetPodsUseCase
    val getNamespacesUseCase: GetNamespacesUseCase
    val getLastRefreshedUseCase: GetLastRefreshedUseCase
    val refreshWorkloadsUseCase: RefreshWorkloadsUseCase
    val describePodUseCase: DescribePodUseCase
    val deletePodUseCase: DeletePodUseCase
    val deleteNamespaceUseCase: DeleteNamespaceUseCase
    val getPodLogsUseCase: GetPodLogsUseCase
    val streamPodLogsUseCase: StreamPodLogsUseCase
    val execPodCommandUseCase: ExecPodCommandUseCase
    val startPodTerminalUseCase: StartPodTerminalUseCase
    val startExecSessionUseCase: StartExecSessionUseCase
    val addClusterUseCase: AddClusterUseCase
    val setActiveClusterUseCase: SetActiveClusterUseCase
    val deleteClusterUseCase: DeleteClusterUseCase
    val updateClusterNameUseCase: UpdateClusterNameUseCase
    val testClusterConnectionUseCase: TestClusterConnectionUseCase
    val logcatRepository: LogcatRepository
    val getLogcatStreamUseCase: GetLogcatStreamUseCase
    val dumpLogcatUseCase: DumpLogcatUseCase
    val clearLogcatUseCase: ClearLogcatUseCase
    val networkMonitor: NetworkMonitor
    val kubeconfigEncryptor: KubeconfigEncryptor
}

class DefaultAppContainer(private val appContext: Context) : AppContainer {

    override val dispatcherProvider: DispatcherProvider by lazy {
        DefaultDispatcherProvider()
    }

    override val kubeconfigEncryptor: KubeconfigEncryptor by lazy {
        AndroidKeystoreKubeconfigEncryptor()
    }

    override val nativeBridge: KubeNexusNativeBridge by lazy {
        KubeNexusNativeBridgeImpl(appContext)
    }

    private val database: KubeNexusDatabase by lazy {
        KubeNexusDatabase.getInstance(appContext)
    }

    private val clusterConnectionTester: ClusterConnectionTester by lazy {
        ClusterConnectionTester(nativeBridge)
    }

    override val clusterRepository: ClusterRepository by lazy {
        ClusterRepositoryImpl(
            clusterDao = database.clusterDao(),
            connectionTester = clusterConnectionTester,
            encryptor = kubeconfigEncryptor,
            dispatcherProvider = dispatcherProvider,
        )
    }

    override val podRepository: PodRepository by lazy {
        PodRepositoryImpl(
            clusterDao = database.clusterDao(),
            podDao = database.podDao(),
            namespaceDao = database.namespaceDao(),
            nativeBridge = nativeBridge,
            encryptor = kubeconfigEncryptor,
            dispatcherProvider = dispatcherProvider,
        )
    }

    private val themePreferencesLocalDataSource: ThemePreferencesLocalDataSource by lazy {
        SharedPrefsThemePreferencesDataSource(
            context = appContext,
            dispatcherProvider = dispatcherProvider,
        )
    }

    override val themePreferencesRepository: ThemePreferencesRepository by lazy {
        ThemePreferencesRepositoryImpl(
            localDataSource = themePreferencesLocalDataSource,
        )
    }

    override val getClustersUseCase: GetClustersUseCase by lazy {
        GetClustersUseCase(
            repository = clusterRepository,
            dispatcherProvider = dispatcherProvider,
        )
    }

    override val getActiveClusterUseCase: GetActiveClusterUseCase by lazy {
        GetActiveClusterUseCase(
            repository = clusterRepository,
            dispatcherProvider = dispatcherProvider,
        )
    }

    override val getPodsUseCase: GetPodsUseCase by lazy {
        GetPodsUseCase(
            podRepository = podRepository,
        )
    }

    override val getNamespacesUseCase: GetNamespacesUseCase by lazy {
        GetNamespacesUseCase(
            podRepository = podRepository,
        )
    }

    override val getLastRefreshedUseCase: GetLastRefreshedUseCase by lazy {
        GetLastRefreshedUseCase(
            podRepository = podRepository,
        )
    }

    override val refreshWorkloadsUseCase: RefreshWorkloadsUseCase by lazy {
        RefreshWorkloadsUseCase(
            podRepository = podRepository,
        )
    }

    override val describePodUseCase: DescribePodUseCase by lazy {
        DescribePodUseCase(
            podRepository = podRepository,
        )
    }

    override val deletePodUseCase: DeletePodUseCase by lazy {
        DeletePodUseCase(
            podRepository = podRepository,
        )
    }

    override val deleteNamespaceUseCase: DeleteNamespaceUseCase by lazy {
        DeleteNamespaceUseCase(
            podRepository = podRepository,
        )
    }

    override val getPodLogsUseCase: GetPodLogsUseCase by lazy {
        GetPodLogsUseCase(
            podRepository = podRepository,
        )
    }

    override val streamPodLogsUseCase: StreamPodLogsUseCase by lazy {
        StreamPodLogsUseCase(
            podRepository = podRepository,
        )
    }

    override val execPodCommandUseCase: ExecPodCommandUseCase by lazy {
        ExecPodCommandUseCase(
            podRepository = podRepository,
        )
    }

    override val startPodTerminalUseCase: StartPodTerminalUseCase by lazy {
        StartPodTerminalUseCase(
            podRepository = podRepository,
        )
    }

    override val startExecSessionUseCase: StartExecSessionUseCase by lazy {
        StartExecSessionUseCase(
            podRepository = podRepository,
        )
    }

    override val addClusterUseCase: AddClusterUseCase by lazy {
        AddClusterUseCase(
            repository = clusterRepository,
            dispatcherProvider = dispatcherProvider,
        )
    }

    override val setActiveClusterUseCase: SetActiveClusterUseCase by lazy {
        SetActiveClusterUseCase(
            repository = clusterRepository,
            dispatcherProvider = dispatcherProvider,
        )
    }

    override val deleteClusterUseCase: DeleteClusterUseCase by lazy {
        DeleteClusterUseCase(
            repository = clusterRepository,
            dispatcherProvider = dispatcherProvider,
        )
    }

    override val updateClusterNameUseCase: UpdateClusterNameUseCase by lazy {
        UpdateClusterNameUseCase(
            clusterRepository = clusterRepository,
        )
    }

    override val testClusterConnectionUseCase: TestClusterConnectionUseCase by lazy {
        TestClusterConnectionUseCase(
            repository = clusterRepository,
            dispatcherProvider = dispatcherProvider,
        )
    }

    private val logcatLocalDataSource: LogcatLocalDataSource by lazy {
        DefaultLogcatLocalDataSource(
            dispatcherProvider = dispatcherProvider,
        )
    }

    override val logcatRepository: LogcatRepository by lazy {
        dev.hridaya.kubenexus.data.repository.LogcatRepositoryImpl(
            localDataSource = logcatLocalDataSource,
            dispatcherProvider = dispatcherProvider,
        )
    }

    override val getLogcatStreamUseCase: GetLogcatStreamUseCase by lazy {
        GetLogcatStreamUseCase(
            repository = logcatRepository,
        )
    }

    override val dumpLogcatUseCase: DumpLogcatUseCase by lazy {
        DumpLogcatUseCase(
            repository = logcatRepository,
        )
    }

    override val clearLogcatUseCase: ClearLogcatUseCase by lazy {
        ClearLogcatUseCase(
            repository = logcatRepository,
        )
    }

    override val networkMonitor: NetworkMonitor by lazy {
        ConnectivityNetworkMonitor(
            context = appContext,
            dispatcherProvider = dispatcherProvider,
        )
    }
}
