package com.ilsecondodasinistra.majon.ui.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.domain.model.RowNote
import com.ilsecondodasinistra.majon.domain.repository.MajonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface NotesUiState {
    data object Loading : NotesUiState
    data class Ready(
        val partName: String,
        val totalRows: Int,
        val notes: List<RowNote>,
    ) : NotesUiState
}

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: MajonRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val partId: Long = checkNotNull(savedStateHandle["partId"])

    val uiState: StateFlow<NotesUiState> = combine(
        repository.observePart(partId),
        repository.observeNotes(partId),
    ) { part, notes ->
        if (part == null) {
            NotesUiState.Loading
        } else {
            NotesUiState.Ready(partName = part.name, totalRows = part.totalRows, notes = notes)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesUiState.Loading)

    fun deleteNote(id: Long) {
        viewModelScope.launch { repository.deleteNote(id) }
    }
}
