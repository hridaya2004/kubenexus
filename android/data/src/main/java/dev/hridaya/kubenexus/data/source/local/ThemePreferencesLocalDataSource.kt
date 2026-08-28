package dev.hridaya.kubenexus.data.source.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

interface ThemePreferencesLocalDataSource {
    fun getThemeModeStream(): Flow<ThemeMode>
    fun getAmoledDarkStream(): Flow<Boolean>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAmoledDark(enabled: Boolean)
}

@Singleton
class DataStoreThemePreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val dispatcherProvider: DispatcherProvider,
) : ThemePreferencesLocalDataSource {

    override fun getThemeModeStream(): Flow<ThemeMode> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val modeName = preferences[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
                runCatching { ThemeMode.valueOf(modeName) }.getOrDefault(ThemeMode.SYSTEM)
            }
            .flowOn(dispatcherProvider.io)
    }

    override fun getAmoledDarkStream(): Flow<Boolean> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[KEY_AMOLED_DARK] ?: false
            }
            .flowOn(dispatcherProvider.io)
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        withContext(dispatcherProvider.io) {
            dataStore.edit { preferences ->
                preferences[KEY_THEME_MODE] = mode.name
            }
        }
    }

    override suspend fun setAmoledDark(enabled: Boolean) {
        withContext(dispatcherProvider.io) {
            dataStore.edit { preferences ->
                preferences[KEY_AMOLED_DARK] = enabled
            }
        }
    }

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("key_theme_mode")
        val KEY_AMOLED_DARK = booleanPreferencesKey("key_amoled_dark")
    }
}
