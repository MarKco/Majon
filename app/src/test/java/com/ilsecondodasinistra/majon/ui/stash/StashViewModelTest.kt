package com.ilsecondodasinistra.majon.ui.stash

import app.cash.turbine.test
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.NeedleMaterial
import com.ilsecondodasinistra.majon.domain.model.NeedleType
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.testutil.FakeMajonRepository
import com.ilsecondodasinistra.majon.testutil.MainDispatcherRule
import com.ilsecondodasinistra.majon.ui.counter.awaitItemOfType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StashViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMajonRepository()
    private fun viewModel() = StashViewModel(repository)

    @Test
    fun `state lists yarns by name and needles by size`() = runTest {
        repository.upsertYarn(Yarn(name = "zebra"))
        repository.upsertYarn(Yarn(name = "Alpaca"))
        repository.upsertNeedle(Needle(type = NeedleType.DPN, sizeMm = 4.0))
        repository.upsertNeedle(Needle(type = NeedleType.CIRCULAR, sizeMm = 2.5))
        viewModel().uiState.test {
            val s = awaitItemOfType<StashUiState.Ready>()
            assertEquals(listOf("Alpaca", "zebra"), s.yarns.map { it.name })
            assertEquals(listOf(2.5, 4.0), s.needles.map { it.sizeMm })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a needle can be deleted`() = runTest {
        val id = repository.upsertNeedle(Needle(type = NeedleType.DPN, sizeMm = 2.0))
        viewModel().deleteNeedle(id)
        assertTrue(repository.observeNeedles().first().isEmpty())
    }

    @Test
    fun `a yarn can be deleted from the list`() = runTest {
        val id = repository.upsertYarn(Yarn(name = "Lana"))
        viewModel().deleteYarn(id)
        assertTrue(repository.observeYarns().first().isEmpty())
    }
}
