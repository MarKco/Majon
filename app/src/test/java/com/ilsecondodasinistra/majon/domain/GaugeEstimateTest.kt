package com.ilsecondodasinistra.majon.domain

import com.ilsecondodasinistra.majon.domain.model.MeasureKey
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.ProjectLinks
import com.ilsecondodasinistra.majon.domain.model.ProjectYarnLink
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.domain.model.YarnCheck
import com.ilsecondodasinistra.majon.domain.model.estimate
import com.ilsecondodasinistra.majon.domain.model.neededSkeins
import com.ilsecondodasinistra.majon.domain.model.normalized
import com.ilsecondodasinistra.majon.domain.model.yarnCheck
import com.ilsecondodasinistra.majon.domain.model.yarnNeededGrams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GaugeEstimateTest {

    // The label of the Bamboo yarn: 26 stitches x 34 rows over 10 x 10 cm, 3 mm needles
    private val swatch = Swatch(id = 1, yarnId = 1, stitches = 26, rows = 34, needleSizeMm = 3.0, weightG = 4.0)

    private fun part(stitchCm: Double? = null, rowCm: Double? = null) = Part(
        projectId = 1, name = "Sciarpa", totalRows = 10,
        stitchMeasure = MeasureKey.WIDTH, stitchCm = stitchCm,
        rowMeasure = MeasureKey.LENGTH, rowCm = rowCm,
    )

    @Test
    fun `stitches and rows scale with the measure`() {
        val e = part(stitchCm = 20.0, rowCm = 150.0).estimate(swatch)
        assertEquals(52, e.stitches)   // 20 cm = 2 swatches wide
        assertEquals(510, e.rows)      // 150 cm = 15 swatches tall
    }

    @Test
    fun `estimates are rounded to the nearest whole`() {
        val e = part(stitchCm = 15.0, rowCm = 7.0).estimate(swatch)
        assertEquals(39, e.stitches)   // 39.0
        assertEquals(24, e.rows)       // 23.8
    }

    @Test
    fun `no swatch or no measure gives no estimate`() {
        assertNull(part(stitchCm = 20.0, rowCm = 100.0).estimate(null).stitches)
        assertNull(part(stitchCm = 20.0, rowCm = 100.0).estimate(null).rows)
        val partial = part(stitchCm = 20.0).estimate(swatch)
        assertEquals(52, partial.stitches)
        assertNull(partial.rows)
    }

    @Test
    fun `a zero or negative measure gives no estimate`() {
        assertNull(part(stitchCm = 0.0).estimate(swatch).stitches)
        assertNull(part(rowCm = -5.0).estimate(swatch).rows)
    }

    @Test
    fun `yarn needed comes from the area and the swatch weight`() {
        // 20 x 150 cm = 3000 cm2 = 30 swatches of 100 cm2 -> 30 * 4 g
        assertEquals(120.0, yarnNeededGrams(listOf(part(20.0, 150.0)), swatch)!!, 0.0001)
    }

    @Test
    fun `yarn needed adds up the parts and skips the unmeasured ones`() {
        val parts = listOf(part(20.0, 100.0), part(10.0, 50.0), part(stitchCm = 30.0))
        assertEquals((2000.0 + 500.0) / 100 * 4.0, yarnNeededGrams(parts, swatch)!!, 0.0001)
    }

    @Test
    fun `yarn needed is unknown without swatch weight or measures`() {
        assertNull(yarnNeededGrams(listOf(part(20.0, 150.0)), null))
        assertNull(yarnNeededGrams(listOf(part(20.0, 150.0)), swatch.copy(weightG = null)))
        assertNull(yarnNeededGrams(listOf(part()), swatch))
        assertNull(yarnNeededGrams(emptyList(), swatch))
    }

    @Test
    fun `needed skeins prefer what the pattern says`() {
        val yarn = Yarn(name = "Bamboo", skeinWeightG = 100)
        assertEquals(4.0, neededSkeins(yarn, plannedSkeins = 4.0, neededGrams = 120.0)!!, 0.0)
        assertEquals(1.2, neededSkeins(yarn, plannedSkeins = null, neededGrams = 120.0)!!, 0.0001)
    }

    @Test
    fun `needed skeins are unknown without a skein weight`() {
        assertNull(neededSkeins(Yarn(name = "X"), plannedSkeins = null, neededGrams = 120.0))
        assertNull(neededSkeins(Yarn(name = "X", skeinWeightG = 100), plannedSkeins = null, neededGrams = null))
        assertEquals(3.0, neededSkeins(Yarn(name = "X"), plannedSkeins = 3.0, neededGrams = null)!!, 0.0)
    }

    @Test
    fun `check says enough, missing or unknown`() {
        assertEquals(YarnCheck.Unknown, yarnCheck(null, 2.0))
        assertEquals(YarnCheck.Enough(needed = 1.2, have = 2.5), yarnCheck(1.2, 2.5))
        assertEquals(YarnCheck.Enough(needed = 2.5, have = 2.5), yarnCheck(2.5, 2.5))
        val missing = yarnCheck(3.2, 2.5) as YarnCheck.Missing
        assertEquals(0.7, missing.missing, 0.0001)
    }

    @Test
    fun `missing skeins round up to a quarter`() {
        assertEquals(0.75, (yarnCheck(3.2, 2.5) as YarnCheck.Missing).missingRoundedUp, 0.0)
        assertEquals(0.25, (yarnCheck(2.6, 2.5) as YarnCheck.Missing).missingRoundedUp, 0.0)
        assertEquals(1.0, (yarnCheck(3.5, 2.5) as YarnCheck.Missing).missingRoundedUp, 0.0)
    }

    // --- links ---

    @Test
    fun `a single yarn is always the primary one`() {
        val links = ProjectLinks(yarns = listOf(ProjectYarnLink(yarnId = 5))).normalized()
        assertTrue(links.yarns.single().isPrimary)
    }

    @Test
    fun `only one yarn can be primary and the first marked wins`() {
        val links = ProjectLinks(
            yarns = listOf(
                ProjectYarnLink(1, isPrimary = false),
                ProjectYarnLink(2, isPrimary = true),
                ProjectYarnLink(3, isPrimary = true),
            ),
        ).normalized()
        assertEquals(listOf(false, true, false), links.yarns.map { it.isPrimary })
    }

    @Test
    fun `with no primary marked the first yarn becomes primary`() {
        val links = ProjectLinks(yarns = listOf(ProjectYarnLink(1), ProjectYarnLink(2))).normalized()
        assertEquals(listOf(true, false), links.yarns.map { it.isPrimary })
    }

    @Test
    fun `duplicate yarns and needles are dropped`() {
        val links = ProjectLinks(
            yarns = listOf(ProjectYarnLink(1), ProjectYarnLink(1, plannedSkeins = 3.0)),
            needleIds = listOf(7, 7, 8),
        ).normalized()
        assertEquals(1, links.yarns.size)
        assertEquals(listOf(7L, 8L), links.needleIds)
    }

    @Test
    fun `empty links stay empty`() {
        assertEquals(ProjectLinks(), ProjectLinks().normalized())
    }
}
