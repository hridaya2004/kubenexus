package dev.hridaya.kubenexus.domain.repository

import dev.hridaya.kubenexus.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface ThemePreferencesRepository {
    fun getThemeModeStream(): Flow<ThemeMode>
    fun getAmoledDarkStream(): Flow<Boolean>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAmoledDark(enabled: Boolean)
}
