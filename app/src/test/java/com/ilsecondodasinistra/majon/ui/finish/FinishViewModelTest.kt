package com.ilsecondodasinistra.majon.ui.finish

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ilsecondodasinistra.majon.domain.model.MeasureKey
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.ProjectLinks
import com.ilsecondodasinistra.majon.domain.model.ProjectYarnLink
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.testutil.FakeMajonRepository
import com.ilsecondodasinistra.majon.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** The screen shown when the last part is finished: how much of each yarn was used (never taken by itself). */
class FinishViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMajonRepository()
    private var projectId = 0L
    private var yarnId = 0L

    @Before
    fun setUp() = runTest {
        projectId = repository.upsertProject(Project(name = "Sciarpa"))
        yarnId = repository.upsertYarn(Yarn(name = "Bamboo", colorName = "Mustard", skeinWeightG = 100, stockSkeins = 2.5))
        repository.upsertSwatch(Swatch(yarnId = yarnId, stitches = 26, rows = 34, weightG = 4.0))
        repository.setProjectLinks(projectId, ProjectLinks(yarns = listOf(ProjectYarnLink(yarnId, isPrimary = true))))
        // 20 x 150 cm at that gauge: 120 g = 1.2 skeins
        repository.upsertPart(
            Part(
                projectId = projectId, name = "Corpo", totalRows = 2, completedRows = 2,
                stitchMeasure = MeasureKey.WIDTH, stitchCm = 20.0, rowMeasure = MeasureKey.LENGTH, rowCm = 150.0,
            ),
        )
    }

    private fun viewModel() = FinishViewModel(repository, SavedStateHandle(mapOf("projectId" to projectId)))

    private suspend fun stock() = repository.observeYarn(yarnId).first()!!.stockSkeins

    @Test
    fun `asks about each linked yarn with the estimate filled in, rounded up to a quarter`() = runTest {
        val form = viewModel().form.value
        assertEquals(listOf("Bamboo Mustard"), form.yarns.map { it.name })
        assertEquals("1.25", form.amounts[yarnId])
        assertEquals(2.5, stock(), 0.0) // nothing taken yet
    }

    @Test
    fun `confirming takes the typed amounts off the stock and announces it`() = runTest {
        val vm = viewModel()
        vm.saved.test {
            vm.setAmount(yarnId, "1,5")
            assertTrue(vm.save())
            awaitItem()
        }
        assertEquals(1.0, stock(), 0.0001)
    }

    @Test
    fun `an empty or zero amount takes nothing`() = runTest {
        val vm = viewModel()
        vm.setAmount(yarnId, "")
        assertTrue(vm.save())
        assertEquals(2.5, stock(), 0.0)
        vm.setAmount(yarnId, "0")
        assertTrue(vm.save())
        assertEquals(2.5, stock(), 0.0)
    }

    @Test
    fun `a yarn that is used up is not asked about`() = runTest {
        repository.adjustYarnStock(yarnId, -10.0)
        assertTrue(viewModel().form.value.yarns.isEmpty())
    }
}
