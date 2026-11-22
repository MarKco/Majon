package com.ilsecondodasinistra.majon.ui.stash

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.NeedleMaterial
import com.ilsecondodasinistra.majon.domain.model.NeedleType
import com.ilsecondodasinistra.majon.domain.model.formatDecimalInput
import com.ilsecondodasinistra.majon.domain.model.parseDecimal
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

/** The needle editor's fields, as typed. */
data class NeedleForm(
    val type: NeedleType = NeedleType.CIRCULAR,
    val material: NeedleMaterial? = null,
    val sizeMm: String = "",
    val lengthCm: String = "",
    val quantity: String = "1",
    val brand: String = "",
    val notes: String = "",
    val error: Boolean = false,
    val isEditing: Boolean = false,
)

@HiltViewModel
class NeedleEditViewModel @Inject constructor(
    private val repository: MajonRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val needleId: Long = savedStateHandle["needleId"] ?: 0L

    private val _form = MutableStateFlow(NeedleForm(isEditing = needleId != 0L))
    val form: StateFlow<NeedleForm> = _form

    private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saved: SharedFlow<Unit> = _saved.asSharedFlow()

    init {
        if (needleId != 0L) {
            viewModelScope.launch {
                val n = repository.observeNeedles().first().firstOrNull { it.id == needleId } ?: return@launch
                _form.value = NeedleForm(
                    type = n.type,
                    material = n.material,
                    sizeMm = n.sizeMm?.let(::formatDecimalInput).orEmpty(),
                    lengthCm = n.lengthCm?.toString().orEmpty(),
                    quantity = n.quantity.toString(),
                    brand = n.brand.orEmpty(),
                    notes = n.notes.orEmpty(),
                    isEditing = true,
                )
            }
        }
    }

    fun update(transform: (NeedleForm) -> NeedleForm) {
        _form.value = transform(_form.value).copy(error = false)
    }

    /**
     * Saves the needle. Size, length, brand and notes may be empty; the quantity must be a whole number of at
     * least one, and a size or length, when given, must be above zero. Returns false (error set) otherwise.
     */
    fun save(): Boolean {
        val f = _form.value
        val quantity = f.quantity.trim().toIntOrNull()?.takeIf { it >= 1 }
        val size = parseDecimal(f.sizeMm)
        val length = f.lengthCm.trim().toIntOrNull()
        val bad = quantity == null ||
            (f.sizeMm.isNotBlank() && (size == null || size <= 0.0)) ||
            (f.lengthCm.isNotBlank() && (length == null || length <= 0))
        if (bad) {
            _form.value = f.copy(error = true)
            return false
        }
        viewModelScope.launch {
            repository.upsertNeedle(
                Needle(
                    id = needleId,
                    type = f.type,
                    material = f.material,
                    sizeMm = size,
                    lengthCm = length,
                    quantity = quantity!!,
                    brand = f.brand.trim().ifEmpty { null },
                    notes = f.notes.trim().ifEmpty { null },
                ),
            )
            _saved.emit(Unit)
        }
        return true
    }
}
