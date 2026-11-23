package com.ilsecondodasinistra.majon.ui.importer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilsecondodasinistra.majon.R
import com.ilsecondodasinistra.majon.data.export.NotesIssue
import com.ilsecondodasinistra.majon.data.export.NotesIssueKind
import com.ilsecondodasinistra.majon.domain.model.NotesFilePart
import com.ilsecondodasinistra.majon.domain.model.PartChoice
import com.ilsecondodasinistra.majon.domain.model.PlanIssue
import com.ilsecondodasinistra.majon.domain.model.PlanIssueKind
import com.ilsecondodasinistra.majon.ui.components.FormSection
import com.ilsecondodasinistra.majon.ui.components.KeyboardBackHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Importing a `.majon` (a project) or `.majon-notes` (notes) file, from a file or pasted text: the text is checked
 * (and can be tidied) before anything is added, and what it would do is shown first.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    onOpenProject: (Long) -> Unit,
    onOpenGuide: () -> Unit,
    viewModel: ImportViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val fileError = stringResource(R.string.import_file_error)
    val errorsCopied = stringResource(R.string.import_issues_copied)
    KeyboardBackHandler()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) { ImportFiles.readText(context, uri) }
            if (text == null) snackbar.showSnackbar(fileError) else viewModel.analyze(text)
        }
    }

    val issueTexts = ui.issues.mapIndexed { i, issue ->
        val line = ui.issueLines.getOrNull(i)
        (if (line != null) stringResource(R.string.import_issue_line, line) + " — " else "") + issueText(issue)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val done = ui.done
            if (done != null) {
                DoneCard(done, onOpenProject = onOpenProject, onAgain = viewModel::reset, onBack = onBack)
                return@Column
            }

            FormSection(null) {
                Text(stringResource(R.string.import_intro), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { picker.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Default.FileOpen, contentDescription = null)
                        Spacer(Modifier.padding(start = 8.dp))
                        Text(stringResource(R.string.import_choose_file))
                    }
                    OutlinedButton(
                        onClick = { clipboard.getText()?.text?.takeIf { it.isNotBlank() }?.let(viewModel::analyze) },
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null)
                        Spacer(Modifier.padding(start = 8.dp))
                        Text(stringResource(R.string.import_paste_button))
                    }
                }
                OutlinedTextField(
                    value = ui.text,
                    onValueChange = viewModel::setText,
                    placeholder = { Text(stringResource(R.string.import_text_hint)) },
                    minLines = 6,
                    maxLines = 14,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 17.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = viewModel::formatAndCheck, enabled = ui.text.isNotBlank()) {
                        Text(stringResource(R.string.import_format_button))
                    }
                    TextButton(onClick = onOpenGuide) { Text(stringResource(R.string.ai_guide_title)) }
                }
                Text(
                    stringResource(R.string.import_format_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (ui.issues.isNotEmpty()) {
                FormSection(stringResource(R.string.import_issues_title)) {
                    issueTexts.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(issueTexts.joinToString("\n")))
                            scope.launch { snackbar.showSnackbar(errorsCopied) }
                        },
                    ) { Text(stringResource(R.string.import_copy_issues)) }
                    Text(
                        stringResource(R.string.import_copy_issues_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when (ui.kind) {
                ImportKind.PROJECT -> ProjectPreview(ui, viewModel)
                ImportKind.NOTES -> NotesPreview(ui, viewModel)
                ImportKind.NONE -> if (ui.text.isBlank()) {
                    Text(
                        stringResource(R.string.import_nothing_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProjectPreview(ui: ImportUi, viewModel: ImportViewModel) {
    val data = ui.projectFile ?: return
    FormSection(stringResource(R.string.import_project_title)) {
        data.projects.forEach { full ->
            Column {
                Text(full.project.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.import_project_counts, full.parts.size, full.parts.sumOf { it.notes.size }),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    ui.stashPreview?.takeIf { it.yarnsKnown + it.yarnsNew + it.needlesKnown + it.needlesNew > 0 }?.let { stash ->
        FormSection(stringResource(R.string.import_stash_title)) {
            if (stash.yarnsKnown + stash.yarnsNew > 0) {
                Text(stringResource(R.string.import_stash_yarns, stash.yarnsKnown, stash.yarnsNew), style = MaterialTheme.typography.bodyMedium)
            }
            if (stash.needlesKnown + stash.needlesNew > 0) {
                Text(stringResource(R.string.import_stash_needles, stash.needlesKnown, stash.needlesNew), style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                stringResource(R.string.import_stash_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Button(onClick = viewModel::importProject, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Text(stringResource(R.string.import_as_project))
    }
    if (ui.hasNotes) {
        OutlinedButton(onClick = { viewModel.useNotesOnly(true) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.import_only_notes))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotesPreview(ui: ImportUi, viewModel: ImportViewModel) {
    val file = ui.notesFile ?: return

    if (ui.notesOnly) {
        val names = ui.projectNames
        if (names.size > 1) {
            FormSection(stringResource(R.string.import_source_project)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    names.forEachIndexed { i, name ->
                        FilterChip(selected = ui.sourceProject == i, onClick = { viewModel.selectSourceProject(i) }, label = { Text(name) })
                    }
                }
            }
        }
    }

    if (!ui.targetLocked) {
        FormSection(stringResource(R.string.import_target_title)) {
            if (ui.targets.isEmpty()) {
                Text(stringResource(R.string.import_target_none), style = MaterialTheme.typography.bodyMedium)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ui.targets.forEach { project ->
                        FilterChip(
                            selected = ui.targetId == project.id,
                            onClick = { viewModel.selectTarget(project.id) },
                            label = { Text(project.name) },
                        )
                    }
                }
            }
        }
    }

    val plan = ui.plan
    if (ui.targetId != null && plan != null) {
        FormSection(stringResource(R.string.import_parts_title)) {
            file.parts.forEachIndexed { index, filePart ->
                PartMapping(
                    filePart = filePart,
                    choice = ui.choices.getOrNull(index) ?: PartChoice.Skip,
                    targetParts = ui.targetParts.map { it.id to it.name },
                    onChoice = { viewModel.setChoice(index, it) },
                )
            }
        }

        Text(stringResource(R.string.import_summary, plan.noteCount), style = MaterialTheme.typography.titleMedium)
        if (plan.duplicateCount > 0) {
            Text(
                stringResource(R.string.import_summary_duplicates, plan.duplicateCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        plan.errors.forEach { Text(planIssueText(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
        if (plan.warnings.isNotEmpty()) {
            FormSection(stringResource(R.string.import_warnings_title)) {
                plan.warnings.forEach { Text(planIssueText(it), style = MaterialTheme.typography.bodyMedium) }
            }
        }
        Button(
            onClick = viewModel::importNotes,
            enabled = plan.canImport,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(stringResource(R.string.import_notes_button, plan.noteCount)) }
    }
    if (ui.notesOnly) {
        TextButton(onClick = { viewModel.useNotesOnly(false) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.import_back_to_project))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PartMapping(
    filePart: NotesFilePart,
    choice: PartChoice,
    targetParts: List<Pair<Long, String>>,
    onChoice: (PartChoice) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Text(filePart.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.import_part_notes, filePart.notes.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            targetParts.forEach { (id, name) ->
                FilterChip(selected = choice == PartChoice.Existing(id), onClick = { onChoice(PartChoice.Existing(id)) }, label = { Text(name) })
            }
            FilterChip(selected = choice == PartChoice.Create, onClick = { onChoice(PartChoice.Create) }, label = { Text(stringResource(R.string.import_choice_create)) })
            FilterChip(selected = choice == PartChoice.Skip, onClick = { onChoice(PartChoice.Skip) }, label = { Text(stringResource(R.string.import_choice_skip)) })
        }
    }
}

@Composable
private fun DoneCard(done: ImportDone, onOpenProject: (Long) -> Unit, onAgain: () -> Unit, onBack: () -> Unit) {
    FormSection(stringResource(R.string.import_done_title)) {
        when (done) {
            is ImportDone.Notes -> {
                Text(stringResource(R.string.import_done_notes, done.count), style = MaterialTheme.typography.bodyLarge)
                Button(onClick = { onOpenProject(done.projectId) }) { Text(stringResource(R.string.import_open_project)) }
            }
            is ImportDone.Project -> {
                Text(stringResource(R.string.import_done_project, done.projects, done.yarns, done.needles), style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onBack) { Text(stringResource(R.string.back)) }
            }
        }
        TextButton(onClick = onAgain) { Text(stringResource(R.string.import_again)) }
    }
}

@Composable
private fun issueText(issue: NotesIssue): String {
    val part = issue.part.orEmpty()
    val note = issue.note ?: 0
    return when (issue.kind) {
        NotesIssueKind.SYNTAX -> stringResource(R.string.import_issue_syntax)
        NotesIssueKind.NOT_A_NOTES_FILE -> stringResource(R.string.import_issue_not_notes)
        NotesIssueKind.NOT_A_MAJON_FILE -> stringResource(R.string.import_issue_not_majon)
        NotesIssueKind.FUTURE_VERSION -> stringResource(R.string.import_issue_future)
        NotesIssueKind.NO_NOTES -> stringResource(R.string.import_issue_no_notes)
        NotesIssueKind.PART_WITHOUT_NAME -> stringResource(R.string.import_issue_part_no_name)
        NotesIssueKind.TEXT_BLANK -> stringResource(R.string.import_issue_text_blank, part, note)
        NotesIssueKind.MISSING_ROW -> stringResource(R.string.import_issue_missing_row, part, note)
        NotesIssueKind.ROW_BELOW_ONE -> stringResource(R.string.import_issue_row_below_one, part, note)
        NotesIssueKind.END_BEFORE_START -> stringResource(R.string.import_issue_end_before_start, part, note)
        NotesIssueKind.STEP_INVALID -> stringResource(R.string.import_issue_step, part, note)
        NotesIssueKind.ROWS_EMPTY -> stringResource(R.string.import_issue_rows_empty, part, note)
        NotesIssueKind.UNKNOWN_FREQUENCY -> stringResource(R.string.import_issue_frequency, part, note, issue.detail.orEmpty())
    }
}

@Composable
private fun planIssueText(issue: PlanIssue): String = when (issue.kind) {
    PlanIssueKind.OPEN_END_UNKNOWN -> stringResource(R.string.import_error_open_end, issue.partName, issue.note ?: 0)
    PlanIssueKind.BEYOND_PART -> stringResource(R.string.import_warning_beyond, issue.partName, issue.note ?: 0, issue.detail.orEmpty())
    PlanIssueKind.PREP_ROW_MISSING -> stringResource(R.string.import_warning_prep, issue.partName)
    PlanIssueKind.FINAL_ROW_MISSING -> stringResource(R.string.import_warning_final, issue.partName)
}
