package com.ilsecondodasinistra.majon.domain

import com.ilsecondodasinistra.majon.domain.model.hsvToArgb
import com.ilsecondodasinistra.majon.domain.model.argbToHsv
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorMathTest {

    @Test
    fun `primary colours convert to the right rgb`() {
        assertEquals(0xFFFF0000.toInt(), hsvToArgb(0f, 1f, 1f))
        assertEquals(0xFF00FF00.toInt(), hsvToArgb(120f, 1f, 1f))
        assertEquals(0xFF0000FF.toInt(), hsvToArgb(240f, 1f, 1f))
    }

    @Test
    fun `no saturation is a grey, no brightness is black`() {
        assertEquals(0xFF808080.toInt(), hsvToArgb(200f, 0f, 0.5f).let { it })
        assertEquals(0xFF000000.toInt(), hsvToArgb(200f, 1f, 0f))
        assertEquals(0xFFFFFFFF.toInt(), hsvToArgb(0f, 0f, 1f))
    }

    @Test
    fun `hue wraps around`() {
        assertEquals(hsvToArgb(10f, 1f, 1f), hsvToArgb(370f, 1f, 1f))
    }

    @Test
    fun `a colour survives the round trip within one step`() {
        listOf(0xFFE0A800, 0xFF3C9D5B, 0xFF8E4585, 0xFF2B2B2B, 0xFFF5F0E6).map { it.toInt() }.forEach { argb ->
            val (h, s, v) = argbToHsv(argb)
            val back = hsvToArgb(h, s, v)
            for (shift in listOf(16, 8, 0)) {
                val a = (argb shr shift) and 0xFF
                val b = (back shr shift) and 0xFF
                assertEquals("channel at $shift of ${argb.toString(16)}", a.toDouble(), b.toDouble(), 1.0)
            }
        }
    }

    @Test
    fun `hsv of pure red and of a grey`() {
        val (h, s, v) = argbToHsv(0xFFFF0000.toInt())
        assertEquals(0f, h, 0.01f); assertEquals(1f, s, 0.01f); assertEquals(1f, v, 0.01f)
        val grey = argbToHsv(0xFF808080.toInt())
        assertEquals(0f, grey.second, 0.01f)
    }
}
