package com.ilsecondodasinistra.majon.domain

import com.ilsecondodasinistra.majon.domain.model.NoteDraft
import com.ilsecondodasinistra.majon.domain.model.NoteFrequency
import com.ilsecondodasinistra.majon.domain.model.NoteValidationError
import com.ilsecondodasinistra.majon.domain.model.RowNote
import com.ilsecondodasinistra.majon.domain.model.matchingRows
import com.ilsecondodasinistra.majon.domain.model.notesForRow
import com.ilsecondodasinistra.majon.domain.model.parseRowList
import com.ilsecondodasinistra.majon.domain.model.validateNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Notes for exact rows ("rows 10, 20, 30") and for every Nth row ("every 6 rows, from 12 to 190"). */
class RowNoteKindsTest {

    private fun specific(vararg rows: Int) =
        RowNote(partId = 1L, rowStart = rows.min(), rowEnd = rows.max(), frequency = NoteFrequency.SPECIFIC, rows = rows.toList(), text = "x")

    private fun every(step: Int, from: Int, to: Int) =
        RowNote(partId = 1L, rowStart = from, rowEnd = to, frequency = NoteFrequency.EVERY_NTH, step = step, text = "x")

    // --- which rows a note applies to ---

    @Test
    fun `a note on exact rows applies to those rows only`() {
        val note = specific(10, 20, 30)
        assertTrue(note.appliesTo(10) && note.appliesTo(20) && note.appliesTo(30))
        assertFalse(note.appliesTo(11))
        assertFalse(note.appliesTo(40))
        assertFalse(note.appliesTo(5))
    }

    @Test
    fun `a note every Nth row counts from its first row`() {
        val note = every(step = 6, from = 12, to = 36)
        assertEquals(listOf(12, 18, 24, 30, 36), (1..50).filter(note::appliesTo))
    }

    @Test
    fun `every Nth stops at the last row of its range`() {
        assertFalse(every(step = 6, from = 12, to = 30).appliesTo(36))
        assertTrue(every(step = 6, from = 12, to = 30).appliesTo(30))
    }

    @Test
    fun `the old kinds work as before`() {
        val odd = RowNote(partId = 1L, rowStart = 3, rowEnd = 9, frequency = NoteFrequency.ODD_ROWS, text = "x")
        assertEquals(listOf(3, 5, 7, 9), (1..20).filter(odd::appliesTo))
        val one = RowNote(partId = 1L, rowStart = 7, rowEnd = 7, text = "x")
        assertEquals(listOf(7), (1..20).filter(one::appliesTo))
    }

    @Test
    fun `the notes of a row include every kind`() {
        val notes = listOf(
            specific(10, 20),
            every(step = 5, from = 5, to = 50).copy(text = "ogni cinque"),
            RowNote(partId = 1L, rowStart = 10, rowEnd = 10, text = "solo qui"),
        )
        assertEquals(setOf("x", "ogni cinque", "solo qui"), notes.notesForRow(10).map { it.text }.toSet())
        assertEquals(listOf("ogni cinque"), notes.notesForRow(15).map { it.text })
    }

    @Test
    fun `the rows a note covers, listed`() {
        assertEquals(listOf(10, 20, 30), specific(30, 10, 20).matchingRows())
        assertEquals(listOf(12, 18, 24), every(step = 6, from = 12, to = 29).matchingRows())
        assertEquals(listOf(5), RowNote(partId = 1L, rowStart = 5, rowEnd = 5, text = "x").matchingRows())
        assertEquals(listOf(2, 4), RowNote(partId = 1L, rowStart = 1, rowEnd = 5, frequency = NoteFrequency.EVEN_ROWS, text = "x").matchingRows())
    }

    // --- validation ---

    @Test
    fun `exact rows need at least one row, each within the part`() {
        assertEquals(NoteValidationError.ROWS_EMPTY, validateNote(specific(1).copy(rows = emptyList()), totalRows = 50))
        assertEquals(NoteValidationError.OUT_OF_RANGE, validateNote(specific(10, 60), totalRows = 50))
        assertEquals(NoteValidationError.START_BELOW_ONE, validateNote(specific(0, 5), totalRows = 50))
        assertNull(validateNote(specific(10, 50), totalRows = 50))
    }

    @Test
    fun `every Nth needs a step of at least two and a valid range`() {
        assertEquals(NoteValidationError.STEP_INVALID, validateNote(every(step = 1, from = 5, to = 20), totalRows = 50))
        assertEquals(NoteValidationError.STEP_INVALID, validateNote(every(step = 0, from = 5, to = 20), totalRows = 50))
        assertEquals(NoteValidationError.END_BEFORE_START, validateNote(every(step = 3, from = 20, to = 5), totalRows = 50))
        assertEquals(NoteValidationError.OUT_OF_RANGE, validateNote(every(step = 3, from = 5, to = 60), totalRows = 50))
        assertNull(validateNote(every(step = 3, from = 5, to = 50), totalRows = 50))
    }

    @Test
    fun `a blank text is rejected for every kind`() {
        assertEquals(NoteValidationError.TEXT_BLANK, validateNote(specific(1, 2).copy(text = " "), totalRows = 50))
        assertEquals(NoteValidationError.TEXT_BLANK, validateNote(every(2, 1, 10).copy(text = ""), totalRows = 50))
    }

    // --- the list of rows as typed ---

    @Test
    fun `a typed list of rows is split on commas, spaces and semicolons`() {
        assertEquals(listOf(10, 20, 30), parseRowList("10, 20 30").rows)
        assertEquals(listOf(5, 10, 15), parseRowList("15;5 , 10").rows)
    }

    @Test
    fun `a range inside the list is expanded`() {
        assertEquals(listOf(3, 4, 5, 9), parseRowList("3-5, 9").rows)
        assertEquals(listOf(3, 4, 5), parseRowList("3–5").rows) // en dash
    }

    @Test
    fun `duplicates are dropped and rows come out sorted`() {
        assertEquals(listOf(1, 2, 3), parseRowList("3, 1, 2, 2, 3").rows)
    }

    @Test
    fun `what is not a number or a range is reported`() {
        val parsed = parseRowList("10, venti, 30, 7-3")
        assertEquals(listOf(10, 30), parsed.rows)
        assertEquals(listOf("venti", "7-3"), parsed.invalid)
        assertTrue(parseRowList("").rows.isEmpty())
        assertTrue(parseRowList("  ").invalid.isEmpty())
    }

    // --- drafts, as written in a file ---

    @Test
    fun `a draft with no end is a single row`() {
        val note = NoteDraft(rowStart = 7, text = "aumenta").toRowNote(partId = 3L, totalRows = 100)
        assertEquals(7, note.rowStart)
        assertEquals(7, note.rowEnd)
        assertEquals(3L, note.partId)
        assertEquals(NoteFrequency.EVERY_ROW, note.frequency)
    }

    @Test
    fun `an every-Nth draft with no end goes to the last row of the part`() {
        val note = NoteDraft(rowStart = 12, frequency = NoteFrequency.EVERY_NTH, step = 6, text = "diminuisci").toRowNote(1L, totalRows = 190)
        assertEquals(190, note.rowEnd)
        assertEquals(6, note.step)
    }

    @Test
    fun `a draft with exact rows keeps them sorted and takes its range from them`() {
        val note = NoteDraft(rowStart = 0, frequency = NoteFrequency.SPECIFIC, rows = listOf(30, 10, 20), text = "x").toRowNote(1L, totalRows = 100)
        assertEquals(listOf(10, 20, 30), note.rows)
        assertEquals(10, note.rowStart)
        assertEquals(30, note.rowEnd)
    }

    @Test
    fun `the text of a draft is trimmed`() {
        assertEquals("aumenta", NoteDraft(rowStart = 1, text = "  aumenta ").toRowNote(1L, 10).text)
    }
}
