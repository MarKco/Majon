package com.ilsecondodasinistra.majon.domain.model

/**
 * An instruction for some rows of a part. What it applies to depends on [frequency]:
 * every row, odd or even rows of [rowStart]..[rowEnd]; every [step]th row from [rowStart] to [rowEnd]
 * ([NoteFrequency.EVERY_NTH]); or just the exact [rows] ([NoteFrequency.SPECIFIC], where [rowStart] and [rowEnd]
 * are the first and last of them, kept for ordering).
 */
data class RowNote(
    val id: Long = 0L,
    val partId: Long,
    val rowStart: Int,
    val rowEnd: Int,
    val frequency: NoteFrequency = NoteFrequency.EVERY_ROW,
    val text: String,
    /** Only for [NoteFrequency.EVERY_NTH]: every how many rows. */
    val step: Int = 1,
    /** Only for [NoteFrequency.SPECIFIC]: the exact rows, sorted. */
    val rows: List<Int> = emptyList(),
) {
    fun appliesTo(row: Int): Boolean = when (frequency) {
        NoteFrequency.SPECIFIC -> row in rows
        else -> {
            if (row !in rowStart..rowEnd) {
                false
            } else {
                when (frequency) {
                    NoteFrequency.ODD_ROWS -> row % 2 == 1
                    NoteFrequency.EVEN_ROWS -> row % 2 == 0
                    NoteFrequency.EVERY_NTH -> step >= 1 && (row - rowStart) % step == 0
                    else -> true
                }
            }
        }
    }
}

/**
 * [EVERY_ROW], [ODD_ROWS] and [EVEN_ROWS] cover a range of rows; [EVERY_NTH] every Nth row of a range;
 * [SPECIFIC] a list of exact rows.
 */
enum class NoteFrequency {
    EVERY_ROW, ODD_ROWS, EVEN_ROWS, EVERY_NTH, SPECIFIC
}

/** The rows of the part this note applies to, in order. */
fun RowNote.matchingRows(): List<Int> = when (frequency) {
    NoteFrequency.SPECIFIC -> rows.distinct().sorted()
    else -> (rowStart..rowEnd).filter { appliesTo(it) }
}

fun List<RowNote>.notesForRow(row: Int): List<RowNote> =
    filter { it.appliesTo(row) }.sortedBy { it.rowStart }

enum class NoteValidationError {
    START_BELOW_ONE, END_BEFORE_START, OUT_OF_RANGE, TEXT_BLANK, ROWS_EMPTY, ROWS_INVALID, STEP_INVALID
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

/** Checks a note of any kind against the number of rows of its part. */
fun validateNote(note: RowNote, totalRows: Int): NoteValidationError? = when {
    note.text.isBlank() -> NoteValidationError.TEXT_BLANK
    note.frequency == NoteFrequency.SPECIFIC -> when {
        note.rows.isEmpty() -> NoteValidationError.ROWS_EMPTY
        note.rows.any { it < 1 } -> NoteValidationError.START_BELOW_ONE
        note.rows.max() > totalRows -> NoteValidationError.OUT_OF_RANGE
        else -> null
    }
    note.frequency == NoteFrequency.EVERY_NTH && note.step < 2 -> NoteValidationError.STEP_INVALID
    else -> validateNoteRange(note.rowStart, note.rowEnd, totalRows)
}

/** The rows typed in a field ("10, 20, 30-35"): the valid ones, sorted and without repeats, and what was not understood. */
data class RowList(val rows: List<Int>, val invalid: List<String>)

fun parseRowList(text: String): RowList {
    val rows = sortedSetOf<Int>()
    val invalid = mutableListOf<String>()
    text.split(Regex("[,;\\s]+")).filter { it.isNotBlank() }.forEach { token ->
        val range = Regex("^(\\d+)\\s*[-\u2013\u2014]\\s*(\\d+)$").find(token)
        val single = token.toIntOrNull()
        when {
            range != null -> {
                val a = range.groupValues[1].toIntOrNull()
                val b = range.groupValues[2].toIntOrNull()
                if (a != null && b != null && a <= b && b - a < 10_000) rows += a..b else invalid += token
            }
            single != null && single >= 0 -> rows += single
            else -> invalid += token
        }
    }
    return RowList(rows.toList(), invalid)
}

/**
 * A note as written in a file, before it is known which part it goes to: [rowEnd] may be missing (one row, or, for
 * "every Nth", up to the last row of the part).
 */
data class NoteDraft(
    val rowStart: Int,
    val rowEnd: Int? = null,
    val frequency: NoteFrequency = NoteFrequency.EVERY_ROW,
    val step: Int = 1,
    val rows: List<Int> = emptyList(),
    val text: String,
) {
    fun toRowNote(partId: Long, totalRows: Int): RowNote {
        val clean = text.trim()
        if (frequency == NoteFrequency.SPECIFIC) {
            val sorted = rows.distinct().sorted()
            return RowNote(
                partId = partId, rowStart = sorted.firstOrNull() ?: rowStart, rowEnd = sorted.lastOrNull() ?: rowStart,
                frequency = frequency, text = clean, rows = sorted,
            )
        }
        val end = rowEnd ?: if (frequency == NoteFrequency.EVERY_NTH && totalRows > 0) totalRows else rowStart
        return RowNote(
            partId = partId, rowStart = rowStart, rowEnd = end, frequency = frequency, text = clean,
            step = if (frequency == NoteFrequency.EVERY_NTH) step else 1,
        )
    }
}

/**
 * The texts of the next row that are not already in the current row's: the counter shows a "next note" only for
 * what changes, so a note that goes on for several rows is not written twice. Sameness ignores case and spaces.
 */
fun newNoteTexts(current: List<String>, next: List<String>): List<String> {
    val known = current.map { it.trim().lowercase() }.toSet()
    return next.filter { it.trim().lowercase() !in known }
}
