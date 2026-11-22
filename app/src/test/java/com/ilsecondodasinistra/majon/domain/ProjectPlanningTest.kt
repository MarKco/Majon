package com.ilsecondodasinistra.majon.domain

import com.ilsecondodasinistra.majon.domain.model.MeasureKey
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.ProjectLinks
import com.ilsecondodasinistra.majon.domain.model.ProjectYarnLink
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.domain.model.YarnCheck
import com.ilsecondodasinistra.majon.domain.model.defaultSwatch
import com.ilsecondodasinistra.majon.domain.model.planYarns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProjectPlanningTest {

    // The Bamboo yarn on the label: 100 g, 350 m; a 26 x 34 swatch at 3 mm that weighs 4 g
    private val bamboo = Yarn(id = 1, name = "Bamboo", skeinWeightG = 100, skeinLengthM = 350, stockSkeins = 2.5)
    private val swatch = Swatch(id = 10, yarnId = 1, stitches = 26, rows = 34, needleSizeMm = 3.0, weightG = 4.0)
    private val other = Swatch(id = 11, yarnId = 1, stitches = 22, rows = 30, weightG = 3.0)

    private val scarf = Part(
        projectId = 1, name = "Sciarpa", totalRows = 510,
        stitchMeasure = MeasureKey.WIDTH, stitchCm = 20.0, rowMeasure = MeasureKey.LENGTH, rowCm = 150.0,
    )

    private fun plan(
        parts: List<Part> = listOf(scarf),
        links: List<ProjectYarnLink>,
        yarns: List<Yarn> = listOf(bamboo),
        swatches: List<Swatch> = listOf(swatch),
    ) = planYarns(parts, ProjectLinks(yarns = links), yarns.associateBy { it.id }, swatches.groupBy { it.yarnId })

    @Test
    fun `the scarf of the label needs a skein and a fifth and the stock covers it`() {
        val p = plan(links = listOf(ProjectYarnLink(1, isPrimary = true))).single()
        assertEquals(120.0, p.neededGrams!!, 0.0001)
        assertEquals(1.2, p.neededSkeins!!, 0.0001)
        assertEquals(YarnCheck.Enough(1.2, 2.5), p.check)
        assertEquals(10L, p.swatch!!.id)
    }

    @Test
    fun `a bigger project says how much is missing`() {
        val blanket = scarf.copy(stitchCm = 100.0, rowCm = 150.0) // 150 swatches * 4 g = 600 g = 6 skeins
        val p = plan(parts = listOf(blanket), links = listOf(ProjectYarnLink(1, isPrimary = true))).single()
        val check = p.check as YarnCheck.Missing
        assertEquals(6.0, check.needed, 0.0001)
        assertEquals(3.5, check.missing, 0.0001)
    }

    @Test
    fun `the amount from the pattern wins over the estimate`() {
        val p = plan(links = listOf(ProjectYarnLink(1, isPrimary = true, plannedSkeins = 4.0))).single()
        assertEquals(4.0, p.neededSkeins!!, 0.0)
        assertEquals(YarnCheck.Missing(4.0, 2.5), p.check)
    }

    @Test
    fun `the chosen swatch is used for the estimate`() {
        val p = plan(
            links = listOf(ProjectYarnLink(1, isPrimary = true, swatchId = 11)),
            swatches = listOf(swatch, other),
        ).single()
        assertEquals(11L, p.swatch!!.id)
        assertEquals(30 * 3.0, p.neededGrams!!, 0.0001)
    }

    @Test
    fun `without a chosen swatch the first one with a weight is used`() {
        val noWeight = swatch.copy(id = 9, weightG = null)
        val p = plan(links = listOf(ProjectYarnLink(1, isPrimary = true)), swatches = listOf(noWeight, other)).single()
        assertEquals(11L, p.swatch!!.id)
    }

    @Test
    fun `no swatch means no estimate unless the pattern says`() {
        val estimated = plan(links = listOf(ProjectYarnLink(1, isPrimary = true)), swatches = emptyList()).single()
        assertNull(estimated.neededGrams)
        assertEquals(YarnCheck.Unknown, estimated.check)
        val planned = plan(links = listOf(ProjectYarnLink(1, isPrimary = true, plannedSkeins = 2.0)), swatches = emptyList()).single()
        assertEquals(YarnCheck.Enough(2.0, 2.5), planned.check)
    }

    @Test
    fun `only the main yarn is estimated, the others need a planned amount`() {
        val second = Yarn(id = 2, name = "Seta", skeinWeightG = 50, stockSkeins = 1.0)
        val plans = plan(
            links = listOf(ProjectYarnLink(1, isPrimary = true), ProjectYarnLink(2, plannedSkeins = 1.5)),
            yarns = listOf(bamboo, second),
            swatches = listOf(swatch, Swatch(id = 20, yarnId = 2, stitches = 30, rows = 40, weightG = 2.0)),
        )
        assertEquals(2, plans.size)
        assertEquals(1.2, plans[0].neededSkeins!!, 0.0001)
        assertNull(plans[1].neededGrams)
        assertEquals(YarnCheck.Missing(1.5, 1.0), plans[1].check)
    }

    @Test
    fun `a second yarn without a planned amount is unknown`() {
        val second = Yarn(id = 2, name = "Seta", skeinWeightG = 50, stockSkeins = 1.0)
        val plans = plan(
            links = listOf(ProjectYarnLink(1, isPrimary = true), ProjectYarnLink(2)),
            yarns = listOf(bamboo, second),
        )
        assertEquals(YarnCheck.Unknown, plans[1].check)
    }

    @Test
    fun `links to a yarn that is gone are skipped`() {
        assertEquals(emptyList<Any>(), plan(links = listOf(ProjectYarnLink(99, isPrimary = true))))
    }

    @Test
    fun `the default swatch of a project is the one of its main yarn`() {
        val links = ProjectLinks(yarns = listOf(ProjectYarnLink(1, isPrimary = true, swatchId = 11)))
        assertEquals(11L, defaultSwatch(links, listOf(swatch, other).groupBy { it.yarnId })!!.id)
        assertNull(defaultSwatch(ProjectLinks(), emptyMap()))
    }
}
