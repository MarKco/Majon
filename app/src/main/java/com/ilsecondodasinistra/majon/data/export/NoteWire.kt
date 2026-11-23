package com.ilsecondodasinistra.majon.data.export

import com.ilsecondodasinistra.majon.domain.model.NoteDraft
import com.ilsecondodasinistra.majon.domain.model.NoteFrequency
import com.ilsecondodasinistra.majon.domain.model.RowNote
import kotlinx.serialization.Serializable

/**
 * A note as written in `.majon` and `.majon-notes` files. Everything but [text] is optional, so a file written
 * by hand or by an AI can say it the short way:
 * - one row: `rowStart`; a range: `rowStart` and `rowEnd`, with `frequency` EVERY_ROW (default), ODD_ROWS or EVEN_ROWS;
 * - exact rows: `rows` (frequency SPECIFIC is implied);
 * - every Nth row: `every`, from `rowStart` (default 1) to `rowEnd` (default: the last row of the part).
 */
@Serializable
internal data class WireNote(
    val rowStart: Int? = null,
    val rowEnd: Int? = null,
    val frequency: String? = null,
    val every: Int? = null,
    val rows: List<Int> = emptyList(),
    val text: String = "",
)

/** What is wrong with a note of a file. */
internal enum class NoteProblem {
    TEXT_BLANK, MISSING_ROW, ROW_BELOW_ONE, END_BEFORE_START, STEP_INVALID, ROWS_EMPTY, UNKNOWN_FREQUENCY
}

internal sealed interface WireNoteResult {
    data class Ok(val draft: NoteDraft) : WireNoteResult
    data class Bad(val problem: NoteProblem, val detail: String? = null) : WireNoteResult
}

private fun frequencyOf(name: String): NoteFrequency? = NoteFrequency.entries.firstOrNull { it.name == name.trim().uppercase() }

/**
 * Turns a written note into a draft. A [lenient] reading (a backup of the app's own) repairs what it can, the
 * strict one (a file somebody wrote) reports every problem instead.
 */
internal fun WireNote.toDraft(lenient: Boolean): WireNoteResult {
    if (text.isBlank()) return WireNoteResult.Bad(NoteProblem.TEXT_BLANK)
    var kind = when {
        frequency != null -> frequencyOf(frequency)
            ?: if (lenient) NoteFrequency.EVERY_ROW else return WireNoteResult.Bad(NoteProblem.UNKNOWN_FREQUENCY, frequency)
        rows.isNotEmpty() -> NoteFrequency.SPECIFIC
        (every ?: 1) >= 2 -> NoteFrequency.EVERY_NTH
        else -> NoteFrequency.EVERY_ROW
    }
    if (kind == NoteFrequency.EVERY_NTH && (every ?: 0) < 2) {
        if (lenient) kind = NoteFrequency.EVERY_ROW else return WireNoteResult.Bad(NoteProblem.STEP_INVALID, every?.toString())
    }

    if (kind == NoteFrequency.SPECIFIC) {
        val clean = rows.distinct().sorted()
        if (!lenient && clean.any { it < 1 }) return WireNoteResult.Bad(NoteProblem.ROW_BELOW_ONE, clean.first { it < 1 }.toString())
        val valid = clean.filter { it >= 1 }
        if (valid.isEmpty()) return WireNoteResult.Bad(NoteProblem.ROWS_EMPTY)
        return WireNoteResult.Ok(NoteDraft(rowStart = valid.first(), rowEnd = valid.last(), frequency = kind, rows = valid, text = text))
    }

    // "Every Nth" may leave out its start (row 1); anything else has to say which row
    val start = rowStart ?: if (kind == NoteFrequency.EVERY_NTH) 1 else return WireNoteResult.Bad(NoteProblem.MISSING_ROW)
    if (start < 1 && !lenient) return WireNoteResult.Bad(NoteProblem.ROW_BELOW_ONE, start.toString())
    val from = start.coerceAtLeast(1)
    var end = rowEnd
    if (end != null && end < from) {
        if (lenient) end = from else return WireNoteResult.Bad(NoteProblem.END_BEFORE_START, "$from-$end")
    }
    return WireNoteResult.Ok(
        NoteDraft(
            rowStart = from,
            // Left open for "every Nth" (up to the end of the part); a single row or a range closes on its own
            rowEnd = end ?: if (kind == NoteFrequency.EVERY_NTH) null else from,
            frequency = kind,
            step = if (kind == NoteFrequency.EVERY_NTH) every ?: 1 else 1,
            text = text,
        ),
    )
}

internal fun RowNote.toWire() = WireNote(
    rowStart = rowStart,
    rowEnd = rowEnd,
    frequency = frequency.name,
    every = step.takeIf { frequency == NoteFrequency.EVERY_NTH },
    rows = if (frequency == NoteFrequency.SPECIFIC) rows else emptyList(),
    text = text,
)
