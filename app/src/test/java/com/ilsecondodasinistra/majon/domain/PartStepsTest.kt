package com.ilsecondodasinistra.majon.domain

import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.RowStep
import com.ilsecondodasinistra.majon.domain.model.nextPart
import com.ilsecondodasinistra.majon.domain.model.projectProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PartStepsTest {

    private fun part(
        total: Int = 10,
        completed: Int = 0,
        prep: Boolean = false,
        final: Boolean = false,
        id: Long = 1L,
    ) = Part(
        id = id,
        projectId = 1L,
        name = "p$id",
        totalRows = total,
        completedRows = completed,
        hasPrepRow = prep,
        hasFinalRow = final,
    )

    @Test
    fun `without extras steps equal rows`() {
        val p = part(total = 10, completed = 3)
        assertEquals(10, p.totalSteps)
        assertEquals(RowStep.Row(4), p.currentStep)
        assertEquals(4, p.currentRow)
    }

    @Test
    fun `preparation row comes first as row zero`() {
        val p = part(prep = true, completed = 0)
        assertEquals(11, p.totalSteps)
        assertEquals(RowStep.Prep, p.currentStep)
        assertEquals(0, p.currentRow)
        assertEquals(RowStep.Row(1), p.copy(completedRows = 1).currentStep)
        assertEquals(RowStep.Row(10), p.copy(completedRows = 10).currentStep)
    }

    @Test
    fun `final row comes after the last normal row`() {
        val p = part(final = true, completed = 9)
        assertEquals(11, p.totalSteps)
        assertEquals(RowStep.Row(10), p.currentStep)
        assertEquals(RowStep.Final, p.copy(completedRows = 10).currentStep)
        assertEquals(11, p.copy(completedRows = 10).currentRow)
    }

    @Test
    fun `both extras`() {
        val p = part(prep = true, final = true)
        assertEquals(12, p.totalSteps)
        assertEquals(RowStep.Prep, p.currentStep)
        assertEquals(RowStep.Row(1), p.copy(completedRows = 1).currentStep)
        assertEquals(RowStep.Row(10), p.copy(completedRows = 10).currentStep)
        assertEquals(RowStep.Final, p.copy(completedRows = 11).currentStep)
    }

    @Test
    fun `part with final row is not complete after the last normal row`() {
        val p = part(final = true, completed = 10)
        assertFalse(p.isComplete)
        assertTrue(p.copy(completedRows = 11).isComplete)
    }

    @Test
    fun `complete part stays on the last step`() {
        val p = part(prep = true, final = true, completed = 12)
        assertTrue(p.isComplete)
        assertEquals(RowStep.Final, p.currentStep)
    }

    @Test
    fun `rowsDone leaves the extra steps out`() {
        assertEquals(0, part(prep = true, completed = 1).rowsDone)
        assertEquals(4, part(prep = true, completed = 5).rowsDone)
        assertEquals(10, part(prep = true, final = true, completed = 12).rowsDone)
        assertEquals(3, part(completed = 3).rowsDone)
    }

    @Test
    fun `a part whose rows are still to define has no progress and is not complete`() {
        val p = part(total = 0)
        assertEquals(0f, p.progress, 0f)
        assertFalse(p.isComplete)
        assertFalse(p.isLastStep)
        assertEquals(0f, projectProgress(listOf(p)), 0f)
    }

    @Test
    fun `progress counts the extra steps`() {
        assertEquals(0.5f, part(total = 4, prep = true, final = true, completed = 3).progress, 0.0001f)
    }

    @Test
    fun `isLastStep is true only on the very last step`() {
        assertTrue(part(total = 3, completed = 2).isLastStep)
        assertFalse(part(total = 3, completed = 1).isLastStep)
        assertFalse(part(total = 3, completed = 3).isLastStep)
        assertTrue(part(total = 3, final = true, completed = 3).isLastStep)
        assertFalse(part(total = 3, final = true, completed = 2).isLastStep)
    }

    @Test
    fun `completedRowsForRow maps a row to the steps done before it`() {
        assertEquals(0, part().completedRowsForRow(1))
        assertEquals(4, part().completedRowsForRow(5))
        assertEquals(1, part(prep = true).completedRowsForRow(1))
        assertEquals(5, part(prep = true).completedRowsForRow(5))
    }

    @Test
    fun `enabling the preparation row mid-work keeps the same row`() {
        val before = part(completed = 4) // working row 5
        val after = before.withRowOptions(prep = true, final = false)
        assertEquals(RowStep.Row(5), after.currentStep)
    }

    @Test
    fun `enabling the preparation row before starting stays on it`() {
        val after = part(completed = 0).withRowOptions(prep = true, final = false)
        assertEquals(RowStep.Prep, after.currentStep)
    }

    @Test
    fun `disabling the preparation row mid-work keeps the same row`() {
        val before = part(prep = true, completed = 5) // working row 5
        val after = before.withRowOptions(prep = false, final = false)
        assertEquals(RowStep.Row(5), after.currentStep)
    }

    @Test
    fun `disabling the preparation row while on it goes to row one`() {
        val after = part(prep = true, completed = 0).withRowOptions(prep = false, final = false)
        assertEquals(RowStep.Row(1), after.currentStep)
        assertEquals(0, after.completedRows)
    }

    @Test
    fun `enabling the final row on a finished part reopens it on the final row`() {
        val after = part(completed = 10).withRowOptions(prep = false, final = true)
        assertFalse(after.isComplete)
        assertEquals(RowStep.Final, after.currentStep)
    }

    @Test
    fun `disabling the final row clamps progress`() {
        val after = part(final = true, completed = 11).withRowOptions(prep = false, final = false)
        assertEquals(10, after.completedRows)
        assertTrue(after.isComplete)
    }

    @Test
    fun `withRowOptions clamps when total shrinks`() {
        val p = part(total = 10, prep = true, completed = 8).copy(totalRows = 4)
        assertEquals(5, p.clampedToTotal().completedRows)
    }

    @Test
    fun `project progress is weighted by steps`() {
        val parts = listOf(
            part(total = 8, prep = true, final = true, completed = 5, id = 1), // 5 of 10
            part(total = 10, completed = 10, id = 2), // 10 of 10
        )
        assertEquals(15f / 20f, projectProgress(parts), 0.0001f)
    }

    @Test
    fun `nextPart is the first unfinished part after the current one`() {
        val parts = listOf(
            part(total = 5, completed = 5, id = 1),
            part(total = 5, completed = 5, id = 2),
            part(total = 5, completed = 2, id = 3),
            part(total = 5, completed = 0, id = 4),
        )
        assertEquals(3L, nextPart(parts, parts[0])?.id)
        assertEquals(4L, nextPart(parts, parts[2])?.id)
    }

    @Test
    fun `nextPart skips finished parts and ignores earlier ones`() {
        val parts = listOf(
            part(total = 5, completed = 0, id = 1),
            part(total = 5, completed = 5, id = 2),
            part(total = 5, completed = 5, id = 3),
        )
        assertNull(nextPart(parts, parts[0]))
        assertNull(nextPart(parts, parts[1]))
        assertNull(nextPart(parts, parts[2]))
    }

    @Test
    fun `nextPart follows creation order even when the list is shuffled`() {
        val parts = listOf(
            part(total = 5, id = 9),
            part(total = 5, id = 2),
            part(total = 5, id = 5),
        )
        assertEquals(5L, nextPart(parts, parts.first { it.id == 2L })?.id)
    }
}
