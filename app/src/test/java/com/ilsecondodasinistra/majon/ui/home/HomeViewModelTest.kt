package com.ilsecondodasinistra.majon.ui.home

import app.cash.turbine.test
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.testutil.FakeMajonRepository
import com.ilsecondodasinistra.majon.testutil.MainDispatcherRule
import com.ilsecondodasinistra.majon.ui.counter.awaitItemOfType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `projects appear with aggregated progress`() = runTest {
        val repository = FakeMajonRepository()
        val projectId = repository.upsertProject(Project(name = "Maglione"))
        repository.upsertPart(Part(projectId = projectId, name = "Davanti", totalRows = 100, completedRows = 30))
        repository.upsertPart(Part(projectId = projectId, name = "Dietro", totalRows = 100, completedRows = 10))

        HomeViewModel(repository).uiState.test {
            val state = awaitItemOfType<HomeUiState.Ready>()
            assertEquals(1, state.projects.size)
            assertEquals("Maglione", state.projects.first().project.name)
            assertEquals(0.2f, state.projects.first().progress, 0.0001f)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty list yields empty ready state`() = runTest {
        HomeViewModel(FakeMajonRepository()).uiState.test {
            val state = awaitItemOfType<HomeUiState.Ready>()
            assertTrue(state.projects.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteProject removes it from the list`() = runTest {
        val repository = FakeMajonRepository()
        val projectId = repository.upsertProject(Project(name = "Da cancellare"))
        val vm = HomeViewModel(repository)
        vm.uiState.test {
            assertEquals(1, awaitItemOfType<HomeUiState.Ready>().projects.size)
            vm.deleteProject(projectId)
            assertTrue(awaitItemOfType<HomeUiState.Ready>().projects.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
