package com.ilsecondodasinistra.majon.domain

import com.ilsecondodasinistra.majon.domain.model.MeasureKey
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.PresetPart
import com.ilsecondodasinistra.majon.domain.model.PresetPartName
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.estimate
import com.ilsecondodasinistra.majon.domain.model.isAcross
import com.ilsecondodasinistra.majon.domain.model.oriented
import com.ilsecondodasinistra.majon.domain.model.worksSideways
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A part is worked "sideways" when its stitches follow the length/height and its rows the width/circumference. */
class PartOrientationTest {

    private val part = Part(projectId = 1L, name = "Corpo", totalRows = 10)

    @Test
    fun `width and circumference run across, length and height along`() {
        assertTrue(MeasureKey.WIDTH.isAcross)
        assertTrue(MeasureKey.CIRCUMFERENCE.isAcross)
        assertFalse(MeasureKey.LENGTH.isAcross)
        assertFalse(MeasureKey.HEIGHT.isAcross)
    }

    @Test
    fun `a part is sideways when its stitches follow the length or the height`() {
        assertFalse(part.worksSideways) // no measures: standard
        assertFalse(part.copy(stitchMeasure = MeasureKey.WIDTH, rowMeasure = MeasureKey.LENGTH).worksSideways)
        assertTrue(part.copy(stitchMeasure = MeasureKey.LENGTH, rowMeasure = MeasureKey.WIDTH).worksSideways)
        assertTrue(part.copy(stitchMeasure = MeasureKey.HEIGHT, rowMeasure = MeasureKey.CIRCUMFERENCE).worksSideways)
    }

    @Test
    fun `a preset is turned to the wanted orientation, whatever it starts as`() {
        val standard = PresetPart(PresetPartName.HAND, MeasureKey.CIRCUMFERENCE, MeasureKey.HEIGHT)
        val sideways = standard.oriented(sideways = true)
        assertEquals(MeasureKey.HEIGHT, sideways.stitchMeasure)
        assertEquals(MeasureKey.CIRCUMFERENCE, sideways.rowMeasure)
        assertEquals(standard, standard.oriented(sideways = false))
        assertEquals(sideways, sideways.oriented(sideways = true))
        assertEquals(standard, sideways.oriented(sideways = false))
    }

    @Test
    fun `the estimate follows the measure that gives the stitches`() {
        val swatch = Swatch(yarnId = 1L, stitches = 20, rows = 30)
        // 20 cm wide, 150 cm long; worked sideways the 150 cm take the stitches and the 20 cm the rows
        val sideways = part.copy(
            stitchMeasure = MeasureKey.LENGTH, stitchCm = 150.0,
            rowMeasure = MeasureKey.WIDTH, rowCm = 20.0,
        )
        val estimate = sideways.estimate(swatch)
        assertEquals(300, estimate.stitches)
        assertEquals(60, estimate.rows)
    }
}
