package com.ilsecondodasinistra.majon.domain.model

data class Part(
    val id: Long = 0L,
    val projectId: Long,
    val name: String,
    val totalRows: Int,
    val completedRows: Int = 0,
    val createdAt: Long = 0L,
) {
    val progress: Float
        get() = if (totalRows <= 0) 0f else (completedRows.toFloat() / totalRows).coerceIn(0f, 1f)

    val isComplete: Boolean
        get() = totalRows > 0 && completedRows >= totalRows

    /** The row currently being worked (1-based). Stays on the last row once complete. */
    val currentRow: Int
        get() = (completedRows + 1).coerceAtMost(maxOf(totalRows, 1))
}

fun projectProgress(parts: List<Part>): Float {
    val total = parts.sumOf { it.totalRows }
    if (total <= 0) return 0f
    val completed = parts.sumOf { it.completedRows.coerceAtMost(it.totalRows) }
    return (completed.toFloat() / total).coerceIn(0f, 1f)
}
