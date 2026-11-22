package com.ilsecondodasinistra.majon.ui.partedit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ilsecondodasinistra.majon.domain.model.MeasureKey
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.PresetPart
import com.ilsecondodasinistra.majon.domain.model.PresetPartName
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.ProjectIcon
import com.ilsecondodasinistra.majon.domain.model.ProjectLinks
import com.ilsecondodasinistra.majon.domain.model.ProjectYarnLink
import com.ilsecondodasinistra.majon.domain.model.RowStep
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

class PartEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMajonRepository()
    private var projectId = 0L
    private var yarnId = 0L
    private var swatchId = 0L

    @Before
    fun setUp() = runTest {
        projectId = repository.upsertProject(Project(name = "Sciarpa", icon = ProjectIcon.SCARF))
        yarnId = repository.upsertYarn(Yarn(name = "Bamboo", skeinWeightG = 100, stockSkeins = 2.5))
        // The label of the Bamboo yarn: 26 stitches x 34 rows over 10 x 10 cm
        swatchId = repository.upsertSwatch(Swatch(yarnId = yarnId, stitches = 26, rows = 34, needleSizeMm = 3.0, weightG = 4.0))
    }

    private suspend fun linkYarn() =
        repository.setProjectLinks(projectId, ProjectLinks(yarns = listOf(ProjectYarnLink(yarnId, isPrimary = true))))

    private fun viewModel(partId: Long = 0L) =
        PartEditViewModel(repository, SavedStateHandle(mapOf("projectId" to projectId, "partId" to partId)))

    private suspend fun parts() = repository.observeProject(projectId).first()!!.parts

    // --- a new part ---

    @Test
    fun `a new part starts with the measures typical of the project type`() = runTest {
        val form = viewModel().form.value
        assertEquals(MeasureKey.WIDTH, form.stitchMeasure)   // a scarf: width x length
        assertEquals(MeasureKey.LENGTH, form.rowMeasure)
        assertFalse(form.isEditing)
        val hat = repository.upsertProject(Project(name = "Cappello", icon = ProjectIcon.HAT))
        val hatForm = PartEditViewModel(repository, SavedStateHandle(mapOf("projectId" to hat))).form.value
        assertEquals(MeasureKey.CIRCUMFERENCE, hatForm.stitchMeasure)
        assertEquals(MeasureKey.HEIGHT, hatForm.rowMeasure)
    }

    @Test
    fun `the context offers the suggested parts and the swatches of the linked yarns`() = runTest {
        linkYarn()
        val ctx = viewModel().context.value
        assertEquals(ProjectIcon.SCARF, ctx.projectIcon)
        assertEquals(listOf(PresetPartName.MAIN), ctx.presets.map { it.name })
        assertEquals(listOf(swatchId), ctx.swatches.map { it.id })
        assertEquals(swatchId, ctx.defaultSwatch!!.id)
        assertEquals(mapOf(yarnId to "Bamboo"), ctx.yarnNames)
    }

    @Test
    fun `picking a suggested part fills the name and the measures`() = runTest {
        val vm = viewModel()
        vm.applyPreset(PresetPart(PresetPartName.SLEEVE, MeasureKey.CIRCUMFERENCE, MeasureKey.LENGTH), "Manica")
        assertEquals("Manica", vm.form.value.name)
        assertEquals(MeasureKey.CIRCUMFERENCE, vm.form.value.stitchMeasure)
        assertEquals(MeasureKey.LENGTH, vm.form.value.rowMeasure)
    }

    @Test
    fun `a name is required`() = runTest {
        val vm = viewModel()
        vm.update { it.copy(rows = "10") }
        assertFalse(vm.save())
        assertTrue(vm.form.value.nameError)
        assertTrue(parts().isEmpty())
        vm.update { it.copy(name = "Davanti") }
        assertFalse(vm.form.value.nameError)
    }

    @Test
    fun `rows are required when there is nothing to estimate them from`() = runTest {
        val vm = viewModel()
        vm.update { it.copy(name = "Davanti") }
        assertFalse(vm.save())
        assertTrue(vm.form.value.rowsError)
        vm.update { it.copy(rows = "0") }
        assertFalse(vm.save())
        vm.update { it.copy(rows = "abc") }
        assertFalse(vm.save())
        assertTrue(parts().isEmpty())
    }

    @Test
    fun `a part is saved with its rows`() = runTest {
        val vm = viewModel()
        vm.saved.test {
            vm.update { it.copy(name = " Davanti ", rows = "120") }
            assertTrue(vm.save())
            awaitItem()
        }
        val part = parts().single()
        assertEquals("Davanti", part.name)
        assertEquals(120, part.totalRows)
        assertEquals(projectId, part.projectId)
    }

    // --- rows from the measures ---

    @Test
    fun `the estimate follows the measures and the swatch`() = runTest {
        linkYarn()
        val vm = viewModel()
        vm.update { it.copy(stitchCm = "20", rowCm = "150") }
        assertEquals(52, vm.estimate.value.stitches)
        assertEquals(510, vm.estimate.value.rows)
        vm.update { it.copy(stitchCm = "20,5") } // comma decimals
        assertEquals(53, vm.estimate.value.stitches)
    }

    @Test
    fun `without a linked yarn there is no estimate`() = runTest {
        val vm = viewModel()
        vm.update { it.copy(stitchCm = "20", rowCm = "150") }
        assertNull(vm.estimate.value.stitches)
        assertNull(vm.estimate.value.rows)
    }

    @Test
    fun `rows left empty are taken from the measures`() = runTest {
        linkYarn()
        val vm = viewModel()
        vm.update { it.copy(name = "Davanti", stitchCm = "12", rowCm = "22") }
        assertTrue(vm.save())
        assertEquals(75, parts().single().totalRows)   // 22 cm = 2.2 swatches of 34 rows
    }

    @Test
    fun `rows typed win over the estimate`() = runTest {
        linkYarn()
        val vm = viewModel()
        vm.update { it.copy(name = "Davanti", rows = "80", stitchCm = "12", rowCm = "22") }
        assertTrue(vm.save())
        assertEquals(80, parts().single().totalRows)
    }

    @Test
    fun `measures that give no rows still leave the rows required`() = runTest {
        linkYarn()
        val vm = viewModel()
        vm.update { it.copy(name = "Davanti", stitchCm = "12") } // only the stitch direction
        assertFalse(vm.save())
        assertTrue(vm.form.value.rowsError)
    }

    @Test
    fun `the estimated rows can be copied into the field`() = runTest {
        linkYarn()
        val vm = viewModel()
        vm.update { it.copy(rowCm = "150") }
        vm.useEstimatedRows()
        assertEquals("510", vm.form.value.rows)
    }

    @Test
    fun `a part can use another swatch than the main yarn's`() = runTest {
        linkYarn()
        val loose = repository.upsertSwatch(Swatch(yarnId = yarnId, stitches = 20, rows = 28))
        val vm = viewModel()
        vm.update { it.copy(stitchCm = "20", rowCm = "100", swatchId = loose) }
        assertEquals(40, vm.estimate.value.stitches)
        assertEquals(280, vm.estimate.value.rows)
        vm.update { it.copy(name = "P", swatchId = loose) }
        vm.save()
        assertEquals(loose, parts().single().swatchId)
    }

    // --- measures and extras are stored ---

    @Test
    fun `measures are stored, invalid ones as none`() = runTest {
        val vm = viewModel()
        vm.update {
            it.copy(
                name = "A", rows = "10",
                stitchMeasure = MeasureKey.CIRCUMFERENCE, stitchCm = "55,5",
                rowMeasure = MeasureKey.HEIGHT, rowCm = "abc",
            )
        }
        vm.save()
        val part = parts().single()
        assertEquals(MeasureKey.CIRCUMFERENCE, part.stitchMeasure)
        assertEquals(55.5, part.stitchCm!!, 0.0)
        assertEquals(MeasureKey.HEIGHT, part.rowMeasure)
        assertNull(part.rowCm)
    }

    @Test
    fun `extra rows are stored with trimmed notes, blank notes as none`() = runTest {
        val vm = viewModel()
        vm.update {
            it.copy(name = "Manica", rows = "40", prepRow = true, prepNote = " Avvia 50 ", finalRow = true, finalNote = "  ")
        }
        vm.save()
        val part = parts().single()
        assertTrue(part.hasPrepRow && part.hasFinalRow)
        assertEquals("Avvia 50", part.prepNote)
        assertNull(part.finalNote)
    }

    // --- editing ---

    private suspend fun existing(completed: Int = 0, prep: Boolean = false, final: Boolean = false, total: Int = 100) =
        repository.upsertPart(
            Part(
                projectId = projectId, name = "Davanti", totalRows = total, completedRows = completed,
                hasPrepRow = prep, prepNote = if (prep) "Avvia" else null, hasFinalRow = final,
                stitchMeasure = MeasureKey.WIDTH, stitchCm = 20.0, rowMeasure = MeasureKey.LENGTH, rowCm = 150.0,
                swatchId = swatchId,
            ),
        )

    @Test
    fun `editing loads the part into the form`() = runTest {
        val id = existing(completed = 41, prep = true)
        val form = viewModel(id).form.value
        assertTrue(form.isEditing)
        assertEquals("Davanti", form.name)
        assertEquals("100", form.rows)
        assertTrue(form.prepRow)
        assertEquals("Avvia", form.prepNote)
        assertEquals("20", form.stitchCm)
        assertEquals("150", form.rowCm)
        assertEquals(swatchId, form.swatchId)
        assertEquals(40, form.rowsDone) // 41 steps = the preparation row and 40 rows
    }

    @Test
    fun `saving an edit keeps the id and the progress`() = runTest {
        val id = existing(completed = 30)
        val vm = viewModel(id)
        vm.update { it.copy(name = "Davanti v2", rows = "90") }
        assertTrue(vm.save())
        val part = repository.observePart(id).first()!!
        assertEquals("Davanti v2", part.name)
        assertEquals(90, part.totalRows)
        assertEquals(30, part.completedRows)
        assertEquals(1, parts().size)
    }

    @Test
    fun `shrinking the total clamps the progress, counting the extra rows`() = runTest {
        val id = existing(completed = 80, final = true)
        val vm = viewModel(id)
        vm.update { it.copy(rows = "50") }
        vm.save()
        assertEquals(51, repository.observePart(id).first()!!.completedRows) // 50 rows + the final row
    }

    @Test
    fun `turning the preparation row on keeps the knitter on the same row`() = runTest {
        val id = existing(completed = 4, total = 10)
        val vm = viewModel(id)
        vm.update { it.copy(prepRow = true) }
        vm.save()
        val part = repository.observePart(id).first()!!
        assertEquals(5, part.completedRows)
        assertEquals(RowStep.Row(5), part.currentStep)
    }

    @Test
    fun `editing clears measures that were emptied`() = runTest {
        val id = existing()
        val vm = viewModel(id)
        vm.update { it.copy(stitchCm = "", rowCm = "") }
        vm.save()
        val part = repository.observePart(id).first()!!
        assertNull(part.stitchCm)
        assertNull(part.rowCm)
    }
}
