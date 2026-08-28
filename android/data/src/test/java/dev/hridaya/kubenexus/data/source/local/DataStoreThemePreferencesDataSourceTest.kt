package dev.hridaya.kubenexus.data.source.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.domain.model.ThemeMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreThemePreferencesDataSourceTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val testDispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private lateinit var dataSource: DataStoreThemePreferencesDataSource

    @Before
    fun setUp() {
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { File(tmpFolder.root, "test_app_prefs.preferences_pb") },
        )
        dataSource = DataStoreThemePreferencesDataSource(
            dataStore = testDataStore,
            dispatcherProvider = testDispatcherProvider,
        )
    }

    @Test
    fun `default theme mode is SYSTEM`() = testScope.runTest {
        val mode = dataSource.getThemeModeStream().first()
        assertEquals(ThemeMode.SYSTEM, mode)
    }

    @Test
    fun `default amoled dark is false`() = testScope.runTest {
        val amoled = dataSource.getAmoledDarkStream().first()
        assertEquals(false, amoled)
    }

    @Test
    fun `setThemeMode persists and emits new theme mode`() = testScope.runTest {
        dataSource.setThemeMode(ThemeMode.DARK)
        val mode = dataSource.getThemeModeStream().first()
        assertEquals(ThemeMode.DARK, mode)

        dataSource.setThemeMode(ThemeMode.LIGHT)
        val lightMode = dataSource.getThemeModeStream().first()
        assertEquals(ThemeMode.LIGHT, lightMode)
    }

    @Test
    fun `setAmoledDark persists and emits new amoled setting`() = testScope.runTest {
        dataSource.setAmoledDark(true)
        val amoled = dataSource.getAmoledDarkStream().first()
        assertEquals(true, amoled)

        dataSource.setAmoledDark(false)
        val amoledFalse = dataSource.getAmoledDarkStream().first()
        assertEquals(false, amoledFalse)
    }
}
