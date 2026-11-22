package com.ilsecondodasinistra.majon.ui.stash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.NeedleMaterial
import com.ilsecondodasinistra.majon.domain.model.NeedleType
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

    /**
     * Saves a needle from the editor's text fields. Size, length, brand and notes may be empty; the
     * quantity must be a whole number of at least one, and a size, when given, must be above zero.
     * Returns false (nothing saved) when a filled field is not valid.
     */
    fun saveNeedle(
        id: Long?,
        type: NeedleType,
        material: NeedleMaterial?,
        sizeMm: String,
        lengthCm: String,
        quantity: String,
        brand: String,
        notes: String,
    ): Boolean {
        val qty = quantity.trim().toIntOrNull() ?: return false
        if (qty < 1) return false
        val size = parseDecimal(sizeMm)
        if (sizeMm.isNotBlank() && (size == null || size <= 0.0)) return false
        val length = lengthCm.trim().toIntOrNull()
        if (lengthCm.isNotBlank() && (length == null || length <= 0)) return false
        viewModelScope.launch {
            repository.upsertNeedle(
                Needle(
                    id = id ?: 0L,
                    type = type,
                    material = material,
                    sizeMm = size,
                    lengthCm = length,
                    quantity = qty,
                    brand = brand.trim().ifEmpty { null },
                    notes = notes.trim().ifEmpty { null },
                ),
            )
        }
        return true
    }
}
