package com.ilsecondodasinistra.majon.ui.stash

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.Yarn
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
