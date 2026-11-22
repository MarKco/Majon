package com.ilsecondodasinistra.majon.domain.model

/** What the counter is currently on: the preparation row (row 0), a normal row, or the final joining row. */
sealed interface RowStep {
    data object Prep : RowStep
    data class Row(val number: Int) : RowStep
    data object Final : RowStep
}

/**
 * A piece of a project (front, sleeve...). [totalRows] counts the normal rows 1..N only. The optional
 * preparation row (shown as row 0) and final joining row are extra steps around them, so
 * [completedRows] counts steps: preparation + normal rows + final.
 */
data class Part(
    val id: Long = 0L,
    val projectId: Long,
    val name: String,
    val totalRows: Int,
    val completedRows: Int = 0,
    val createdAt: Long = 0L,
    val hasPrepRow: Boolean = false,
    val hasFinalRow: Boolean = false,
    val prepNote: String? = null,
    val finalNote: String? = null,
) {
    private val prepOffset: Int get() = if (hasPrepRow) 1 else 0

    /** Normal rows plus the optional preparation and final rows. */
    val totalSteps: Int
        get() = totalRows + prepOffset + (if (hasFinalRow) 1 else 0)

    val progress: Float
        get() = if (totalRows <= 0) 0f else (completedRows.toFloat() / totalSteps).coerceIn(0f, 1f)

    val isComplete: Boolean
        get() = totalRows > 0 && completedRows >= totalSteps

    /** Normal rows (1..N) already finished, not counting the preparation or final rows. */
    val rowsDone: Int
        get() = (completedRows - prepOffset).coerceIn(0, totalRows)

    /** True when the step in progress is the very last one of the part. */
    val isLastStep: Boolean
        get() = totalRows > 0 && completedRows == totalSteps - 1

    /** The step being worked. Stays on the last step once complete. */
    val currentStep: RowStep
        get() {
            val index = completedRows.coerceIn(0, maxOf(totalSteps - 1, 0))
            if (hasPrepRow && index == 0) return RowStep.Prep
            val row = index - prepOffset + 1
            return if (hasFinalRow && row > totalRows) RowStep.Final else RowStep.Row(row.coerceAtLeast(1))
        }

    /** Number shown for the step: 0 for the preparation row, the row number, or N+1 for the final row. */
    val currentRow: Int
        get() = when (val step = currentStep) {
            RowStep.Prep -> 0
            is RowStep.Row -> step.number.coerceAtMost(maxOf(totalRows, 1))
            RowStep.Final -> totalRows + 1
        }

    /** Value of [completedRows] that makes normal row [row] (1-based) the one in progress. */
    fun completedRowsForRow(row: Int): Int = row - 1 + prepOffset

    /** Same position clamped to the steps that exist (after the total shrank, say). */
    fun clampedToTotal(): Part = copy(completedRows = completedRows.coerceIn(0, totalSteps))

    /**
     * Turns the preparation / final rows on or off while keeping the knitter on the same real row:
     * once work has started, adding or removing the preparation row shifts the step count by one.
     */
    fun withRowOptions(prep: Boolean, final: Boolean, prepNote: String? = this.prepNote, finalNote: String? = this.finalNote): Part {
        val shift = when {
            prep == hasPrepRow || completedRows == 0 -> 0
            prep -> 1
            else -> -1
        }
        return copy(
            hasPrepRow = prep,
            hasFinalRow = final,
            prepNote = prepNote,
            finalNote = finalNote,
            completedRows = completedRows + shift,
        ).clampedToTotal()
    }
}

fun projectProgress(parts: List<Part>): Float {
    val total = parts.sumOf { it.totalSteps }
    if (total <= 0) return 0f
    val completed = parts.sumOf { it.completedRows.coerceAtMost(it.totalSteps) }
    return (completed.toFloat() / total).coerceIn(0f, 1f)
}

/** The first unfinished part created after [current] (parts follow creation order, i.e. id). */
fun nextPart(parts: List<Part>, current: Part): Part? =
    parts.filter { it.id > current.id && !it.isComplete }.minByOrNull { it.id }
