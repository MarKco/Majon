package com.ilsecondodasinistra.majon.domain

import com.ilsecondodasinistra.majon.domain.model.MeasureKey
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.estimate
import com.ilsecondodasinistra.majon.domain.model.projectProgress
import com.ilsecondodasinistra.majon.domain.model.yarnNeededGrams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A part knitted more than once (two sleeves) and a part that widens or narrows (a sleeve, cuff to shoulder). */
class PartCopiesTest {

    private val sleeve = Part(projectId = 1L, name = "Manica", totalRows = 10, copies = 2)

    @Test
    fun `a part starts with one copy, the first`() {
        val part = Part(projectId = 1L, name = "Corpo", totalRows = 10)
        assertEquals(1, part.copies)
        assertEquals(1, part.currentCopy)
        assertNull(part.castOnStitches)
        assertNull(part.endStitchCm)
    }

    @Test
    fun `the part is complete only when its last copy is`() {
        assertFalse(sleeve.copy(completedRows = 10).isComplete) // copy 1 of 2 done: the second is still to do
        assertTrue(sleeve.copy(currentCopy = 2, completedRows = 10).isComplete)
        assertFalse(sleeve.copy(currentCopy = 2, completedRows = 9).isComplete)
    }

    @Test
    fun `progress counts every copy`() {
        assertEquals(0.25f, sleeve.copy(completedRows = 5).progress, 0.0001f)
        assertEquals(0.5f, sleeve.copy(currentCopy = 2, completedRows = 0).progress, 0.0001f)
        assertEquals(0.75f, sleeve.copy(currentCopy = 2, completedRows = 5).progress, 0.0001f)
        assertEquals(1f, sleeve.copy(currentCopy = 2, completedRows = 10).progress, 0.0001f)
    }

    @Test
    fun `the progress of the copy in hand is its own`() {
        assertEquals(0.5f, sleeve.copy(currentCopy = 2, completedRows = 5).copyProgress, 0.0001f)
    }

    @Test
    fun `the project progress counts every copy of every part`() {
        val body = Part(projectId = 1L, name = "Corpo", totalRows = 10, completedRows = 10)
        // 10 done of 10, plus 5 of 20 (the sleeve): 15 of 30
        assertEquals(0.5f, projectProgress(listOf(body, sleeve.copy(completedRows = 5))), 0.0001f)
    }

    @Test
    fun `starting the next copy puts the count back to the start`() {
        val next = sleeve.copy(completedRows = 9).startNextCopy()
        assertEquals(2, next.currentCopy)
        assertEquals(0, next.completedRows)
        assertFalse(next.hasMoreCopies)
        assertTrue(sleeve.hasMoreCopies)
    }

    @Test
    fun `asking for more copies of a finished part goes on to the next`() {
        val done = Part(projectId = 1L, name = "Manica", totalRows = 10, completedRows = 10)
        assertTrue(done.isComplete)
        val more = done.withCopies(2)
        assertEquals(2, more.copies)
        assertEquals(2, more.currentCopy)
        assertEquals(0, more.completedRows)
    }

    @Test
    fun `asking for more copies of a part under way keeps the place`() {
        val more = Part(projectId = 1L, name = "Manica", totalRows = 10, completedRows = 4).withCopies(3)
        assertEquals(3, more.copies)
        assertEquals(1, more.currentCopy)
        assertEquals(4, more.completedRows)
    }

    @Test
    fun `fewer copies than the one in hand leave the last`() {
        val fewer = sleeve.copy(currentCopy = 2, completedRows = 3).withCopies(1)
        assertEquals(1, fewer.copies)
        assertEquals(1, fewer.currentCopy)
        assertEquals(3, fewer.completedRows)
        assertEquals(1, sleeve.withCopies(0).copies) // never below one
    }

    @Test
    fun `the yarn needed counts every copy`() {
        val swatch = Swatch(yarnId = 1L, stitches = 20, rows = 30, weightG = 4.0)
        val one = Part(projectId = 1L, name = "A", totalRows = 1, stitchCm = 20.0, rowCm = 50.0)
        // 20 x 50 cm = 1000 cm2 = 10 swatches of 4 g
        assertEquals(40.0, yarnNeededGrams(listOf(one), swatch)!!, 0.0001)
        assertEquals(80.0, yarnNeededGrams(listOf(one.copy(copies = 2)), swatch)!!, 0.0001)
    }

    @Test
    fun `a part that widens takes the mean width for the yarn`() {
        val swatch = Swatch(yarnId = 1L, stitches = 31, rows = 34, weightG = 4.0)
        val sleeveShape = Part(
            projectId = 1L, name = "Manica", totalRows = 1,
            stitchMeasure = MeasureKey.CIRCUMFERENCE, stitchCm = 21.0, endStitchCm = 47.0, rowCm = 56.0,
        )
        // (21 + 47) / 2 = 34 cm wide, 56 cm long: 1904 cm2
        assertEquals(1904.0 / 100.0 * 4.0, yarnNeededGrams(listOf(sleeveShape), swatch)!!, 0.0001)
        // without the end width it is a rectangle of 21 cm
        assertEquals(21.0 * 56.0 / 100.0 * 4.0, yarnNeededGrams(listOf(sleeveShape.copy(endStitchCm = null)), swatch)!!, 0.0001)
    }

    @Test
    fun `the estimate gives the stitches at the start and at the end`() {
        val swatch = Swatch(yarnId = 1L, stitches = 31, rows = 34)
        val estimate = Part(
            projectId = 1L, name = "Manica", totalRows = 1, stitchCm = 21.0, endStitchCm = 47.0, rowCm = 56.0,
        ).estimate(swatch)
        assertEquals(65, estimate.stitches)
        assertEquals(146, estimate.endStitches)
        assertEquals(190, estimate.rows)
        assertNull(Part(projectId = 1L, name = "A", totalRows = 1, stitchCm = 21.0, rowCm = 56.0).estimate(swatch).endStitches)
    }

    @Test
    fun `a zero or negative end width is ignored`() {
        val swatch = Swatch(yarnId = 1L, stitches = 31, rows = 34, weightG = 4.0)
        val part = Part(projectId = 1L, name = "A", totalRows = 1, stitchCm = 20.0, endStitchCm = 0.0, rowCm = 50.0)
        assertNull(part.estimate(swatch).endStitches)
        assertEquals(20.0 * 50.0 / 100.0 * 4.0, yarnNeededGrams(listOf(part), swatch)!!, 0.0001)
    }
}
