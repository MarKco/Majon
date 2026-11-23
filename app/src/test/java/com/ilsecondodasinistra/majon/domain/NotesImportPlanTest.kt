package com.ilsecondodasinistra.majon.domain

import com.ilsecondodasinistra.majon.domain.model.NoteDraft
import com.ilsecondodasinistra.majon.domain.model.NoteFrequency
import com.ilsecondodasinistra.majon.domain.model.NotesFile
import com.ilsecondodasinistra.majon.domain.model.NotesFilePart
import com.ilsecondodasinistra.majon.domain.model.NotesImportTarget
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.PartChoice
import com.ilsecondodasinistra.majon.domain.model.PlanIssueKind
import com.ilsecondodasinistra.majon.domain.model.RowNote
import com.ilsecondodasinistra.majon.domain.model.planNotesImport
import com.ilsecondodasinistra.majon.domain.model.suggestChoices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Putting the notes of a file on the parts of a project: what goes where, what is a duplicate, what is wrong. */
class NotesImportPlanTest {

    private val davanti = Part(id = 1L, projectId = 9L, name = "Davanti", totalRows = 100, hasPrepRow = true)
    private val manica = Part(id = 2L, projectId = 9L, name = "Manica", totalRows = 50)
    private val parts = listOf(davanti, manica)

    private fun part(name: String, vararg notes: NoteDraft, prep: String? = null, final: String? = null) =
        NotesFilePart(name, prep, final, notes.toList())

    private fun note(row: Int, text: String = "x") = NoteDraft(rowStart = row, rowEnd = row, text = text)

    // --- which part gets what ---

    @Test
    fun `parts are matched by name ignoring case and spaces, the others are to be created`() {
        val file = NotesFile(listOf(part(" davanti ", note(1)), part("MANICA", note(1)), part("Collo", note(1))))
        assertEquals(
            listOf(PartChoice.Existing(1L), PartChoice.Existing(2L), PartChoice.Create),
            suggestChoices(file, parts),
        )
    }

    @Test
    fun `notes go to the part chosen, with its id`() {
        val file = NotesFile(listOf(part("Davanti", note(5, "aumenta"), note(9, "diminuisci"))))
        val plan = planNotesImport(file, listOf(PartChoice.Existing(1L)), parts, existing = emptyMap())
        assertTrue(plan.errors.isEmpty())
        val addition = plan.additions.single()
        assertEquals(NotesImportTarget.Existing(1L, "Davanti"), addition.target)
        assertEquals(listOf(1L, 1L), addition.notes.map { it.partId })
        assertEquals(listOf("aumenta", "diminuisci"), addition.notes.map { it.text })
        assertEquals(2, plan.noteCount)
    }

    @Test
    fun `a file part can be sent to a part with another name`() {
        val file = NotesFile(listOf(part("Parte anteriore", note(5))))
        val plan = planNotesImport(file, listOf(PartChoice.Existing(2L)), parts, emptyMap())
        assertEquals(2L, plan.additions.single().notes.single().partId)
    }

    @Test
    fun `a skipped part brings nothing`() {
        val file = NotesFile(listOf(part("Davanti", note(5)), part("Manica", note(6))))
        val plan = planNotesImport(file, listOf(PartChoice.Skip, PartChoice.Existing(2L)), parts, emptyMap())
        assertEquals(listOf("Manica"), plan.additions.map { it.filePartName })
    }

    @Test
    fun `a part to create takes the highest row of its notes as its rows`() {
        val file = NotesFile(listOf(part("Collo", note(5), NoteDraft(rowStart = 3, rowEnd = 40, text = "y"))))
        val plan = planNotesImport(file, listOf(PartChoice.Create), parts, emptyMap())
        assertEquals(NotesImportTarget.New("Collo", totalRows = 40, hasPrepRow = false, hasFinalRow = false), plan.additions.single().target)
        assertTrue(plan.warnings.isEmpty())
    }

    // --- not adding twice ---

    @Test
    fun `a note already in the part is not added again`() {
        val existing = mapOf(1L to listOf(RowNote(partId = 1L, rowStart = 5, rowEnd = 5, text = "Aumenta")))
        val file = NotesFile(listOf(part("Davanti", note(5, " aumenta "), note(6, "nuova"))))
        val plan = planNotesImport(file, listOf(PartChoice.Existing(1L)), parts, existing)
        assertEquals(listOf("nuova"), plan.additions.single().notes.map { it.text })
        assertEquals(1, plan.additions.single().duplicates)
        assertEquals(1, plan.duplicateCount)
    }

    @Test
    fun `the same note twice in the file counts once`() {
        val file = NotesFile(listOf(part("Davanti", note(5), note(5))))
        val plan = planNotesImport(file, listOf(PartChoice.Existing(1L)), parts, emptyMap())
        assertEquals(1, plan.additions.single().notes.size)
        assertEquals(1, plan.duplicateCount)
    }

    @Test
    fun `the same words on other rows are another note`() {
        val existing = mapOf(1L to listOf(RowNote(partId = 1L, rowStart = 5, rowEnd = 5, text = "x")))
        val plan = planNotesImport(NotesFile(listOf(part("Davanti", note(6)))), listOf(PartChoice.Existing(1L)), parts, existing)
        assertEquals(1, plan.additions.single().notes.size)
    }

    // --- every Nth to the end ---

    @Test
    fun `every Nth with no end runs to the last row of the part`() {
        val file = NotesFile(listOf(part("Manica", NoteDraft(rowStart = 5, frequency = NoteFrequency.EVERY_NTH, step = 6, text = "x"))))
        val note = planNotesImport(file, listOf(PartChoice.Existing(2L)), parts, emptyMap()).additions.single().notes.single()
        assertEquals(50, note.rowEnd)
        assertEquals(6, note.step)
    }

    @Test
    fun `every Nth with no end cannot go into a part with no rows yet`() {
        val empty = Part(id = 3L, projectId = 9L, name = "Vuota", totalRows = 0)
        val file = NotesFile(listOf(part("Vuota", NoteDraft(rowStart = 5, frequency = NoteFrequency.EVERY_NTH, step = 6, text = "x"))))
        val plan = planNotesImport(file, listOf(PartChoice.Existing(3L)), listOf(empty), emptyMap())
        assertEquals(PlanIssueKind.OPEN_END_UNKNOWN, plan.errors.single().kind)
        assertFalse(plan.canImport)
    }

    @Test
    fun `every Nth with no end cannot make a new part either`() {
        val file = NotesFile(listOf(part("Nuova", NoteDraft(rowStart = 5, frequency = NoteFrequency.EVERY_NTH, step = 6, text = "x"))))
        val plan = planNotesImport(file, listOf(PartChoice.Create), parts, emptyMap())
        assertEquals(PlanIssueKind.OPEN_END_UNKNOWN, plan.errors.single().kind)
    }

    // --- warnings ---

    @Test
    fun `a row beyond the end of the part is a warning, not an error`() {
        val file = NotesFile(listOf(part("Manica", note(60))))
        val plan = planNotesImport(file, listOf(PartChoice.Existing(2L)), parts, emptyMap())
        assertTrue(plan.errors.isEmpty())
        assertEquals(PlanIssueKind.BEYOND_PART, plan.warnings.single().kind)
        assertEquals(1, plan.additions.single().notes.size) // still imported
        assertTrue(plan.canImport)
    }

    @Test
    fun `a part with rows still to define has no row beyond its end`() {
        val empty = Part(id = 3L, projectId = 9L, name = "Vuota", totalRows = 0)
        val plan = planNotesImport(NotesFile(listOf(part("Vuota", note(60)))), listOf(PartChoice.Existing(3L)), listOf(empty), emptyMap())
        assertTrue(plan.warnings.isEmpty())
    }

    // --- row 0 and the final row ---

    @Test
    fun `the preparation note goes to a part that has the row, unless it already has a note`() {
        val file = NotesFile(listOf(part("Davanti", note(1), prep = "Avvia 164 maglie")))
        assertEquals("Avvia 164 maglie", planNotesImport(file, listOf(PartChoice.Existing(1L)), parts, emptyMap()).additions.single().prepNote)
        val withNote = listOf(davanti.copy(prepNote = "già scritta"))
        assertEquals(null, planNotesImport(file, listOf(PartChoice.Existing(1L)), withNote, emptyMap()).additions.single().prepNote)
    }

    @Test
    fun `a preparation note for a part with no such row is left out with a warning`() {
        val file = NotesFile(listOf(part("Manica", note(1), prep = "Avvia")))
        val plan = planNotesImport(file, listOf(PartChoice.Existing(2L)), parts, emptyMap())
        assertEquals(null, plan.additions.single().prepNote)
        assertEquals(PlanIssueKind.PREP_ROW_MISSING, plan.warnings.single().kind)
    }

    @Test
    fun `a new part has the rows its notes ask for`() {
        val file = NotesFile(listOf(part("Collo", note(1), prep = "Avvia", final = "Chiudi")))
        val target = planNotesImport(file, listOf(PartChoice.Create), parts, emptyMap()).additions.single().target as NotesImportTarget.New
        assertTrue(target.hasPrepRow && target.hasFinalRow)
    }

    @Test
    fun `a part with nothing to bring is left out of the plan`() {
        val plan = planNotesImport(NotesFile(listOf(part("Davanti"))), listOf(PartChoice.Existing(1L)), parts, emptyMap())
        assertTrue(plan.additions.isEmpty())
        assertFalse(plan.canImport)
    }
}
