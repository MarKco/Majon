package com.ilsecondodasinistra.majon.data

import com.ilsecondodasinistra.majon.data.export.BackupCodec
import com.ilsecondodasinistra.majon.domain.model.FullPart
import com.ilsecondodasinistra.majon.domain.model.FullProject
import com.ilsecondodasinistra.majon.domain.model.NoteFrequency
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.RowNote
import org.junit.Assert.assertEquals
import org.junit.Test

/** How notes of every kind are written to a backup and read back, including old files and files written by hand. */
class BackupCodecNotesTest {

    private val codec = BackupCodec()

    private fun notesOf(json: String, totalRows: Int = 100): List<RowNote> {
        val text = """{"app":"majon","version":3,"exportedAt":1,"projects":[{"name":"X","parts":[{"name":"P","totalRows":$totalRows,"notes":$json}]}]}"""
        return codec.decode(text).getOrThrow().single().parts.single().notes
    }

    @Test
    fun `notes on exact rows and every Nth row round-trip`() {
        val project = FullProject(
            project = Project(name = "Maglione"),
            parts = listOf(
                FullPart(
                    Part(projectId = 1L, name = "Davanti", totalRows = 190),
                    listOf(
                        RowNote(partId = 1L, rowStart = 10, rowEnd = 30, frequency = NoteFrequency.SPECIFIC, rows = listOf(10, 20, 30), text = "diminuisci"),
                        RowNote(partId = 1L, rowStart = 12, rowEnd = 190, frequency = NoteFrequency.EVERY_NTH, step = 6, text = "aumenta"),
                        RowNote(partId = 1L, rowStart = 3, rowEnd = 9, frequency = NoteFrequency.ODD_ROWS, text = "dritto"),
                    ),
                ),
            ),
        )
        val notes = codec.decode(codec.encode(listOf(project), exportedAt = 1L)).getOrThrow().single().parts.single().notes
        assertEquals(listOf(10, 20, 30), notes[0].rows)
        assertEquals(NoteFrequency.SPECIFIC, notes[0].frequency)
        assertEquals(NoteFrequency.EVERY_NTH, notes[1].frequency)
        assertEquals(6, notes[1].step)
        assertEquals(12, notes[1].rowStart)
        assertEquals(190, notes[1].rowEnd)
        assertEquals(NoteFrequency.ODD_ROWS, notes[2].frequency)
    }

    @Test
    fun `a note written the old way still reads`() {
        val notes = notesOf("""[{"rowStart":20,"rowEnd":30,"frequency":"ODD_ROWS","text":"diminuisci di due"}]""")
        assertEquals(NoteFrequency.ODD_ROWS, notes.single().frequency)
        assertEquals(20, notes.single().rowStart)
        assertEquals(30, notes.single().rowEnd)
        assertEquals(1, notes.single().step)
        assertEquals(emptyList<Int>(), notes.single().rows)
    }

    @Test
    fun `exact rows can be written without a frequency or a range`() {
        val note = notesOf("""[{"rows":[30,10,20],"text":"x"}]""").single()
        assertEquals(NoteFrequency.SPECIFIC, note.frequency)
        assertEquals(listOf(10, 20, 30), note.rows)
        assertEquals(10, note.rowStart)
        assertEquals(30, note.rowEnd)
    }

    @Test
    fun `every Nth can be written with only the step, and runs to the end of the part`() {
        val note = notesOf("""[{"every":6,"text":"x"}]""", totalRows = 190).single()
        assertEquals(NoteFrequency.EVERY_NTH, note.frequency)
        assertEquals(1, note.rowStart)
        assertEquals(190, note.rowEnd)
    }

    @Test
    fun `a single row needs only its number`() {
        val note = notesOf("""[{"rowStart":7,"text":"x"}]""").single()
        assertEquals(7, note.rowStart)
        assertEquals(7, note.rowEnd)
        assertEquals(NoteFrequency.EVERY_ROW, note.frequency)
    }

    @Test
    fun `a backup is read leniently, what can be repaired is and what makes no sense is left out`() {
        val notes = notesOf(
            """[{"rowStart":0,"rowEnd":-4,"text":"a"},
                {"rowStart":5,"frequency":"NOPE","text":"b"},
                {"rowStart":5,"text":"  "},
                {"rows":[],"text":"c"},
                {"rows":[0,3,-1],"text":"d"},
                {"every":1,"rowStart":4,"text":"e"},
                {"text":"no rows at all"}]""",
        )
        assertEquals(listOf("a", "b", "d", "e"), notes.map { it.text })
        assertEquals(1, notes[0].rowStart)
        assertEquals(1, notes[0].rowEnd)
        assertEquals(NoteFrequency.EVERY_ROW, notes[1].frequency)
        assertEquals(listOf(3), notes[2].rows)
        assertEquals(NoteFrequency.EVERY_ROW, notes[3].frequency) // a step of 1 is just every row
    }
}
