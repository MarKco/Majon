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
    fun `addPart validates name and total rows`() = runTest {
        val vm = viewModel()
        assertFalse(vm.addPart("", 100))
        assertFalse(vm.addPart("Manica", 0))
        assertFalse(vm.addPart("Manica", -5))
        assertTrue(vm.addPart("Manica", 80))

        val parts = repository.observeProject(projectId).first()!!.parts
        assertEquals(listOf("Manica"), parts.map { it.name })
    }

    @Test
    fun `updatePart renames and resizes`() = runTest {
        val partId = repository.upsertPart(Part(projectId = projectId, name = "Davanti", totalRows = 100))
        val vm = viewModel()
        assertTrue(vm.updatePart(partId, "Davanti v2", 90))
        val part = repository.observePart(partId).first()!!
        assertEquals("Davanti v2", part.name)
        assertEquals(90, part.totalRows)
    }

    @Test
    fun `updatePart clamps completed rows to the new total`() = runTest {
        val partId = repository.upsertPart(Part(projectId = projectId, name = "Davanti", totalRows = 100, completedRows = 80))
        assertTrue(viewModel().updatePart(partId, "Davanti", 50))
        assertEquals(50, repository.observePart(partId).first()!!.completedRows)
    }

    @Test
    fun `addPart can start with a preparation and a final row`() = runTest {
        val vm = viewModel()
        assertTrue(vm.addPart("Manica", 40, RowExtras(prepRow = true, prepNote = " Avvia 50 ", finalRow = true, finalNote = "")))
        val part = repository.observeProject(projectId).first()!!.parts.single()
        assertTrue(part.hasPrepRow)
        assertEquals("Avvia 50", part.prepNote)
        assertTrue(part.hasFinalRow)
        assertEquals(null, part.finalNote) // blank notes are not stored
    }

    @Test
    fun `updatePart without extras leaves them alone`() = runTest {
        val partId = repository.upsertPart(
            Part(projectId = projectId, name = "Davanti", totalRows = 10, hasPrepRow = true, prepNote = "x", completedRows = 3),
        )
        assertTrue(viewModel().updatePart(partId, "Davanti", 12))
        val part = repository.observePart(partId).first()!!
        assertTrue(part.hasPrepRow)
        assertEquals(3, part.completedRows)
    }

    @Test
    fun `updatePart turning the preparation row on keeps the knitter on the same row`() = runTest {
        val partId = repository.upsertPart(Part(projectId = projectId, name = "Davanti", totalRows = 10, completedRows = 4))
        assertTrue(viewModel().updatePart(partId, "Davanti", 10, RowExtras(prepRow = true)))
        val part = repository.observePart(partId).first()!!
        assertEquals(5, part.completedRows)
        assertEquals(com.ilsecondodasinistra.majon.domain.model.RowStep.Row(5), part.currentStep)
    }

    @Test
    fun `updatePart shrinking the total keeps the extra rows in the clamp`() = runTest {
        val partId = repository.upsertPart(
            Part(projectId = projectId, name = "Davanti", totalRows = 100, completedRows = 80, hasFinalRow = true),
        )
        assertTrue(viewModel().updatePart(partId, "Davanti", 50))
        assertEquals(51, repository.observePart(partId).first()!!.completedRows)
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
