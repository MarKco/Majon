package com.ilsecondodasinistra.majon.ui.projectdetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.testutil.FakeMajonRepository
import com.ilsecondodasinistra.majon.testutil.MainDispatcherRule
import com.ilsecondodasinistra.majon.ui.counter.awaitItemOfType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ProjectDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeMajonRepository
    private var projectId = 0L

    @Before
    fun setUp() = runTest {
        repository = FakeMajonRepository()
        projectId = repository.upsertProject(Project(name = "Maglione", yarnType = "Merino"))
    }

    private fun viewModel() = ProjectDetailViewModel(
        repository = repository,
        savedStateHandle = SavedStateHandle(mapOf("projectId" to projectId)),
    )

    @Test
    fun `state exposes project with parts and total progress`() = runTest {
        repository.upsertPart(Part(projectId = projectId, name = "Davanti", totalRows = 100, completedRows = 50))
        viewModel().uiState.test {
            val state = awaitItemOfType<ProjectDetailUiState.Ready>()
            assertEquals("Maglione", state.projectWithParts.project.name)
            assertEquals(1, state.projectWithParts.parts.size)
            assertEquals(0.5f, state.projectWithParts.progress, 0.0001f)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deletePart removes it`() = runTest {
        val partId = repository.upsertPart(Part(projectId = projectId, name = "Collo", totalRows = 20))
        viewModel().deletePart(partId)
        assertTrue(repository.observeProject(projectId).first()!!.parts.isEmpty())
    }

    @Test
    fun `deleteProject removes whole project`() = runTest {
        viewModel().deleteProject()
        assertTrue(repository.observeProjects().first().isEmpty())
    }
}
