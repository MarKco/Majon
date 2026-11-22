package com.ilsecondodasinistra.majon.ui.counter

import androidx.lifecycle.SavedStateHandle
import com.ilsecondodasinistra.majon.domain.model.MeasureKey
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.ProjectLinks
import com.ilsecondodasinistra.majon.domain.model.ProjectYarnLink
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.domain.voice.VoiceCommand
import com.ilsecondodasinistra.majon.testutil.FakeMajonRepository
import com.ilsecondodasinistra.majon.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** When the last part of a project is finished the app opens the "how much yarn was used" screen (the amounts are asked in `FinishViewModelTest`). */
class CounterFinishTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMajonRepository()
    private var projectId = 0L
    private var yarnId = 0L
    private var partId = 0L

    @Before
    fun setUp() = runTest {
        projectId = repository.upsertProject(Project(name = "Sciarpa"))
        yarnId = repository.upsertYarn(Yarn(name = "Bamboo", colorName = "Mustard", skeinWeightG = 100, stockSkeins = 2.5))
        repository.upsertSwatch(Swatch(yarnId = yarnId, stitches = 26, rows = 34, weightG = 4.0))
        repository.setProjectLinks(projectId, ProjectLinks(yarns = listOf(ProjectYarnLink(yarnId, isPrimary = true))))
        // 20 x 150 cm at that gauge: 120 g = 1.2 skeins
        partId = repository.upsertPart(
            Part(
                projectId = projectId, name = "Corpo", totalRows = 2, completedRows = 1,
                stitchMeasure = MeasureKey.WIDTH, stitchCm = 20.0, rowMeasure = MeasureKey.LENGTH, rowCm = 150.0,
            ),
        )
    }

    private fun viewModel() = CounterViewModel(repository, SavedStateHandle(mapOf("partId" to partId)))

    private suspend fun stock() = repository.observeYarn(yarnId).first()!!.stockSkeins

    @Test
    fun `finishing the last part asks about the project's yarn, taking nothing by itself`() = runTest {
        val vm = viewModel()
        assertNull(vm.finishPrompt.value)
        vm.increment()
        assertEquals(projectId, vm.finishPrompt.value)
        assertEquals(2.5, stock(), 0.0)
    }

    @Test
    fun `the same happens by voice`() = runTest {
        val vm = viewModel()
        vm.onVoiceCommand(VoiceCommand.Increment)
        assertEquals(projectId, vm.finishPrompt.value)
    }

    @Test
    fun `no question while other parts are still to do`() = runTest {
        repository.upsertPart(Part(projectId = projectId, name = "Altra", totalRows = 5))
        val vm = viewModel()
        // The next part exists, so the next-part prompt comes up instead of finishing
        vm.increment()
        assertNull(vm.finishPrompt.value)
        vm.dismissNextPart()
        assertNull(vm.finishPrompt.value)
    }

    @Test
    fun `no question without a linked yarn`() = runTest {
        repository.setProjectLinks(projectId, ProjectLinks())
        val vm = viewModel()
        vm.increment()
        assertNull(vm.finishPrompt.value)
    }

    @Test
    fun `no question about a yarn that is used up`() = runTest {
        repository.adjustYarnStock(yarnId, -10.0)
        val vm = viewModel()
        vm.increment()
        assertNull(vm.finishPrompt.value)
    }

    @Test
    fun `once the screen is opened the prompt is cleared`() = runTest {
        val vm = viewModel()
        vm.increment()
        vm.dismissFinish()
        assertNull(vm.finishPrompt.value)
        assertEquals(2.5, stock(), 0.0)
    }

    @Test
    fun `stepping back after finishing does not ask again`() = runTest {
        val vm = viewModel()
        vm.increment()
        vm.dismissFinish()
        vm.decrement()
        assertNull(vm.finishPrompt.value)
    }
}
