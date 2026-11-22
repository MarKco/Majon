package com.ilsecondodasinistra.majon.ui.projectdetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ilsecondodasinistra.majon.domain.model.MeasureKey
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.NeedleType
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.PresetPart
import com.ilsecondodasinistra.majon.domain.model.PresetPartName
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.ProjectIcon
import com.ilsecondodasinistra.majon.domain.model.ProjectLinks
import com.ilsecondodasinistra.majon.domain.model.ProjectYarnLink
import com.ilsecondodasinistra.majon.domain.model.RowNote
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.domain.model.YarnCheck
import com.ilsecondodasinistra.majon.testutil.FakeMajonRepository
import com.ilsecondodasinistra.majon.testutil.MainDispatcherRule
import com.ilsecondodasinistra.majon.ui.counter.awaitItemOfType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Planning a project: its yarns, the measures of its parts, and copying it. */
class ProjectPlanViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMajonRepository()
    private var projectId = 0L
    private var yarnId = 0L
    private var swatchId = 0L

    @Before
    fun setUp() = runTest {
        projectId = repository.upsertProject(Project(name = "Sciarpa", icon = ProjectIcon.SCARF))
        yarnId = repository.upsertYarn(Yarn(name = "Bamboo", skeinWeightG = 100, skeinLengthM = 350, stockSkeins = 2.5))
        swatchId = repository.upsertSwatch(Swatch(yarnId = yarnId, stitches = 26, rows = 34, needleSizeMm = 3.0, weightG = 4.0))
    }

    private fun viewModel() = ProjectDetailViewModel(repository, SavedStateHandle(mapOf("projectId" to projectId)))

    private suspend fun linkYarn() =
        repository.setProjectLinks(projectId, ProjectLinks(yarns = listOf(ProjectYarnLink(yarnId, isPrimary = true))))

    @Test
    fun `the state says whether the yarn is enough for the measured parts`() = runTest {
        linkYarn()
        repository.upsertPart(
            Part(
                projectId = projectId, name = "Corpo", totalRows = 510,
                stitchMeasure = MeasureKey.WIDTH, stitchCm = 20.0, rowMeasure = MeasureKey.LENGTH, rowCm = 150.0,
            ),
        )
        viewModel().uiState.test {
            val s = awaitItemOfType<ProjectDetailUiState.Ready>()
            val plan = s.plans.single()
            assertEquals("Bamboo", plan.yarn.name)
            assertEquals(1.2, plan.neededSkeins!!, 0.0001)
            assertTrue(plan.check is YarnCheck.Enough)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the state exposes the needles and the default swatch`() = runTest {
        linkYarn()
        val n = repository.upsertNeedle(Needle(type = NeedleType.CIRCULAR, sizeMm = 3.0))
        repository.setProjectLinks(
            projectId,
            ProjectLinks(yarns = listOf(ProjectYarnLink(yarnId, isPrimary = true)), needleIds = listOf(n)),
        )
        viewModel().uiState.test {
            val s = awaitItemOfType<ProjectDetailUiState.Ready>()
            assertEquals(listOf(n), s.needles.map { it.id })
            assertEquals(swatchId, s.defaultSwatch!!.id)
            assertEquals(listOf(swatchId), s.swatches.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `without links there are no plans and no default swatch`() = runTest {
        viewModel().uiState.test {
            val s = awaitItemOfType<ProjectDetailUiState.Ready>()
            assertTrue(s.plans.isEmpty())
            assertNull(s.defaultSwatch)
            assertTrue(s.needles.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `using skeins takes them off the stock`() = runTest {
        linkYarn()
        val vm = viewModel()
        vm.useSkeins(yarnId, 1.25)
        assertEquals(1.25, repository.observeYarn(yarnId).first()!!.stockSkeins, 0.0001)
    }

    // --- measures on parts ---

    // --- suggested parts ---

    @Test
    fun `the suggested parts are created in one go with their rows still to define`() = runTest {
        val vm = viewModel()
        vm.addSuggestedParts(
            listOf(
                PresetPart(PresetPartName.FRONT, MeasureKey.WIDTH, MeasureKey.LENGTH) to "Davanti",
                PresetPart(PresetPartName.SLEEVE, MeasureKey.CIRCUMFERENCE, MeasureKey.LENGTH) to "Manica",
            ),
        )
        val parts = repository.observeProject(projectId).first()!!.parts
        assertEquals(listOf("Davanti", "Manica"), parts.map { it.name })
        assertEquals(listOf(0, 0), parts.map { it.totalRows })
        assertEquals(MeasureKey.CIRCUMFERENCE, parts[1].stitchMeasure)
        assertEquals(MeasureKey.LENGTH, parts[1].rowMeasure)
        assertTrue(parts.all { it.completedRows == 0 && !it.isComplete })
    }

    @Test
    fun `suggested parts can all be created worked sideways`() = runTest {
        viewModel().addSuggestedParts(
            listOf(
                PresetPart(PresetPartName.FRONT, MeasureKey.WIDTH, MeasureKey.LENGTH) to "Davanti",
                PresetPart(PresetPartName.SLEEVE, MeasureKey.CIRCUMFERENCE, MeasureKey.HEIGHT) to "Manica",
            ),
            sideways = true,
        )
        val parts = repository.observeProject(projectId).first()!!.parts
        assertEquals(listOf(MeasureKey.LENGTH, MeasureKey.HEIGHT), parts.map { it.stitchMeasure })
        assertEquals(listOf(MeasureKey.WIDTH, MeasureKey.CIRCUMFERENCE), parts.map { it.rowMeasure })
    }

    @Test
    fun `suggested parts that already exist are not created twice`() = runTest {
        repository.upsertPart(Part(projectId = projectId, name = "Davanti", totalRows = 120))
        viewModel().addSuggestedParts(
            listOf(
                PresetPart(PresetPartName.FRONT, MeasureKey.WIDTH, MeasureKey.LENGTH) to "davanti",
                PresetPart(PresetPartName.BACK, MeasureKey.WIDTH, MeasureKey.LENGTH) to "Dietro",
            ),
        )
        assertEquals(listOf("Davanti", "Dietro"), repository.observeProject(projectId).first()!!.parts.map { it.name })
    }

    // --- copying ---

    @Test
    fun `duplicating a project announces the copy`() = runTest {
        repository.upsertPart(Part(projectId = projectId, name = "Corpo", totalRows = 10, completedRows = 4))
        val vm = viewModel()
        vm.duplicated.test {
            vm.duplicateProject("Sciarpa (copia)")
            val newId = awaitItem()
            val copy = repository.observeProject(newId).first()!!
            assertEquals("Sciarpa (copia)", copy.project.name)
            assertEquals(0, copy.parts.single().completedRows)
            assertFalse(newId == projectId)
        }
    }

    @Test
    fun `duplicating keeps the row notes`() = runTest {
        val part = repository.upsertPart(Part(projectId = projectId, name = "Corpo", totalRows = 10))
        repository.upsertNote(RowNote(partId = part, rowStart = 2, rowEnd = 2, text = "aumenta"))
        val vm = viewModel()
        vm.duplicated.test {
            vm.duplicateProject("Copia")
            val newId = awaitItem()
            val newPart = repository.observeProject(newId).first()!!.parts.single()
            assertEquals(1, repository.observeNotes(newPart.id).first().size)
        }
    }
}
