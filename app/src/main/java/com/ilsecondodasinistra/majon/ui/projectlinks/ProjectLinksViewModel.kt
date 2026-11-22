package com.ilsecondodasinistra.majon.ui.projectlinks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.ProjectLinks
import com.ilsecondodasinistra.majon.domain.model.ProjectYarnLink
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.domain.model.normalized
import com.ilsecondodasinistra.majon.domain.model.parseDecimal
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Choosing, from the stash, the yarns and needles a project is made with. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProjectLinksViewModel @Inject constructor(
    private val repository: MajonRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val projectId: Long = checkNotNull(savedStateHandle["projectId"])

    private val _links = MutableStateFlow(ProjectLinks())
    val links: StateFlow<ProjectLinks> = _links.asStateFlow()

    val yarns: StateFlow<List<Yarn>> =
        repository.observeYarns().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val needles: StateFlow<List<Needle>> =
        repository.observeNeedles().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val swatchesByYarn: StateFlow<Map<Long, List<Swatch>>> = repository.observeYarns()
        .flatMapLatest { all ->
            if (all.isEmpty()) {
                flowOf(emptyMap())
            } else {
                combine(all.map { y -> repository.observeSwatches(y.id).map { y.id to it } }) { it.toMap() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val ownName = MutableStateFlow<String?>(null)

    /** For each needle, the other unfinished projects using it (this project is left out). */
    val needleUsage: StateFlow<Map<Long, List<String>>> =
        combine(repository.observeNeedleUsage(), ownName) { usage, own ->
            if (own == null) usage else usage.mapValues { (_, names) -> names - own }.filterValues { it.isNotEmpty() }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saved: SharedFlow<Unit> = _saved.asSharedFlow()

    init {
        viewModelScope.launch {
            _links.value = repository.observeProjectLinks(projectId).first()
            ownName.value = repository.observeProject(projectId).first()?.project?.name
        }
    }

    /** Adds the yarn to the project, or removes it when already there. The first one added is the main yarn. */
    fun toggleYarn(yarnId: Long) {
        val current = _links.value.yarns
        _links.value = _links.value.copy(
            yarns = if (current.any { it.yarnId == yarnId }) current.filterNot { it.yarnId == yarnId } else current + ProjectYarnLink(yarnId),
        ).normalized()
    }

    fun setPrimaryYarn(yarnId: Long) {
        _links.value = _links.value.copy(yarns = _links.value.yarns.map { it.copy(isPrimary = it.yarnId == yarnId) }).normalized()
    }

    fun setYarnSwatch(yarnId: Long, swatchId: Long?) = updateYarnLink(yarnId) { it.copy(swatchId = swatchId) }

    /** The skeins the pattern asks for; empty or not a positive number clears it. */
    fun setPlannedSkeins(yarnId: Long, text: String) =
        updateYarnLink(yarnId) { it.copy(plannedSkeins = parseDecimal(text)?.takeIf { n -> n > 0.0 }) }

    fun toggleNeedle(needleId: Long) {
        val current = _links.value.needleIds
        _links.value = _links.value.copy(needleIds = if (needleId in current) current - needleId else current + needleId)
    }

    private fun updateYarnLink(yarnId: Long, change: (ProjectYarnLink) -> ProjectYarnLink) {
        _links.value = _links.value.copy(yarns = _links.value.yarns.map { if (it.yarnId == yarnId) change(it) else it })
    }

    fun save() {
        viewModelScope.launch {
            repository.setProjectLinks(projectId, _links.value)
            _saved.emit(Unit)
        }
    }
}
