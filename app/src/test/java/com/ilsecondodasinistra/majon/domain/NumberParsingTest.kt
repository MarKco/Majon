package com.ilsecondodasinistra.majon.domain

import com.ilsecondodasinistra.majon.domain.model.formatDecimalInput
import com.ilsecondodasinistra.majon.domain.model.parseDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumberParsingTest {
    @Test
    fun `accepts comma and dot`() {
        assertEquals(2.5, parseDecimal("2,5")!!, 0.0)
        assertEquals(2.5, parseDecimal("2.5")!!, 0.0)
        assertEquals(3.0, parseDecimal(" 3 ")!!, 0.0)
    }

    @Test
    fun `blank or invalid is null`() {
        assertNull(parseDecimal(""))
        assertNull(parseDecimal("  "))
        assertNull(parseDecimal("abc"))
        assertNull(parseDecimal("1,2,3"))
        assertNull(parseDecimal("NaN"))
        assertNull(parseDecimal("Infinity"))
    }

    @Test
    fun `input text drops a useless decimal`() {
        assertEquals("3", formatDecimalInput(3.0))
        assertEquals("2.5", formatDecimalInput(2.5))
        assertEquals("0.25", formatDecimalInput(0.25))
        assertEquals("1.33", formatDecimalInput(1.333))
    }
}
