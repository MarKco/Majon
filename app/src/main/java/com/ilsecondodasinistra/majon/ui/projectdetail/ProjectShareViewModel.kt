package com.ilsecondodasinistra.majon.ui.projectdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.ilsecondodasinistra.majon.data.export.BackupCodec
import com.ilsecondodasinistra.majon.domain.repository.MajonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** A project ready to give away: the name of the file and what is in it. */
data class SharedProject(val fileName: String, val content: String)

@HiltViewModel
class ProjectShareViewModel @Inject constructor(
    private val repository: MajonRepository,
    private val codec: BackupCodec,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val projectId: Long = checkNotNull(savedStateHandle["projectId"])

    /**
     * The project as a `.majon` file. With [includeProgress] false it is a model to start from scratch.
     * What is private to the owner (stock, prices, shops...) is never in it.
     */
    suspend fun build(includeProgress: Boolean): SharedProject {
        val project = repository.getFullProjects().first { it.project.id == projectId }
        val stash = repository.getStash()
        return SharedProject(
            fileName = fileNameFor(project.project.name),
            content = codec.encodeForSharing(project, stash, includeProgress, exportedAt = System.currentTimeMillis()),
        )
    }

    private fun fileNameFor(projectName: String): String {
        val safe = projectName.replace(Regex("[^\\p{L}\\p{N} _-]"), "_").trim().trim('_').trim()
        return (safe.ifEmpty { "progetto" }) + ".majon"
    }
}
