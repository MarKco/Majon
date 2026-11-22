package com.ilsecondodasinistra.majon.ui.counter

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.domain.model.ProjectColor
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.ProjectIcon
import com.ilsecondodasinistra.majon.domain.model.RowStep
import com.ilsecondodasinistra.majon.domain.model.nextPart
import com.ilsecondodasinistra.majon.domain.model.RowNote
import com.ilsecondodasinistra.majon.domain.model.notesForRow
import com.ilsecondodasinistra.majon.domain.repository.MajonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.ilsecondodasinistra.majon.domain.voice.VoiceCommand
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
        /** Number shown big: 0 on the preparation row, the row number, N+1 on the final row. */
        val currentRow: Int,
        val step: RowStep,
        /** The step after this one, null on the very last step. */
        val nextStep: RowStep?,
        /** Note of the preparation / final row when that is the current step. */
        val stepNote: String?,
        /** Note of the preparation / final row when that is the next step. */
        val nextStepNote: String?,
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

/** Shown as a dialog when '+' is pressed on the last step of a part that has a part after it. */
data class NextPartPrompt(val partId: Long, val partName: String, val finishedPartName: String)

/** What the app says (and shows) back after a voice command. */
sealed interface VoiceReply {
    /** [row] is the row now in progress. */
    data class RowAdvanced(val row: Int) : VoiceReply
    data class RowUndone(val row: Int) : VoiceReply
    data object PrepRowNow : VoiceReply
    data object FinalRowNow : VoiceReply
    data class AskNextPartConfirm(val partName: String) : VoiceReply
    data class NextPartOpening(val partName: String) : VoiceReply
    data object NextPartCancelled : VoiceReply
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
            val step = part.currentStep
            val nextStep = part.stepAfter()
            CounterUiState.Ready(
                partName = part.name,
                currentRow = part.currentRow,
                step = step,
                nextStep = nextStep,
                stepNote = part.noteOf(step),
                nextStepNote = nextStep?.let(part::noteOf),
                totalRows = part.totalRows,
                completedRows = part.completedRows,
                progress = part.progress,
                isComplete = part.isComplete,
                currentNotes = (step as? RowStep.Row)?.let { notes.notesForRow(it.number) }.orEmpty(),
                nextNotes = (nextStep as? RowStep.Row)?.let { notes.notesForRow(it.number) }.orEmpty(),
                projectIcon = projectWithParts?.project?.icon ?: ProjectIcon.YARN,
                projectColor = projectWithParts?.project?.color ?: ProjectColor.TERRACOTTA,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CounterUiState.Loading)

    private val _nextPartPrompt = MutableStateFlow<NextPartPrompt?>(null)
    val nextPartPrompt: StateFlow<NextPartPrompt?> = _nextPartPrompt

    private val _navigateToPart = MutableSharedFlow<Long>(extraBufferCapacity = 1)

    /** Emits the part to open once the current one has been finished and confirmed. */
    val navigateToPart: SharedFlow<Long> = _navigateToPart.asSharedFlow()

    /** The first unfinished part after [part], if any. */
    private suspend fun partAfter(part: Part): Part? {
        val parts = repository.observeProject(part.projectId).first()?.parts ?: return null
        return nextPart(parts, part)
    }

    /**
     * Finishes the row in progress. On the last step of a part that has an unfinished part after it,
     * nothing changes yet: [nextPartPrompt] asks first, so a stray tap never jumps away.
     * Returns the part to move to when it is asking, null when the count simply advanced.
     */
    private suspend fun advance(part: Part): Part? {
        if (part.isLastStep) {
            val next = partAfter(part)
            if (next != null) {
                _nextPartPrompt.value = NextPartPrompt(next.id, next.name, part.name)
                return next
            }
        }
        repository.adjustCompletedRows(partId, +1)
        return null
    }

    fun increment() {
        viewModelScope.launch {
            val part = repository.observePart(partId).first() ?: return@launch
            if (part.isComplete || _nextPartPrompt.value != null) return@launch
            advance(part)
        }
    }

    /** Completes the part, lets the celebration play, then asks the screen to open the next part. */
    fun confirmNextPart() {
        val prompt = _nextPartPrompt.value ?: return
        _nextPartPrompt.value = null
        viewModelScope.launch {
            repository.adjustCompletedRows(partId, +1)
            delay(NEXT_PART_DELAY_MILLIS)
            _navigateToPart.emit(prompt.partId)
        }
    }

    fun dismissNextPart() {
        _nextPartPrompt.value = null
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
            repository.setCompletedRows(partId, part.completedRowsForRow(clamped))
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
            val prompt = _nextPartPrompt.value
            if (prompt != null) {
                if (command == VoiceCommand.Confirm) {
                    confirmNextPart()
                    _voiceReplies.emit(VoiceReply.NextPartOpening(prompt.partName))
                } else {
                    dismissNextPart()
                    _voiceReplies.emit(VoiceReply.NextPartCancelled)
                }
                return@launch
            }
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
                        val asking = advance(part)
                        if (asking != null) {
                            _voiceReplies.emit(VoiceReply.AskNextPartConfirm(asking.name))
                        } else {
                            val after = part.copy(completedRows = part.completedRows + 1)
                            _voiceReplies.emit(
                                if (after.isComplete) VoiceReply.PartFinished(part.totalRows)
                                else after.stepReply(advanced = true),
                            )
                        }
                    }
                }
                VoiceCommand.Decrement -> {
                    repository.adjustCompletedRows(partId, -1)
                    val after = part.copy(completedRows = (part.completedRows - 1).coerceAtLeast(0))
                    _voiceReplies.emit(after.stepReply(advanced = false))
                }
                is VoiceCommand.GoToRow -> {
                    if (command.row in 1..part.totalRows) {
                        repository.setCompletedRows(partId, part.completedRowsForRow(command.row))
                        _voiceReplies.emit(VoiceReply.WentTo(command.row))
                    } else {
                        _voiceReplies.emit(VoiceReply.OutOfRange(part.totalRows))
                    }
                }
                VoiceCommand.ReadNote -> {
                    val texts = when (val step = part.currentStep) {
                        is RowStep.Row -> repository.observeNotes(partId).first().notesForRow(step.number).map { it.text }
                        else -> listOfNotNull(part.noteOf(step))
                    }
                    _voiceReplies.emit(if (texts.isEmpty()) VoiceReply.NoNote else VoiceReply.Note(texts))
                }
                VoiceCommand.RemainingRows ->
                    _voiceReplies.emit(VoiceReply.Remaining((part.totalSteps - part.completedRows).coerceAtLeast(0)))
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

        /** How long the "part finished" celebration plays before the next part opens. */
        const val NEXT_PART_DELAY_MILLIS = 1_200L
    }
}

/** The step after the current one; null when the current step is the last. */
private fun Part.stepAfter(): RowStep? =
    if (isLastStep || isComplete) null else copy(completedRows = completedRows + 1).currentStep

private fun Part.noteOf(step: RowStep): String? = when (step) {
    RowStep.Prep -> prepNote
    RowStep.Final -> finalNote
    is RowStep.Row -> null
}?.takeIf { it.isNotBlank() }

/** What to say when the counter lands on this part's current step. */
private fun Part.stepReply(advanced: Boolean): VoiceReply = when (val step = currentStep) {
    RowStep.Prep -> VoiceReply.PrepRowNow
    RowStep.Final -> VoiceReply.FinalRowNow
    is RowStep.Row -> if (advanced) VoiceReply.RowAdvanced(step.number) else VoiceReply.RowUndone(step.number)
}
