package com.ilsecondodasinistra.majon.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.ilsecondodasinistra.majon.data.settings.AppLanguage
import com.ilsecondodasinistra.majon.data.settings.SettingsRepository
import com.ilsecondodasinistra.majon.data.settings.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val scope = CoroutineScope(Job() + UnconfinedTestDispatcher())

    private fun repository(): SettingsRepository {
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            tmpFolder.newFile("settings.preferences_pb")
        }
        return SettingsRepository(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `defaults are system language, system theme, craft palette, haptics on, keep screen on`() = runTest {
        val settings = repository().settings.first()
        assertEquals(AppLanguage.SYSTEM, settings.language)
        assertEquals(ThemeMode.SYSTEM, settings.theme)
        assertFalse(settings.dynamicColor)
        assertTrue(settings.haptics)
        assertTrue(settings.keepScreenOn)
    }

    @Test
    fun `language change is persisted`() = runTest {
        val repo = repository()
        repo.setLanguage(AppLanguage.ENGLISH)
        assertEquals(AppLanguage.ENGLISH, repo.settings.first().language)
    }

    @Test
    fun `theme change is persisted`() = runTest {
        val repo = repository()
        repo.setTheme(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repo.settings.first().theme)
    }

    @Test
    fun `toggles are persisted`() = runTest {
        val repo = repository()
        repo.setDynamicColor(true)
        repo.setHaptics(false)
        repo.setKeepScreenOn(false)
        val settings = repo.settings.first()
        assertTrue(settings.dynamicColor)
        assertFalse(settings.haptics)
        assertFalse(settings.keepScreenOn)
    }
}
