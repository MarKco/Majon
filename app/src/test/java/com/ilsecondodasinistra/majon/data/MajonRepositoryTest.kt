package com.ilsecondodasinistra.majon.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ilsecondodasinistra.majon.data.db.MajonDatabase
import com.ilsecondodasinistra.majon.data.repository.MajonRepositoryImpl
import com.ilsecondodasinistra.majon.domain.model.NoteFrequency
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.ProjectColor
import com.ilsecondodasinistra.majon.domain.model.ProjectIcon
import com.ilsecondodasinistra.majon.domain.model.RowNote
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MajonRepositoryTest {

    private lateinit var db: MajonDatabase
    private lateinit var repository: MajonRepositoryImpl
    private var now = 1_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MajonDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MajonRepositoryImpl(
            projectDao = db.projectDao(),
            partDao = db.partDao(),
            noteDao = db.noteDao(),
            clock = { now },
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun newProject(name: String = "Maglione rosso") = Project(
        name = name,
        icon = ProjectIcon.SWEATER,
        color = ProjectColor.CORAL,
        yarnType = "Merino 100%",
        needleSize = "4.5",
    )

    @Test
    fun `created project appears in observed list with fields intact`() = runTest {
        val id = repository.upsertProject(newProject())
        val projects = repository.observeProjects().first()
        assertEquals(1, projects.size)
        with(projects.first().project) {
            assertEquals(id, this.id)
            assertEquals("Maglione rosso", name)
            assertEquals(ProjectIcon.SWEATER, icon)
            assertEquals(ProjectColor.CORAL, color)
            assertEquals("Merino 100%", yarnType)
            assertEquals("4.5", needleSize)
            assertEquals(now, createdAt)
        }
    }

    @Test
    fun `updating a project keeps createdAt and refreshes updatedAt`() = runTest {
        val id = repository.upsertProject(newProject())
        now = 2_000L
        val saved = repository.observeProject(id).first()!!.project
        repository.upsertProject(saved.copy(name = "Maglione blu"))
        val updated = repository.observeProject(id).first()!!.project
        assertEquals("Maglione blu", updated.name)
        assertEquals(1_000L, updated.createdAt)
        assertEquals(2_000L, updated.updatedAt)
    }

    @Test
    fun `parts are linked to their project`() = runTest {
        val projectId = repository.upsertProject(newProject())
        repository.upsertPart(Part(projectId = projectId, name = "Davanti", totalRows = 120))
        repository.upsertPart(Part(projectId = projectId, name = "Dietro", totalRows = 120))

        val withParts = repository.observeProject(projectId).first()!!
        assertEquals(listOf("Davanti", "Dietro"), withParts.parts.map { it.name })
    }

    @Test
    fun `setCompletedRows updates the counter`() = runTest {
        val projectId = repository.upsertProject(newProject())
        val partId = repository.upsertPart(Part(projectId = projectId, name = "Davanti", totalRows = 120))
        repository.setCompletedRows(partId, 42)
        assertEquals(42, repository.observePart(partId).first()!!.completedRows)
    }

    @Test
    fun `deleting a project cascades to parts and notes`() = runTest {
        val projectId = repository.upsertProject(newProject())
        val partId = repository.upsertPart(Part(projectId = projectId, name = "Davanti", totalRows = 120))
        repository.upsertNote(RowNote(partId = partId, rowStart = 1, rowEnd = 5, text = "costine"))

        repository.deleteProject(projectId)

        assertTrue(repository.observeProjects().first().isEmpty())
        assertNull(repository.observePart(partId).first())
        assertTrue(repository.observeNotes(partId).first().isEmpty())
    }

    @Test
    fun `notes round-trip with frequency and range`() = runTest {
        val projectId = repository.upsertProject(newProject())
        val partId = repository.upsertPart(Part(projectId = projectId, name = "Davanti", totalRows = 120))
        repository.upsertNote(
            RowNote(partId = partId, rowStart = 20, rowEnd = 30, frequency = NoteFrequency.ODD_ROWS, text = "diminuisci di due"),
        )
        val notes = repository.observeNotes(partId).first()
        assertEquals(1, notes.size)
        with(notes.first()) {
            assertEquals(20, rowStart)
            assertEquals(30, rowEnd)
            assertEquals(NoteFrequency.ODD_ROWS, frequency)
            assertEquals("diminuisci di due", text)
        }
    }

    @Test
    fun `notes are sorted by start row`() = runTest {
        val projectId = repository.upsertProject(newProject())
        val partId = repository.upsertPart(Part(projectId = projectId, name = "Davanti", totalRows = 120))
        repository.upsertNote(RowNote(partId = partId, rowStart = 50, rowEnd = 50, text = "b"))
        repository.upsertNote(RowNote(partId = partId, rowStart = 10, rowEnd = 20, text = "a"))
        assertEquals(listOf("a", "b"), repository.observeNotes(partId).first().map { it.text })
    }

    @Test
    fun `deleting a part removes it and its notes`() = runTest {
        val projectId = repository.upsertProject(newProject())
        val partId = repository.upsertPart(Part(projectId = projectId, name = "Manica", totalRows = 80))
        repository.upsertNote(RowNote(partId = partId, rowStart = 1, rowEnd = 1, text = "x"))

        repository.deletePart(partId)

        assertNull(repository.observePart(partId).first())
        assertTrue(repository.observeNotes(partId).first().isEmpty())
    }

    @Test
    fun `deleting a note removes only that note`() = runTest {
        val projectId = repository.upsertProject(newProject())
        val partId = repository.upsertPart(Part(projectId = projectId, name = "Davanti", totalRows = 120))
        val keepId = repository.upsertNote(RowNote(partId = partId, rowStart = 1, rowEnd = 1, text = "keep"))
        val dropId = repository.upsertNote(RowNote(partId = partId, rowStart = 2, rowEnd = 2, text = "drop"))

        repository.deleteNote(dropId)

        val notes = repository.observeNotes(partId).first()
        assertEquals(listOf(keepId), notes.map { it.id })
    }
}
