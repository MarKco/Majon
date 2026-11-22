package com.ilsecondodasinistra.majon.domain

import com.ilsecondodasinistra.majon.domain.model.PartShape
import com.ilsecondodasinistra.majon.domain.model.PresetPartName
import com.ilsecondodasinistra.majon.domain.model.ProjectIcon
import com.ilsecondodasinistra.majon.domain.model.partShape
import com.ilsecondodasinistra.majon.domain.model.presetParts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PartShapeTest {

    @Test
    fun `the parts of a sweater have their own shapes`() {
        assertEquals(PartShape.FRONT, partShape(ProjectIcon.SWEATER, PresetPartName.FRONT))
        assertEquals(PartShape.BACK, partShape(ProjectIcon.SWEATER, PresetPartName.BACK))
        assertEquals(PartShape.SLEEVE, partShape(ProjectIcon.SWEATER, PresetPartName.SLEEVE))
    }

    @Test
    fun `the fronts of a cardigan lean to their side`() {
        assertEquals(PartShape.LEFT_FRONT, partShape(ProjectIcon.CARDIGAN, PresetPartName.LEFT_FRONT))
        assertEquals(PartShape.RIGHT_FRONT, partShape(ProjectIcon.CARDIGAN, PresetPartName.RIGHT_FRONT))
    }

    @Test
    fun `a single piece takes the shape of what it makes`() {
        assertEquals(PartShape.LONG_RECTANGLE, partShape(ProjectIcon.SCARF, PresetPartName.MAIN))
        assertEquals(PartShape.TRIANGLE, partShape(ProjectIcon.SHAWL, PresetPartName.MAIN))
        assertEquals(PartShape.DOME, partShape(ProjectIcon.HAT, PresetPartName.MAIN))
        assertEquals(PartShape.BAND, partShape(ProjectIcon.HEADBAND, PresetPartName.MAIN))
        assertEquals(PartShape.BAND, partShape(ProjectIcon.COWL, PresetPartName.MAIN))
        assertEquals(PartShape.RECTANGLE, partShape(ProjectIcon.BLANKET, PresetPartName.MAIN))
        assertEquals(PartShape.SQUARE, partShape(ProjectIcon.DISHCLOTH, PresetPartName.MAIN))
    }

    @Test
    fun `socks, gloves, bags and slippers have shapes for each of their parts`() {
        assertEquals(PartShape.TUBE, partShape(ProjectIcon.SOCKS, PresetPartName.LEG))
        assertEquals(PartShape.FOOT, partShape(ProjectIcon.SOCKS, PresetPartName.FOOT))
        assertEquals(PartShape.BAND, partShape(ProjectIcon.GLOVES, PresetPartName.CUFF))
        assertEquals(PartShape.MITTEN, partShape(ProjectIcon.GLOVES, PresetPartName.HAND))
        assertEquals(PartShape.BAG, partShape(ProjectIcon.BAG, PresetPartName.BODY))
        assertEquals(PartShape.STRAP, partShape(ProjectIcon.BAG, PresetPartName.STRAP))
        assertEquals(PartShape.SOLE, partShape(ProjectIcon.SLIPPERS, PresetPartName.SOLE))
        assertEquals(PartShape.UPPER, partShape(ProjectIcon.SLIPPERS, PresetPartName.UPPER))
    }

    @Test
    fun `a part with no known name takes the first suggested part of the type`() {
        assertEquals(PartShape.FRONT, partShape(ProjectIcon.SWEATER, null))
        assertEquals(PartShape.DOME, partShape(ProjectIcon.HAT, null))
    }

    @Test
    fun `every suggested part of every type has a shape`() {
        ProjectIcon.entries.forEach { icon ->
            icon.presetParts().forEach { assertNotNull("$icon ${it.name}", partShape(icon, it.name)) }
        }
    }
}
