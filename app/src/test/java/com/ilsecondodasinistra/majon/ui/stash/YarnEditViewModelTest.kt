package com.ilsecondodasinistra.majon.ui.stash

import androidx.lifecycle.SavedStateHandle
import com.ilsecondodasinistra.majon.domain.model.MaterialShare
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.domain.model.YarnMaterial
import com.ilsecondodasinistra.majon.domain.model.YarnWeight
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

class YarnEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMajonRepository()

    private fun viewModel(yarnId: Long = 0L) = YarnEditViewModel(
        repository = repository,
        savedStateHandle = SavedStateHandle(mapOf("yarnId" to yarnId)),
    )

    @Test
    fun `a name is required`() = runTest {
        val vm = viewModel()
        assertFalse(vm.save())
        assertTrue(vm.form.value.nameError)
        assertTrue(repository.observeYarns().first().isEmpty())
        vm.update { it.copy(name = "Bamboo") }
        assertFalse(vm.form.value.nameError)
    }

    @Test
    fun `saves every field, accepting comma decimals`() = runTest {
        val vm = viewModel()
        vm.update {
            it.copy(
                name = " Bamboo ", brand = "WAK", colorName = "Mustard", colorArgb = 0xFFE0A800.toInt(),
                dyeLot = "L1", weight = YarnWeight.DK,
                skeinWeightG = "100", skeinLengthM = "350", stockSkeins = "2,5",
                pricePerSkein = "6,90", shop = "Negozio", care = "30°", notes = "morbida",
            )
        }
        vm.setMaterials(listOf(MaterialRow(YarnMaterial.BAMBOO, percent = "100")))
        assertTrue(vm.save())
        val yarn = repository.observeYarns().first().single()
        assertEquals("Bamboo", yarn.name)
        assertEquals(YarnWeight.DK, yarn.weight)
        assertEquals(100, yarn.skeinWeightG)
        assertEquals(350, yarn.skeinLengthM)
        assertEquals(2.5, yarn.stockSkeins, 0.0)
        assertEquals(6.9, yarn.pricePerSkein!!, 0.0)
        assertEquals(listOf(MaterialShare(YarnMaterial.BAMBOO, percent = 100)), yarn.materials)
    }

    @Test
    fun `blank optional fields are stored as null`() = runTest {
        val vm = viewModel()
        vm.update { it.copy(name = "Lana", brand = "  ", skeinWeightG = "", pricePerSkein = "abc") }
        assertTrue(vm.save())
        val yarn = repository.observeYarns().first().single()
        assertNull(yarn.brand)
        assertNull(yarn.skeinWeightG)
        assertNull(yarn.pricePerSkein)
        assertEquals(0.0, yarn.stockSkeins, 0.0)
    }

    @Test
    fun `a blend over one hundred percent is rejected`() = runTest {
        val vm = viewModel()
        vm.update { it.copy(name = "Lana") }
        vm.setMaterials(
            listOf(MaterialRow(YarnMaterial.WOOL, percent = "70"), MaterialRow(YarnMaterial.SILK, percent = "40")),
        )
        assertFalse(vm.save())
        assertTrue(vm.form.value.materialsError)
        assertTrue(repository.observeYarns().first().isEmpty())
    }

    @Test
    fun `a blend without percentages is fine`() = runTest {
        val vm = viewModel()
        vm.update { it.copy(name = "Lana") }
        vm.setMaterials(listOf(MaterialRow(YarnMaterial.WOOL), MaterialRow(YarnMaterial.SILK)))
        assertTrue(vm.save())
        assertEquals(2, repository.observeYarns().first().single().materials.size)
    }

    @Test
    fun `custom fibre keeps its name`() = runTest {
        val vm = viewModel()
        vm.update { it.copy(name = "Lana") }
        vm.setMaterials(listOf(MaterialRow(YarnMaterial.OTHER, customName = " Yak ", percent = "20")))
        assertTrue(vm.save())
        val share = repository.observeYarns().first().single().materials.single()
        assertEquals(YarnMaterial.OTHER, share.material)
        assertEquals("Yak", share.customName)
    }

    @Test
    fun `editing loads the yarn and keeps its id`() = runTest {
        val id = repository.upsertYarn(
            Yarn(name = "Merino", stockSkeins = 1.25, materials = listOf(MaterialShare(YarnMaterial.MERINO, percent = 100))),
        )
        val vm = viewModel(id)
        assertEquals("Merino", vm.form.value.name)
        assertEquals("1.25", vm.form.value.stockSkeins)
        assertTrue(vm.form.value.isEditing)
        assertEquals(listOf(MaterialRow(YarnMaterial.MERINO, percent = "100")), vm.form.value.materials)
        vm.update { it.copy(name = "Merino extra") }
        assertTrue(vm.save())
        val all = repository.observeYarns().first()
        assertEquals(1, all.size)
        assertEquals("Merino extra", all.single().name)
        assertEquals(id, all.single().id)
    }

    @Test
    fun `the stock stepper moves by a quarter and never below zero`() = runTest {
        val vm = viewModel()
        vm.changeStock(+0.25)
        vm.changeStock(+0.25)
        assertEquals("0.5", vm.form.value.stockSkeins)
        vm.changeStock(+1.0)
        assertEquals("1.5", vm.form.value.stockSkeins)
        vm.changeStock(-5.0)
        assertEquals("0", vm.form.value.stockSkeins)
    }

    @Test
    fun `the stepper starts from what was typed`() = runTest {
        val vm = viewModel()
        vm.update { it.copy(stockSkeins = "2,5") }
        vm.changeStock(+0.25)
        assertEquals("2.75", vm.form.value.stockSkeins)
    }

    @Test
    fun `the photo path is kept`() = runTest {
        val vm = viewModel()
        vm.update { it.copy(name = "Lana") }
        vm.setPhoto("/data/yarn_1.jpg")
        assertTrue(vm.save())
        assertEquals("/data/yarn_1.jpg", repository.observeYarns().first().single().photoPath)
    }
}
