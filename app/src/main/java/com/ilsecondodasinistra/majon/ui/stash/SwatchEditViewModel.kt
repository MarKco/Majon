package com.ilsecondodasinistra.majon.ui.stash

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.domain.model.StitchPattern
import com.ilsecondodasinistra.majon.domain.model.Swatch
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

/** The swatch editor's fields, as typed: the numbers of the 10 x 10 cm square and the rest. */
data class SwatchForm(
    val stitches: String = "",
    val rows: String = "",
    val needleMm: String = "",
    val pattern: StitchPattern = StitchPattern.STOCKINETTE,
    val weightG: String = "",
    val notes: String = "",
    val error: Boolean = false,
    val isEditing: Boolean = false,
)

@HiltViewModel
class SwatchEditViewModel @Inject constructor(
    private val repository: MajonRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val yarnId: Long = checkNotNull(savedStateHandle["yarnId"])
    private val swatchId: Long = savedStateHandle["swatchId"] ?: 0L

    private val _form = MutableStateFlow(SwatchForm(isEditing = swatchId != 0L))
    val form: StateFlow<SwatchForm> = _form

    private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saved: SharedFlow<Unit> = _saved.asSharedFlow()

    init {
        if (swatchId != 0L) {
            viewModelScope.launch {
                val s = repository.observeSwatches(yarnId).first().firstOrNull { it.id == swatchId } ?: return@launch
                _form.value = SwatchForm(
                    stitches = s.stitches.toString(),
                    rows = s.rows.toString(),
                    needleMm = s.needleSizeMm?.let(::formatDecimalInput).orEmpty(),
                    pattern = s.pattern,
                    weightG = s.weightG?.let(::formatDecimalInput).orEmpty(),
                    notes = s.notes.orEmpty(),
                    isEditing = true,
                )
            }
        }
    }

    fun update(transform: (SwatchForm) -> SwatchForm) {
        _form.value = transform(_form.value).copy(error = false)
    }

    /**
     * Saves the swatch. Stitches and rows (over 10 x 10 cm) must be whole numbers above zero; needle size and
     * swatch weight are optional but must be above zero when filled. Returns false (error set) otherwise.
     */
    fun save(): Boolean {
        val f = _form.value
        val stitches = f.stitches.trim().toIntOrNull()?.takeIf { it > 0 }
        val rows = f.rows.trim().toIntOrNull()?.takeIf { it > 0 }
        val needle = parseDecimal(f.needleMm)
        val weight = parseDecimal(f.weightG)
        val bad = stitches == null || rows == null ||
            (f.needleMm.isNotBlank() && (needle == null || needle <= 0.0)) ||
            (f.weightG.isNotBlank() && (weight == null || weight <= 0.0))
        if (bad) {
            _form.value = f.copy(error = true)
            return false
        }
        viewModelScope.launch {
            repository.upsertSwatch(
                Swatch(
                    id = swatchId,
                    yarnId = yarnId,
                    stitches = stitches!!,
                    rows = rows!!,
                    needleSizeMm = needle,
                    pattern = f.pattern,
                    weightG = weight,
                    notes = f.notes.trim().ifEmpty { null },
                ),
            )
            _saved.emit(Unit)
        }
        return true
    }
}
