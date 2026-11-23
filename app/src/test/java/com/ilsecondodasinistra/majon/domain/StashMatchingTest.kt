package com.ilsecondodasinistra.majon.domain

import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.NeedleMaterial
import com.ilsecondodasinistra.majon.domain.model.NeedleType
import com.ilsecondodasinistra.majon.domain.model.StitchPattern
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.domain.model.sameNeedleAs
import com.ilsecondodasinistra.majon.domain.model.sameSwatchAs
import com.ilsecondodasinistra.majon.domain.model.sameYarnAs
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** When a yarn, a swatch or a needle of an imported file is one the stash already has. */
class StashMatchingTest {

    private val bamboo = Yarn(name = "Bamboo", brand = "We Are Knitters", colorName = "Mustard")

    @Test
    fun `a yarn is the same with the same name, brand and colour, ignoring case and spaces`() {
        assertTrue(bamboo.sameYarnAs(Yarn(name = " bamboo ", brand = "we are knitters", colorName = "MUSTARD")))
    }

    @Test
    fun `a different colour, brand or name is another yarn`() {
        assertFalse(bamboo.sameYarnAs(bamboo.copy(colorName = "Sage")))
        assertFalse(bamboo.sameYarnAs(bamboo.copy(brand = "Altra")))
        assertFalse(bamboo.sameYarnAs(bamboo.copy(name = "Cotton")))
    }

    @Test
    fun `no brand or colour on both sides is a match, one side only is not`() {
        assertTrue(Yarn(name = "Lana").sameYarnAs(Yarn(name = "lana", brand = " ", colorName = "")))
        assertFalse(Yarn(name = "Lana").sameYarnAs(Yarn(name = "Lana", colorName = "Rosso")))
    }

    @Test
    fun `the things that are mine do not count for a match`() {
        assertTrue(bamboo.sameYarnAs(bamboo.copy(stockSkeins = 3.0, pricePerSkein = 4.5, shop = "x", dyeLot = "12")))
    }

    @Test
    fun `a swatch is the same with the same count, needle and stitch`() {
        val s = Swatch(yarnId = 1L, stitches = 31, rows = 34, needleSizeMm = 3.0, pattern = StitchPattern.STOCKINETTE)
        assertTrue(s.sameSwatchAs(s.copy(yarnId = 9L, weightG = 4.0, notes = "n")))
        assertFalse(s.sameSwatchAs(s.copy(stitches = 30)))
        assertFalse(s.sameSwatchAs(s.copy(rows = 30)))
        assertFalse(s.sameSwatchAs(s.copy(needleSizeMm = 3.5)))
        assertFalse(s.sameSwatchAs(s.copy(pattern = StitchPattern.RIB)))
    }

    @Test
    fun `a needle is the same with the same type, size and length`() {
        val n = Needle(type = NeedleType.CIRCULAR, sizeMm = 3.0, lengthCm = 80, material = NeedleMaterial.BAMBOO, brand = "KnitPro")
        assertTrue(n.sameNeedleAs(n.copy(material = null, brand = null, quantity = 4, notes = "x")))
        assertFalse(n.sameNeedleAs(n.copy(sizeMm = 3.5)))
        assertFalse(n.sameNeedleAs(n.copy(lengthCm = 60)))
        assertFalse(n.sameNeedleAs(n.copy(type = NeedleType.DPN)))
        assertTrue(Needle(type = NeedleType.DPN).sameNeedleAs(Needle(type = NeedleType.DPN)))
    }
}
