package com.ilsecondodasinistra.majon.data.export

import com.ilsecondodasinistra.majon.domain.model.FullProject
import com.ilsecondodasinistra.majon.domain.model.NoteDraft
import com.ilsecondodasinistra.majon.domain.model.NotesFile
import com.ilsecondodasinistra.majon.domain.model.NotesFilePart
import com.ilsecondodasinistra.majon.domain.model.RowNote
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** What is wrong with a `.majon-notes` file. */
enum class NotesIssueKind {
    SYNTAX, NOT_A_NOTES_FILE, NOT_A_MAJON_FILE, FUTURE_VERSION, NO_NOTES, PART_WITHOUT_NAME,
    TEXT_BLANK, MISSING_ROW, ROW_BELOW_ONE, END_BEFORE_START, STEP_INVALID, ROWS_EMPTY, UNKNOWN_FREQUENCY,
}

/** One problem, with where it is: [part] and the 1-based number of the [note] in it, when it is about one. */
data class NotesIssue(
    val kind: NotesIssueKind,
    val part: String? = null,
    val note: Int? = null,
    val detail: String? = null,
    /** 1-based line of the text, when it can be told (a syntax error); see also [partIndex]. */
    val line: Int? = null,
    /** Position of the [part] in the file's `parts` list, to find its line in the tidied text. */
    val partIndex: Int? = null,
)

sealed interface NotesDecodeResult {
    data class Success(val file: NotesFile) : NotesDecodeResult
    data class Failure(val issues: List<NotesIssue>) : NotesDecodeResult
}

/** What a piece of text turned out to be. */
sealed interface DetectedImport {
    data class Notes(val file: NotesFile) : DetectedImport
    data class Project(val data: BackupData) : DetectedImport
    data class Invalid(val issues: List<NotesIssue>) : DetectedImport
}

@Serializable
private data class WireNotesFile(
    val app: String? = null,
    val version: Int? = null,
    val parts: List<WireNotesPart> = emptyList(),
)

@Serializable
private data class WireNotesPart(
    val part: String = "",
    val prepNote: String? = null,
    val finalNote: String? = null,
    val notes: List<WireNote> = emptyList(),
)

/**
 * The `.majon-notes` format: the notes of the parts of a project, by part name, to be put on a project of the
 * reader's choice. Meant to be written by an AI from the instructions of a pattern, so it is read strictly (every
 * problem reported with its place) but forgivingly in shape: the marker and version may be missing, and the text may
 * come wrapped in a code block with words around it.
 */
class NotesCodec @Inject constructor(private val backupCodec: BackupCodec) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = false
    }

    fun encode(file: NotesFile): String = json.encodeToString(
        WireNotesFile(
            app = APP,
            version = VERSION,
            parts = file.parts.map { part ->
                WireNotesPart(
                    part = part.name,
                    prepNote = part.prepNote,
                    finalNote = part.finalNote,
                    notes = part.notes.map { it.toWire() },
                )
            },
        ),
    )

    fun decode(text: String): NotesDecodeResult {
        val body = extractJson(text)
        val parsed = try {
            json.decodeFromString<WireNotesFile>(body)
        } catch (e: SerializationException) {
            return failure(NotesIssue(NotesIssueKind.SYNTAX, detail = e.message?.lineSequence()?.firstOrNull(), line = lineOf(body, e.message)))
        } catch (e: IllegalArgumentException) {
            return failure(NotesIssue(NotesIssueKind.SYNTAX, detail = e.message?.lineSequence()?.firstOrNull(), line = lineOf(body, e.message)))
        }
        if (parsed.app != null && parsed.app != APP) return failure(NotesIssue(NotesIssueKind.NOT_A_NOTES_FILE, detail = parsed.app))
        if ((parsed.version ?: VERSION) > VERSION) {
            return failure(NotesIssue(NotesIssueKind.FUTURE_VERSION, detail = "${parsed.version} > $VERSION"))
        }

        val issues = mutableListOf<NotesIssue>()
        val parts = linkedMapOf<String, NotesFilePart>()
        parsed.parts.forEachIndexed { partIndex, wirePart ->
            val name = wirePart.part.trim()
            if (name.isEmpty()) issues += NotesIssue(NotesIssueKind.PART_WITHOUT_NAME, partIndex = partIndex)
            val drafts = wirePart.notes.mapIndexedNotNull { i, wire ->
                when (val result = wire.toDraft(lenient = false)) {
                    is WireNoteResult.Ok -> result.draft
                    is WireNoteResult.Bad -> {
                        issues += NotesIssue(result.problem.toKind(), part = name, note = i + 1, detail = result.detail, partIndex = partIndex)
                        null
                    }
                }
            }
            if (name.isNotEmpty()) {
                val key = name.lowercase()
                val before = parts[key]
                parts[key] = NotesFilePart(
                    name = before?.name ?: name,
                    prepNote = wirePart.prepNote?.takeIf { it.isNotBlank() } ?: before?.prepNote,
                    finalNote = wirePart.finalNote?.takeIf { it.isNotBlank() } ?: before?.finalNote,
                    notes = before?.notes.orEmpty() + drafts,
                )
            }
        }
        if (issues.isNotEmpty()) return NotesDecodeResult.Failure(issues)
        val file = NotesFile(parts.values.toList())
        if (file.parts.none { it.notes.isNotEmpty() || it.prepNote != null || it.finalNote != null }) {
            return failure(NotesIssue(NotesIssueKind.NO_NOTES))
        }
        return NotesDecodeResult.Success(file)
    }

    /** Works out what [text] is: a notes file, a project file (`.majon`), or neither. */
    fun detect(text: String): DetectedImport {
        val body = extractJson(text)
        val root = try {
            json.parseToJsonElement(body).jsonObject
        } catch (e: Exception) {
            return DetectedImport.Invalid(listOf(NotesIssue(NotesIssueKind.SYNTAX, detail = e.message?.lineSequence()?.firstOrNull(), line = lineOf(body, e.message))))
        }
        return when (root.appName()) {
            "majon" -> backupCodec.decodeAll(body).fold(
                onSuccess = { data ->
                    if (data.projects.isEmpty() && data.stash.yarns.isEmpty() && data.stash.needles.isEmpty()) {
                        DetectedImport.Invalid(listOf(NotesIssue(NotesIssueKind.NOT_A_MAJON_FILE)))
                    } else {
                        DetectedImport.Project(data)
                    }
                },
                onFailure = { DetectedImport.Invalid(listOf(NotesIssue(NotesIssueKind.NOT_A_MAJON_FILE, detail = it.message))) },
            )
            null, APP -> when (val result = decode(body)) {
                is NotesDecodeResult.Success -> DetectedImport.Notes(result.file)
                is NotesDecodeResult.Failure -> DetectedImport.Invalid(result.issues)
            }
            else -> DetectedImport.Invalid(listOf(NotesIssue(NotesIssueKind.NOT_A_MAJON_FILE, detail = root.appName())))
        }
    }

    /** The notes of a project of a `.majon` file, as a notes file: only the parts that have something to bring. */
    fun notesOf(project: FullProject): NotesFile = NotesFile(
        project.parts
            .filter { it.notes.isNotEmpty() || it.part.prepNote != null || it.part.finalNote != null }
            .map { fp ->
                NotesFilePart(
                    name = fp.part.name,
                    prepNote = fp.part.prepNote,
                    finalNote = fp.part.finalNote,
                    notes = fp.notes.map { it.toDraft() },
                )
            },
    )

    private fun failure(issue: NotesIssue) = NotesDecodeResult.Failure(listOf(issue))

    private fun JsonObject.appName(): String? = this["app"]?.jsonPrimitive?.contentOrNull

    companion object {
        const val APP = "majon-notes"
        const val VERSION = 1

        /**
         * The JSON of a piece of text: what is inside a ``` code block if there is one, else from the first `{` to the
         * last `}` (so words before and after, as chat assistants add, do no harm). Text with no braces is returned as is.
         */
        internal fun extractJson(text: String): String {
            val fenced = Regex("```[a-zA-Z]*\\s*\\n([\\s\\S]*?)```").find(text)?.groupValues?.get(1) ?: text
            val start = fenced.indexOf('{')
            val end = fenced.lastIndexOf('}')
            return if (start in 0 until end) fenced.substring(start, end + 1) else fenced.trim()
        }

        private fun lineOf(text: String, message: String?): Int? {
            val offset = message?.let { Regex("offset (\\d+)").find(it)?.groupValues?.get(1)?.toIntOrNull() } ?: return null
            return text.take(offset.coerceAtMost(text.length)).count { it == '\n' } + 1
        }
    }
}

private fun NoteProblem.toKind() = when (this) {
    NoteProblem.TEXT_BLANK -> NotesIssueKind.TEXT_BLANK
    NoteProblem.MISSING_ROW -> NotesIssueKind.MISSING_ROW
    NoteProblem.ROW_BELOW_ONE -> NotesIssueKind.ROW_BELOW_ONE
    NoteProblem.END_BEFORE_START -> NotesIssueKind.END_BEFORE_START
    NoteProblem.STEP_INVALID -> NotesIssueKind.STEP_INVALID
    NoteProblem.ROWS_EMPTY -> NotesIssueKind.ROWS_EMPTY
    NoteProblem.UNKNOWN_FREQUENCY -> NotesIssueKind.UNKNOWN_FREQUENCY
}

private fun NoteDraft.toWire() = WireNote(
    rowStart = rowStart.takeIf { frequency != com.ilsecondodasinistra.majon.domain.model.NoteFrequency.SPECIFIC },
    rowEnd = rowEnd.takeIf { frequency != com.ilsecondodasinistra.majon.domain.model.NoteFrequency.SPECIFIC },
    frequency = frequency.name,
    every = step.takeIf { frequency == com.ilsecondodasinistra.majon.domain.model.NoteFrequency.EVERY_NTH },
    rows = rows,
    text = text,
)

internal fun RowNote.toDraft() = NoteDraft(
    rowStart = rowStart, rowEnd = rowEnd, frequency = frequency, step = step, rows = rows, text = text,
)
