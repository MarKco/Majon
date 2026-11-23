package com.ilsecondodasinistra.majon.ui.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.domain.model.NoteDraft
import com.ilsecondodasinistra.majon.domain.model.NoteFrequency
import com.ilsecondodasinistra.majon.domain.model.NoteValidationError
import com.ilsecondodasinistra.majon.domain.model.RowNote
import com.ilsecondodasinistra.majon.domain.model.parseRowList
import com.ilsecondodasinistra.majon.domain.model.validateNote
import com.ilsecondodasinistra.majon.domain.repository.MajonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What a note applies to, as chosen in the editor. */
enum class NoteScope { RANGE, ROWS, EVERY }

/** The note editor's fields, as typed. */
data class NoteForm(
    val text: String = "",
    val scope: NoteScope = NoteScope.RANGE,
    /** First and last row: of the range, or of "every Nth". */
    val rowStart: String = "",
    val rowEnd: String = "",
    /** For a range: every row, odd rows or even rows. */
    val frequency: NoteFrequency = NoteFrequency.EVERY_ROW,
    /** For exact rows: "10, 20, 30-35". */
    val rowsText: String = "",
    /** For "every Nth": the N. */
    val step: String = "",
    val totalRows: Int = 0,
    val error: NoteValidationError? = null,
    val isEditing: Boolean = false,
)

@HiltViewModel
class NoteEditViewModel @Inject constructor(
    private val repository: MajonRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val partId: Long = checkNotNull(savedStateHandle["partId"])
    private val noteId: Long = savedStateHandle["noteId"] ?: 0L

    private val _form = MutableStateFlow(NoteForm(isEditing = noteId != 0L))
    val form: StateFlow<NoteForm> = _form

    private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saved: SharedFlow<Unit> = _saved.asSharedFlow()

    init {
        viewModelScope.launch {
            val totalRows = repository.observePart(partId).first()?.totalRows ?: 0
            val note = if (noteId != 0L) repository.observeNotes(partId).first().firstOrNull { it.id == noteId } else null
            _form.value = if (note == null) {
                _form.value.copy(totalRows = totalRows)
            } else {
                note.toForm(totalRows)
            }
        }
    }

    fun update(transform: (NoteForm) -> NoteForm) {
        _form.value = transform(_form.value).copy(error = null)
    }

    /**
     * Validates and saves the note, as the kind chosen (the fields of the other kinds are left alone). A range with
     * no end is a single row; "every Nth" with no start begins at row 1, with no end goes to the last row.
     * Returns false (error set) when invalid.
     */
    fun save(): Boolean {
        val f = _form.value
        val built: RowNote = when (f.scope) {
            NoteScope.RANGE -> {
                val start = f.rowStart.toIntOrNull() ?: 0
                val end = f.rowEnd.toIntOrNull() ?: start
                val kind = f.frequency.takeIf { it in RANGE_KINDS } ?: NoteFrequency.EVERY_ROW
                RowNote(id = noteId, partId = partId, rowStart = start, rowEnd = end, frequency = kind, text = f.text.trim())
            }
            NoteScope.ROWS -> {
                val parsed = parseRowList(f.rowsText)
                if (parsed.invalid.isNotEmpty()) return fail(f, NoteValidationError.ROWS_INVALID)
                NoteDraft(rowStart = 0, frequency = NoteFrequency.SPECIFIC, rows = parsed.rows, text = f.text)
                    .toRowNote(partId, f.totalRows).copy(id = noteId)
            }
            NoteScope.EVERY -> {
                val start = f.rowStart.toIntOrNull() ?: 1
                val end = f.rowEnd.toIntOrNull() ?: f.totalRows
                RowNote(
                    id = noteId, partId = partId, rowStart = start, rowEnd = end, frequency = NoteFrequency.EVERY_NTH,
                    step = f.step.toIntOrNull() ?: 0, text = f.text.trim(),
                )
            }
        }
        val error = if (f.totalRows <= 0) NoteValidationError.OUT_OF_RANGE else validateNote(built, f.totalRows)
        if (error != null) return fail(f, error)
        viewModelScope.launch {
            repository.upsertNote(built)
            _saved.emit(Unit)
        }
        return true
    }

    private fun fail(form: NoteForm, error: NoteValidationError): Boolean {
        _form.value = form.copy(error = error)
        return false
    }

    private companion object {
        val RANGE_KINDS = setOf(NoteFrequency.EVERY_ROW, NoteFrequency.ODD_ROWS, NoteFrequency.EVEN_ROWS)
    }
}

private fun RowNote.toForm(totalRows: Int) = when (frequency) {
    NoteFrequency.SPECIFIC -> NoteForm(
        text = text, scope = NoteScope.ROWS, rowsText = rows.joinToString(", "),
        totalRows = totalRows, isEditing = true,
    )
    NoteFrequency.EVERY_NTH -> NoteForm(
        text = text, scope = NoteScope.EVERY, rowStart = rowStart.toString(), rowEnd = rowEnd.toString(),
        step = step.toString(), totalRows = totalRows, isEditing = true,
    )
    else -> NoteForm(
        text = text, scope = NoteScope.RANGE, rowStart = rowStart.toString(), rowEnd = rowEnd.toString(),
        frequency = frequency, totalRows = totalRows, isEditing = true,
    )
}
