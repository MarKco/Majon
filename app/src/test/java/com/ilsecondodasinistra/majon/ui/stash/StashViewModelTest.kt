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
    fun `a needle needs only a type`() = runTest {
        assertTrue(viewModel().saveNeedle(null, NeedleType.STRAIGHT, null, "", "", "1", "", ""))
        val needle = repository.observeNeedles().first().single()
        assertEquals(NeedleType.STRAIGHT, needle.type)
        assertEquals(null, needle.sizeMm)
        assertEquals(null, needle.lengthCm)
    }

    @Test
    fun `a needle is saved with every field`() = runTest {
        val vm = viewModel()
        assertTrue(vm.saveNeedle(null, NeedleType.CIRCULAR, NeedleMaterial.BAMBOO, "3,5", "80", "2", " KnitPro ", "ok"))
        val needle = repository.observeNeedles().first().single()
        assertEquals(3.5, needle.sizeMm!!, 0.0)
        assertEquals(80, needle.lengthCm)
        assertEquals(NeedleMaterial.BAMBOO, needle.material)
        assertEquals(2, needle.quantity)
        assertEquals("KnitPro", needle.brand)
    }

    @Test
    fun `quantity below one or a zero size is rejected`() = runTest {
        val vm = viewModel()
        assertFalse(vm.saveNeedle(null, NeedleType.DPN, null, "3", "", "0", "", ""))
        assertFalse(vm.saveNeedle(null, NeedleType.DPN, null, "0", "", "1", "", ""))
        assertFalse(vm.saveNeedle(null, NeedleType.DPN, null, "", "", "", "", ""))
        assertTrue(repository.observeNeedles().first().isEmpty())
    }

    @Test
    fun `a needle can be edited and deleted`() = runTest {
        val vm = viewModel()
        vm.saveNeedle(null, NeedleType.DPN, null, "2", "", "5", "", "")
        val id = repository.observeNeedles().first().single().id
        assertTrue(vm.saveNeedle(id, NeedleType.DPN, null, "2,5", "", "5", "", ""))
        assertEquals(2.5, repository.observeNeedles().first().single().sizeMm!!, 0.0)
        vm.deleteNeedle(id)
        assertTrue(repository.observeNeedles().first().isEmpty())
    }

    @Test
    fun `a yarn can be deleted from the list`() = runTest {
        val id = repository.upsertYarn(Yarn(name = "Lana"))
        viewModel().deleteYarn(id)
        assertTrue(repository.observeYarns().first().isEmpty())
    }
}
