package com.ilsecondodasinistra.majon.ui.stash

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.domain.model.StitchPattern
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.domain.model.parseDecimal
import com.ilsecondodasinistra.majon.domain.repository.MajonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface YarnDetailUiState {
    data object Loading : YarnDetailUiState
    data object Deleted : YarnDetailUiState
    data class Ready(val yarn: Yarn, val swatches: List<Swatch>) : YarnDetailUiState
}

@HiltViewModel
class YarnDetailViewModel @Inject constructor(
    private val repository: MajonRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val yarnId: Long = checkNotNull(savedStateHandle["yarnId"])
    private var deleted = false

    val uiState: StateFlow<YarnDetailUiState> = combine(
        repository.observeYarn(yarnId),
        repository.observeSwatches(yarnId),
    ) { yarn, swatches ->
        when {
            yarn != null -> YarnDetailUiState.Ready(yarn, swatches)
            deleted -> YarnDetailUiState.Deleted
            else -> YarnDetailUiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YarnDetailUiState.Loading)

    /** Adds [deltaSkeins] to the stock (negative = used some); never goes below zero. */
    fun adjustStock(deltaSkeins: Double) {
        viewModelScope.launch { repository.adjustYarnStock(yarnId, deltaSkeins) }
    }

    /**
     * Saves a gauge swatch from the editor's text fields. Stitches and rows (over 10 x 10 cm) must be
     * whole numbers above zero; needle size and swatch weight are optional but must be numbers above
     * zero when filled. Returns false (nothing saved) otherwise.
     */
    fun saveSwatch(
        id: Long?,
        stitches: String,
        rows: String,
        needleSizeMm: String,
        pattern: StitchPattern,
        weightG: String,
        notes: String,
    ): Boolean {
        val st = stitches.trim().toIntOrNull()?.takeIf { it > 0 } ?: return false
        val ro = rows.trim().toIntOrNull()?.takeIf { it > 0 } ?: return false
        val needle = parseDecimal(needleSizeMm)
        if (needleSizeMm.isNotBlank() && (needle == null || needle <= 0.0)) return false
        val weight = parseDecimal(weightG)
        if (weightG.isNotBlank() && (weight == null || weight <= 0.0)) return false
        viewModelScope.launch {
            repository.upsertSwatch(
                Swatch(
                    id = id ?: 0L,
                    yarnId = yarnId,
                    stitches = st,
                    rows = ro,
                    needleSizeMm = needle,
                    pattern = pattern,
                    weightG = weight,
                    notes = notes.trim().ifEmpty { null },
                ),
            )
        }
        return true
    }

    fun deleteSwatch(id: Long) {
        viewModelScope.launch { repository.deleteSwatch(id) }
    }

    fun deleteYarn() {
        viewModelScope.launch {
            deleted = true
            repository.deleteYarn(yarnId)
        }
    }
}
