package com.ilsecondodasinistra.majon.domain

import com.ilsecondodasinistra.majon.domain.model.MeasureKey
import com.ilsecondodasinistra.majon.domain.model.PresetPartName
import com.ilsecondodasinistra.majon.domain.model.ProjectIcon
import com.ilsecondodasinistra.majon.domain.model.presetParts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectPresetsTest {

    @Test
    fun `every project type has at least one suggested part`() {
        ProjectIcon.entries.forEach { type ->
            assertTrue("$type has no preset", type.presetParts().isNotEmpty())
        }
    }

    @Test
    fun `a project type exists for each requested kind`() {
        val names = ProjectIcon.entries.map { it.name }
        listOf("SWEATER", "SCARF", "HAT", "SOCKS", "GLOVES", "BLANKET", "BAG", "YARN", "HEADBAND").forEach {
            assertTrue("missing $it", it in names)
        }
    }

    @Test
    fun `a headband is measured by head circumference and height`() {
        val part = ProjectIcon.HEADBAND.presetParts().single()
        assertEquals(MeasureKey.CIRCUMFERENCE, part.stitchMeasure)
        assertEquals(MeasureKey.HEIGHT, part.rowMeasure)
    }

    @Test
    fun `a scarf is a single part measured by width and length`() {
        val part = ProjectIcon.SCARF.presetParts().single()
        assertEquals(PresetPartName.MAIN, part.name)
        assertEquals(MeasureKey.WIDTH, part.stitchMeasure)
        assertEquals(MeasureKey.LENGTH, part.rowMeasure)
    }

    @Test
    fun `a sweater suggests front, back and sleeve`() {
        assertEquals(
            listOf(PresetPartName.FRONT, PresetPartName.BACK, PresetPartName.SLEEVE),
            ProjectIcon.SWEATER.presetParts().map { it.name },
        )
    }

    @Test
    fun `a cardigan has two fronts`() {
        val names = ProjectIcon.CARDIGAN.presetParts().map { it.name }
        assertTrue(PresetPartName.LEFT_FRONT in names && PresetPartName.RIGHT_FRONT in names)
    }

    @Test
    fun `tubes are measured by circumference, flat pieces by width`() {
        listOf(ProjectIcon.HAT, ProjectIcon.COWL, ProjectIcon.HEADBAND).forEach {
            assertEquals(MeasureKey.CIRCUMFERENCE, it.presetParts().first().stitchMeasure)
        }
        listOf(ProjectIcon.SCARF, ProjectIcon.BLANKET, ProjectIcon.SHAWL).forEach {
            assertEquals(MeasureKey.WIDTH, it.presetParts().first().stitchMeasure)
        }
    }
}
