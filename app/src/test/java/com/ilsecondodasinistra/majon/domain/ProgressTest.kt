package com.ilsecondodasinistra.majon.domain

import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.projectProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressTest {

    private fun part(total: Int, completed: Int) = Part(
        id = 0L,
        projectId = 1L,
        name = "test",
        totalRows = total,
        completedRows = completed,
    )

    @Test
    fun `part progress is completed over total`() {
        assertEquals(0.5f, part(120, 60).progress, 0.0001f)
    }

    @Test
    fun `part progress is zero when nothing done`() {
        assertEquals(0f, part(120, 0).progress, 0.0001f)
    }

    @Test
    fun `part progress is zero when total is zero`() {
        assertEquals(0f, part(0, 0).progress, 0.0001f)
    }

    @Test
    fun `part progress caps at one`() {
        assertEquals(1f, part(10, 15).progress, 0.0001f)
    }

    @Test
    fun `part is complete when completed reaches total`() {
        assertTrue(part(10, 10).isComplete)
        assertFalse(part(10, 9).isComplete)
        assertFalse(part(0, 0).isComplete)
    }

    @Test
    fun `current row is completed plus one`() {
        assertEquals(1, part(10, 0).currentRow)
        assertEquals(5, part(10, 4).currentRow)
    }

    @Test
    fun `current row does not exceed total when complete`() {
        assertEquals(10, part(10, 10).currentRow)
    }

    @Test
    fun `project progress weights parts by row count`() {
        // 120 + 80 = 200 total, 60 + 20 = 80 done -> 40%
        val parts = listOf(part(120, 60), part(80, 20))
        assertEquals(0.4f, projectProgress(parts), 0.0001f)
    }

    @Test
    fun `project progress is zero with no parts`() {
        assertEquals(0f, projectProgress(emptyList()), 0.0001f)
    }

    @Test
    fun `project progress is one when all parts complete`() {
        val parts = listOf(part(10, 10), part(20, 20))
        assertEquals(1f, projectProgress(parts), 0.0001f)
    }
}
