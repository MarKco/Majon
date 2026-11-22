package com.ilsecondodasinistra.majon.domain

import com.ilsecondodasinistra.majon.domain.model.MaterialShare
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.NeedleType
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.domain.model.YarnMaterial
import com.ilsecondodasinistra.majon.domain.model.formatMillimeters
import com.ilsecondodasinistra.majon.domain.model.formatSkeins
import com.ilsecondodasinistra.majon.domain.model.materialsTotalPercent
import com.ilsecondodasinistra.majon.domain.model.materialsExceedHundred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StashTest {

    private fun yarn(
        stock: Double = 2.5,
        weightG: Int? = 100,
        lengthM: Int? = 350,
        materials: List<MaterialShare> = emptyList(),
    ) = Yarn(
        name = "Bamboo",
        stockSkeins = stock,
        skeinWeightG = weightG,
        skeinLengthM = lengthM,
        materials = materials,
    )

    @Test
    fun `stock in grams and meters follows the skein size`() {
        val y = yarn(stock = 2.5)
        assertEquals(250.0, y.stockGrams!!, 0.0001)
        assertEquals(875.0, y.stockMeters!!, 0.0001)
    }

    @Test
    fun `stock in grams or meters is unknown without the skein size`() {
        assertNull(yarn(weightG = null).stockGrams)
        assertNull(yarn(lengthM = null).stockMeters)
    }

    @Test
    fun `stock never shows negative`() {
        assertEquals(0.0, yarn(stock = -1.0).stockSkeins.coerceAtLeast(0.0), 0.0)
        assertEquals(0.0, yarn(stock = 0.0).stockGrams!!, 0.0)
    }

    @Test
    fun `skeins are shown with fraction glyphs`() {
        assertEquals("0", formatSkeins(0.0))
        assertEquals("1", formatSkeins(1.0))
        assertEquals("½", formatSkeins(0.5))
        assertEquals("¼", formatSkeins(0.25))
        assertEquals("¾", formatSkeins(0.75))
        assertEquals("2½", formatSkeins(2.5))
        assertEquals("3¼", formatSkeins(3.25))
    }

    @Test
    fun `other fractions are shown as trimmed decimals`() {
        assertEquals("0,3", formatSkeins(0.3))
        assertEquals("1,33", formatSkeins(1.333))
        assertEquals("2", formatSkeins(1.999)) // rounds to the nearest 1/100
        assertEquals("0,3", formatSkeins(0.3, decimalSeparator = ','))
        assertEquals("0.3", formatSkeins(0.3, decimalSeparator = '.'))
    }

    @Test
    fun `negative skeins show as zero`() {
        assertEquals("0", formatSkeins(-0.5))
    }

    @Test
    fun `material shares sum up and flag more than a hundred percent`() {
        val ok = listOf(MaterialShare(YarnMaterial.WOOL, percent = 70), MaterialShare(YarnMaterial.SILK, percent = 30))
        val over = ok + MaterialShare(YarnMaterial.COTTON, percent = 10)
        assertEquals(100, materialsTotalPercent(ok))
        assertFalse(materialsExceedHundred(ok))
        assertEquals(110, materialsTotalPercent(over))
        assertTrue(materialsExceedHundred(over))
    }

    @Test
    fun `shares without a percent do not count`() {
        val list = listOf(MaterialShare(YarnMaterial.WOOL), MaterialShare(YarnMaterial.SILK, percent = 20))
        assertEquals(20, materialsTotalPercent(list))
    }

    @Test
    fun `millimetres drop a useless decimal`() {
        assertEquals("3", formatMillimeters(3.0))
        assertEquals("3,5", formatMillimeters(3.5))
        assertEquals("2,75", formatMillimeters(2.75))
        assertEquals("3.5", formatMillimeters(3.5, decimalSeparator = '.'))
    }

    @Test
    fun `defaults are all optional`() {
        val needle = Needle(type = NeedleType.CIRCULAR)
        assertNull(needle.sizeMm)
        assertEquals(1, needle.quantity)
        val swatch = Swatch(yarnId = 1, stitches = 26, rows = 34)
        assertNull(swatch.needleSizeMm)
        assertNull(swatch.weightG)
    }
}
