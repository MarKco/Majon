package com.ilsecondodasinistra.majon.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.domain.model.ProjectWithParts
import com.ilsecondodasinistra.majon.domain.repository.MajonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(val projects: List<ProjectWithParts>) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MajonRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = repository.observeProjects()
        .map<List<ProjectWithParts>, HomeUiState> { HomeUiState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    private val _duplicated = MutableSharedFlow<Long>(extraBufferCapacity = 1)

    /** The id of the copy once [duplicateProject] has made it. */
    val duplicated: SharedFlow<Long> = _duplicated.asSharedFlow()

    fun duplicateProject(id: Long, copyName: String) {
        viewModelScope.launch { repository.duplicateProject(id, copyName)?.let { _duplicated.emit(it) } }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch { repository.deleteProject(id) }
    }
}
