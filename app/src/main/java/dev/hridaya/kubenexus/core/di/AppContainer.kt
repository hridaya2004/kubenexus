package dev.hridaya.kubenexus.core.di

import android.content.Context
import dev.hridaya.kubenexus.core.common.dispatcher.DefaultDispatcherProvider
import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.nativebridge.KubeNexusNativeBridge
import dev.hridaya.kubenexus.core.nativebridge.KubeNexusNativeBridgeImpl
import dev.hridaya.kubenexus.data.kubeconfig.ClusterConnectionTester
import dev.hridaya.kubenexus.data.repository.ClusterRepositoryImpl
import dev.hridaya.kubenexus.data.repository.ThemePreferencesRepositoryImpl
import dev.hridaya.kubenexus.data.source.local.KubeNexusDatabase
import dev.hridaya.kubenexus.data.source.local.SharedPrefsThemePreferencesDataSource
import dev.hridaya.kubenexus.data.source.local.ThemePreferencesLocalDataSource
import dev.hridaya.kubenexus.domain.repository.ClusterRepository
import dev.hridaya.kubenexus.domain.repository.ThemePreferencesRepository
import dev.hridaya.kubenexus.domain.usecase.AddClusterUseCase
import dev.hridaya.kubenexus.domain.usecase.DeleteClusterUseCase
import dev.hridaya.kubenexus.domain.usecase.GetActiveClusterUseCase
import dev.hridaya.kubenexus.domain.usecase.GetClustersUseCase
import dev.hridaya.kubenexus.domain.usecase.SetActiveClusterUseCase
import dev.hridaya.kubenexus.domain.usecase.TestClusterConnectionUseCase

interface AppContainer {
    val dispatcherProvider: DispatcherProvider
    val nativeBridge: KubeNexusNativeBridge
    val clusterRepository: ClusterRepository
    val themePreferencesRepository: ThemePreferencesRepository
    val getClustersUseCase: GetClustersUseCase
    val getActiveClusterUseCase: GetActiveClusterUseCase
    val addClusterUseCase: AddClusterUseCase
    val setActiveClusterUseCase: SetActiveClusterUseCase
    val deleteClusterUseCase: DeleteClusterUseCase
    val testClusterConnectionUseCase: TestClusterConnectionUseCase
}

class DefaultAppContainer(
    private val appContext: Context
) : AppContainer {

    override val dispatcherProvider: DispatcherProvider by lazy {
        DefaultDispatcherProvider()
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
            dispatcherProvider = dispatcherProvider
        )
    }

    private val themePreferencesLocalDataSource: ThemePreferencesLocalDataSource by lazy {
        SharedPrefsThemePreferencesDataSource(
            context = appContext,
            dispatcherProvider = dispatcherProvider
        )
    }

    override val themePreferencesRepository: ThemePreferencesRepository by lazy {
        ThemePreferencesRepositoryImpl(
            localDataSource = themePreferencesLocalDataSource
        )
    }

    override val getClustersUseCase: GetClustersUseCase by lazy {
        GetClustersUseCase(
            repository = clusterRepository,
            dispatcherProvider = dispatcherProvider
        )
    }

    override val getActiveClusterUseCase: GetActiveClusterUseCase by lazy {
        GetActiveClusterUseCase(
            repository = clusterRepository,
            dispatcherProvider = dispatcherProvider
        )
    }

    override val addClusterUseCase: AddClusterUseCase by lazy {
        AddClusterUseCase(
            repository = clusterRepository,
            dispatcherProvider = dispatcherProvider
        )
    }

    override val setActiveClusterUseCase: SetActiveClusterUseCase by lazy {
        SetActiveClusterUseCase(
            repository = clusterRepository,
            dispatcherProvider = dispatcherProvider
        )
    }

    override val deleteClusterUseCase: DeleteClusterUseCase by lazy {
        DeleteClusterUseCase(
            repository = clusterRepository,
            dispatcherProvider = dispatcherProvider
        )
    }

    override val testClusterConnectionUseCase: TestClusterConnectionUseCase by lazy {
        TestClusterConnectionUseCase(
            repository = clusterRepository,
            dispatcherProvider = dispatcherProvider
        )
    }
}
