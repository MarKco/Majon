package com.ilsecondodasinistra.majon.ui.importer

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.testutil.FakeMajonRepository
import com.ilsecondodasinistra.majon.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class NotesGuideViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMajonRepository()

    private fun viewModel(projectId: Long) = NotesGuideViewModel(repository, SavedStateHandle(mapOf("projectId" to projectId)))

    @Test
    fun `with no project the guide is general`() = runTest {
        viewModel(0L).context.test {
            val context = expectMostRecentItem()
            assertNull(context.projectName)
            assertEquals(emptyList<Part>(), context.parts)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `for a project the guide knows its parts, in order`() = runTest {
        val id = repository.upsertProject(Project(name = "Mint sweater"))
        repository.upsertPart(Part(projectId = id, name = "Dietro", totalRows = 192))
        repository.upsertPart(Part(projectId = id, name = "Manica", totalRows = 190))
        viewModel(id).context.test {
            val context = expectMostRecentItem()
            assertEquals("Mint sweater", context.projectName)
            assertEquals(listOf("Dietro", "Manica"), context.parts.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an unknown project is the general guide`() = runTest {
        viewModel(999L).context.test {
            assertNull(expectMostRecentItem().projectName)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
