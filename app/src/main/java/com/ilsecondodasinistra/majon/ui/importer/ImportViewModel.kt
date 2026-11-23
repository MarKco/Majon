package com.ilsecondodasinistra.majon.ui.importer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.data.export.BackupData
import com.ilsecondodasinistra.majon.data.export.DetectedImport
import com.ilsecondodasinistra.majon.data.export.ImportInbox
import com.ilsecondodasinistra.majon.data.export.JsonFormatter
import com.ilsecondodasinistra.majon.data.export.NotesCodec
import com.ilsecondodasinistra.majon.data.export.NotesIssue
import com.ilsecondodasinistra.majon.domain.model.NotesFile
import com.ilsecondodasinistra.majon.domain.model.NotesImportPlan
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.PartChoice
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.RowNote
import com.ilsecondodasinistra.majon.domain.model.applyNotesImport
import com.ilsecondodasinistra.majon.domain.model.planNotesImport
import com.ilsecondodasinistra.majon.domain.model.sameNeedleAs
import com.ilsecondodasinistra.majon.domain.model.sameYarnAs
import com.ilsecondodasinistra.majon.domain.model.suggestChoices
import com.ilsecondodasinistra.majon.domain.repository.MajonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What the text turned out to be. */
enum class ImportKind { NONE, NOTES, PROJECT }

/** What a project file would add to the stash. */
data class StashPreview(val yarnsKnown: Int, val yarnsNew: Int, val needlesKnown: Int, val needlesNew: Int)

sealed interface ImportDone {
    data class Project(val projects: Int, val yarns: Int, val needles: Int) : ImportDone
    data class Notes(val count: Int, val projectId: Long) : ImportDone
}

data class ImportUi(
    /** The text in the box, as typed, pasted, loaded from a file or tidied. */
    val text: String = "",
    val kind: ImportKind = ImportKind.NONE,
    val issues: List<NotesIssue> = emptyList(),
    /** For each of [issues], its line in [text] when it is known (after the text has been tidied). */
    val issueLines: List<Int?> = emptyList(),
    /** Set when the text is a project file, even while only its notes are being taken. */
    val projectFile: BackupData? = null,
    val projectNames: List<String> = emptyList(),
    val stashPreview: StashPreview? = null,
    val hasNotes: Boolean = false,
    val notesOnly: Boolean = false,
    val sourceProject: Int = 0,
    val notesFile: NotesFile? = null,
    val targets: List<Project> = emptyList(),
    val targetId: Long? = null,
    /** The notes are going to the project the person came from: there is nothing to choose. */
    val targetLocked: Boolean = false,
    val targetParts: List<Part> = emptyList(),
    val choices: List<PartChoice> = emptyList(),
    val plan: NotesImportPlan? = null,
    val done: ImportDone? = null,
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val repository: MajonRepository,
    private val codec: NotesCodec,
    private val inbox: ImportInbox,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val lockedProjectId: Long = savedStateHandle["projectId"] ?: 0L

    private val _ui = MutableStateFlow(ImportUi(targetLocked = lockedProjectId != 0L))
    val ui: StateFlow<ImportUi> = _ui

    private var existingNotes: Map<Long, List<RowNote>> = emptyMap()

    init {
        // A file opened from outside the app lands here, whether the screen was open or not
        viewModelScope.launch {
            inbox.pending.collect { text ->
                if (text != null) {
                    inbox.take()
                    analyze(text)
                }
            }
        }
    }

    /** The person is typing or pasting: whatever was worked out before is out of date. */
    fun setText(text: String) {
        _ui.value = ImportUi(text = text, targetLocked = _ui.value.targetLocked)
    }

    fun reset() = setText("")

    /** Tidies the text in the box (when it is JSON) and checks it, so every problem shows with its line. */
    fun formatAndCheck() {
        val formatted = JsonFormatter.format(_ui.value.text)
        analyze(formatted?.text ?: _ui.value.text, formatted?.lines)
    }

    /** Reads [text] (put in the box) and works out what it is. */
    fun analyze(text: String, lines: Map<String, Int>? = null) {
        viewModelScope.launch {
            val base = ImportUi(text = text, targetLocked = lockedProjectId != 0L)
            when (val detected = codec.detect(text)) {
                is DetectedImport.Invalid -> _ui.value = base.copy(
                    issues = detected.issues,
                    issueLines = detected.issues.map { it.lineIn(lines) },
                )
                is DetectedImport.Notes -> {
                    _ui.value = base.copy(kind = ImportKind.NOTES, notesFile = detected.file)
                    loadTargets()
                }
                is DetectedImport.Project -> {
                    val data = detected.data
                    val have = repository.getStash()
                    val yarnsKnown = data.stash.yarns.count { y -> have.yarns.any { it.yarn.sameYarnAs(y.yarn) } }
                    val needlesKnown = data.stash.needles.count { n -> have.needles.any { it.sameNeedleAs(n) } }
                    _ui.value = base.copy(
                        kind = ImportKind.PROJECT,
                        projectFile = data,
                        projectNames = data.projects.map { it.project.name },
                        stashPreview = StashPreview(
                            yarnsKnown, data.stash.yarns.size - yarnsKnown, needlesKnown, data.stash.needles.size - needlesKnown,
                        ),
                        hasNotes = data.projects.any { codec.notesOf(it).parts.isNotEmpty() },
                    )
                }
            }
        }
    }

    /** For a project file: take only its notes (onto a project of the reader's) instead of the whole project. */
    fun useNotesOnly(on: Boolean) {
        val file = _ui.value.projectFile ?: return
        if (!on) {
            _ui.value = _ui.value.copy(kind = ImportKind.PROJECT, notesOnly = false, notesFile = null, plan = null, targetId = null, choices = emptyList())
            return
        }
        selectSourceProject(_ui.value.sourceProject.coerceIn(0, (file.projects.size - 1).coerceAtLeast(0)), notesOnly = true)
    }

    fun selectSourceProject(index: Int, notesOnly: Boolean = true) {
        val file = _ui.value.projectFile ?: return
        val project = file.projects.getOrNull(index) ?: return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                kind = ImportKind.NOTES, notesOnly = notesOnly, sourceProject = index, notesFile = codec.notesOf(project),
                targetId = null, choices = emptyList(), plan = null,
            )
            loadTargets()
        }
    }

    private suspend fun loadTargets() {
        val projects = repository.observeProjects().first().map { it.project }
        _ui.value = _ui.value.copy(targets = projects)
        if (lockedProjectId != 0L) selectTarget(lockedProjectId)
    }

    fun selectTarget(projectId: Long) {
        viewModelScope.launch {
            val file = _ui.value.notesFile ?: return@launch
            val parts = repository.observeProject(projectId).first()?.parts.orEmpty()
            existingNotes = parts.associate { it.id to repository.observeNotes(it.id).first() }
            val choices = suggestChoices(file, parts)
            _ui.value = _ui.value.copy(
                targetId = projectId,
                targetParts = parts,
                choices = choices,
                plan = planNotesImport(file, choices, parts, existingNotes),
            )
        }
    }

    fun setChoice(index: Int, choice: PartChoice) {
        val ui = _ui.value
        val file = ui.notesFile ?: return
        if (index !in ui.choices.indices) return
        val choices = ui.choices.toMutableList().also { it[index] = choice }
        _ui.value = ui.copy(choices = choices, plan = planNotesImport(file, choices, ui.targetParts, existingNotes))
    }

    /** Puts the notes of the plan on the chosen project. Nothing is taken away. */
    fun importNotes() {
        val ui = _ui.value
        val plan = ui.plan ?: return
        val target = ui.targetId ?: return
        if (!plan.canImport) return
        viewModelScope.launch {
            val added = repository.applyNotesImport(target, plan)
            _ui.value = ui.copy(done = ImportDone.Notes(added, target))
        }
    }

    /** Imports the whole project file: its projects, with the yarns and needles they use. */
    fun importProject() {
        val ui = _ui.value
        val data = ui.projectFile ?: return
        viewModelScope.launch {
            val counts = repository.importBackup(data.projects, data.stash)
            _ui.value = ui.copy(done = ImportDone.Project(counts.projects, counts.yarns, counts.needles))
        }
    }

    private fun NotesIssue.lineIn(lines: Map<String, Int>?): Int? {
        if (line != null) return line
        if (lines == null || partIndex == null) return null
        return (note?.let { lines["parts[$partIndex].notes[${it - 1}]"] }) ?: lines["parts[$partIndex]"]
    }
}
