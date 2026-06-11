package com.ilsecondodasinistra.majon.domain.model

data class RowNote(
    val id: Long = 0L,
    val partId: Long,
    val rowStart: Int,
    val rowEnd: Int,
    val frequency: NoteFrequency = NoteFrequency.EVERY_ROW,
    val text: String,
) {
    fun appliesTo(row: Int): Boolean {
        if (row !in rowStart..rowEnd) return false
        return when (frequency) {
            NoteFrequency.EVERY_ROW -> true
            NoteFrequency.ODD_ROWS -> row % 2 == 1
            NoteFrequency.EVEN_ROWS -> row % 2 == 0
        }
    }
}

enum class NoteFrequency {
    EVERY_ROW, ODD_ROWS, EVEN_ROWS
}

fun List<RowNote>.notesForRow(row: Int): List<RowNote> =
    filter { it.appliesTo(row) }.sortedBy { it.rowStart }

enum class NoteValidationError {
    START_BELOW_ONE, END_BEFORE_START, OUT_OF_RANGE, TEXT_BLANK
}

fun validateNoteRange(rowStart: Int, rowEnd: Int, totalRows: Int): NoteValidationError? = when {
    rowStart < 1 -> NoteValidationError.START_BELOW_ONE
    rowEnd < rowStart -> NoteValidationError.END_BEFORE_START
    rowEnd > totalRows -> NoteValidationError.OUT_OF_RANGE
    else -> null
}

fun validateNote(text: String, rowStart: Int, rowEnd: Int, totalRows: Int): NoteValidationError? =
    if (text.isBlank()) NoteValidationError.TEXT_BLANK
    else validateNoteRange(rowStart, rowEnd, totalRows)
