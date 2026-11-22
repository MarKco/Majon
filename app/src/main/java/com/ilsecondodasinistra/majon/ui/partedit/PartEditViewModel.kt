package com.ilsecondodasinistra.majon.ui.partedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.domain.model.MeasureKey
import com.ilsecondodasinistra.majon.domain.model.isAcross
import com.ilsecondodasinistra.majon.domain.model.oriented
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.PartEstimate
import com.ilsecondodasinistra.majon.domain.model.PresetPart
import com.ilsecondodasinistra.majon.domain.model.ProjectIcon
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.defaultSwatch
import com.ilsecondodasinistra.majon.domain.model.estimate
import com.ilsecondodasinistra.majon.domain.model.formatDecimalInput
import com.ilsecondodasinistra.majon.domain.model.parseDecimal
import com.ilsecondodasinistra.majon.domain.model.presetParts
import com.ilsecondodasinistra.majon.domain.repository.MajonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The part editor's fields, as typed. */
data class PartForm(
    val name: String = "",
    val rows: String = "",
    val prepRow: Boolean = false,
    val prepNote: String = "",
    val finalRow: Boolean = false,
    val finalNote: String = "",
    val stitchMeasure: MeasureKey = MeasureKey.WIDTH,
    val stitchCm: String = "",
    val rowMeasure: MeasureKey = MeasureKey.LENGTH,
    val rowCm: String = "",
    /** Swatch for this part; null = the one of the project's main yarn. */
    val swatchId: Long? = null,
    val nameError: Boolean = false,
    val rowsError: Boolean = false,
    val isEditing: Boolean = false,
    /** Normal rows already finished when editing, to warn when the total drops below them. */
    val rowsDone: Int = 0,
    /** How many times the part is knitted (digits as typed, at least 1). */
    val copies: String = "1",
    /** The copy being worked when editing, 1-based. */
    val currentCopy: Int = 1,
    /** The stitches to cast on, as the pattern says. */
    val castOn: String = "",
    /** Where the stitch measure ends up at the last row, when the part widens or narrows. */
    val endStitchCm: String = "",
) {
    /** Worked sideways: the stitches follow the length/height, the rows the width/circumference. */
    val sideways: Boolean get() = !stitchMeasure.isAcross
}

/** What the editor needs to know about the project the part belongs to. */
data class PartEditContext(
    val projectIcon: ProjectIcon = ProjectIcon.YARN,
    val presets: List<PresetPart> = emptyList(),
    /** Swatches of the project's yarns, to pick one for this part. */
    val swatches: List<Swatch> = emptyList(),
    val yarnNames: Map<Long, String> = emptyMap(),
    val defaultSwatch: Swatch? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PartEditViewModel @Inject constructor(
    private val repository: MajonRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val projectId: Long = checkNotNull(savedStateHandle["projectId"])
    private val partId: Long = savedStateHandle["partId"] ?: 0L
    private var original: Part? = null

    private val _form = MutableStateFlow(PartForm(isEditing = partId != 0L))
    val form: StateFlow<PartForm> = _form

    private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saved: SharedFlow<Unit> = _saved.asSharedFlow()

    private val swatchesByYarn = repository.observeYarns().flatMapLatest { all ->
        if (all.isEmpty()) {
            flowOf(emptyMap<Long, List<Swatch>>())
        } else {
            combine(all.map { y -> repository.observeSwatches(y.id).map { y.id to it } }) { it.toMap() }
        }
    }

    val context: StateFlow<PartEditContext> = combine(
        repository.observeProject(projectId),
        repository.observeProjectLinks(projectId),
        repository.observeYarns(),
        swatchesByYarn,
    ) { project, links, yarns, swatches ->
        val icon = project?.project?.icon ?: ProjectIcon.YARN
        PartEditContext(
            projectIcon = icon,
            presets = icon.presetParts(),
            swatches = links.yarns.flatMap { swatches[it.yarnId].orEmpty() },
            yarnNames = links.yarns.mapNotNull { l -> yarns.firstOrNull { it.id == l.yarnId }?.let { it.id to it.name } }.toMap(),
            defaultSwatch = defaultSwatch(links, swatches),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PartEditContext())

    /** Stitches to cast on and rows to knit, from the measures typed and the swatch that applies. */
    val estimate: StateFlow<PartEstimate> = combine(_form, context) { f, c ->
        val swatch = c.swatches.firstOrNull { it.id == f.swatchId } ?: c.defaultSwatch
        Part(
            projectId = 0L, name = "", totalRows = 1,
            stitchCm = parseDecimal(f.stitchCm), endStitchCm = parseDecimal(f.endStitchCm), rowCm = parseDecimal(f.rowCm),
        ).estimate(swatch)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PartEstimate(null, null))

    init {
        if (partId != 0L) {
            viewModelScope.launch {
                val part = repository.observePart(partId).first() ?: return@launch
                original = part
                _form.value = part.toForm()
            }
        } else {
            // Start from the measures typical of this kind of project (a hat is measured round, a scarf across)
            viewModelScope.launch {
                val icon = repository.observeProject(projectId).first()?.project?.icon ?: return@launch
                icon.presetParts().firstOrNull()?.let { first ->
                    _form.value = _form.value.copy(stitchMeasure = first.stitchMeasure, rowMeasure = first.rowMeasure)
                }
            }
        }
    }

    fun update(transform: (PartForm) -> PartForm) {
        _form.value = transform(_form.value).let { it.copy(nameError = it.nameError && it.name.isBlank(), rowsError = false) }
    }

    /** Fills the name and the measures of a suggested part. */
    fun applyPreset(preset: PresetPart, label: String) = update {
        val oriented = preset.oriented(it.sideways)
        it.copy(name = label, stitchMeasure = oriented.stitchMeasure, rowMeasure = oriented.rowMeasure)
    }

    /**
     * Turns the part to be worked sideways (or back): what gives the stitches and what gives the rows trade
     * places, each measure keeping its own number of centimetres.
     */
    fun setSideways(sideways: Boolean) = update {
        if (it.sideways == sideways) {
            it
        } else {
            // The end width belongs to the old stitch direction, so it goes
            it.copy(
                stitchMeasure = it.rowMeasure, stitchCm = it.rowCm, rowMeasure = it.stitchMeasure, rowCm = it.stitchCm,
                endStitchCm = "",
            )
        }
    }

    /** Copies the stitches estimated from the measures into the cast-on field. */
    fun useEstimatedCastOn() {
        estimate.value.stitches?.let { stitches -> update { it.copy(castOn = stitches.toString()) } }
    }

    /** Copies the rows estimated from the measures into the rows field. */
    fun useEstimatedRows() {
        estimate.value.rows?.let { rows -> update { it.copy(rows = rows.toString()) } }
    }

    /**
     * Saves the part. The rows are the number typed, or, when the field is left empty, the estimate from the
     * measures. Returns false (nothing saved, errors set on the form) when the name or the rows are missing.
     */
    fun save(): Boolean {
        val f = _form.value
        val rows = f.rows.trim().toIntOrNull()?.takeIf { it > 0 } ?: estimate.value.rows?.takeIf { it > 0 }
        val copies = f.copies.trim().toIntOrNull()?.coerceIn(1, MAX_COPIES) ?: 1
        val nameError = f.name.isBlank()
        val rowsError = rows == null
        if (nameError || rowsError) {
            _form.value = f.copy(nameError = nameError, rowsError = rowsError)
            return false
        }
        viewModelScope.launch {
            val base = original
            val part = if (base == null) {
                Part(
                    projectId = projectId,
                    name = f.name.trim(),
                    totalRows = rows!!,
                    hasPrepRow = f.prepRow,
                    hasFinalRow = f.finalRow,
                    prepNote = f.prepNote.orNull(),
                    finalNote = f.finalNote.orNull(),
                    copies = copies,
                )
            } else {
                // Toggle the extra rows first (it shifts the count to stay on the same row), then resize
                base.withRowOptions(prep = f.prepRow, final = f.finalRow, prepNote = f.prepNote.orNull(), finalNote = f.finalNote.orNull())
                    .copy(name = f.name.trim(), totalRows = rows!!)
                    .clampedToTotal()
                    .withCopies(copies)
                    .let { resized ->
                        // Moving to another copy starts it from its first row
                        if (f.currentCopy == base.currentCopy) resized
                        else resized.copy(currentCopy = f.currentCopy.coerceIn(1, resized.copies), completedRows = 0)
                    }
            }
            repository.upsertPart(
                part.copy(
                    stitchMeasure = f.stitchMeasure,
                    stitchCm = parseDecimal(f.stitchCm)?.takeIf { it > 0.0 },
                    rowMeasure = f.rowMeasure,
                    rowCm = parseDecimal(f.rowCm)?.takeIf { it > 0.0 },
                    endStitchCm = parseDecimal(f.endStitchCm)?.takeIf { it > 0.0 },
                    castOnStitches = f.castOn.trim().toIntOrNull()?.takeIf { it > 0 },
                    swatchId = f.swatchId,
                ),
            )
            _saved.emit(Unit)
        }
        return true
    }
}

/** Most copies of a part the editor accepts (two sleeves, a few pockets...). */
const val MAX_COPIES = 10

private fun String.orNull() = trim().ifEmpty { null }

private fun Part.toForm() = PartForm(
    name = name,
    // Zero means "to define": the field starts empty, not showing a 0 to delete
    rows = if (totalRows > 0) totalRows.toString() else "",
    prepRow = hasPrepRow,
    prepNote = prepNote.orEmpty(),
    finalRow = hasFinalRow,
    finalNote = finalNote.orEmpty(),
    stitchMeasure = stitchMeasure ?: MeasureKey.WIDTH,
    stitchCm = stitchCm?.let(::formatDecimalInput).orEmpty(),
    rowMeasure = rowMeasure ?: MeasureKey.LENGTH,
    rowCm = rowCm?.let(::formatDecimalInput).orEmpty(),
    swatchId = swatchId,
    isEditing = true,
    rowsDone = rowsDone,
    copies = copies.toString(),
    currentCopy = currentCopy,
    castOn = castOnStitches?.toString().orEmpty(),
    endStitchCm = endStitchCm?.let(::formatDecimalInput).orEmpty(),
)
