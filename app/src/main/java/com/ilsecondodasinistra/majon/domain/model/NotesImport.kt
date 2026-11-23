package com.ilsecondodasinistra.majon.domain.model

import com.ilsecondodasinistra.majon.domain.repository.MajonRepository
import kotlinx.coroutines.flow.firstOrNull

/** The notes of a part as found in a `.majon-notes` file (or in a `.majon`), by the name of the part. */
data class NotesFilePart(
    val name: String,
    /** The note of the preparation row (row 0) and of the final row, when the file has them. */
    val prepNote: String?,
    val finalNote: String?,
    val notes: List<NoteDraft>,
)

data class NotesFile(val parts: List<NotesFilePart>)

/** What to do with a part of the file when the notes are put on a project. */
sealed interface PartChoice {
    /** Add its notes to this part of the project. */
    data class Existing(val partId: Long) : PartChoice

    /** Make a new part, named as in the file. */
    data object Create : PartChoice

    data object Skip : PartChoice
}

private fun String.nameKey() = trim().lowercase()

/** The first guess: a part with the same name (ignoring case and spaces) gets the notes, the others are created. */
fun suggestChoices(file: NotesFile, parts: List<Part>): List<PartChoice> =
    file.parts.map { filePart ->
        parts.firstOrNull { it.name.nameKey() == filePart.name.nameKey() }
            ?.let { PartChoice.Existing(it.id) } ?: PartChoice.Create
    }

/** Where the notes of one file part end up. */
sealed interface NotesImportTarget {
    data class Existing(val partId: Long, val partName: String) : NotesImportTarget
    data class New(val name: String, val totalRows: Int, val hasPrepRow: Boolean, val hasFinalRow: Boolean) : NotesImportTarget
}

enum class PlanIssueKind {
    /** "Every Nth" with no end row, going to a part that does not say how many rows it has: an error. */
    OPEN_END_UNKNOWN,

    /** A note reaching past the last row of its part: imported all the same, a warning. */
    BEYOND_PART,

    PREP_ROW_MISSING,
    FINAL_ROW_MISSING,
}

data class PlanIssue(val kind: PlanIssueKind, val partName: String, val note: Int? = null, val detail: String? = null)

data class PartAddition(
    val filePartName: String,
    val target: NotesImportTarget,
    /** New notes only: the partId is the target's, or 0 for a part still to be made. */
    val notes: List<RowNote>,
    /** Notes of the file left out because the part already has them. */
    val duplicates: Int,
    val prepNote: String?,
    val finalNote: String?,
)

data class NotesImportPlan(
    val additions: List<PartAddition>,
    val errors: List<PlanIssue>,
    val warnings: List<PlanIssue>,
) {
    val noteCount: Int get() = additions.sumOf { it.notes.size }
    val duplicateCount: Int get() = additions.sumOf { it.duplicates }
    private val hasContent: Boolean get() = additions.any { it.notes.isNotEmpty() || it.prepNote != null || it.finalNote != null }

    /** Something to add and nothing wrong enough to stop (warnings do not stop). */
    val canImport: Boolean get() = errors.isEmpty() && hasContent
}

private fun RowNote.sameAs(other: RowNote) =
    frequency == other.frequency && rowStart == other.rowStart && rowEnd == other.rowEnd && step == other.step &&
        rows == other.rows && text.nameKey() == other.text.nameKey()

/**
 * Works out what importing [file] onto a project does, given [choices] (one per part of the file, in order), the
 * project's [parts] and the notes they already have. Nothing is ever taken away: notes only get added, the ones
 * the part already has are left out as duplicates, and an existing preparation / final note is not overwritten.
 */
fun planNotesImport(
    file: NotesFile,
    choices: List<PartChoice>,
    parts: List<Part>,
    existing: Map<Long, List<RowNote>>,
): NotesImportPlan {
    val additions = mutableListOf<PartAddition>()
    val errors = mutableListOf<PlanIssue>()
    val warnings = mutableListOf<PlanIssue>()

    file.parts.forEachIndexed { index, filePart ->
        val choice = choices.getOrNull(index) ?: PartChoice.Skip
        val part = (choice as? PartChoice.Existing)?.let { c -> parts.firstOrNull { it.id == c.partId } }
        if (choice == PartChoice.Skip || (choice is PartChoice.Existing && part == null)) return@forEachIndexed

        // A new part has as many rows as its notes reach
        // (an "every Nth" with no end does not reach anywhere on its own)
        val reach = filePart.notes
            .filterNot { it.frequency == NoteFrequency.EVERY_NTH && it.rowEnd == null }
            .maxOfOrNull { n -> maxOf(n.rowStart, n.rowEnd ?: 0, n.rows.maxOrNull() ?: 0) } ?: 0
        val totalRows = part?.totalRows ?: reach
        val partId = part?.id ?: 0L

        val known = existing[partId].orEmpty().toMutableList()
        val added = mutableListOf<RowNote>()
        var duplicates = 0
        filePart.notes.forEachIndexed { i, draft ->
            if (draft.frequency == NoteFrequency.EVERY_NTH && draft.rowEnd == null && totalRows <= 0) {
                errors += PlanIssue(PlanIssueKind.OPEN_END_UNKNOWN, filePart.name, i + 1)
                return@forEachIndexed
            }
            val note = draft.toRowNote(partId, totalRows)
            if (known.any { it.sameAs(note) }) {
                duplicates++
            } else {
                known += note
                added += note
                if (part != null && totalRows > 0 && note.rowEnd > totalRows) {
                    warnings += PlanIssue(PlanIssueKind.BEYOND_PART, filePart.name, i + 1, note.rowEnd.toString())
                }
            }
        }

        val prep = filePart.prepNote?.trim()?.takeIf { it.isNotEmpty() }
        val final = filePart.finalNote?.trim()?.takeIf { it.isNotEmpty() }
        val target: NotesImportTarget
        var prepToSet: String? = null
        var finalToSet: String? = null
        if (part != null) {
            target = NotesImportTarget.Existing(part.id, part.name)
            if (prep != null) {
                if (part.hasPrepRow) {
                    if (part.prepNote.isNullOrBlank()) prepToSet = prep
                } else {
                    warnings += PlanIssue(PlanIssueKind.PREP_ROW_MISSING, filePart.name)
                }
            }
            if (final != null) {
                if (part.hasFinalRow) {
                    if (part.finalNote.isNullOrBlank()) finalToSet = final
                } else {
                    warnings += PlanIssue(PlanIssueKind.FINAL_ROW_MISSING, filePart.name)
                }
            }
        } else {
            target = NotesImportTarget.New(filePart.name.trim(), totalRows, hasPrepRow = prep != null, hasFinalRow = final != null)
            prepToSet = prep
            finalToSet = final
        }
        if (added.isNotEmpty() || prepToSet != null || finalToSet != null || duplicates > 0) {
            additions += PartAddition(filePart.name, target, added, duplicates, prepToSet, finalToSet)
        }
    }
    return NotesImportPlan(additions, errors, warnings)
}

/**
 * Carries out a plan on [projectId]: new parts are made, notes added, preparation / final notes filled in.
 * Returns how many notes were added.
 */
suspend fun MajonRepository.applyNotesImport(projectId: Long, plan: NotesImportPlan): Int {
    var added = 0
    plan.additions.forEach { addition ->
        when (val target = addition.target) {
            is NotesImportTarget.Existing -> {
                addition.notes.forEach { upsertNote(it) }
                if (addition.prepNote != null || addition.finalNote != null) {
                    observePartOnce(target.partId)?.let { part ->
                        upsertPart(
                            part.copy(
                                prepNote = addition.prepNote ?: part.prepNote,
                                finalNote = addition.finalNote ?: part.finalNote,
                            ),
                        )
                    }
                }
            }
            is NotesImportTarget.New -> {
                val newId = upsertPart(
                    Part(
                        projectId = projectId, name = target.name, totalRows = target.totalRows,
                        hasPrepRow = target.hasPrepRow, hasFinalRow = target.hasFinalRow,
                        prepNote = addition.prepNote, finalNote = addition.finalNote,
                    ),
                )
                addition.notes.forEach { upsertNote(it.copy(partId = newId)) }
            }
        }
        added += addition.notes.size
    }
    return added
}

private suspend fun MajonRepository.observePartOnce(id: Long): Part? = observePart(id).firstOrNull()
