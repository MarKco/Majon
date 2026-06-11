package com.ilsecondodasinistra.majon.ui.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.ilsecondodasinistra.majon.data.export.BackupCodec
import com.ilsecondodasinistra.majon.data.settings.AppLanguage
import com.ilsecondodasinistra.majon.data.settings.SettingsRepository
import com.ilsecondodasinistra.majon.data.settings.ThemeMode
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.testutil.FakeMajonRepository
import com.ilsecondodasinistra.majon.testutil.MainDispatcherRule
import com.ilsecondodasinistra.majon.ui.editproject.awaitItemMatching
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val scope = CoroutineScope(Job() + UnconfinedTestDispatcher())
    private val repository = FakeMajonRepository()

    private fun viewModel(): SettingsViewModel {
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            tmpFolder.newFile("settings.preferences_pb")
        }
        return SettingsViewModel(
            settingsRepository = SettingsRepository(dataStore),
            majonRepository = repository,
            backupCodec = BackupCodec(),
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `setters persist into settings state`() = runTest {
        val vm = viewModel()
        vm.setLanguage(AppLanguage.ENGLISH)
        vm.setTheme(ThemeMode.DARK)
        vm.setDynamicColor(true)
        vm.settings.test {
            val s = awaitItemMatching { it.language == AppLanguage.ENGLISH }
            assertEquals(ThemeMode.DARK, s.theme)
            assertTrue(s.dynamicColor)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `exportData produces json importable back`() = runTest {
        val projectId = repository.upsertProject(Project(name = "Sciarpa"))
        repository.upsertPart(Part(projectId = projectId, name = "Unica", totalRows = 60, completedRows = 10))

        val vm = viewModel()
        val json = vm.exportData()
        assertTrue(json.contains("Sciarpa"))

        repository.deleteProject(projectId)
        assertTrue(repository.observeProjects().first().isEmpty())

        val result = vm.importData(json)
        assertEquals(1, result.getOrThrow())
        assertEquals("Sciarpa", repository.observeProjects().first().single().project.name)
    }

    @Test
    fun `importData with garbage fails gracefully`() = runTest {
        val result = viewModel().importData("garbage{{{")
        assertTrue(result.isFailure)
    }
}
