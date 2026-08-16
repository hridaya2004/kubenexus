package dev.hridaya.kubenexus.data.source.local

import android.content.Context
import android.content.SharedPreferences
import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.ui.theme.ThemeMode
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

interface ThemePreferencesLocalDataSource {
    fun getThemeModeStream(): Flow<ThemeMode>
    fun getAmoledDarkStream(): Flow<Boolean>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAmoledDark(enabled: Boolean)
}

class SharedPrefsThemePreferencesDataSource(
    context: Context,
    private val dispatcherProvider: DispatcherProvider
) : ThemePreferencesLocalDataSource {

    private val prefs: SharedPreferences = context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)

    override fun getThemeModeStream(): Flow<ThemeMode> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_THEME_MODE || key == null) {
                val modeName = prefs.getString(KEY_THEME_MODE, ThemeMode.LIGHT.name) ?: ThemeMode.LIGHT.name
                val mode = runCatching { ThemeMode.valueOf(modeName) }.getOrDefault(ThemeMode.LIGHT)
                trySend(mode)
            }
        }
        val initialName = prefs.getString(KEY_THEME_MODE, ThemeMode.LIGHT.name) ?: ThemeMode.LIGHT.name
        val initialMode = runCatching { ThemeMode.valueOf(initialName) }.getOrDefault(ThemeMode.LIGHT)
        trySend(initialMode)

        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.flowOn(dispatcherProvider.io)

    override fun getAmoledDarkStream(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_AMOLED_DARK || key == null) {
                trySend(prefs.getBoolean(KEY_AMOLED_DARK, false))
            }
        }
        trySend(prefs.getBoolean(KEY_AMOLED_DARK, false))

        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.flowOn(dispatcherProvider.io)

    override suspend fun setThemeMode(mode: ThemeMode) = withContext(dispatcherProvider.io) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    override suspend fun setAmoledDark(enabled: Boolean) = withContext(dispatcherProvider.io) {
        prefs.edit().putBoolean(KEY_AMOLED_DARK, enabled).apply()
    }

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_AMOLED_DARK = "key_amoled_dark"
    }
}
