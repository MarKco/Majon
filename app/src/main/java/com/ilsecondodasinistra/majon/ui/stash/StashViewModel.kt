package com.ilsecondodasinistra.majon.ui.stash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.domain.repository.MajonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface StashUiState {
    data object Loading : StashUiState
    data class Ready(val yarns: List<Yarn>, val needles: List<Needle>) : StashUiState
}

/** The Stash tab: all yarns and needles, plus the needle editor (yarns have their own screens). */
@HiltViewModel
class StashViewModel @Inject constructor(
    private val repository: MajonRepository,
) : ViewModel() {

    val uiState: StateFlow<StashUiState> = combine(
        repository.observeYarns(),
        repository.observeNeedles(),
    ) { yarns, needles -> StashUiState.Ready(yarns, needles) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StashUiState.Loading)

    fun deleteYarn(id: Long) {
        viewModelScope.launch { repository.deleteYarn(id) }
    }

    fun deleteNeedle(id: Long) {
        viewModelScope.launch { repository.deleteNeedle(id) }
    }
}
