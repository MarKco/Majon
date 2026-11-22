package com.ilsecondodasinistra.majon.data

import com.ilsecondodasinistra.majon.data.export.BackupCodec
import com.ilsecondodasinistra.majon.domain.model.FinishingTask
import com.ilsecondodasinistra.majon.domain.model.FullPart
import com.ilsecondodasinistra.majon.domain.model.FullProject
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecPartExtrasTest {

    private val codec = BackupCodec()

    @Test
    fun `copies, cast-on stitches, end width and tasks round-trip`() {
        val project = FullProject(
            project = Project(name = "Maglione"),
            parts = listOf(
                FullPart(
                    Part(
                        projectId = 1L, name = "Manica", totalRows = 190, completedRows = 20,
                        copies = 2, currentCopy = 2, castOnStitches = 64, stitchCm = 21.0, endStitchCm = 47.0,
                    ),
                    emptyList(),
                ),
            ),
            finishing = listOf(
                FinishingTask(id = 5L, projectId = 1L, title = "Cuci le spalle", done = true, position = 0),
                FinishingTask(id = 6L, projectId = 1L, title = "Fili", position = 1),
            ),
        )
        val decoded = codec.decode(codec.encode(listOf(project), exportedAt = 1L)).getOrThrow().single()
        val part = decoded.parts.single().part
        assertEquals(2, part.copies)
        assertEquals(2, part.currentCopy)
        assertEquals(64, part.castOnStitches)
        assertEquals(47.0, part.endStitchCm!!, 0.0)
        assertEquals(listOf("Cuci le spalle", "Fili"), decoded.finishing.map { it.title })
        assertEquals(listOf(true, false), decoded.finishing.map { it.done })
        assertEquals(listOf(0, 1), decoded.finishing.map { it.position })
        assertTrue(decoded.finishing.all { it.id == 0L }) // ids are not exported
    }

    @Test
    fun `an old file without these fields decodes with the defaults`() {
        val json = """{"app":"majon","version":2,"exportedAt":1,"projects":[{"name":"X","parts":[{"name":"P","totalRows":10}]}]}"""
        val project = codec.decode(json).getOrThrow().single()
        val part = project.parts.single().part
        assertEquals(1, part.copies)
        assertEquals(1, part.currentCopy)
        assertNull(part.castOnStitches)
        assertNull(part.endStitchCm)
        assertTrue(project.finishing.isEmpty())
    }

    @Test
    fun `bad values are sanitized`() {
        val json = """{"app":"majon","version":2,"exportedAt":1,"projects":[{"name":"X",
          "parts":[{"name":"P","totalRows":10,"copies":0,"currentCopy":9,"castOnStitches":-3,"endStitchCm":0},
                   {"name":"Q","totalRows":10,"copies":3,"currentCopy":7}],
          "finishing":[{"title":"  "},{"title":" Fili ","done":true}]}]}"""
        val project = codec.decode(json).getOrThrow().single()
        val p = project.parts[0].part
        assertEquals(1, p.copies)
        assertEquals(1, p.currentCopy)
        assertNull(p.castOnStitches)
        assertNull(p.endStitchCm)
        assertEquals(3, project.parts[1].part.currentCopy) // never past the last copy
        assertEquals(listOf("Fili"), project.finishing.map { it.title })
        assertTrue(project.finishing.single().done)
        assertFalse(project.finishing.isEmpty())
    }
}
