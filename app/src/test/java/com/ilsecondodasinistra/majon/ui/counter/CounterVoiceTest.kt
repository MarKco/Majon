package com.ilsecondodasinistra.majon.ui.counter

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ilsecondodasinistra.majon.domain.model.NoteFrequency
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.RowNote
import com.ilsecondodasinistra.majon.domain.voice.VoiceCommand
import com.ilsecondodasinistra.majon.testutil.FakeMajonRepository
import com.ilsecondodasinistra.majon.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CounterVoiceTest {

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

    private suspend fun completedRows() = repository.observePart(partId).first()!!.completedRows

    @Test
    fun `increment advances and announces the new row`() = runTest {
        val vm = viewModel()
        vm.voiceReplies.test {
            vm.onVoiceCommand(VoiceCommand.Increment)
            assertEquals(VoiceReply.RowAdvanced(2), awaitItem())
            assertEquals(1, completedRows())
        }
    }

    @Test
    fun `last increment announces the part is finished`() = runTest {
        repository.setCompletedRows(partId, 9)
        val vm = viewModel()
        vm.voiceReplies.test {
            vm.onVoiceCommand(VoiceCommand.Increment)
            assertEquals(VoiceReply.PartFinished(10), awaitItem())
            vm.onVoiceCommand(VoiceCommand.Increment)
            assertEquals(VoiceReply.PartFinished(10), awaitItem())
            assertEquals(10, completedRows())
        }
    }

    @Test
    fun `decrement goes back one row but never below the first`() = runTest {
        repository.setCompletedRows(partId, 3)
        val vm = viewModel()
        vm.voiceReplies.test {
            vm.onVoiceCommand(VoiceCommand.Decrement)
            assertEquals(VoiceReply.RowUndone(3), awaitItem())
            assertEquals(2, completedRows())
            repository.setCompletedRows(partId, 0)
            vm.onVoiceCommand(VoiceCommand.Decrement)
            assertEquals(VoiceReply.RowUndone(1), awaitItem())
            assertEquals(0, completedRows())
        }
    }

    @Test
    fun `go to row jumps inside the range and rejects outside`() = runTest {
        val vm = viewModel()
        vm.voiceReplies.test {
            vm.onVoiceCommand(VoiceCommand.GoToRow(7))
            assertEquals(VoiceReply.WentTo(7), awaitItem())
            assertEquals(6, completedRows())
            vm.onVoiceCommand(VoiceCommand.GoToRow(99))
            assertEquals(VoiceReply.OutOfRange(10), awaitItem())
            vm.onVoiceCommand(VoiceCommand.GoToRow(0))
            assertEquals(VoiceReply.OutOfRange(10), awaitItem())
            assertEquals(6, completedRows())
        }
    }

    @Test
    fun `read note speaks the notes of the current row`() = runTest {
        val vm = viewModel()
        vm.voiceReplies.test {
            repository.setCompletedRows(partId, 1) // row 2
            vm.onVoiceCommand(VoiceCommand.ReadNote)
            assertEquals(VoiceReply.Note(listOf("aumenta di due")), awaitItem())
            repository.setCompletedRows(partId, 3) // row 4: even, no note
            vm.onVoiceCommand(VoiceCommand.ReadNote)
            assertEquals(VoiceReply.NoNote, awaitItem())
        }
    }

    @Test
    fun `remaining rows counts the row in progress`() = runTest {
        repository.setCompletedRows(partId, 4)
        val vm = viewModel()
        vm.voiceReplies.test {
            vm.onVoiceCommand(VoiceCommand.RemainingRows)
            assertEquals(VoiceReply.Remaining(6), awaitItem())
        }
    }

    @Test
    fun `reset needs a spoken confirmation`() = runTest {
        repository.setCompletedRows(partId, 5)
        val vm = viewModel()
        vm.voiceReplies.test {
            vm.onVoiceCommand(VoiceCommand.Reset)
            assertEquals(VoiceReply.AskResetConfirm, awaitItem())
            assertEquals(5, completedRows())
            vm.onVoiceCommand(VoiceCommand.Confirm)
            assertEquals(VoiceReply.ResetDone, awaitItem())
            assertEquals(0, completedRows())
        }
    }

    @Test
    fun `any other command cancels a pending reset without running`() = runTest {
        repository.setCompletedRows(partId, 5)
        val vm = viewModel()
        vm.voiceReplies.test {
            vm.onVoiceCommand(VoiceCommand.Reset)
            assertEquals(VoiceReply.AskResetConfirm, awaitItem())
            vm.onVoiceCommand(VoiceCommand.Decrement)
            assertEquals(VoiceReply.ResetCancelled, awaitItem())
            assertEquals(5, completedRows())
        }
    }

    @Test
    fun `a pending reset expires after the timeout`() = runTest {
        repository.setCompletedRows(partId, 5)
        val vm = viewModel()
        vm.voiceReplies.test {
            vm.onVoiceCommand(VoiceCommand.Reset)
            assertEquals(VoiceReply.AskResetConfirm, awaitItem())
            advanceTimeBy(CounterViewModel.RESET_CONFIRM_MILLIS + 1)
            vm.onVoiceCommand(VoiceCommand.Confirm) // nothing pending any more
            vm.onVoiceCommand(VoiceCommand.Increment)
            assertEquals(VoiceReply.RowAdvanced(7), awaitItem())
            assertEquals(6, completedRows())
        }
    }

    @Test
    fun `stray confirm does nothing`() = runTest {
        repository.setCompletedRows(partId, 5)
        val vm = viewModel()
        vm.voiceReplies.test {
            vm.onVoiceCommand(VoiceCommand.Confirm)
            expectNoEvents()
            assertEquals(5, completedRows())
        }
    }
}
