package com.ilsecondodasinistra.majon.ui.notes

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.RowNote
import com.ilsecondodasinistra.majon.testutil.FakeMajonRepository
import com.ilsecondodasinistra.majon.testutil.MainDispatcherRule
import com.ilsecondodasinistra.majon.ui.counter.awaitItemOfType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NotesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeMajonRepository
    private var partId = 0L

    @Before
    fun setUp() = runTest {
        repository = FakeMajonRepository()
        val projectId = repository.upsertProject(Project(name = "Maglione"))
        partId = repository.upsertPart(Part(projectId = projectId, name = "Davanti", totalRows = 50))
    }

    private fun viewModel() = NotesViewModel(
        repository = repository,
        savedStateHandle = SavedStateHandle(mapOf("partId" to partId)),
    )

    @Test
    fun `notes list is exposed sorted`() = runTest {
        repository.upsertNote(RowNote(partId = partId, rowStart = 30, rowEnd = 30, text = "b"))
        repository.upsertNote(RowNote(partId = partId, rowStart = 5, rowEnd = 10, text = "a"))
        viewModel().uiState.test {
            val state = awaitItemOfType<NotesUiState.Ready>()
            assertEquals(listOf("a", "b"), state.notes.map { it.text })
            assertEquals(50, state.totalRows)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteNote removes it`() = runTest {
        val noteId = repository.upsertNote(RowNote(partId = partId, rowStart = 1, rowEnd = 1, text = "x"))
        viewModel().deleteNote(noteId)
        assertTrue(repository.observeNotes(partId).first().isEmpty())
    }
}
