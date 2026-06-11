package com.ilsecondodasinistra.majon.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ilsecondodasinistra.majon.data.db.MajonDatabase
import com.ilsecondodasinistra.majon.data.repository.MajonRepositoryImpl
import com.ilsecondodasinistra.majon.domain.model.FullPart
import com.ilsecondodasinistra.majon.domain.model.FullProject
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.RowNote
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImportExportRepositoryTest {

    private lateinit var db: MajonDatabase
    private lateinit var repository: MajonRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MajonDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MajonRepositoryImpl(db.projectDao(), db.partDao(), db.noteDao(), clock = { 1L })
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `getFullProjects returns projects with parts and notes`() = runTest {
        val projectId = repository.upsertProject(Project(name = "P1"))
        val partId = repository.upsertPart(Part(projectId = projectId, name = "Davanti", totalRows = 100))
        repository.upsertNote(RowNote(partId = partId, rowStart = 1, rowEnd = 3, text = "n1"))

        val full = repository.getFullProjects()
        assertEquals(1, full.size)
        assertEquals("P1", full.first().project.name)
        assertEquals(1, full.first().parts.size)
        assertEquals("n1", full.first().parts.first().notes.first().text)
    }

    @Test
    fun `importProjects adds new projects keeping existing data`() = runTest {
        repository.upsertProject(Project(name = "Esistente"))

        val imported = listOf(
            FullProject(
                project = Project(name = "Importato"),
                parts = listOf(
                    FullPart(
                        part = Part(projectId = 0L, name = "Manica", totalRows = 80, completedRows = 12),
                        notes = listOf(RowNote(partId = 0L, rowStart = 5, rowEnd = 5, text = "marker")),
                    ),
                ),
            ),
        )
        val count = repository.importProjects(imported)
        assertEquals(1, count)

        val all = repository.observeProjects().first()
        assertEquals(setOf("Esistente", "Importato"), all.map { it.project.name }.toSet())

        val importedProject = all.first { it.project.name == "Importato" }
        assertEquals(1, importedProject.parts.size)
        val part = importedProject.parts.first()
        assertEquals(80, part.totalRows)
        assertEquals(12, part.completedRows)
        assertEquals("marker", repository.observeNotes(part.id).first().first().text)
    }
}
