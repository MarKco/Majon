package com.ilsecondodasinistra.majon.ui.projectdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.ProjectWithParts
import com.ilsecondodasinistra.majon.domain.repository.MajonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ProjectDetailUiState {
    data object Loading : ProjectDetailUiState
    data object Deleted : ProjectDetailUiState
    data class Ready(val projectWithParts: ProjectWithParts) : ProjectDetailUiState
}

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val repository: MajonRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val projectId: Long = checkNotNull(savedStateHandle["projectId"])
    private var deleted = false

    val uiState: StateFlow<ProjectDetailUiState> = repository.observeProject(projectId)
        .map { project ->
            when {
                project != null -> ProjectDetailUiState.Ready(project)
                deleted -> ProjectDetailUiState.Deleted
                else -> ProjectDetailUiState.Loading
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProjectDetailUiState.Loading)

    fun addPart(name: String, totalRows: Int): Boolean {
        if (name.isBlank() || totalRows <= 0) return false
        viewModelScope.launch {
            repository.upsertPart(Part(projectId = projectId, name = name.trim(), totalRows = totalRows))
        }
        return true
    }

    fun updatePart(partId: Long, name: String, totalRows: Int): Boolean {
        if (name.isBlank() || totalRows <= 0) return false
        viewModelScope.launch {
            val part = repository.observePart(partId).first() ?: return@launch
            repository.upsertPart(part.copy(name = name.trim(), totalRows = totalRows))
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
