package com.ilsecondodasinistra.majon.ui.counter

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ilsecondodasinistra.majon.domain.model.NoteFrequency
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.RowNote
import com.ilsecondodasinistra.majon.testutil.FakeMajonRepository
import com.ilsecondodasinistra.majon.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CounterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeMajonRepository
    private var partId = 0L

    @Before
    fun setUp() = runTest {
        repository = FakeMajonRepository()
        val projectId = repository.upsertProject(Project(name = "Maglione"))
        partId = repository.upsertPart(Part(projectId = projectId, name = "Davanti", totalRows = 10))
        repository.upsertNote(RowNote(partId = partId, rowStart = 2, rowEnd = 2, text = "aumenta di due"))
        repository.upsertNote(
            RowNote(partId = partId, rowStart = 1, rowEnd = 10, frequency = NoteFrequency.ODD_ROWS, text = "punto diritto"),
        )
    }

    private fun viewModel() = CounterViewModel(
        repository = repository,
        savedStateHandle = SavedStateHandle(mapOf("partId" to partId)),
    )

    @Test
    fun `initial state shows row 1 with its notes and next row notes`() = runTest {
        viewModel().uiState.test {
            val state = awaitItemOfType<CounterUiState.Ready>()
            assertEquals("Davanti", state.partName)
            assertEquals(1, state.currentRow)
            assertEquals(10, state.totalRows)
            assertEquals(listOf("punto diritto"), state.currentNotes.map { it.text })
            assertEquals(listOf("aumenta di due"), state.nextNotes.map { it.text })
            assertEquals(0f, state.progress, 0.0001f)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `increment moves to next row and updates progress and notes`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitItemOfType<CounterUiState.Ready>()
            vm.increment()
            val state = awaitItemOfType<CounterUiState.Ready>()
            assertEquals(2, state.currentRow)
            assertEquals(0.1f, state.progress, 0.0001f)
            assertEquals(listOf("aumenta di due"), state.currentNotes.map { it.text })
            assertEquals(listOf("punto diritto"), state.nextNotes.map { it.text })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `increment stops at total rows and marks complete`() = runTest {
        repository.setCompletedRows(partId, 9)
        val vm = viewModel()
        vm.uiState.test {
            val before = awaitItemOfType<CounterUiState.Ready>()
            assertEquals(10, before.currentRow)
            assertFalse(before.isComplete)
            vm.increment()
            val state = awaitItemOfType<CounterUiState.Ready>()
            assertTrue(state.isComplete)
            assertEquals(1f, state.progress, 0.0001f)
            vm.increment() // no-op when complete
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `decrement undoes a completed row but not below zero`() = runTest {
        repository.setCompletedRows(partId, 3)
        val vm = viewModel()
        vm.uiState.test {
            assertEquals(4, awaitItemOfType<CounterUiState.Ready>().currentRow)
            vm.decrement()
            assertEquals(3, awaitItemOfType<CounterUiState.Ready>().currentRow)
            cancelAndIgnoreRemainingEvents()
        }
        repository.setCompletedRows(partId, 0)
        vm.decrement() // must not go negative
        vm.uiState.test {
            assertEquals(1, awaitItemOfType<CounterUiState.Ready>().currentRow)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setCurrentRow jumps to given row clamped to valid range`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitItemOfType<CounterUiState.Ready>()
            vm.setCurrentRow(7)
            assertEquals(7, awaitItemOfType<CounterUiState.Ready>().currentRow)
            vm.setCurrentRow(99)
            assertEquals(10, awaitItemOfType<CounterUiState.Ready>().currentRow)
            vm.setCurrentRow(-5)
            assertEquals(1, awaitItemOfType<CounterUiState.Ready>().currentRow)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reset returns to row one keeping notes`() = runTest {
        repository.setCompletedRows(partId, 8)
        val vm = viewModel()
        vm.uiState.test {
            awaitItemOfType<CounterUiState.Ready>()
            vm.reset()
            val state = awaitItemOfType<CounterUiState.Ready>()
            assertEquals(1, state.currentRow)
            assertEquals(listOf("punto diritto"), state.currentNotes.map { it.text })
            cancelAndIgnoreRemainingEvents()
        }
    }
}

suspend inline fun <reified T> app.cash.turbine.TurbineTestContext<*>.awaitItemOfType(): T {
    while (true) {
        val item = awaitItem()
        if (item is T) return item
    }
}
