package com.ilsecondodasinistra.majon.ui.projectdetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.testutil.FakeMajonRepository
import com.ilsecondodasinistra.majon.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** The list of things to do once the knitting is done. */
class ProjectFinishingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeMajonRepository
    private var projectId = 0L

    @Before
    fun setUp() = runTest {
        repository = FakeMajonRepository()
        projectId = repository.upsertProject(Project(name = "Maglione"))
    }

    private fun viewModel() = ProjectDetailViewModel(repository, SavedStateHandle(mapOf("projectId" to projectId)))

    private suspend fun tasks() = repository.observeFinishingTasks(projectId).first()

    @Test
    fun `a new task goes at the end, trimmed, still to do`() = runTest {
        val vm = viewModel()
        vm.addFinishingTask("  Cuci le spalle ")
        vm.addFinishingTask("Nascondi i fili")
        val all = tasks()
        assertEquals(listOf("Cuci le spalle", "Nascondi i fili"), all.map { it.title })
        assertEquals(listOf(0, 1), all.map { it.position })
        assertTrue(all.none { it.done })
    }

    @Test
    fun `a blank task is not added`() = runTest {
        viewModel().addFinishingTask("   ")
        assertTrue(tasks().isEmpty())
    }

    @Test
    fun `a task can be ticked and unticked`() = runTest {
        val vm = viewModel()
        vm.addFinishingTask("Fili")
        vm.toggleFinishingTask(tasks().single())
        assertTrue(tasks().single().done)
        vm.toggleFinishingTask(tasks().single())
        assertFalse(tasks().single().done)
    }

    @Test
    fun `a task can be renamed, but not to nothing`() = runTest {
        val vm = viewModel()
        vm.addFinishingTask("Fili")
        vm.renameFinishingTask(tasks().single(), " Nascondi i fili ")
        assertEquals("Nascondi i fili", tasks().single().title)
        vm.renameFinishingTask(tasks().single(), "  ")
        assertEquals("Nascondi i fili", tasks().single().title)
    }

    @Test
    fun `a task can be deleted`() = runTest {
        val vm = viewModel()
        vm.addFinishingTask("A")
        vm.addFinishingTask("B")
        vm.deleteFinishingTask(tasks().first())
        assertEquals(listOf("B"), tasks().map { it.title })
    }

    @Test
    fun `suggested tasks are added after the ones there, skipping what is already listed`() = runTest {
        val vm = viewModel()
        vm.addFinishingTask("nascondi i fili")
        vm.addSuggestedFinishing(listOf("Cuci le spalle", "Nascondi i fili", "Cuci le maniche"))
        assertEquals(listOf("nascondi i fili", "Cuci le spalle", "Cuci le maniche"), tasks().map { it.title })
        assertEquals(listOf(0, 1, 2), tasks().map { it.position })
    }

    @Test
    fun `the tasks are exposed to the screen in order`() = runTest {
        val vm = viewModel()
        vm.addFinishingTask("A")
        vm.addFinishingTask("B")
        vm.finishingTasks.test {
            val tasks = expectMostRecentItem()
            assertEquals(listOf("A", "B"), tasks.map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }
}
