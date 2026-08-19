package dev.hridaya.kubenexus.data.repository

import dev.hridaya.kubenexus.data.source.local.ThemePreferencesLocalDataSource
import dev.hridaya.kubenexus.domain.repository.ThemePreferencesRepository
import dev.hridaya.kubenexus.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ThemePreferencesRepositoryImpl @Inject constructor(
    private val localDataSource: ThemePreferencesLocalDataSource
) : ThemePreferencesRepository {
    override fun getThemeModeStream(): Flow<ThemeMode> = localDataSource.getThemeModeStream()
    override fun getAmoledDarkStream(): Flow<Boolean> = localDataSource.getAmoledDarkStream()
    override suspend fun setThemeMode(mode: ThemeMode) = localDataSource.setThemeMode(mode)
    override suspend fun setAmoledDark(enabled: Boolean) = localDataSource.setAmoledDark(enabled)
}
