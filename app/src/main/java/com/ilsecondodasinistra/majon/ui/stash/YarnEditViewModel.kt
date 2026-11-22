package com.ilsecondodasinistra.majon.ui.stash

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.domain.model.MaterialShare
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.domain.model.YarnMaterial
import com.ilsecondodasinistra.majon.domain.model.YarnWeight
import com.ilsecondodasinistra.majon.domain.model.formatDecimalInput
import com.ilsecondodasinistra.majon.domain.model.materialsExceedHundred
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

/** One fibre row of the blend, with the percentage as typed (may be empty). */
data class MaterialRow(
    val material: YarnMaterial,
    val customName: String = "",
    val percent: String = "",
)

/** The yarn editor's fields, all as typed. */
data class YarnForm(
    val name: String = "",
    val brand: String = "",
    val colorName: String = "",
    val colorArgb: Int? = null,
    val dyeLot: String = "",
    val weight: YarnWeight? = null,
    val materials: List<MaterialRow> = emptyList(),
    val skeinWeightG: String = "",
    val skeinLengthM: String = "",
    val stockSkeins: String = "",
    val pricePerSkein: String = "",
    val shop: String = "",
    val care: String = "",
    val notes: String = "",
    val photoPath: String? = null,
    val nameError: Boolean = false,
    val materialsError: Boolean = false,
    val isEditing: Boolean = false,
)

@HiltViewModel
class YarnEditViewModel @Inject constructor(
    private val repository: MajonRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val yarnId: Long = savedStateHandle["yarnId"] ?: 0L
    private var original: Yarn? = null

    private val _form = MutableStateFlow(YarnForm(isEditing = yarnId != 0L))
    val form: StateFlow<YarnForm> = _form

    private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saved: SharedFlow<Unit> = _saved.asSharedFlow()

    init {
        if (yarnId != 0L) {
            viewModelScope.launch {
                val yarn = repository.observeYarn(yarnId).first() ?: return@launch
                original = yarn
                _form.value = yarn.toForm()
            }
        }
    }

    fun update(transform: (YarnForm) -> YarnForm) {
        _form.value = transform(_form.value).let {
            it.copy(
                nameError = it.nameError && it.name.isBlank(),
                materialsError = it.materialsError && materialsExceedHundred(it.toShares()),
            )
        }
    }

    fun setMaterials(materials: List<MaterialRow>) = update { it.copy(materials = materials) }

    fun setPhoto(path: String?) = update { it.copy(photoPath = path) }

    /** Moves the stock field by [delta] skeins (e.g. ±0.25), starting from what is typed; never below zero. */
    fun changeStock(delta: Double) = update {
        val current = parseDecimal(it.stockSkeins) ?: 0.0
        it.copy(stockSkeins = formatDecimalInput((current + delta).coerceAtLeast(0.0)))
    }

    /** Validates and saves; false (with the field errors set) when the form is not valid. */
    fun save(): Boolean {
        val f = _form.value
        val nameError = f.name.isBlank()
        val materialsError = materialsExceedHundred(f.toShares())
        if (nameError || materialsError) {
            _form.value = f.copy(nameError = nameError, materialsError = materialsError)
            return false
        }
        viewModelScope.launch {
            repository.upsertYarn(f.toYarn(original))
            _saved.emit(Unit)
        }
        return true
    }
}

private fun String.orNull() = trim().ifEmpty { null }

private fun YarnForm.toShares(): List<MaterialShare> = materials.map {
    MaterialShare(
        material = it.material,
        customName = if (it.material == YarnMaterial.OTHER) it.customName.orNull() else null,
        percent = it.percent.trim().toIntOrNull()?.coerceIn(0, 100),
    )
}

private fun YarnForm.toYarn(original: Yarn?): Yarn = (original ?: Yarn(name = "")).copy(
    name = name.trim(),
    brand = brand.orNull(),
    colorName = colorName.orNull(),
    colorArgb = colorArgb,
    dyeLot = dyeLot.orNull(),
    weight = weight,
    materials = toShares(),
    skeinWeightG = skeinWeightG.trim().toIntOrNull()?.takeIf { it > 0 },
    skeinLengthM = skeinLengthM.trim().toIntOrNull()?.takeIf { it > 0 },
    stockSkeins = (parseDecimal(stockSkeins) ?: 0.0).coerceAtLeast(0.0),
    pricePerSkein = parseDecimal(pricePerSkein)?.takeIf { it >= 0.0 },
    shop = shop.orNull(),
    care = care.orNull(),
    notes = notes.orNull(),
    photoPath = photoPath,
)

private fun Yarn.toForm() = YarnForm(
    name = name,
    brand = brand.orEmpty(),
    colorName = colorName.orEmpty(),
    colorArgb = colorArgb,
    dyeLot = dyeLot.orEmpty(),
    weight = weight,
    materials = materials.map {
        MaterialRow(it.material, it.customName.orEmpty(), it.percent?.toString().orEmpty())
    },
    skeinWeightG = skeinWeightG?.toString().orEmpty(),
    skeinLengthM = skeinLengthM?.toString().orEmpty(),
    stockSkeins = formatDecimalInput(stockSkeins),
    pricePerSkein = pricePerSkein?.let(::formatDecimalInput).orEmpty(),
    shop = shop.orEmpty(),
    care = care.orEmpty(),
    notes = notes.orEmpty(),
    photoPath = photoPath,
    isEditing = true,
)
