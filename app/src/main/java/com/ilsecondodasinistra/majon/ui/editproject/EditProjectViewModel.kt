package com.ilsecondodasinistra.majon.ui.editproject

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.ProjectLinks
import com.ilsecondodasinistra.majon.domain.model.ProjectYarnLink
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.domain.model.normalized
import com.ilsecondodasinistra.majon.domain.model.parseDecimal
import com.ilsecondodasinistra.majon.domain.model.ProjectColor
import com.ilsecondodasinistra.majon.domain.model.ProjectIcon
import com.ilsecondodasinistra.majon.domain.repository.MajonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class EditProjectForm(
    val name: String = "",
    val icon: ProjectIcon = ProjectIcon.SWEATER,
    val color: ProjectColor = ProjectColor.TERRACOTTA,
    val yarnType: String = "",
    val needleSize: String = "",
    val nameError: Boolean = false,
    val isEditing: Boolean = false,
    /** True when the project has the old free-text yarn / needle notes, which stay editable. */
    val hasLegacyText: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EditProjectViewModel @Inject constructor(
    private val repository: MajonRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val projectId: Long = savedStateHandle["projectId"] ?: 0L

    private val _form = MutableStateFlow(EditProjectForm())
    val form: StateFlow<EditProjectForm> = _form.asStateFlow()

    private val _savedEvent = MutableStateFlow<Unit?>(null)
    val savedEvent: StateFlow<Unit?> = _savedEvent.asStateFlow()

    private var loaded: Project? = null

    // --- what the project is made with, chosen from the stash ---

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

    init {
        if (projectId != 0L) {
            viewModelScope.launch {
                _links.value = repository.observeProjectLinks(projectId).first()
                repository.observeProject(projectId).first()?.let { existing ->
                    loaded = existing.project
                    ownName.value = existing.project.name
                    _form.value = EditProjectForm(
                        name = existing.project.name,
                        icon = existing.project.icon,
                        color = existing.project.color,
                        yarnType = existing.project.yarnType.orEmpty(),
                        needleSize = existing.project.needleSize.orEmpty(),
                        isEditing = true,
                        hasLegacyText = !existing.project.yarnType.isNullOrBlank() || !existing.project.needleSize.isNullOrBlank(),
                    )
                }
            }
        }
    }

    fun updateName(value: String) {
        _form.value = _form.value.copy(name = value, nameError = false)
    }

    fun updateIcon(value: ProjectIcon) {
        _form.value = _form.value.copy(icon = value)
    }

    fun updateColor(value: ProjectColor) {
        _form.value = _form.value.copy(color = value)
    }

    fun updateYarnType(value: String) {
        _form.value = _form.value.copy(yarnType = value)
    }

    fun updateNeedleSize(value: String) {
        _form.value = _form.value.copy(needleSize = value)
    }

    fun save() {
        val form = _form.value
        if (form.name.isBlank()) {
            _form.value = form.copy(nameError = true)
            return
        }
        viewModelScope.launch {
            val base = loaded ?: Project(name = "")
            val savedId = repository.upsertProject(
                base.copy(
                    id = if (projectId != 0L) projectId else 0L,
                    name = form.name.trim(),
                    icon = form.icon,
                    color = form.color,
                    yarnType = form.yarnType.trim().ifBlank { null },
                    needleSize = form.needleSize.trim().ifBlank { null },
                ),
            )
            repository.setProjectLinks(savedId, _links.value)
            _savedEvent.value = Unit
        }
    }
}
