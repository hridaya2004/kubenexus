package dev.hridaya.kubenexus.core.di

import android.content.Context
import dev.hridaya.kubenexus.core.common.dispatcher.DefaultDispatcherProvider
import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.data.repository.SampleRepositoryImpl
import dev.hridaya.kubenexus.data.repository.ThemePreferencesRepositoryImpl
import dev.hridaya.kubenexus.data.source.local.InMemorySampleLocalDataSource
import dev.hridaya.kubenexus.data.source.local.SampleLocalDataSource
import dev.hridaya.kubenexus.data.source.local.SharedPrefsThemePreferencesDataSource
import dev.hridaya.kubenexus.data.source.local.ThemePreferencesLocalDataSource
import dev.hridaya.kubenexus.data.source.remote.SampleRemoteDataSource
import dev.hridaya.kubenexus.data.source.remote.SimulatedSampleRemoteDataSource
import dev.hridaya.kubenexus.domain.repository.SampleRepository
import dev.hridaya.kubenexus.domain.repository.ThemePreferencesRepository
import dev.hridaya.kubenexus.domain.usecase.AddSampleItemUseCase
import dev.hridaya.kubenexus.domain.usecase.GetSampleItemsUseCase
import dev.hridaya.kubenexus.domain.usecase.RefreshSampleItemsUseCase

interface AppContainer {
    val dispatcherProvider: DispatcherProvider
    val sampleRepository: SampleRepository
    val themePreferencesRepository: ThemePreferencesRepository
    val getSampleItemsUseCase: GetSampleItemsUseCase
    val addSampleItemUseCase: AddSampleItemUseCase
    val refreshSampleItemsUseCase: RefreshSampleItemsUseCase
}

class DefaultAppContainer(
    private val appContext: Context
) : AppContainer {

    override val dispatcherProvider: DispatcherProvider by lazy {
        DefaultDispatcherProvider()
    }

    private val localDataSource: SampleLocalDataSource by lazy {
        InMemorySampleLocalDataSource()
    }

    private val remoteDataSource: SampleRemoteDataSource by lazy {
        SimulatedSampleRemoteDataSource()
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

    override val sampleRepository: SampleRepository by lazy {
        SampleRepositoryImpl(
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            dispatcherProvider = dispatcherProvider
        )
    }

    override val getSampleItemsUseCase: GetSampleItemsUseCase by lazy {
        GetSampleItemsUseCase(
            repository = sampleRepository,
            dispatcherProvider = dispatcherProvider
        )
    }

    override val addSampleItemUseCase: AddSampleItemUseCase by lazy {
        AddSampleItemUseCase(
            repository = sampleRepository,
            dispatcherProvider = dispatcherProvider
        )
    }

    override val refreshSampleItemsUseCase: RefreshSampleItemsUseCase by lazy {
        RefreshSampleItemsUseCase(
            repository = sampleRepository,
            dispatcherProvider = dispatcherProvider
        )
    }
}
