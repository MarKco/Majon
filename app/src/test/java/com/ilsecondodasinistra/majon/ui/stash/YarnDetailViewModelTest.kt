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
    fun `a swatch can be deleted`() = runTest {
        val id = repository.upsertSwatch(Swatch(yarnId = yarnId, stitches = 20, rows = 28))
        viewModel().deleteSwatch(id)
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
