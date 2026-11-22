package com.ilsecondodasinistra.majon.ui.stash

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ilsecondodasinistra.majon.domain.model.StitchPattern
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.Yarn
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

class SwatchEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMajonRepository()
    private var yarnId = 0L

    @Before
    fun setUp() = runTest {
        yarnId = repository.upsertYarn(Yarn(name = "Bamboo"))
    }

    private fun viewModel(swatchId: Long = 0L) =
        SwatchEditViewModel(repository, SavedStateHandle(mapOf("yarnId" to yarnId, "swatchId" to swatchId)))

    private suspend fun swatches() = repository.observeSwatches(yarnId).first()

    @Test
    fun `a new swatch starts empty, in stockinette`() = runTest {
        val form = viewModel().form.value
        assertEquals("", form.stitches)
        assertEquals("", form.rows)
        assertEquals(StitchPattern.STOCKINETTE, form.pattern)
        assertFalse(form.isEditing)
    }

    @Test
    fun `stitches and rows must be whole numbers above zero`() = runTest {
        val vm = viewModel()
        assertFalse(vm.save())
        assertTrue(vm.form.value.error)
        vm.update { it.copy(stitches = "0", rows = "34") }
        assertFalse(vm.save())
        vm.update { it.copy(stitches = "26", rows = "") }
        assertFalse(vm.save())
        vm.update { it.copy(stitches = "abc", rows = "34") }
        assertFalse(vm.save())
        assertTrue(swatches().isEmpty())
    }

    @Test
    fun `fixing the numbers clears the error`() = runTest {
        val vm = viewModel()
        vm.save()
        assertTrue(vm.form.value.error)
        vm.update { it.copy(stitches = "26") }
        assertFalse(vm.form.value.error)
    }

    @Test
    fun `a swatch is saved with comma decimals and optional fields`() = runTest {
        val vm = viewModel()
        vm.saved.test {
            vm.update {
                it.copy(stitches = "26", rows = "34", needleMm = "3,5", pattern = StitchPattern.GARTER, weightG = "4,2", notes = " lavato ")
            }
            assertTrue(vm.save())
            awaitItem()
        }
        val swatch = swatches().single()
        assertEquals(26, swatch.stitches)
        assertEquals(34, swatch.rows)
        assertEquals(3.5, swatch.needleSizeMm!!, 0.0)
        assertEquals(StitchPattern.GARTER, swatch.pattern)
        assertEquals(4.2, swatch.weightG!!, 0.0)
        assertEquals("lavato", swatch.notes)
        assertEquals(yarnId, swatch.yarnId)
    }

    @Test
    fun `optional fields can stay empty`() = runTest {
        val vm = viewModel()
        vm.update { it.copy(stitches = "20", rows = "28") }
        assertTrue(vm.save())
        val swatch = swatches().single()
        assertNull(swatch.needleSizeMm)
        assertNull(swatch.weightG)
        assertNull(swatch.notes)
    }

    @Test
    fun `a needle size or weight that is not above zero is rejected`() = runTest {
        val vm = viewModel()
        vm.update { it.copy(stitches = "20", rows = "28", needleMm = "0") }
        assertFalse(vm.save())
        vm.update { it.copy(needleMm = "", weightG = "-2") }
        assertFalse(vm.save())
        vm.update { it.copy(weightG = "abc") }
        assertFalse(vm.save())
        assertTrue(swatches().isEmpty())
    }

    @Test
    fun `editing loads the swatch and saves over it`() = runTest {
        val id = repository.upsertSwatch(
            Swatch(yarnId = yarnId, stitches = 26, rows = 34, needleSizeMm = 3.0, pattern = StitchPattern.RIB, weightG = 4.0, notes = "n"),
        )
        val vm = viewModel(id)
        val form = vm.form.value
        assertTrue(form.isEditing)
        assertEquals("26", form.stitches)
        assertEquals("34", form.rows)
        assertEquals("3", form.needleMm)
        assertEquals(StitchPattern.RIB, form.pattern)
        assertEquals("4", form.weightG)
        vm.update { it.copy(stitches = "28") }
        assertTrue(vm.save())
        val all = swatches()
        assertEquals(1, all.size)
        assertEquals(28, all.single().stitches)
        assertEquals(id, all.single().id)
    }
}
