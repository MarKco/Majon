package com.ilsecondodasinistra.majon.ui.projectdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilsecondodasinistra.majon.R
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.ui.components.AnimatedProgressBar
import com.ilsecondodasinistra.majon.ui.components.ConfirmDialog
import com.ilsecondodasinistra.majon.ui.components.EmptyState
import com.ilsecondodasinistra.majon.ui.components.ProjectIconBadge
import com.ilsecondodasinistra.majon.ui.theme.toColor
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    onBack: () -> Unit,
    onPartClick: (Long) -> Unit,
    onPartNotes: (Long) -> Unit,
    onEditProject: (Long) -> Unit,
    viewModel: ProjectDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var partDialog by remember { mutableStateOf<PartDialogState?>(null) }
    var partToDelete by remember { mutableStateOf<Part?>(null) }
    var deleteProjectDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is ProjectDetailUiState.Deleted) onBack()
    }

    val state = uiState as? ProjectDetailUiState.Ready ?: return
    val project = state.projectWithParts.project
    val parts = state.projectWithParts.parts

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project.name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    var menuOpen by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit)) },
                                onClick = {
                                    menuOpen = false
                                    onEditProject(project.id)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete)) },
                                onClick = {
                                    menuOpen = false
                                    deleteProjectDialog = true
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { partDialog = PartDialogState() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_part)) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ProjectHeader(
                    state = state,
                )
            }
            if (parts.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        EmptyState(
                            emoji = "🪡",
                            title = stringResource(R.string.parts_empty_title),
                            subtitle = stringResource(R.string.parts_empty_subtitle),
                        )
                    }
                }
            } else {
                items(parts, key = { it.id }) { part ->
                    PartCard(
                        part = part,
                        accentColor = project.color.toColor(),
                        onClick = { onPartClick(part.id) },
                        onEdit = { partDialog = PartDialogState(part) },
                        onNotes = { onPartNotes(part.id) },
                        onDelete = { partToDelete = part },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    partDialog?.let { dialog ->
        PartEditorDialog(
            state = dialog,
            onDismiss = { partDialog = null },
            onSave = { name, rows ->
                val ok = if (dialog.part == null) {
                    viewModel.addPart(name, rows)
                } else {
                    viewModel.updatePart(dialog.part.id, name, rows)
                }
                if (ok) partDialog = null
                ok
            },
        )
    }

    partToDelete?.let { part ->
        ConfirmDialog(
            title = stringResource(R.string.delete_part_title),
            message = stringResource(R.string.delete_part_message, part.name),
            confirmLabel = stringResource(R.string.delete),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = {
                viewModel.deletePart(part.id)
                partToDelete = null
            },
            onDismiss = { partToDelete = null },
        )
    }

    if (deleteProjectDialog) {
        ConfirmDialog(
            title = stringResource(R.string.delete_project_title),
            message = stringResource(R.string.delete_project_message, project.name),
            confirmLabel = stringResource(R.string.delete),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = {
                deleteProjectDialog = false
                viewModel.deleteProject()
            },
            onDismiss = { deleteProjectDialog = false },
        )
    }
}

@Composable
private fun ProjectHeader(state: ProjectDetailUiState.Ready) {
    val project = state.projectWithParts.project
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = project.color.toColor().copy(alpha = 0.12f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProjectIconBadge(icon = project.icon, color = project.color, size = 64.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.total_progress),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${(state.projectWithParts.progress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        color = project.color.toColor(),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            AnimatedProgressBar(progress = state.projectWithParts.progress, color = project.color.toColor())
            val yarn = project.yarnType
            val needle = project.needleSize
            if (!yarn.isNullOrBlank() || !needle.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!yarn.isNullOrBlank()) {
                        AssistChip(onClick = {}, label = { Text("🧶 $yarn") })
                    }
                    if (!needle.isNullOrBlank()) {
                        AssistChip(onClick = {}, label = { Text("🪡 $needle") })
                    }
                }
            }
        }
    }
}

@Composable
private fun PartCard(
    part: Part,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onNotes: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(part.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (part.isComplete) {
                        Text("✓", style = MaterialTheme.typography.titleMedium, color = accentColor)
                    }
                }
                Spacer(Modifier.height(8.dp))
                AnimatedProgressBar(progress = part.progress, color = accentColor, height = 8.dp)
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.rows_progress, part.completedRows, part.totalRows),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit_part)) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.notes)) },
                        onClick = {
                            menuOpen = false
                            onNotes()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

private data class PartDialogState(val part: Part? = null)

@Composable
private fun PartEditorDialog(
    state: PartDialogState,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Boolean,
) {
    var name by rememberSaveable { mutableStateOf(state.part?.name.orEmpty()) }
    var rowsText by rememberSaveable { mutableStateOf(state.part?.totalRows?.toString().orEmpty()) }
    var showError by rememberSaveable { mutableStateOf(false) }
    val rows = rowsText.toIntOrNull() ?: 0
    val reduceWarning = state.part != null && rows in 1 until state.part.completedRows

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (state.part == null) R.string.add_part else R.string.edit_part))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.part_name)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = rowsText,
                    onValueChange = {
                        rowsText = it.filter { c -> c.isDigit() }
                        showError = false
                    },
                    label = { Text(stringResource(R.string.total_rows)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = showError,
                    supportingText = if (showError) {
                        { Text(stringResource(R.string.rows_invalid)) }
                    } else {
                        null
                    },
                )
                if (reduceWarning) {
                    Text(
                        stringResource(R.string.part_reduce_warning, state.part!!.completedRows),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val ok = onSave(name, rows)
                    if (!ok) showError = true
                },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
