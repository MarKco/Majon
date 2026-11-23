package com.ilsecondodasinistra.majon.data

import com.ilsecondodasinistra.majon.data.export.BackupCodec
import com.ilsecondodasinistra.majon.data.export.DetectedImport
import com.ilsecondodasinistra.majon.data.export.NotesCodec
import com.ilsecondodasinistra.majon.data.export.NotesDecodeResult
import com.ilsecondodasinistra.majon.data.export.NotesIssue
import com.ilsecondodasinistra.majon.data.export.NotesIssueKind
import com.ilsecondodasinistra.majon.domain.model.NoteDraft
import com.ilsecondodasinistra.majon.domain.model.NoteFrequency
import com.ilsecondodasinistra.majon.domain.model.NotesFile
import com.ilsecondodasinistra.majon.domain.model.NotesFilePart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reading a `.majon-notes` file: written by hand or by an AI, so every problem is reported at once, with where it is. */
class NotesCodecTest {

    private val codec = NotesCodec(BackupCodec())

    private fun ok(text: String): NotesFile = (codec.decode(text) as NotesDecodeResult.Success).file

    private fun issues(text: String): List<NotesIssue> = (codec.decode(text) as NotesDecodeResult.Failure).issues

    private val sample = """
        {"app":"majon-notes","version":1,"parts":[
          {"part":"Davanti","prepNote":"Avvia 164 maglie","notes":[
            {"rowStart":7,"text":"aumenta"},
            {"rowStart":20,"rowEnd":30,"frequency":"ODD_ROWS","text":"diminuisci"},
            {"rows":[10,20,30],"text":"segnapunti"},
            {"every":6,"rowStart":12,"text":"ogni sei"}]},
          {"part":"Manica","notes":[{"rowStart":1,"text":"coste"}]}]}
    """.trimIndent()

    // --- reading ---

    @Test
    fun `a complete file is read, with every kind of note`() {
        val file = ok(sample)
        assertEquals(listOf("Davanti", "Manica"), file.parts.map { it.name })
        val davanti = file.parts[0]
        assertEquals("Avvia 164 maglie", davanti.prepNote)
        assertNull(davanti.finalNote)
        assertEquals(4, davanti.notes.size)
        assertEquals(NoteDraft(rowStart = 7, rowEnd = 7, text = "aumenta"), davanti.notes[0])
        assertEquals(NoteFrequency.ODD_ROWS, davanti.notes[1].frequency)
        assertEquals(NoteFrequency.SPECIFIC, davanti.notes[2].frequency)
        assertEquals(listOf(10, 20, 30), davanti.notes[2].rows)
        assertEquals(NoteFrequency.EVERY_NTH, davanti.notes[3].frequency)
        assertEquals(null, davanti.notes[3].rowEnd) // up to the last row of whichever part it goes to
    }

    @Test
    fun `the marker and the version may be left out`() {
        val file = ok("""{"parts":[{"part":"A","notes":[{"rowStart":1,"text":"x"}]}]}""")
        assertEquals(1, file.parts.single().notes.size)
    }

    @Test
    fun `an AI answer in a code block with words around it is read`() {
        val answer = "Ecco il file:\n```json\n$sample\n```\nFammi sapere se serve altro."
        assertEquals(2, ok(answer).parts.size)
    }

    @Test
    fun `a code block with no language is read too`() {
        assertEquals(2, ok("```\n$sample\n```").parts.size)
    }

    @Test
    fun `a part appearing twice is merged`() {
        val file = ok("""{"parts":[{"part":"A","notes":[{"rowStart":1,"text":"x"}]},{"part":" a ","notes":[{"rowStart":2,"text":"y"}]}]}""")
        assertEquals(1, file.parts.size)
        assertEquals(listOf("x", "y"), file.parts.single().notes.map { it.text })
    }

    @Test
    fun `unknown fields are ignored`() {
        assertEquals(1, ok("""{"parts":[{"part":"A","colore":"rosso","notes":[{"rowStart":1,"text":"x","altro":1}]}],"extra":true}""").parts.size)
    }

    // --- what is wrong ---

    @Test
    fun `text that is not JSON is a syntax problem`() {
        assertEquals(NotesIssueKind.SYNTAX, issues("questo non è json").single().kind)
        assertEquals(NotesIssueKind.SYNTAX, issues("").single().kind)
        assertEquals(NotesIssueKind.SYNTAX, issues("""{"parts":[{"part":"A",}""").single().kind)
    }

    @Test
    fun `a file of another kind is refused`() {
        assertEquals(NotesIssueKind.NOT_A_NOTES_FILE, issues("""{"app":"proportion","parts":[]}""").single().kind)
    }

    @Test
    fun `a newer version is refused`() {
        assertEquals(NotesIssueKind.FUTURE_VERSION, issues("""{"app":"majon-notes","version":99,"parts":[{"part":"A","notes":[{"rowStart":1,"text":"x"}]}]}""").single().kind)
    }

    @Test
    fun `a file with no parts or no notes is refused`() {
        assertEquals(NotesIssueKind.NO_NOTES, issues("""{"parts":[]}""").single().kind)
        assertEquals(NotesIssueKind.NO_NOTES, issues("""{"parts":[{"part":"A","notes":[]}]}""").single().kind)
    }

    @Test
    fun `every problem is reported, with the part and the number of the note`() {
        val found = issues(
            """{"parts":[
              {"part":"Davanti","notes":[
                {"rowStart":0,"text":"a"},
                {"rowStart":5,"text":"  "},
                {"rowStart":9,"rowEnd":4,"text":"c"},
                {"rowStart":2,"frequency":"SPESSO","text":"d"},
                {"every":1,"frequency":"EVERY_NTH","text":"e"},
                {"frequency":"SPECIFIC","text":"f"},
                {"text":"g"}]},
              {"part":" ","notes":[{"rowStart":1,"text":"h"}]}]}""",
        )
        fun kind(part: String?, note: Int?) = found.first { it.part == part && it.note == note }.kind
        assertEquals(NotesIssueKind.ROW_BELOW_ONE, kind("Davanti", 1))
        assertEquals(NotesIssueKind.TEXT_BLANK, kind("Davanti", 2))
        assertEquals(NotesIssueKind.END_BEFORE_START, kind("Davanti", 3))
        assertEquals(NotesIssueKind.UNKNOWN_FREQUENCY, kind("Davanti", 4))
        assertEquals(NotesIssueKind.STEP_INVALID, kind("Davanti", 5))
        assertEquals(NotesIssueKind.ROWS_EMPTY, kind("Davanti", 6))
        assertEquals(NotesIssueKind.MISSING_ROW, kind("Davanti", 7))
        assertTrue(found.any { it.kind == NotesIssueKind.PART_WITHOUT_NAME })
        assertEquals(8, found.size)
        assertEquals("SPESSO", found.first { it.kind == NotesIssueKind.UNKNOWN_FREQUENCY }.detail)
    }

    // --- writing ---

    @Test
    fun `a file written and read back is the same`() {
        val file = NotesFile(
            listOf(
                NotesFilePart(
                    "Davanti", prepNote = "Avvia", finalNote = "Chiudi",
                    notes = listOf(
                        NoteDraft(rowStart = 7, rowEnd = 7, text = "aumenta"),
                        NoteDraft(rowStart = 10, rowEnd = 30, frequency = NoteFrequency.SPECIFIC, rows = listOf(10, 20, 30), text = "x"),
                        NoteDraft(rowStart = 12, rowEnd = null, frequency = NoteFrequency.EVERY_NTH, step = 6, text = "y"),
                    ),
                ),
            ),
        )
        assertEquals(file, ok(codec.encode(file)))
    }

    @Test
    fun `the written file says what it is`() {
        val text = codec.encode(NotesFile(listOf(NotesFilePart("A", null, null, listOf(NoteDraft(rowStart = 1, text = "x"))))))
        assertTrue(text.contains("\"app\": \"majon-notes\""))
        assertTrue(text.contains("\"version\": 1"))
    }

    // --- which kind of file it is ---

    @Test
    fun `a notes file is recognised`() {
        assertTrue(codec.detect(sample) is DetectedImport.Notes)
    }

    @Test
    fun `a project file is recognised`() {
        val project = """{"app":"majon","version":3,"exportedAt":1,"projects":[{"name":"X","parts":[{"name":"P","totalRows":10,"notes":[{"rowStart":1,"text":"x"}]}]}]}"""
        val detected = codec.detect(project) as DetectedImport.Project
        assertEquals("X", detected.data.projects.single().project.name)
    }

    @Test
    fun `the notes of a project file can be taken on their own`() {
        val project = """{"app":"majon","version":3,"exportedAt":1,"projects":[
            {"name":"X","parts":[{"name":"P","totalRows":10,"notes":[{"rowStart":1,"text":"x"},{"rows":[3,4],"text":"y"}]},{"name":"Vuota","totalRows":5}]}]}"""
        val detected = codec.detect(project) as DetectedImport.Project
        val notes = codec.notesOf(detected.data.projects.single())
        assertEquals(listOf("P"), notes.parts.map { it.name }) // a part with no notes brings nothing
        assertEquals(listOf("x", "y"), notes.parts.single().notes.map { it.text })
        assertEquals(listOf(3, 4), notes.parts.single().notes[1].rows)
    }

    @Test
    fun `anything else is invalid`() {
        assertTrue(codec.detect("ciao") is DetectedImport.Invalid)
        assertTrue(codec.detect("""{"app":"altro"}""") is DetectedImport.Invalid)
    }

    @Test
    fun `a project file with no valid project is invalid`() {
        assertTrue(codec.detect("""{"app":"majon","version":3,"exportedAt":1,"projects":[{"name":" "}]}""") is DetectedImport.Invalid)
    }
}
