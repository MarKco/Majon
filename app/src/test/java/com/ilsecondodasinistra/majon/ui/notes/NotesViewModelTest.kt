package com.ilsecondodasinistra.majon.ui.notes

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ilsecondodasinistra.majon.domain.model.NoteFrequency
import com.ilsecondodasinistra.majon.domain.model.NoteValidationError
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.RowNote
import com.ilsecondodasinistra.majon.testutil.FakeMajonRepository
import com.ilsecondodasinistra.majon.testutil.MainDispatcherRule
import com.ilsecondodasinistra.majon.ui.counter.awaitItemOfType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `saveNote rejects invalid range against part total`() = runTest {
        val vm = viewModel()
        assertEquals(
            NoteValidationError.OUT_OF_RANGE,
            vm.saveNote(id = 0L, rowStart = 45, rowEnd = 60, frequency = NoteFrequency.EVERY_ROW, text = "x"),
        )
        assertEquals(
            NoteValidationError.END_BEFORE_START,
            vm.saveNote(id = 0L, rowStart = 10, rowEnd = 5, frequency = NoteFrequency.EVERY_ROW, text = "x"),
        )
        assertTrue(repository.observeNotes(partId).first().isEmpty())
    }

    @Test
    fun `saveNote rejects blank text and start below one`() = runTest {
        val vm = viewModel()
        assertEquals(
            NoteValidationError.TEXT_BLANK,
            vm.saveNote(id = 0L, rowStart = 1, rowEnd = 5, frequency = NoteFrequency.EVERY_ROW, text = "   "),
        )
        assertEquals(
            NoteValidationError.START_BELOW_ONE,
            vm.saveNote(id = 0L, rowStart = 0, rowEnd = 5, frequency = NoteFrequency.EVERY_ROW, text = "x"),
        )
        assertTrue(repository.observeNotes(partId).first().isEmpty())
    }

    @Test
    fun `valid note is saved`() = runTest {
        val vm = viewModel()
        val error = vm.saveNote(id = 0L, rowStart = 20, rowEnd = 25, frequency = NoteFrequency.EVEN_ROWS, text = "diminuisci di due")
        assertNull(error)
        val note = repository.observeNotes(partId).first().single()
        assertEquals(20, note.rowStart)
        assertEquals(25, note.rowEnd)
        assertEquals(NoteFrequency.EVEN_ROWS, note.frequency)
    }

    @Test
    fun `existing note can be edited`() = runTest {
        val noteId = repository.upsertNote(RowNote(partId = partId, rowStart = 1, rowEnd = 1, text = "vecchia"))
        val vm = viewModel()
        val error = vm.saveNote(id = noteId, rowStart = 2, rowEnd = 4, frequency = NoteFrequency.EVERY_ROW, text = "nuova")
        assertNull(error)
        val note = repository.observeNotes(partId).first().single()
        assertEquals(noteId, note.id)
        assertEquals("nuova", note.text)
        assertEquals(2, note.rowStart)
    }

    @Test
    fun `deleteNote removes it`() = runTest {
        val noteId = repository.upsertNote(RowNote(partId = partId, rowStart = 1, rowEnd = 1, text = "x"))
        viewModel().deleteNote(noteId)
        assertTrue(repository.observeNotes(partId).first().isEmpty())
    }
}
