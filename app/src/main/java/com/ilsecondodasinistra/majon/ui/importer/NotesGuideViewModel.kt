package com.ilsecondodasinistra.majon.ui.importer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.repository.MajonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** What the AI guide knows about the project it is for: its name and parts (none for the general guide). */
data class GuideContext(val projectName: String? = null, val parts: List<Part> = emptyList())

@HiltViewModel
class NotesGuideViewModel @Inject constructor(
    repository: MajonRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val projectId: Long = savedStateHandle["projectId"] ?: 0L

    val context: StateFlow<GuideContext> = repository.observeProject(projectId)
        .map { project ->
            if (project == null) GuideContext() else GuideContext(project.project.name, project.parts.sortedBy { it.id })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GuideContext())
}
