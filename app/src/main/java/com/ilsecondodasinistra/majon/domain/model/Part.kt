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
    /** What [stitchCm] measures (the direction of the stitches), if the part is planned by measures. */
    val stitchMeasure: MeasureKey? = null,
    val stitchCm: Double? = null,
    /** What [rowCm] measures (the direction of the rows). */
    val rowMeasure: MeasureKey? = null,
    val rowCm: Double? = null,
    /** The gauge swatch this part is estimated with; null = the project's main yarn swatch. */
    val swatchId: Long? = null,
    /** How many times the part is knitted (two sleeves): the count starts again for each copy. */
    val copies: Int = 1,
    /** The copy being worked, 1-based. */
    val currentCopy: Int = 1,
    /** The stitches to cast on, as the pattern says; null when not noted (see the estimate from the measures). */
    val castOnStitches: Int? = null,
    /** Where [stitchMeasure] ends up at the last row when the part widens or narrows; null = the same all along. */
    val endStitchCm: Double? = null,
) {
    private val prepOffset: Int get() = if (hasPrepRow) 1 else 0

    /** Normal rows plus the optional preparation and final rows. */
    val totalSteps: Int
        get() = totalRows + prepOffset + (if (hasFinalRow) 1 else 0)

    /** How far the copy being worked has got. */
    val copyProgress: Float
        get() = if (totalRows <= 0) 0f else (completedRows.toFloat() / totalSteps).coerceIn(0f, 1f)

    /** Steps of every copy together, and those already done: what the progress of the whole part is made of. */
    val allSteps: Int get() = totalSteps * copies.coerceAtLeast(1)
    val doneSteps: Int get() = (currentCopy.coerceAtLeast(1) - 1) * totalSteps + completedRows.coerceAtMost(totalSteps)

    /** How far the whole part has got, counting every copy. */
    val progress: Float
        get() = if (totalRows <= 0) 0f else (doneSteps.toFloat() / allSteps).coerceIn(0f, 1f)

    /** True while there are copies after the one being worked. */
    val hasMoreCopies: Boolean
        get() = currentCopy < copies

    /** All the copies, the last one included, are finished. */
    val isComplete: Boolean
        get() = totalRows > 0 && !hasMoreCopies && completedRows >= totalSteps

    /** The next copy, at its first step. */
    fun startNextCopy(): Part = copy(currentCopy = (currentCopy + 1).coerceAtMost(copies), completedRows = 0)

    /**
     * Sets how many copies there are (at least one). The copy being worked never goes past the last, and a part
     * that was already finished moves on to the new copy rather than staying "done".
     */
    fun withCopies(count: Int): Part {
        val n = count.coerceAtLeast(1)
        val resized = copy(copies = n, currentCopy = currentCopy.coerceIn(1, n))
        val copyFinished = totalRows > 0 && completedRows >= totalSteps
        return if (copyFinished && resized.hasMoreCopies) resized.startNextCopy() else resized
    }

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

/**
 * The rows already knitted while this step is in progress (the row being worked is not counted): what the
 * counter shows in big, i.e. the rows on the needles. The preparation row is row 0, not counted.
 */
fun RowStep.rowsKnitted(totalRows: Int): Int = when (this) {
    RowStep.Prep -> 0
    is RowStep.Row -> (number - 1).coerceIn(0, totalRows)
    RowStep.Final -> totalRows
}

/** The same count at the moment this step has just been finished. */
fun RowStep.rowsKnittedWhenDone(totalRows: Int): Int = when (this) {
    RowStep.Prep -> 0
    is RowStep.Row -> number.coerceIn(0, totalRows)
    RowStep.Final -> totalRows
}

fun projectProgress(parts: List<Part>): Float {
    val total = parts.sumOf { it.allSteps }
    if (total <= 0) return 0f
    val completed = parts.sumOf { it.doneSteps }
    return (completed.toFloat() / total).coerceIn(0f, 1f)
}

/** The first unfinished part created after [current] (parts follow creation order, i.e. id). */
fun nextPart(parts: List<Part>, current: Part): Part? =
    parts.filter { it.id > current.id && !it.isComplete }.minByOrNull { it.id }
