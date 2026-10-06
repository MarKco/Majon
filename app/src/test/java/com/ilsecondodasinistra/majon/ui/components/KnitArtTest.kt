package com.ilsecondodasinistra.majon.ui.components

import com.ilsecondodasinistra.majon.domain.model.ProjectIcon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KnitArtTest {

    @Test
    fun `every icon has a silhouette with stitches`() {
        ProjectIcon.entries.forEach { icon ->
            assertTrue("$icon has too few stitches", knitCells(icon).size > 40)
        }
    }

    @Test
    fun `no progress colors nothing and full progress colors everything`() {
        ProjectIcon.entries.forEach { icon ->
            val cells = knitCells(icon)
            assertEquals(0, filledStitchCount(cells, 0f))
            assertEquals(cells.size, filledStitchCount(cells, 1f))
        }
    }

    @Test
    fun `colored stitches never decrease as progress grows`() {
        val cells = knitCells(ProjectIcon.SWEATER)
        var previous = 0
        for (step in 0..100) {
            val filled = filledStitchCount(cells, step / 100f)
            assertTrue(filled >= previous)
            previous = filled
        }
    }

    @Test
    fun `rows fill from the bottom of the silhouette`() {
        val cells = knitCells(ProjectIcon.BLANKET)
        val bottomRow = cells.minOf { it.row }
        val rowSize = cells.count { it.row == bottomRow }
        // Just enough progress to complete the bottom row only
        val rows = cells.map { it.row }.distinct().size
        assertEquals(rowSize, filledStitchCount(cells, 1f / rows))
    }
}
