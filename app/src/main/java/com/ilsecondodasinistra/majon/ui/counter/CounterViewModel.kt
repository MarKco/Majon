package com.ilsecondodasinistra.majon.ui.counter

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.domain.model.ProjectColor
import com.ilsecondodasinistra.majon.domain.model.ProjectIcon
import com.ilsecondodasinistra.majon.domain.model.RowNote
import com.ilsecondodasinistra.majon.domain.model.notesForRow
import com.ilsecondodasinistra.majon.domain.repository.MajonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.ilsecondodasinistra.majon.domain.voice.VoiceCommand
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
        val projectIcon: ProjectIcon,
        val projectColor: ProjectColor,
    ) : CounterUiState
}

/** What the app says (and shows) back after a voice command. */
sealed interface VoiceReply {
    /** [row] is the row now in progress. */
    data class RowAdvanced(val row: Int) : VoiceReply
    data class RowUndone(val row: Int) : VoiceReply
    data class WentTo(val row: Int) : VoiceReply
    data class OutOfRange(val totalRows: Int) : VoiceReply
    data class PartFinished(val totalRows: Int) : VoiceReply
    data class Note(val texts: List<String>) : VoiceReply
    data object NoNote : VoiceReply
    data class Remaining(val rows: Int) : VoiceReply
    data object AskResetConfirm : VoiceReply
    data object ResetDone : VoiceReply
    data object ResetCancelled : VoiceReply
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CounterViewModel @Inject constructor(
    private val repository: MajonRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val partId: Long = checkNotNull(savedStateHandle["partId"])

    private val project = repository.observePart(partId)
        .map { it?.projectId }
        .distinctUntilChanged()
        .flatMapLatest { projectId ->
            if (projectId == null) flowOf(null) else repository.observeProject(projectId)
        }

    val uiState: StateFlow<CounterUiState> = combine(
        repository.observePart(partId),
        repository.observeNotes(partId),
        project,
    ) { part, notes, projectWithParts ->
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
                projectIcon = projectWithParts?.project?.icon ?: ProjectIcon.YARN,
                projectColor = projectWithParts?.project?.color ?: ProjectColor.TERRACOTTA,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CounterUiState.Loading)

    fun increment() {
        viewModelScope.launch {
            repository.adjustCompletedRows(partId, +1)
        }
    }

    fun decrement() {
        viewModelScope.launch {
            repository.adjustCompletedRows(partId, -1)
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

    private val _voiceReplies = MutableSharedFlow<VoiceReply>(extraBufferCapacity = 8)
    val voiceReplies: SharedFlow<VoiceReply> = _voiceReplies.asSharedFlow()

    private var resetPending = false
    private var resetTimeout: Job? = null

    /**
     * Runs a spoken command. Resetting needs a spoken confirmation within
     * [RESET_CONFIRM_MILLIS]; any other command in the meantime cancels it, so that a stray
     * word never wipes the count.
     */
    fun onVoiceCommand(command: VoiceCommand) {
        viewModelScope.launch {
            val part = repository.observePart(partId).first() ?: return@launch
            if (resetPending) {
                resetPending = false
                resetTimeout?.cancel()
                if (command == VoiceCommand.Confirm) {
                    repository.setCompletedRows(partId, 0)
                    _voiceReplies.emit(VoiceReply.ResetDone)
                } else {
                    _voiceReplies.emit(VoiceReply.ResetCancelled)
                }
                return@launch
            }
            when (command) {
                VoiceCommand.Increment -> {
                    if (part.isComplete) {
                        _voiceReplies.emit(VoiceReply.PartFinished(part.totalRows))
                    } else {
                        repository.adjustCompletedRows(partId, +1)
                        val done = part.completedRows + 1
                        _voiceReplies.emit(
                            if (done >= part.totalRows) VoiceReply.PartFinished(part.totalRows)
                            else VoiceReply.RowAdvanced(done + 1),
                        )
                    }
                }
                VoiceCommand.Decrement -> {
                    repository.adjustCompletedRows(partId, -1)
                    _voiceReplies.emit(VoiceReply.RowUndone((part.completedRows - 1).coerceAtLeast(0) + 1))
                }
                is VoiceCommand.GoToRow -> {
                    if (command.row in 1..part.totalRows) {
                        repository.setCompletedRows(partId, command.row - 1)
                        _voiceReplies.emit(VoiceReply.WentTo(command.row))
                    } else {
                        _voiceReplies.emit(VoiceReply.OutOfRange(part.totalRows))
                    }
                }
                VoiceCommand.ReadNote -> {
                    val notes = repository.observeNotes(partId).first().notesForRow(part.currentRow)
                    _voiceReplies.emit(
                        if (notes.isEmpty()) VoiceReply.NoNote else VoiceReply.Note(notes.map { it.text }),
                    )
                }
                VoiceCommand.RemainingRows ->
                    _voiceReplies.emit(VoiceReply.Remaining((part.totalRows - part.completedRows).coerceAtLeast(0)))
                VoiceCommand.Reset -> {
                    resetPending = true
                    _voiceReplies.emit(VoiceReply.AskResetConfirm)
                    resetTimeout = launch {
                        delay(RESET_CONFIRM_MILLIS)
                        resetPending = false
                    }
                }
                VoiceCommand.Confirm -> Unit // nothing to confirm
            }
        }
    }

    companion object {
        const val RESET_CONFIRM_MILLIS = 10_000L
    }
}
