package com.ilsecondodasinistra.majon.ui.editproject

import androidx.lifecycle.SavedStateHandle
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.NeedleType
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.ProjectLinks
import com.ilsecondodasinistra.majon.domain.model.ProjectYarnLink
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
import org.junit.Rule
import org.junit.Test

/** Choosing the yarns and needles of a project from the stash. */
class EditProjectLinksTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMajonRepository()

    private fun viewModel(projectId: Long = 0L) =
        EditProjectViewModel(repository, SavedStateHandle(mapOf("projectId" to projectId)))

    private suspend fun yarn(name: String) = repository.upsertYarn(Yarn(name = name))
    private suspend fun needle(size: Double) = repository.upsertNeedle(Needle(type = NeedleType.CIRCULAR, sizeMm = size))

    @Test
    fun `the stash is offered for choosing`() = runTest {
        yarn("Bamboo")
        needle(3.0)
        val vm = viewModel()
        assertEquals(listOf("Bamboo"), vm.yarns.value.map { it.name })
        assertEquals(listOf(3.0), vm.needles.value.map { it.sizeMm })
    }

    @Test
    fun `the first yarn chosen becomes the main one`() = runTest {
        val a = yarn("A")
        val b = yarn("B")
        val vm = viewModel()
        vm.toggleYarn(a)
        vm.toggleYarn(b)
        assertEquals(listOf(a, b), vm.links.value.yarns.map { it.yarnId })
        assertEquals(listOf(true, false), vm.links.value.yarns.map { it.isPrimary })
    }

    @Test
    fun `choosing a yarn again removes it and the next one becomes main`() = runTest {
        val a = yarn("A")
        val b = yarn("B")
        val vm = viewModel()
        vm.toggleYarn(a)
        vm.toggleYarn(b)
        vm.toggleYarn(a)
        assertEquals(listOf(b), vm.links.value.yarns.map { it.yarnId })
        assertTrue(vm.links.value.yarns.single().isPrimary)
    }

    @Test
    fun `the main yarn can be changed`() = runTest {
        val a = yarn("A")
        val b = yarn("B")
        val vm = viewModel()
        vm.toggleYarn(a)
        vm.toggleYarn(b)
        vm.setPrimaryYarn(b)
        assertEquals(listOf(false, true), vm.links.value.yarns.map { it.isPrimary })
    }

    @Test
    fun `swatch and planned skeins are set per yarn`() = runTest {
        val a = yarn("A")
        val swatch = repository.upsertSwatch(Swatch(yarnId = a, stitches = 26, rows = 34))
        val vm = viewModel()
        vm.toggleYarn(a)
        vm.setYarnSwatch(a, swatch)
        vm.setPlannedSkeins(a, "3,5")
        val link = vm.links.value.yarns.single()
        assertEquals(swatch, link.swatchId)
        assertEquals(3.5, link.plannedSkeins!!, 0.0)
        vm.setPlannedSkeins(a, "")
        assertNull(vm.links.value.yarns.single().plannedSkeins)
        vm.setPlannedSkeins(a, "abc")
        assertNull(vm.links.value.yarns.single().plannedSkeins)
    }

    @Test
    fun `swatches of every yarn are available`() = runTest {
        val a = yarn("A")
        repository.upsertSwatch(Swatch(yarnId = a, stitches = 26, rows = 34))
        val vm = viewModel()
        assertEquals(1, vm.swatchesByYarn.value[a]!!.size)
    }

    @Test
    fun `needles are chosen and unchosen`() = runTest {
        val n1 = needle(3.0)
        val n2 = needle(4.0)
        val vm = viewModel()
        vm.toggleNeedle(n1)
        vm.toggleNeedle(n2)
        assertEquals(listOf(n1, n2), vm.links.value.needleIds)
        vm.toggleNeedle(n1)
        assertEquals(listOf(n2), vm.links.value.needleIds)
    }

    @Test
    fun `saving a new project stores its links`() = runTest {
        val a = yarn("A")
        val n = needle(3.0)
        val vm = viewModel()
        vm.updateName("Sciarpa")
        vm.toggleYarn(a)
        vm.toggleNeedle(n)
        vm.save()
        val id = repository.observeProjects().first().single().project.id
        val links = repository.observeProjectLinks(id).first()
        assertEquals(listOf(a), links.yarns.map { it.yarnId })
        assertEquals(listOf(n), links.needleIds)
    }

    @Test
    fun `editing loads the links and saving keeps changes`() = runTest {
        val a = yarn("A")
        val b = yarn("B")
        val id = repository.upsertProject(Project(name = "Sciarpa"))
        repository.setProjectLinks(id, ProjectLinks(yarns = listOf(ProjectYarnLink(a, isPrimary = true, plannedSkeins = 2.0))))

        val vm = viewModel(id)
        assertEquals(listOf(a), vm.links.value.yarns.map { it.yarnId })
        assertEquals(2.0, vm.links.value.yarns.single().plannedSkeins!!, 0.0)
        vm.toggleYarn(b)
        vm.save()
        assertEquals(listOf(a, b), repository.observeProjectLinks(id).first().yarns.map { it.yarnId })
    }

    @Test
    fun `an invalid project saves no links`() = runTest {
        val a = yarn("A")
        val vm = viewModel()
        vm.toggleYarn(a)
        vm.save() // no name
        assertTrue(repository.observeProjects().first().isEmpty())
        assertTrue(vm.form.value.nameError)
    }

    @Test
    fun `needle usage shows other unfinished projects but not this one`() = runTest {
        val n = needle(4.0)
        val other = repository.upsertProject(Project(name = "Cappello"))
        repository.upsertPart(Part(projectId = other, name = "P", totalRows = 10, completedRows = 2))
        repository.setProjectLinks(other, ProjectLinks(needleIds = listOf(n)))
        val mine = repository.upsertProject(Project(name = "Sciarpa"))
        repository.setProjectLinks(mine, ProjectLinks(needleIds = listOf(n)))

        val vm = viewModel(mine)
        assertEquals(listOf("Cappello"), vm.needleUsage.value[n])
        assertFalse(vm.needleUsage.value[n].orEmpty().contains("Sciarpa"))
    }
}
