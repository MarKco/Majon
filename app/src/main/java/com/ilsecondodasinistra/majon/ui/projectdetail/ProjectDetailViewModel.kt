package com.ilsecondodasinistra.majon.ui.projectdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.PresetPart
import com.ilsecondodasinistra.majon.domain.model.ProjectWithParts
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.YarnPlan
import com.ilsecondodasinistra.majon.domain.model.defaultSwatch
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

    /**
     * Creates the suggested parts of the project's type in one go, rows still to define (zero), with the
     * measures typical of each. A part whose name is already there is skipped.
     */
    fun addSuggestedParts(suggested: List<Pair<PresetPart, String>>) {
        viewModelScope.launch {
            val existing = repository.observeProject(projectId).first()?.parts.orEmpty().map { it.name.trim().lowercase() }.toSet()
            suggested.filter { (_, name) -> name.trim().lowercase() !in existing }.forEach { (preset, name) ->
                repository.upsertPart(
                    Part(
                        projectId = projectId,
                        name = name.trim(),
                        totalRows = 0,
                        stitchMeasure = preset.stitchMeasure,
                        rowMeasure = preset.rowMeasure,
                    ),
                )
            }
        }
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
