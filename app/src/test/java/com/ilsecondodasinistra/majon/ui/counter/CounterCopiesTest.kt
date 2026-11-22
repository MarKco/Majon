package com.ilsecondodasinistra.majon.ui.counter

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.voice.VoiceCommand
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

/** A part knitted twice (two sleeves): at the end of the first copy the counter asks, then starts over. */
class CounterCopiesTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeMajonRepository
    private var projectId = 0L
    private var partId = 0L

    @Before
    fun setUp() = runTest {
        repository = FakeMajonRepository()
        projectId = repository.upsertProject(Project(name = "Maglione"))
        partId = repository.upsertPart(
            Part(projectId = projectId, name = "Manica", totalRows = 3, completedRows = 2, copies = 2, castOnStitches = 64),
        )
    }

    private fun viewModel() = CounterViewModel(repository, SavedStateHandle(mapOf("partId" to partId)))

    private suspend fun part() = repository.observePart(partId).first()!!

    @Test
    fun `the counter says which copy it is on and the stitches to cast on`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            val state = awaitItemOfType<CounterUiState.Ready>()
            assertEquals(2, state.copies)
            assertEquals(1, state.currentCopy)
            assertEquals(64, state.castOnStitches)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the last row of the first copy asks before going on, changing nothing`() = runTest {
        val vm = viewModel()
        assertNull(vm.nextCopyPrompt.value)
        vm.increment()
        assertEquals(NextCopyPrompt("Manica", finishedCopy = 1, copies = 2), vm.nextCopyPrompt.value)
        assertEquals(2, part().completedRows)
        assertEquals(1, part().currentCopy)
        assertNull(vm.finishPrompt.value)
    }

    @Test
    fun `confirming starts the next copy from the first row`() = runTest {
        val vm = viewModel()
        vm.increment()
        vm.confirmNextCopy()
        assertNull(vm.nextCopyPrompt.value)
        assertEquals(2, part().currentCopy)
        assertEquals(0, part().completedRows)
        assertFalse(part().isComplete)
    }

    @Test
    fun `refusing leaves the count where it was`() = runTest {
        val vm = viewModel()
        vm.increment()
        vm.dismissNextCopy()
        assertNull(vm.nextCopyPrompt.value)
        assertEquals(1, part().currentCopy)
        assertEquals(2, part().completedRows)
    }

    @Test
    fun `no other tap goes through while the question is open`() = runTest {
        val vm = viewModel()
        vm.increment()
        vm.increment()
        assertEquals(2, part().completedRows)
    }

    @Test
    fun `the last row of the last copy finishes the part`() = runTest {
        repository.upsertPart(part().copy(currentCopy = 2))
        val vm = viewModel()
        vm.increment()
        assertNull(vm.nextCopyPrompt.value)
        assertTrue(part().isComplete)
    }

    @Test
    fun `the progress bar of the counter is the one of the copy in hand`() = runTest {
        repository.upsertPart(part().copy(currentCopy = 2, completedRows = 1))
        viewModel().uiState.test {
            val state = awaitItemOfType<CounterUiState.Ready>()
            assertEquals(1f / 3f, state.progress, 0.0001f)
            assertFalse(state.isComplete)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reset starts the copy in hand over, not the whole part`() = runTest {
        repository.upsertPart(part().copy(currentCopy = 2, completedRows = 2))
        viewModel().reset()
        assertEquals(2, part().currentCopy)
        assertEquals(0, part().completedRows)
    }

    // --- voice ---

    @Test
    fun `by voice the question is asked and confirmed`() = runTest {
        val vm = viewModel()
        vm.voiceReplies.test {
            vm.onVoiceCommand(VoiceCommand.Increment)
            assertEquals(VoiceReply.AskNextCopyConfirm(2), awaitItem())
            vm.onVoiceCommand(VoiceCommand.Confirm)
            assertEquals(VoiceReply.NextCopyStarting(2), awaitItem())
        }
        assertEquals(2, part().currentCopy)
        assertEquals(0, part().completedRows)
    }

    @Test
    fun `by voice any other word cancels the question`() = runTest {
        val vm = viewModel()
        vm.voiceReplies.test {
            vm.onVoiceCommand(VoiceCommand.Increment)
            awaitItem()
            vm.onVoiceCommand(VoiceCommand.Decrement)
            assertEquals(VoiceReply.NextCopyCancelled, awaitItem())
        }
        assertNull(vm.nextCopyPrompt.value)
        assertEquals(1, part().currentCopy)
    }
}
