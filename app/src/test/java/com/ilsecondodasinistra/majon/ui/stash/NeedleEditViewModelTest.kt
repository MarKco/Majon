package com.ilsecondodasinistra.majon.ui.stash

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.NeedleMaterial
import com.ilsecondodasinistra.majon.domain.model.NeedleType
import com.ilsecondodasinistra.majon.testutil.FakeMajonRepository
import com.ilsecondodasinistra.majon.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NeedleEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMajonRepository()

    private fun viewModel(needleId: Long = 0L) =
        NeedleEditViewModel(repository, SavedStateHandle(mapOf("needleId" to needleId)))

    private suspend fun needles() = repository.observeNeedles().first()

    @Test
    fun `a new needle starts as a circular one, quantity one`() = runTest {
        val form = viewModel().form.value
        assertEquals(NeedleType.CIRCULAR, form.type)
        assertEquals("1", form.quantity)
        assertFalse(form.isEditing)
    }

    @Test
    fun `a needle needs only a type`() = runTest {
        val vm = viewModel()
        vm.update { it.copy(type = NeedleType.STRAIGHT) }
        assertTrue(vm.save())
        val needle = needles().single()
        assertEquals(NeedleType.STRAIGHT, needle.type)
        assertNull(needle.sizeMm)
        assertNull(needle.lengthCm)
    }

    @Test
    fun `a needle is saved with every field`() = runTest {
        val vm = viewModel()
        vm.saved.test {
            vm.update {
                it.copy(
                    type = NeedleType.CIRCULAR, material = NeedleMaterial.BAMBOO, sizeMm = "3,5", lengthCm = "80",
                    quantity = "2", brand = " KnitPro ", notes = "ok",
                )
            }
            assertTrue(vm.save())
            awaitItem()
        }
        val needle = needles().single()
        assertEquals(3.5, needle.sizeMm!!, 0.0)
        assertEquals(80, needle.lengthCm)
        assertEquals(NeedleMaterial.BAMBOO, needle.material)
        assertEquals(2, needle.quantity)
        assertEquals("KnitPro", needle.brand)
    }

    @Test
    fun `quantity below one or a zero size is rejected`() = runTest {
        val vm = viewModel()
        vm.update { it.copy(sizeMm = "3", quantity = "0") }
        assertFalse(vm.save())
        assertTrue(vm.form.value.error)
        vm.update { it.copy(sizeMm = "0", quantity = "1") }
        assertFalse(vm.save())
        vm.update { it.copy(sizeMm = "", quantity = "") }
        assertFalse(vm.save())
        vm.update { it.copy(sizeMm = "3", quantity = "1", lengthCm = "0") }
        assertFalse(vm.save())
        assertTrue(needles().isEmpty())
    }

    @Test
    fun `editing loads the needle and saves over it`() = runTest {
        val id = repository.upsertNeedle(
            Needle(type = NeedleType.DPN, material = NeedleMaterial.WOOD, sizeMm = 2.5, lengthCm = 20, quantity = 5, brand = "B", notes = "n"),
        )
        val vm = viewModel(id)
        val form = vm.form.value
        assertTrue(form.isEditing)
        assertEquals(NeedleType.DPN, form.type)
        assertEquals("2.5", form.sizeMm)
        assertEquals("20", form.lengthCm)
        assertEquals("5", form.quantity)
        vm.update { it.copy(sizeMm = "3") }
        assertTrue(vm.save())
        val all = needles()
        assertEquals(1, all.size)
        assertEquals(3.0, all.single().sizeMm!!, 0.0)
        assertEquals(id, all.single().id)
    }
}
