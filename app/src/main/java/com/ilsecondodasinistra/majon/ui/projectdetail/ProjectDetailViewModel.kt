package com.ilsecondodasinistra.majon.ui.projectdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.domain.model.MeasureKey
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.ProjectWithParts
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.YarnPlan
import com.ilsecondodasinistra.majon.domain.model.defaultSwatch
import com.ilsecondodasinistra.majon.domain.model.parseDecimal
import com.ilsecondodasinistra.majon.domain.model.planYarns
import com.ilsecondodasinistra.majon.domain.repository.MajonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ProjectDetailUiState {
    data object Loading : ProjectDetailUiState
    data object Deleted : ProjectDetailUiState
    data class Ready(
        val projectWithParts: ProjectWithParts,
        /** One entry per yarn the project is made with: what is needed and whether the stock covers it. */
        val plans: List<YarnPlan> = emptyList(),
        val needles: List<Needle> = emptyList(),
        /** The swatch the project's measures are turned into stitches and rows with (its main yarn's). */
        val defaultSwatch: Swatch? = null,
        /** Swatches of the project's yarns, to pick one for a part. */
        val swatches: List<Swatch> = emptyList(),
    ) : ProjectDetailUiState
}

/** The optional preparation (row 0) and final joining rows of a part, with their notes. */
data class RowExtras(
    val prepRow: Boolean = false,
    val prepNote: String? = null,
    val finalRow: Boolean = false,
    val finalNote: String? = null,
)

/** The two measures a part is planned by, as typed (cm), and the swatch to use for it. */
data class PartMeasures(
    val stitchMeasure: MeasureKey?,
    val stitchCm: String,
    val rowMeasure: MeasureKey?,
    val rowCm: String,
    val swatchId: Long? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val repository: MajonRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val projectId: Long = checkNotNull(savedStateHandle["projectId"])
    private var deleted = false

    private val swatchesByYarn = repository.observeYarns().flatMapLatest { all ->
        if (all.isEmpty()) {
            flowOf(emptyMap<Long, List<Swatch>>())
        } else {
            combine(all.map { y -> repository.observeSwatches(y.id).map { y.id to it } }) { it.toMap() }
        }
    }

    val uiState: StateFlow<ProjectDetailUiState> = combine(
        repository.observeProject(projectId),
        repository.observeProjectLinks(projectId),
        repository.observeYarns(),
        repository.observeNeedles(),
        swatchesByYarn,
    ) { project, links, yarns, needles, swatches ->
        when {
            project != null -> ProjectDetailUiState.Ready(
                projectWithParts = project,
                plans = planYarns(project.parts, links, yarns.associateBy { it.id }, swatches),
                needles = needles.filter { it.id in links.needleIds },
                defaultSwatch = defaultSwatch(links, swatches),
                swatches = links.yarns.flatMap { swatches[it.yarnId].orEmpty() },
            )
            deleted -> ProjectDetailUiState.Deleted
            else -> ProjectDetailUiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProjectDetailUiState.Loading)

    private val _duplicated = MutableSharedFlow<Long>(extraBufferCapacity = 1)

    /** The id of the copy once [duplicateProject] has made it. */
    val duplicated: SharedFlow<Long> = _duplicated.asSharedFlow()

    fun duplicateProject(copyName: String) {
        viewModelScope.launch { repository.duplicateProject(projectId, copyName)?.let { _duplicated.emit(it) } }
    }

    /** Takes [skeins] off the stock of one of the project's yarns. */
    fun useSkeins(yarnId: Long, skeins: Double) {
        viewModelScope.launch { repository.adjustYarnStock(yarnId, -skeins) }
    }

    fun addPart(name: String, totalRows: Int, extras: RowExtras? = null, measures: PartMeasures? = null): Boolean {
        if (name.isBlank() || totalRows <= 0) return false
        viewModelScope.launch {
            repository.upsertPart(
                Part(
                    projectId = projectId,
                    name = name.trim(),
                    totalRows = totalRows,
                    hasPrepRow = extras?.prepRow ?: false,
                    hasFinalRow = extras?.finalRow ?: false,
                    prepNote = extras?.prepNote?.trim()?.takeIf { it.isNotEmpty() },
                    finalNote = extras?.finalNote?.trim()?.takeIf { it.isNotEmpty() },
                ).withMeasures(measures),
            )
        }
        return true
    }

    /** [extras] / [measures] = null leave the preparation / final rows and the measures as they are. */
    fun updatePart(partId: Long, name: String, totalRows: Int, extras: RowExtras? = null, measures: PartMeasures? = null): Boolean {
        if (name.isBlank() || totalRows <= 0) return false
        viewModelScope.launch {
            val part = repository.observePart(partId).first() ?: return@launch
            // Toggle the extra rows first (it shifts the count to stay on the same row), then resize
            val withExtras = if (extras == null) part else part.withRowOptions(
                prep = extras.prepRow,
                final = extras.finalRow,
                prepNote = extras.prepNote?.trim()?.takeIf { it.isNotEmpty() },
                finalNote = extras.finalNote?.trim()?.takeIf { it.isNotEmpty() },
            )
            repository.upsertPart(
                withExtras.copy(name = name.trim(), totalRows = totalRows).clampedToTotal().withMeasures(measures),
            )
        }
        return true
    }

    fun deletePart(partId: Long) {
        viewModelScope.launch { repository.deletePart(partId) }
    }

    fun deleteProject() {
        viewModelScope.launch {
            deleted = true
            repository.deleteProject(projectId)
        }
    }
}

/** Applies typed measures to a part; empty or invalid numbers mean "no measure". Null leaves the part as it is. */
private fun Part.withMeasures(measures: PartMeasures?): Part = if (measures == null) this else copy(
    stitchMeasure = measures.stitchMeasure,
    stitchCm = parseDecimal(measures.stitchCm)?.takeIf { it > 0.0 },
    rowMeasure = measures.rowMeasure,
    rowCm = parseDecimal(measures.rowCm)?.takeIf { it > 0.0 },
    swatchId = measures.swatchId,
)
