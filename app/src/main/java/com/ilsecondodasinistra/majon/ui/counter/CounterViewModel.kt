package com.ilsecondodasinistra.majon.ui.counter

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.domain.model.RowNote
import com.ilsecondodasinistra.majon.domain.model.notesForRow
import com.ilsecondodasinistra.majon.domain.repository.MajonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CounterUiState {
    data object Loading : CounterUiState
    data class Ready(
        val partName: String,
        val currentRow: Int,
        val totalRows: Int,
        val completedRows: Int,
        val progress: Float,
        val isComplete: Boolean,
        val currentNotes: List<RowNote>,
        val nextNotes: List<RowNote>,
    ) : CounterUiState
}

@HiltViewModel
class CounterViewModel @Inject constructor(
    private val repository: MajonRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val partId: Long = checkNotNull(savedStateHandle["partId"])

    val uiState: StateFlow<CounterUiState> = combine(
        repository.observePart(partId),
        repository.observeNotes(partId),
    ) { part, notes ->
        if (part == null) {
            CounterUiState.Loading
        } else {
            val currentRow = part.currentRow
            CounterUiState.Ready(
                partName = part.name,
                currentRow = currentRow,
                totalRows = part.totalRows,
                completedRows = part.completedRows,
                progress = part.progress,
                isComplete = part.isComplete,
                currentNotes = notes.notesForRow(currentRow),
                nextNotes = if (currentRow < part.totalRows) notes.notesForRow(currentRow + 1) else emptyList(),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CounterUiState.Loading)

    fun increment() {
        viewModelScope.launch {
            val part = repository.observePart(partId).first() ?: return@launch
            if (part.isComplete) return@launch
            repository.setCompletedRows(partId, (part.completedRows + 1).coerceAtMost(part.totalRows))
        }
    }

    fun decrement() {
        viewModelScope.launch {
            val part = repository.observePart(partId).first() ?: return@launch
            repository.setCompletedRows(partId, (part.completedRows - 1).coerceAtLeast(0))
        }
    }

    /** Jump to a specific row (1-based); the given row becomes the row in progress. */
    fun setCurrentRow(row: Int) {
        viewModelScope.launch {
            val part = repository.observePart(partId).first() ?: return@launch
            val clamped = row.coerceIn(1, part.totalRows)
            repository.setCompletedRows(partId, clamped - 1)
        }
    }

    fun reset() {
        viewModelScope.launch {
            repository.setCompletedRows(partId, 0)
        }
    }
}
