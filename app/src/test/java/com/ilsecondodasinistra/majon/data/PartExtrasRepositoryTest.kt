package com.ilsecondodasinistra.majon.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ilsecondodasinistra.majon.data.db.MajonDatabase
import com.ilsecondodasinistra.majon.data.repository.MajonRepositoryImpl
import com.ilsecondodasinistra.majon.domain.model.FinishingTask
import com.ilsecondodasinistra.majon.domain.model.FullPart
import com.ilsecondodasinistra.majon.domain.model.FullProject
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Copies, cast-on stitches and the end width of a part, and the finishing tasks of a project. */
@RunWith(RobolectricTestRunner::class)
class PartExtrasRepositoryTest {

    private lateinit var db: MajonDatabase
    private lateinit var repository: MajonRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MajonDatabase::class.java).allowMainThreadQueries().build()
        repository = MajonRepositoryImpl(
            db, db.projectDao(), db.partDao(), db.noteDao(),
            db.yarnDao(), db.swatchDao(), db.needleDao(), db.projectLinkDao(), clock = { 1L },
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun project() = repository.upsertProject(Project(name = "Maglione"))

    // --- the part ---

    @Test
    fun `a part keeps its copies, cast-on stitches and end width`() = runTest {
        val id = repository.upsertPart(
            Part(
                projectId = project(), name = "Manica", totalRows = 190,
                copies = 2, currentCopy = 2, castOnStitches = 64, stitchCm = 21.0, endStitchCm = 47.0,
            ),
        )
        val part = repository.observePart(id).first()!!
        assertEquals(2, part.copies)
        assertEquals(2, part.currentCopy)
        assertEquals(64, part.castOnStitches)
        assertEquals(47.0, part.endStitchCm!!, 0.0)
    }

    @Test
    fun `a plain part has one copy and nothing else`() = runTest {
        val id = repository.upsertPart(Part(projectId = project(), name = "Corpo", totalRows = 10))
        val part = repository.observePart(id).first()!!
        assertEquals(1, part.copies)
        assertEquals(1, part.currentCopy)
        assertNull(part.castOnStitches)
        assertNull(part.endStitchCm)
    }

    @Test
    fun `duplicating a project keeps the copies and the stitches, with the count back at the first copy`() = runTest {
        val p = project()
        repository.upsertPart(
            Part(
                projectId = p, name = "Manica", totalRows = 190, completedRows = 50,
                copies = 2, currentCopy = 2, castOnStitches = 64, endStitchCm = 47.0,
            ),
        )
        val copy = repository.observeProject(repository.duplicateProject(p, "Copia")!!).first()!!.parts.single()
        assertEquals(2, copy.copies)
        assertEquals(1, copy.currentCopy)
        assertEquals(0, copy.completedRows)
        assertEquals(64, copy.castOnStitches)
        assertEquals(47.0, copy.endStitchCm!!, 0.0)
    }

    // --- finishing tasks ---

    @Test
    fun `finishing tasks come back in order`() = runTest {
        val p = project()
        repository.upsertFinishingTask(FinishingTask(projectId = p, title = "Seconda", position = 1))
        repository.upsertFinishingTask(FinishingTask(projectId = p, title = "Prima", position = 0))
        assertEquals(listOf("Prima", "Seconda"), repository.observeFinishingTasks(p).first().map { it.title })
    }

    @Test
    fun `a task can be ticked, renamed and deleted`() = runTest {
        val p = project()
        val id = repository.upsertFinishingTask(FinishingTask(projectId = p, title = "Cuci le spalle"))
        assertFalse(repository.observeFinishingTasks(p).first().single().done)
        repository.upsertFinishingTask(FinishingTask(id = id, projectId = p, title = "Cuci le spalle", done = true))
        assertTrue(repository.observeFinishingTasks(p).first().single().done)
        repository.upsertFinishingTask(FinishingTask(id = id, projectId = p, title = "Cuci le spalle e il collo", done = true))
        assertEquals("Cuci le spalle e il collo", repository.observeFinishingTasks(p).first().single().title)
        repository.deleteFinishingTask(id)
        assertTrue(repository.observeFinishingTasks(p).first().isEmpty())
    }

    @Test
    fun `tasks belong to their project and go with it`() = runTest {
        val p1 = project()
        val p2 = repository.upsertProject(Project(name = "Altro"))
        repository.upsertFinishingTask(FinishingTask(projectId = p1, title = "A"))
        repository.upsertFinishingTask(FinishingTask(projectId = p2, title = "B"))
        assertEquals(listOf("A"), repository.observeFinishingTasks(p1).first().map { it.title })
        repository.deleteProject(p1)
        assertTrue(repository.observeFinishingTasks(p1).first().isEmpty())
        assertEquals(listOf("B"), repository.observeFinishingTasks(p2).first().map { it.title })
    }

    @Test
    fun `duplicating a project copies its tasks, all still to do`() = runTest {
        val p = project()
        repository.upsertFinishingTask(FinishingTask(projectId = p, title = "A", done = true, position = 0))
        repository.upsertFinishingTask(FinishingTask(projectId = p, title = "B", position = 1))
        val copyId = repository.duplicateProject(p, "Copia")!!
        val tasks = repository.observeFinishingTasks(copyId).first()
        assertEquals(listOf("A", "B"), tasks.map { it.title })
        assertTrue(tasks.none { it.done })
    }

    // --- backup ---

    @Test
    fun `full projects carry their tasks, and importing them makes new ones`() = runTest {
        val p = project()
        repository.upsertFinishingTask(FinishingTask(projectId = p, title = "A", done = true, position = 0))
        repository.upsertFinishingTask(FinishingTask(projectId = p, title = "B", position = 1))
        val full = repository.getFullProjects().single()
        assertEquals(listOf("A", "B"), full.finishing.map { it.title })
        assertEquals(listOf(true, false), full.finishing.map { it.done })

        val before = repository.observeProjects().first().size
        repository.importProjects(
            listOf(
                FullProject(
                    project = Project(name = "Importato"),
                    parts = listOf(FullPart(Part(projectId = 0L, name = "P", totalRows = 3, copies = 2, currentCopy = 2), emptyList())),
                    finishing = listOf(FinishingTask(projectId = 0L, title = "Fili", done = true, position = 0)),
                ),
            ),
        )
        val imported = repository.observeProjects().first().first { it.project.name == "Importato" }
        assertEquals(before + 1, repository.observeProjects().first().size)
        assertEquals(2, imported.parts.single().copies)
        val tasks = repository.observeFinishingTasks(imported.project.id).first()
        assertEquals(listOf("Fili"), tasks.map { it.title })
        assertTrue(tasks.single().done)
    }
}
