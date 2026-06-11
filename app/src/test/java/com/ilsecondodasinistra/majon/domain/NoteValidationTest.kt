package com.ilsecondodasinistra.majon.domain

import com.ilsecondodasinistra.majon.domain.model.NoteValidationError
import com.ilsecondodasinistra.majon.domain.model.validateNoteRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteValidationTest {

    @Test
    fun `valid range passes`() {
        assertNull(validateNoteRange(rowStart = 1, rowEnd = 10, totalRows = 100))
        assertNull(validateNoteRange(rowStart = 100, rowEnd = 100, totalRows = 100))
    }

    @Test
    fun `start below one fails`() {
        assertEquals(
            NoteValidationError.START_BELOW_ONE,
            validateNoteRange(rowStart = 0, rowEnd = 10, totalRows = 100),
        )
    }

    @Test
    fun `end before start fails`() {
        assertEquals(
            NoteValidationError.END_BEFORE_START,
            validateNoteRange(rowStart = 10, rowEnd = 5, totalRows = 100),
        )
    }

    @Test
    fun `end beyond total rows fails`() {
        assertEquals(
            NoteValidationError.OUT_OF_RANGE,
            validateNoteRange(rowStart = 90, rowEnd = 101, totalRows = 100),
        )
    }
}
