package com.ilsecondodasinistra.majon.ui.counter

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.RowNote
import com.ilsecondodasinistra.majon.domain.model.RowStep
import com.ilsecondodasinistra.majon.domain.voice.VoiceCommand
import com.ilsecondodasinistra.majon.testutil.FakeMajonRepository
import com.ilsecondodasinistra.majon.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** Preparation / final rows, and the prompt that moves to the next part. */
class CounterStepsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMajonRepository()
    private var projectId = 0L

    private suspend fun part(
        name: String,
        total: Int = 3,
        completed: Int = 0,
        prep: Boolean = false,
        final: Boolean = false,
    ): Long {
        if (projectId == 0L) projectId = repository.upsertProject(Project(name = "Maglione"))
        return repository.upsertPart(
            Part(
                projectId = projectId, name = name, totalRows = total, completedRows = completed,
                hasPrepRow = prep, hasFinalRow = final,
                prepNote = if (prep) "Avvia 40 maglie" else null,
                finalNote = if (final) "Chiudi con ago" else null,
            ),
        )
    }

    private fun viewModel(partId: Long) = CounterViewModel(
        repository = repository,
        savedStateHandle = SavedStateHandle(mapOf("partId" to partId)),
    )

    private suspend fun completed(partId: Long) = repository.observePart(partId).first()!!.completedRows

    // --- extra rows ---

    @Test
    fun `state starts on the preparation row with its note`() = runTest {
        val id = part("Davanti", prep = true)
        repository.upsertNote(RowNote(partId = id, rowStart = 1, rowEnd = 1, text = "diritto"))
        viewModel(id).uiState.test {
            val s = awaitItemOfType<CounterUiState.Ready>()
            assertEquals(RowStep.Prep, s.step)
            assertEquals(0, s.currentRow)
            assertEquals("Avvia 40 maglie", s.stepNote)
            assertEquals(emptyList<RowNote>(), s.currentNotes)
            assertEquals(RowStep.Row(1), s.nextStep)
            assertEquals(listOf("diritto"), s.nextNotes.map { it.text })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `increment walks preparation, rows and final row then completes`() = runTest {
        val id = part("Davanti", total = 2, prep = true, final = true)
        val vm = viewModel(id)
        vm.uiState.test {
            assertEquals(RowStep.Prep, awaitItemOfType<CounterUiState.Ready>().step)
            vm.increment()
            assertEquals(RowStep.Row(1), awaitItemOfType<CounterUiState.Ready>().step)
            vm.increment()
            assertEquals(RowStep.Row(2), awaitItemOfType<CounterUiState.Ready>().step)
            vm.increment()
            val final = awaitItemOfType<CounterUiState.Ready>()
            assertEquals(RowStep.Final, final.step)
            assertEquals("Chiudi con ago", final.stepNote)
            assertNull(final.nextStep)
            vm.increment()
            assertEquals(true, awaitItemOfType<CounterUiState.Ready>().isComplete)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setCurrentRow accounts for the preparation row`() = runTest {
        val id = part("Davanti", total = 10, prep = true)
        viewModel(id).setCurrentRow(5)
        assertEquals(5, completed(id))
        assertEquals(RowStep.Row(5), repository.observePart(id).first()!!.currentStep)
    }

    // --- next part prompt ---

    @Test
    fun `last step with a next part asks instead of advancing`() = runTest {
        val a = part("Davanti", total = 2, completed = 1)
        part("Dietro")
        val vm = viewModel(a)
        vm.nextPartPrompt.test {
            assertNull(awaitItem())
            vm.increment()
            assertEquals("Dietro", awaitItem()?.partName)
            assertEquals(1, completed(a))
        }
    }

    @Test
    fun `confirming completes the part then opens the next one after a short celebration`() = runTest {
        val a = part("Davanti", total = 2, completed = 1)
        val b = part("Dietro")
        val vm = viewModel(a)
        vm.navigateToPart.test {
            vm.increment()
            vm.confirmNextPart()
            assertEquals(2, completed(a))
            expectNoEvents()
            advanceTimeBy(CounterViewModel.NEXT_PART_DELAY_MILLIS + 1)
            assertEquals(b, awaitItem())
        }
        assertNull(vm.nextPartPrompt.value)
    }

    @Test
    fun `dismissing keeps the part where it was`() = runTest {
        val a = part("Davanti", total = 2, completed = 1)
        part("Dietro")
        val vm = viewModel(a)
        vm.increment()
        vm.dismissNextPart()
        assertNull(vm.nextPartPrompt.value)
        assertEquals(1, completed(a))
    }

    @Test
    fun `last step without a next part completes directly`() = runTest {
        val a = part("Davanti", total = 2, completed = 1)
        val vm = viewModel(a)
        vm.increment()
        assertNull(vm.nextPartPrompt.value)
        assertEquals(2, completed(a))
    }

    @Test
    fun `next part skips parts that are already finished`() = runTest {
        val a = part("Davanti", total = 2, completed = 1)
        part("Dietro", total = 2, completed = 2)
        val c = part("Manica")
        val vm = viewModel(a)
        vm.navigateToPart.test {
            vm.increment()
            assertEquals("Manica", vm.nextPartPrompt.value?.partName)
            vm.confirmNextPart()
            advanceTimeBy(CounterViewModel.NEXT_PART_DELAY_MILLIS + 1)
            assertEquals(c, awaitItem())
        }
    }

    @Test
    fun `earlier unfinished parts are not offered`() = runTest {
        part("Davanti") // earlier, unfinished
        val b = part("Dietro", total = 2, completed = 1)
        val vm = viewModel(b)
        vm.increment()
        assertNull(vm.nextPartPrompt.value)
        assertEquals(2, completed(b))
    }

    // --- voice ---

    @Test
    fun `voice increment on the last step asks and a spoken confirm moves on`() = runTest {
        val a = part("Davanti", total = 2, completed = 1)
        val b = part("Dietro")
        val vm = viewModel(a)
        val opened = mutableListOf<Long>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.navigateToPart.collect { opened += it } }
        vm.voiceReplies.test {
            vm.onVoiceCommand(VoiceCommand.Increment)
            assertEquals(VoiceReply.AskNextPartConfirm("Dietro"), awaitItem())
            assertEquals("Dietro", vm.nextPartPrompt.value?.partName)
            assertEquals(1, completed(a))
            vm.onVoiceCommand(VoiceCommand.Confirm)
            assertEquals(VoiceReply.NextPartOpening("Dietro"), awaitItem())
            assertEquals(2, completed(a))
            advanceTimeBy(CounterViewModel.NEXT_PART_DELAY_MILLIS + 1)
            assertEquals(listOf(b), opened)
        }
    }

    @Test
    fun `any other voice command cancels the next part prompt`() = runTest {
        val a = part("Davanti", total = 2, completed = 1)
        part("Dietro")
        val vm = viewModel(a)
        vm.voiceReplies.test {
            vm.onVoiceCommand(VoiceCommand.Increment)
            assertEquals(VoiceReply.AskNextPartConfirm("Dietro"), awaitItem())
            vm.onVoiceCommand(VoiceCommand.Decrement)
            assertEquals(VoiceReply.NextPartCancelled, awaitItem())
            assertNull(vm.nextPartPrompt.value)
            assertEquals(1, completed(a))
        }
    }

    @Test
    fun `voice announces the final row and the preparation row`() = runTest {
        val id = part("Davanti", total = 2, completed = 2, prep = false, final = true)
        val vm = viewModel(id)
        vm.voiceReplies.test {
            vm.onVoiceCommand(VoiceCommand.Decrement)
            assertEquals(VoiceReply.RowUndone(2), awaitItem())
            vm.onVoiceCommand(VoiceCommand.Increment)
            assertEquals(VoiceReply.FinalRowNow, awaitItem())
        }
        val prepPart = part("Manica", total = 2, completed = 1, prep = true)
        val vm2 = viewModel(prepPart)
        vm2.voiceReplies.test {
            vm2.onVoiceCommand(VoiceCommand.Decrement)
            assertEquals(VoiceReply.PrepRowNow, awaitItem())
        }
    }

    @Test
    fun `voice go to row and remaining count the extra steps`() = runTest {
        val id = part("Davanti", total = 10, completed = 0, prep = true, final = true)
        val vm = viewModel(id)
        vm.voiceReplies.test {
            vm.onVoiceCommand(VoiceCommand.GoToRow(4))
            assertEquals(VoiceReply.WentTo(4), awaitItem())
            assertEquals(4, completed(id))
            vm.onVoiceCommand(VoiceCommand.RemainingRows)
            assertEquals(VoiceReply.Remaining(8), awaitItem()) // rows 4..10 + final
        }
    }
}
