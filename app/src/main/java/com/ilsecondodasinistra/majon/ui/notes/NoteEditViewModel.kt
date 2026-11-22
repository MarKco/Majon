package com.ilsecondodasinistra.majon.ui.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.domain.model.NoteFrequency
import com.ilsecondodasinistra.majon.domain.model.NoteValidationError
import com.ilsecondodasinistra.majon.domain.model.RowNote
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

/** The note editor's fields, as typed. */
data class NoteForm(
    val text: String = "",
    val rowStart: String = "",
    val rowEnd: String = "",
    val frequency: NoteFrequency = NoteFrequency.EVERY_ROW,
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
                NoteForm(
                    text = note.text,
                    rowStart = note.rowStart.toString(),
                    rowEnd = note.rowEnd.toString(),
                    frequency = note.frequency,
                    totalRows = totalRows,
                    isEditing = true,
                )
            }
        }
    }

    fun update(transform: (NoteForm) -> NoteForm) {
        _form.value = transform(_form.value).copy(error = null)
    }

    /** Validates and saves the note. An empty end row means a single-row note. Returns false (error set) when invalid. */
    fun save(): Boolean {
        val f = _form.value
        val start = f.rowStart.toIntOrNull() ?: 0
        val end = f.rowEnd.toIntOrNull() ?: start
        val error = if (f.totalRows <= 0) NoteValidationError.OUT_OF_RANGE else validateNote(f.text, start, end, f.totalRows)
        if (error != null) {
            _form.value = f.copy(error = error)
            return false
        }
        viewModelScope.launch {
            repository.upsertNote(
                RowNote(id = noteId, partId = partId, rowStart = start, rowEnd = end, frequency = f.frequency, text = f.text.trim()),
            )
            _saved.emit(Unit)
        }
        return true
    }
}
