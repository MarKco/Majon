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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NoteEditViewModelTest {

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

    private fun viewModel(noteId: Long = 0L) = NoteEditViewModel(
        repository = repository,
        savedStateHandle = SavedStateHandle(mapOf("partId" to partId, "noteId" to noteId)),
    )

    private suspend fun notes() = repository.observeNotes(partId).first()

    @Test
    fun `a new note starts empty, every row`() = runTest {
        val form = viewModel().form.value
        assertEquals("", form.text)
        assertEquals("", form.rowStart)
        assertEquals(NoteFrequency.EVERY_ROW, form.frequency)
        assertFalse(form.isEditing)
        assertNull(form.error)
    }

    @Test
    fun `an invalid range is rejected against the part total`() = runTest {
        val vm = viewModel()
        vm.update { it.copy(text = "x", rowStart = "45", rowEnd = "60") }
        assertFalse(vm.save())
        assertEquals(NoteValidationError.OUT_OF_RANGE, vm.form.value.error)
        vm.update { it.copy(rowStart = "10", rowEnd = "5") }
        assertFalse(vm.save())
        assertEquals(NoteValidationError.END_BEFORE_START, vm.form.value.error)
        assertTrue(notes().isEmpty())
    }

    @Test
    fun `blank text and a start below one are rejected`() = runTest {
        val vm = viewModel()
        vm.update { it.copy(text = "   ", rowStart = "1", rowEnd = "5") }
        assertFalse(vm.save())
        assertEquals(NoteValidationError.TEXT_BLANK, vm.form.value.error)
        vm.update { it.copy(text = "x", rowStart = "0") }
        assertFalse(vm.save())
        assertEquals(NoteValidationError.START_BELOW_ONE, vm.form.value.error)
        vm.update { it.copy(rowStart = "") }
        assertFalse(vm.save())
        assertTrue(notes().isEmpty())
    }

    @Test
    fun `typing clears the error`() = runTest {
        val vm = viewModel()
        vm.update { it.copy(text = "") }
        assertFalse(vm.save())
        vm.update { it.copy(text = "a") }
        assertNull(vm.form.value.error)
    }

    @Test
    fun `an empty end row means a single row note`() = runTest {
        val vm = viewModel()
        vm.update { it.copy(text = "aumenta", rowStart = "7") }
        assertTrue(vm.save())
        val note = notes().single()
        assertEquals(7, note.rowStart)
        assertEquals(7, note.rowEnd)
    }

    @Test
    fun `a valid note is saved and announced`() = runTest {
        val vm = viewModel()
        vm.saved.test {
            vm.update { it.copy(text = " diminuisci di due ", rowStart = "20", rowEnd = "25", frequency = NoteFrequency.EVEN_ROWS) }
            assertTrue(vm.save())
            awaitItem()
        }
        val note = notes().single()
        assertEquals("diminuisci di due", note.text)
        assertEquals(20, note.rowStart)
        assertEquals(25, note.rowEnd)
        assertEquals(NoteFrequency.EVEN_ROWS, note.frequency)
    }

    @Test
    fun `editing loads the note and saves over it`() = runTest {
        val id = repository.upsertNote(
            RowNote(partId = partId, rowStart = 3, rowEnd = 9, frequency = NoteFrequency.ODD_ROWS, text = "vecchia"),
        )
        val vm = viewModel(id)
        val form = vm.form.value
        assertTrue(form.isEditing)
        assertEquals("vecchia", form.text)
        assertEquals("3", form.rowStart)
        assertEquals("9", form.rowEnd)
        assertEquals(NoteFrequency.ODD_ROWS, form.frequency)
        vm.update { it.copy(text = "nuova", rowStart = "2") }
        assertTrue(vm.save())
        val note = notes().single()
        assertEquals(id, note.id)
        assertEquals("nuova", note.text)
        assertEquals(2, note.rowStart)
    }

    // --- notes on exact rows and on every Nth row ---

    @Test
    fun `a new note starts as a range`() = runTest {
        assertEquals(NoteScope.RANGE, viewModel().form.value.scope)
    }

    @Test
    fun `a note on exact rows is saved with its rows sorted`() = runTest {
        val vm = viewModel()
        vm.update { it.copy(text = "diminuisci", scope = NoteScope.ROWS, rowsText = "30, 10 20-22") }
        assertTrue(vm.save())
        val note = notes().single()
        assertEquals(NoteFrequency.SPECIFIC, note.frequency)
        assertEquals(listOf(10, 20, 21, 22, 30), note.rows)
        assertEquals(10, note.rowStart)
        assertEquals(30, note.rowEnd)
    }

    @Test
    fun `exact rows need at least one row, understood and within the part`() = runTest {
        val vm = viewModel()
        vm.update { it.copy(text = "x", scope = NoteScope.ROWS, rowsText = "") }
        assertFalse(vm.save())
        assertEquals(NoteValidationError.ROWS_EMPTY, vm.form.value.error)
        vm.update { it.copy(rowsText = "10, dieci") }
        assertFalse(vm.save())
        assertEquals(NoteValidationError.ROWS_INVALID, vm.form.value.error)
        vm.update { it.copy(rowsText = "10, 60") }
        assertFalse(vm.save())
        assertEquals(NoteValidationError.OUT_OF_RANGE, vm.form.value.error)
        assertTrue(notes().isEmpty())
    }

    @Test
    fun `a note every Nth row is saved with its step and range`() = runTest {
        val vm = viewModel()
        vm.update { it.copy(text = "diminuisci", scope = NoteScope.EVERY, rowStart = "12", rowEnd = "48", step = "6") }
        assertTrue(vm.save())
        val note = notes().single()
        assertEquals(NoteFrequency.EVERY_NTH, note.frequency)
        assertEquals(6, note.step)
        assertEquals(12, note.rowStart)
        assertEquals(48, note.rowEnd)
    }

    @Test
    fun `every Nth with no start begins at row 1 and with no end goes to the last row`() = runTest {
        val vm = viewModel()
        vm.update { it.copy(text = "x", scope = NoteScope.EVERY, step = "10") }
        assertTrue(vm.save())
        val note = notes().single()
        assertEquals(1, note.rowStart)
        assertEquals(50, note.rowEnd)
    }

    @Test
    fun `every Nth needs a step of at least two`() = runTest {
        val vm = viewModel()
        vm.update { it.copy(text = "x", scope = NoteScope.EVERY, step = "1") }
        assertFalse(vm.save())
        assertEquals(NoteValidationError.STEP_INVALID, vm.form.value.error)
        vm.update { it.copy(step = "") }
        assertFalse(vm.save())
        assertEquals(NoteValidationError.STEP_INVALID, vm.form.value.error)
    }

    @Test
    fun `the fields of the other kinds are ignored`() = runTest {
        val vm = viewModel()
        vm.update { it.copy(text = "x", scope = NoteScope.ROWS, rowsText = "7", rowStart = "999", rowEnd = "1", step = "0") }
        assertTrue(vm.save())
        assertEquals(listOf(7), notes().single().rows)
    }

    @Test
    fun `editing loads a note on exact rows and one every Nth`() = runTest {
        val a = repository.upsertNote(
            RowNote(partId = partId, rowStart = 5, rowEnd = 9, frequency = NoteFrequency.SPECIFIC, rows = listOf(5, 7, 9), text = "a"),
        )
        val b = repository.upsertNote(
            RowNote(partId = partId, rowStart = 10, rowEnd = 40, frequency = NoteFrequency.EVERY_NTH, step = 6, text = "b"),
        )
        val formA = viewModel(a).form.value
        assertEquals(NoteScope.ROWS, formA.scope)
        assertEquals("5, 7, 9", formA.rowsText)
        val formB = viewModel(b).form.value
        assertEquals(NoteScope.EVERY, formB.scope)
        assertEquals("6", formB.step)
        assertEquals("10", formB.rowStart)
        assertEquals("40", formB.rowEnd)
    }

    @Test
    fun `changing the kind of an edited note saves it as the new kind`() = runTest {
        val id = repository.upsertNote(RowNote(partId = partId, rowStart = 3, rowEnd = 3, text = "x"))
        val vm = viewModel(id)
        vm.update { it.copy(scope = NoteScope.ROWS, rowsText = "3, 6") }
        assertTrue(vm.save())
        val note = notes().single()
        assertEquals(id, note.id)
        assertEquals(NoteFrequency.SPECIFIC, note.frequency)
        assertEquals(listOf(3, 6), note.rows)
    }
}
