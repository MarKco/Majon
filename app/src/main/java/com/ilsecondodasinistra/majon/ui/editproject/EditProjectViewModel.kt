package com.ilsecondodasinistra.majon.ui.editproject

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.ProjectColor
import com.ilsecondodasinistra.majon.domain.model.ProjectIcon
import com.ilsecondodasinistra.majon.domain.repository.MajonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
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
)

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

    init {
        if (projectId != 0L) {
            viewModelScope.launch {
                repository.observeProject(projectId).first()?.let { existing ->
                    loaded = existing.project
                    _form.value = EditProjectForm(
                        name = existing.project.name,
                        icon = existing.project.icon,
                        color = existing.project.color,
                        yarnType = existing.project.yarnType.orEmpty(),
                        needleSize = existing.project.needleSize.orEmpty(),
                        isEditing = true,
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
            repository.upsertProject(
                base.copy(
                    id = if (projectId != 0L) projectId else 0L,
                    name = form.name.trim(),
                    icon = form.icon,
                    color = form.color,
                    yarnType = form.yarnType.trim().ifBlank { null },
                    needleSize = form.needleSize.trim().ifBlank { null },
                ),
            )
            _savedEvent.value = Unit
        }
    }
}
