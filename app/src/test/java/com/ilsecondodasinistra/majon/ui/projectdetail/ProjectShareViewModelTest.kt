package com.ilsecondodasinistra.majon.ui.projectdetail

import androidx.lifecycle.SavedStateHandle
import com.ilsecondodasinistra.majon.data.export.BackupCodec
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.NeedleType
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.ProjectLinks
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.domain.model.ProjectYarnLink
import com.ilsecondodasinistra.majon.testutil.FakeMajonRepository
import com.ilsecondodasinistra.majon.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ProjectShareViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMajonRepository()
    private val codec = BackupCodec()
    private var projectId = 0L

    @Before
    fun setUp() = runTest {
        projectId = repository.upsertProject(Project(name = "Mint sweater / taglia L?"))
        repository.upsertPart(Part(projectId = projectId, name = "Davanti", totalRows = 190, completedRows = 50))
        val yarn = repository.upsertYarn(Yarn(name = "Bamboo", stockSkeins = 4.0, shop = "Segreto"))
        val needle = repository.upsertNeedle(Needle(type = NeedleType.CIRCULAR, sizeMm = 3.0, quantity = 2))
        repository.setProjectLinks(projectId, ProjectLinks(yarns = listOf(ProjectYarnLink(yarn, isPrimary = true)), needleIds = listOf(needle)))
    }

    private fun viewModel() = ProjectShareViewModel(repository, codec, SavedStateHandle(mapOf("projectId" to projectId)))

    @Test
    fun `the file is named after the project, safe for any file system`() = runTest {
        assertEquals("Mint sweater _ taglia L.majon", viewModel().build(includeProgress = true).fileName)
    }

    @Test
    fun `an empty name still gets a name`() = runTest {
        repository.upsertProject(Project(id = projectId, name = "???"))
        assertEquals("progetto.majon", viewModel().build(includeProgress = true).fileName)
    }

    @Test
    fun `the content is a project file without what is private`() = runTest {
        val shared = viewModel().build(includeProgress = true)
        assertFalse(shared.content.contains("Segreto"))
        val data = codec.decodeAll(shared.content).getOrThrow()
        assertEquals(50, data.projects.single().parts.single().part.completedRows)
        assertEquals(0.0, data.stash.yarns.single().yarn.stockSkeins, 0.0)
        assertEquals(0, data.stash.needles.single().quantity)
    }

    @Test
    fun `as a model the progress is left out`() = runTest {
        val data = codec.decodeAll(viewModel().build(includeProgress = false).content).getOrThrow()
        assertEquals(0, data.projects.single().parts.single().part.completedRows)
        assertTrue(data.projects.single().parts.single().part.totalRows == 190)
    }
}
