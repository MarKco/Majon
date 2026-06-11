package com.ilsecondodasinistra.majon.ui.editproject

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.ProjectColor
import com.ilsecondodasinistra.majon.domain.model.ProjectIcon
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

class EditProjectViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `new project starts with defaults`() = runTest {
        val vm = EditProjectViewModel(FakeMajonRepository(), SavedStateHandle())
        val form = vm.form.value
        assertEquals("", form.name)
        assertEquals(ProjectIcon.SWEATER, form.icon)
        assertEquals(ProjectColor.TERRACOTTA, form.color)
        assertFalse(form.isEditing)
    }

    @Test
    fun `editing loads existing project into form`() = runTest {
        val repository = FakeMajonRepository()
        val id = repository.upsertProject(
            Project(name = "Sciarpa", icon = ProjectIcon.SCARF, color = ProjectColor.OCEAN, yarnType = "Alpaca", needleSize = "5"),
        )
        val vm = EditProjectViewModel(repository, SavedStateHandle(mapOf("projectId" to id)))
        vm.form.test {
            val form = awaitItemMatching { it.name.isNotEmpty() }
            assertEquals("Sciarpa", form.name)
            assertEquals(ProjectIcon.SCARF, form.icon)
            assertEquals(ProjectColor.OCEAN, form.color)
            assertEquals("Alpaca", form.yarnType)
            assertEquals("5", form.needleSize)
            assertTrue(form.isEditing)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `save with blank name fails validation and does not persist`() = runTest {
        val repository = FakeMajonRepository()
        val vm = EditProjectViewModel(repository, SavedStateHandle())
        vm.updateName("   ")
        vm.save()
        assertTrue(vm.form.value.nameError)
        assertNull(vm.savedEvent.value)
        assertTrue(repository.observeProjects().first().isEmpty())
    }

    @Test
    fun `save persists new project and emits saved event`() = runTest {
        val repository = FakeMajonRepository()
        val vm = EditProjectViewModel(repository, SavedStateHandle())
        vm.updateName("Berretto")
        vm.updateIcon(ProjectIcon.HAT)
        vm.updateColor(ProjectColor.FUCHSIA)
        vm.updateYarnType("Mohair")
        vm.updateNeedleSize("3.5")
        vm.save()

        assertEquals(Unit, vm.savedEvent.value)
        val saved = repository.observeProjects().first().single().project
        assertEquals("Berretto", saved.name)
        assertEquals(ProjectIcon.HAT, saved.icon)
        assertEquals(ProjectColor.FUCHSIA, saved.color)
        assertEquals("Mohair", saved.yarnType)
        assertEquals("3.5", saved.needleSize)
    }

    @Test
    fun `typing after error clears it`() = runTest {
        val vm = EditProjectViewModel(FakeMajonRepository(), SavedStateHandle())
        vm.save()
        assertTrue(vm.form.value.nameError)
        vm.updateName("X")
        assertFalse(vm.form.value.nameError)
    }
}

suspend inline fun <T> app.cash.turbine.TurbineTestContext<T>.awaitItemMatching(predicate: (T) -> Boolean): T {
    while (true) {
        val item = awaitItem()
        if (predicate(item)) return item
    }
}
