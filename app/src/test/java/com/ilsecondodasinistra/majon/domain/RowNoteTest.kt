package com.ilsecondodasinistra.majon.domain

import com.ilsecondodasinistra.majon.domain.model.NoteFrequency
import com.ilsecondodasinistra.majon.domain.model.RowNote
import com.ilsecondodasinistra.majon.domain.model.notesForRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RowNoteTest {

    private fun note(
        start: Int,
        end: Int = start,
        frequency: NoteFrequency = NoteFrequency.EVERY_ROW,
        text: String = "test",
    ) = RowNote(
        id = 0L,
        partId = 1L,
        rowStart = start,
        rowEnd = end,
        frequency = frequency,
        text = text,
    )

    @Test
    fun `single row note applies only to its row`() {
        val n = note(19)
        assertTrue(n.appliesTo(19))
        assertFalse(n.appliesTo(18))
        assertFalse(n.appliesTo(20))
    }

    @Test
    fun `range note applies to every row in range, bounds included`() {
        val n = note(20, 25)
        assertTrue(n.appliesTo(20))
        assertTrue(n.appliesTo(22))
        assertTrue(n.appliesTo(25))
        assertFalse(n.appliesTo(19))
        assertFalse(n.appliesTo(26))
    }

    @Test
    fun `odd rows note applies only to odd rows within range`() {
        val n = note(1, 10, NoteFrequency.ODD_ROWS)
        assertTrue(n.appliesTo(1))
        assertTrue(n.appliesTo(7))
        assertFalse(n.appliesTo(4))
        assertFalse(n.appliesTo(11))
    }

    @Test
    fun `even rows note applies only to even rows within range`() {
        val n = note(1, 10, NoteFrequency.EVEN_ROWS)
        assertTrue(n.appliesTo(2))
        assertTrue(n.appliesTo(10))
        assertFalse(n.appliesTo(5))
        assertFalse(n.appliesTo(12))
    }

    @Test
    fun `odd rows combined with range, like odd rows from 20 to 30`() {
        val n = note(20, 30, NoteFrequency.ODD_ROWS)
        assertTrue(n.appliesTo(21))
        assertTrue(n.appliesTo(29))
        assertFalse(n.appliesTo(20))
        assertFalse(n.appliesTo(19))
        assertFalse(n.appliesTo(31))
    }

    @Test
    fun `notesForRow returns all matching notes sorted by start row`() {
        val all = listOf(
            note(20, 30, text = "diminuisci di due"),
            note(1, 100, NoteFrequency.ODD_ROWS, text = "punto diritto"),
            note(25, text = "marker"),
        )
        val atRow25 = all.notesForRow(25)
        assertEquals(listOf("punto diritto", "diminuisci di due", "marker"), atRow25.map { it.text })

        val atRow24 = all.notesForRow(24)
        assertEquals(listOf("diminuisci di due"), atRow24.map { it.text })
    }

    @Test
    fun `notesForRow returns empty when nothing matches`() {
        val all = listOf(note(10, 12))
        assertTrue(all.notesForRow(5).isEmpty())
    }
}
