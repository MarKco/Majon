package com.ilsecondodasinistra.majon.domain

import com.ilsecondodasinistra.majon.domain.model.RowStep
import com.ilsecondodasinistra.majon.domain.model.newNoteTexts
import com.ilsecondodasinistra.majon.domain.model.rowsKnitted
import com.ilsecondodasinistra.majon.domain.model.rowsKnittedWhenDone
import org.junit.Assert.assertEquals
import org.junit.Test

/** What the counter shows: the rows already on the needles, and a "next note" only when it says something new. */
class CounterDisplayTest {

    @Test
    fun `the big number is the rows already knitted, the row in progress not counted`() {
        assertEquals(0, RowStep.Row(1).rowsKnitted(totalRows = 50))
        assertEquals(4, RowStep.Row(5).rowsKnitted(totalRows = 50))
        assertEquals(49, RowStep.Row(50).rowsKnitted(totalRows = 50))
    }

    @Test
    fun `the preparation row is not a counted row, the final one comes after all of them`() {
        assertEquals(0, RowStep.Prep.rowsKnitted(totalRows = 50))
        assertEquals(50, RowStep.Final.rowsKnitted(totalRows = 50))
    }

    @Test
    fun `a row never counts above the total`() {
        assertEquals(50, RowStep.Row(99).rowsKnitted(totalRows = 50))
    }

    @Test
    fun `the moment a row is done the count already includes it`() {
        assertEquals(5, RowStep.Row(5).rowsKnittedWhenDone(totalRows = 50))
        assertEquals(50, RowStep.Row(50).rowsKnittedWhenDone(totalRows = 50))
        assertEquals(0, RowStep.Prep.rowsKnittedWhenDone(totalRows = 50))
        assertEquals(50, RowStep.Final.rowsKnittedWhenDone(totalRows = 50))
    }

    @Test
    fun `a next note identical to the current one is not repeated`() {
        assertEquals(emptyList<String>(), newNoteTexts(current = listOf("diminuisci"), next = listOf("diminuisci")))
    }

    @Test
    fun `sameness ignores case and surrounding spaces`() {
        assertEquals(emptyList<String>(), newNoteTexts(listOf("Diminuisci di due"), listOf("  diminuisci di due ")))
    }

    @Test
    fun `a different next note is shown`() {
        assertEquals(listOf("aumenta"), newNoteTexts(current = listOf("diminuisci"), next = listOf("aumenta")))
    }

    @Test
    fun `only what is new in the next row is shown`() {
        assertEquals(
            listOf("aumenta"),
            newNoteTexts(current = listOf("punto diritto", "diminuisci"), next = listOf("punto diritto", "aumenta")),
        )
    }

    @Test
    fun `no next note means nothing to show`() {
        assertEquals(emptyList<String>(), newNoteTexts(current = listOf("diminuisci"), next = emptyList()))
        assertEquals(emptyList<String>(), newNoteTexts(current = emptyList(), next = emptyList()))
    }

    @Test
    fun `a next note is shown when the current row has none`() {
        assertEquals(listOf("aumenta"), newNoteTexts(current = emptyList(), next = listOf("aumenta")))
    }
}
