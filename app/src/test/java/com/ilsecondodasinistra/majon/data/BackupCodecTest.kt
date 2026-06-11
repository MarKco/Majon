package com.ilsecondodasinistra.majon.data

import com.ilsecondodasinistra.majon.data.export.BackupCodec
import com.ilsecondodasinistra.majon.domain.model.FullPart
import com.ilsecondodasinistra.majon.domain.model.FullProject
import com.ilsecondodasinistra.majon.domain.model.NoteFrequency
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.ProjectColor
import com.ilsecondodasinistra.majon.domain.model.ProjectIcon
import com.ilsecondodasinistra.majon.domain.model.RowNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    private val codec = BackupCodec()

    private fun sample(): List<FullProject> = listOf(
        FullProject(
            project = Project(
                id = 7L,
                name = "Maglione righe",
                icon = ProjectIcon.SWEATER,
                color = ProjectColor.SAGE,
                yarnType = "Lana merino",
                needleSize = "4",
                createdAt = 111L,
                updatedAt = 222L,
            ),
            parts = listOf(
                FullPart(
                    part = Part(id = 3L, projectId = 7L, name = "Davanti", totalRows = 120, completedRows = 42),
                    notes = listOf(
                        RowNote(id = 9L, partId = 3L, rowStart = 20, rowEnd = 30, frequency = NoteFrequency.ODD_ROWS, text = "diminuisci di due"),
                    ),
                ),
            ),
        ),
    )

    @Test
    fun `export then import preserves all content`() {
        val json = codec.encode(sample(), exportedAt = 12345L)
        val decoded = codec.decode(json).getOrThrow()

        assertEquals(1, decoded.size)
        val project = decoded.first()
        assertEquals("Maglione righe", project.project.name)
        assertEquals(ProjectIcon.SWEATER, project.project.icon)
        assertEquals(ProjectColor.SAGE, project.project.color)
        assertEquals("Lana merino", project.project.yarnType)
        assertEquals("4", project.project.needleSize)

        val part = project.parts.first()
        assertEquals("Davanti", part.part.name)
        assertEquals(120, part.part.totalRows)
        assertEquals(42, part.part.completedRows)

        val note = part.notes.first()
        assertEquals(20, note.rowStart)
        assertEquals(30, note.rowEnd)
        assertEquals(NoteFrequency.ODD_ROWS, note.frequency)
        assertEquals("diminuisci di due", note.text)
    }

    @Test
    fun `decoded ids are zero so import inserts as new records`() {
        val json = codec.encode(sample(), exportedAt = 1L)
        val decoded = codec.decode(json).getOrThrow()
        assertEquals(0L, decoded.first().project.id)
        assertEquals(0L, decoded.first().parts.first().part.id)
        assertEquals(0L, decoded.first().parts.first().notes.first().id)
    }

    @Test
    fun `export contains version and timestamp`() {
        val json = codec.encode(sample(), exportedAt = 98765L)
        assertTrue(json.contains("\"version\""))
        assertTrue(json.contains("98765"))
    }

    @Test
    fun `malformed json returns failure`() {
        assertTrue(codec.decode("not json at all").isFailure)
    }

    @Test
    fun `json without majon marker returns failure`() {
        assertTrue(codec.decode("""{"foo": 1}""").isFailure)
    }

    @Test
    fun `unknown icon or frequency falls back to defaults`() {
        val json = """
            {
              "app": "majon",
              "version": 1,
              "exportedAt": 1,
              "projects": [
                {
                  "name": "X",
                  "icon": "DRAGON",
                  "color": "NEON",
                  "parts": [
                    {
                      "name": "P",
                      "totalRows": 10,
                      "completedRows": 0,
                      "notes": [
                        {"rowStart": 1, "rowEnd": 2, "frequency": "WEIRD", "text": "t"}
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        val decoded = codec.decode(json).getOrThrow()
        assertEquals(ProjectIcon.YARN, decoded.first().project.icon)
        assertEquals(ProjectColor.TERRACOTTA, decoded.first().project.color)
        assertEquals(NoteFrequency.EVERY_ROW, decoded.first().parts.first().notes.first().frequency)
    }

    @Test
    fun `empty project list round-trips`() {
        val json = codec.encode(emptyList(), exportedAt = 1L)
        assertTrue(codec.decode(json).getOrThrow().isEmpty())
    }
}
