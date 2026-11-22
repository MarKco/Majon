package com.ilsecondodasinistra.majon.ui.stash

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ilsecondodasinistra.majon.domain.model.StitchPattern
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.testutil.FakeMajonRepository
import com.ilsecondodasinistra.majon.testutil.MainDispatcherRule
import com.ilsecondodasinistra.majon.ui.counter.awaitItemOfType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class YarnDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMajonRepository()
    private var yarnId = 0L

    @Before
    fun setUp() = runTest {
        yarnId = repository.upsertYarn(Yarn(name = "Bamboo", stockSkeins = 2.5, skeinWeightG = 100, skeinLengthM = 350))
    }

    private fun viewModel() = YarnDetailViewModel(
        repository = repository,
        savedStateHandle = SavedStateHandle(mapOf("yarnId" to yarnId)),
    )

    @Test
    fun `state exposes the yarn and its swatches`() = runTest {
        repository.upsertSwatch(Swatch(yarnId = yarnId, stitches = 26, rows = 34))
        viewModel().uiState.test {
            val s = awaitItemOfType<YarnDetailUiState.Ready>()
            assertEquals("Bamboo", s.yarn.name)
            assertEquals(1, s.swatches.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `using skeins lowers the stock but never below zero`() = runTest {
        val vm = viewModel()
        vm.adjustStock(-0.75)
        assertEquals(1.75, repository.observeYarn(yarnId).first()!!.stockSkeins, 0.0001)
        vm.adjustStock(-10.0)
        assertEquals(0.0, repository.observeYarn(yarnId).first()!!.stockSkeins, 0.0)
        vm.adjustStock(+2.0)
        assertEquals(2.0, repository.observeYarn(yarnId).first()!!.stockSkeins, 0.0001)
    }

    @Test
    fun `a swatch needs positive stitches and rows`() = runTest {
        val vm = viewModel()
        assertFalse(vm.saveSwatch(null, "0", "34", "", StitchPattern.STOCKINETTE, "", ""))
        assertFalse(vm.saveSwatch(null, "26", "", "", StitchPattern.STOCKINETTE, "", ""))
        assertFalse(vm.saveSwatch(null, "abc", "34", "", StitchPattern.STOCKINETTE, "", ""))
        assertTrue(repository.observeSwatches(yarnId).first().isEmpty())
    }

    @Test
    fun `a swatch is saved with comma decimals and optional fields`() = runTest {
        val vm = viewModel()
        assertTrue(vm.saveSwatch(null, "26", "34", "3,5", StitchPattern.GARTER, "4,2", " lavato "))
        val swatch = repository.observeSwatches(yarnId).first().single()
        assertEquals(26, swatch.stitches)
        assertEquals(34, swatch.rows)
        assertEquals(3.5, swatch.needleSizeMm!!, 0.0)
        assertEquals(StitchPattern.GARTER, swatch.pattern)
        assertEquals(4.2, swatch.weightG!!, 0.0)
        assertEquals("lavato", swatch.notes)
    }

    @Test
    fun `optional swatch fields can stay empty`() = runTest {
        assertTrue(viewModel().saveSwatch(null, "20", "28", "", StitchPattern.STOCKINETTE, "", ""))
        val swatch = repository.observeSwatches(yarnId).first().single()
        assertEquals(null, swatch.needleSizeMm)
        assertEquals(null, swatch.weightG)
        assertEquals(null, swatch.notes)
    }

    @Test
    fun `a swatch can be edited and deleted`() = runTest {
        val vm = viewModel()
        vm.saveSwatch(null, "20", "28", "", StitchPattern.STOCKINETTE, "", "")
        val id = repository.observeSwatches(yarnId).first().single().id
        assertTrue(vm.saveSwatch(id, "22", "30", "", StitchPattern.RIB, "", ""))
        assertEquals(22, repository.observeSwatches(yarnId).first().single().stitches)
        vm.deleteSwatch(id)
        assertTrue(repository.observeSwatches(yarnId).first().isEmpty())
    }

    @Test
    fun `deleting the yarn moves the state to deleted`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitItemOfType<YarnDetailUiState.Ready>()
            vm.deleteYarn()
            awaitItemOfType<YarnDetailUiState.Deleted>()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
