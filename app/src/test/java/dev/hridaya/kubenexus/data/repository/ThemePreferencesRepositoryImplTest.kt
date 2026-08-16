package dev.hridaya.kubenexus.data.repository

import dev.hridaya.kubenexus.data.source.local.ThemePreferencesLocalDataSource
import dev.hridaya.kubenexus.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ThemePreferencesRepositoryImplTest {

    private lateinit var fakeDataSource: FakeThemePreferencesLocalDataSource
    private lateinit var repository: ThemePreferencesRepositoryImpl

    @Before
    fun setUp() {
        fakeDataSource = FakeThemePreferencesLocalDataSource()
        repository = ThemePreferencesRepositoryImpl(fakeDataSource)
    }

    @Test
    fun `setThemeMode updates theme mode stream`() = runTest {
        assertEquals(ThemeMode.LIGHT, repository.getThemeModeStream().first())

        repository.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, repository.getThemeModeStream().first())
    }

    @Test
    fun `setAmoledDark updates amoled dark stream`() = runTest {
        assertEquals(false, repository.getAmoledDarkStream().first())

        repository.setAmoledDark(true)

        assertEquals(true, repository.getAmoledDarkStream().first())
    }

    private class FakeThemePreferencesLocalDataSource : ThemePreferencesLocalDataSource {
        private val themeModeFlow = MutableStateFlow(ThemeMode.LIGHT)
        private val amoledDarkFlow = MutableStateFlow(false)

        override fun getThemeModeStream(): Flow<ThemeMode> = themeModeFlow
        override fun getAmoledDarkStream(): Flow<Boolean> = amoledDarkFlow

        override suspend fun setThemeMode(mode: ThemeMode) {
            themeModeFlow.value = mode
        }

        override suspend fun setAmoledDark(enabled: Boolean) {
            amoledDarkFlow.value = enabled
        }
    }
}
