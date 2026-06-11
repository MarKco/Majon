package com.ilsecondodasinistra.majon.ui.notes

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.ilsecondodasinistra.majon.domain.model.NoteFrequency
import com.ilsecondodasinistra.majon.domain.model.NoteValidationError
import com.ilsecondodasinistra.majon.domain.model.RowNote
import com.ilsecondodasinistra.majon.ui.components.ConfirmDialog
import com.ilsecondodasinistra.majon.ui.components.EmptyState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    onBack: () -> Unit,
    viewModel: NotesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var editorNote by remember { mutableStateOf<RowNote?>(null) }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<RowNote?>(null) }

    val state = uiState as? NotesUiState.Ready ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notes_of, state.partName), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editorNote = null
                    editorOpen = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_note)) },
            )
        },
    ) { padding ->
        if (state.notes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    emoji = "📌",
                    title = stringResource(R.string.notes_empty_title),
                    subtitle = stringResource(R.string.notes_empty_subtitle),
                    modifier = Modifier.padding(32.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.notes, key = { it.id }) { note ->
                    NoteItem(
                        note = note,
                        onClick = {
                            editorNote = note
                            editorOpen = true
                        },
                        onDelete = { noteToDelete = note },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    if (editorOpen) {
        NoteEditorDialog(
            note = editorNote,
            totalRows = state.totalRows,
            viewModel = viewModel,
            onDismiss = { editorOpen = false },
        )
    }

    noteToDelete?.let { note ->
        ConfirmDialog(
            title = stringResource(R.string.delete_note_title),
            message = "“${note.text}”",
            confirmLabel = stringResource(R.string.delete),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = {
                viewModel.deleteNote(note.id)
                noteToDelete = null
            },
            onDismiss = { noteToDelete = null },
        )
    }
}

@Composable
private fun frequencyLabel(frequency: NoteFrequency): String = when (frequency) {
    NoteFrequency.EVERY_ROW -> stringResource(R.string.freq_every)
    NoteFrequency.ODD_ROWS -> stringResource(R.string.freq_odd)
    NoteFrequency.EVEN_ROWS -> stringResource(R.string.freq_even)
}

@Composable
private fun NoteItem(
    note: RowNote,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                val rangeText = if (note.rowStart == note.rowEnd) {
                    stringResource(R.string.note_rows_single, note.rowStart)
                } else {
                    stringResource(R.string.note_rows_range, note.rowStart, note.rowEnd)
                }
                val suffix = if (note.frequency != NoteFrequency.EVERY_ROW) {
                    " · ${frequencyLabel(note.frequency)}"
                } else {
                    ""
                }
                Text(
                    rangeText + suffix,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text(note.text, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NoteEditorDialog(
    note: RowNote?,
    totalRows: Int,
    viewModel: NotesViewModel,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(note?.text.orEmpty()) }
    var startText by rememberSaveable { mutableStateOf(note?.rowStart?.toString().orEmpty()) }
    var endText by rememberSaveable { mutableStateOf(note?.rowEnd?.toString().orEmpty()) }
    var frequency by rememberSaveable { mutableStateOf(note?.frequency ?: NoteFrequency.EVERY_ROW) }
    var error by remember { mutableStateOf<NoteValidationError?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (note == null) R.string.add_note else R.string.edit_note))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        error = null
                    },
                    label = { Text(stringResource(R.string.note_text)) },
                    isError = error == NoteValidationError.TEXT_BLANK,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = startText,
                        onValueChange = {
                            startText = it.filter { c -> c.isDigit() }
                            // keep single-row notes simple: mirror start into empty end
                            error = null
                        },
                        label = { Text(stringResource(R.string.from_row)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = error == NoteValidationError.START_BELOW_ONE,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = endText,
                        onValueChange = {
                            endText = it.filter { c -> c.isDigit() }
                            error = null
                        },
                        label = { Text(stringResource(R.string.to_row)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = error == NoteValidationError.END_BEFORE_START ||
                            error == NoteValidationError.OUT_OF_RANGE,
                        modifier = Modifier.weight(1f),
                    )
                }
                Column {
                    Text(stringResource(R.string.frequency), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        NoteFrequency.entries.forEachIndexed { index, freq ->
                            SegmentedButton(
                                selected = frequency == freq,
                                onClick = { frequency = freq },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = NoteFrequency.entries.size),
                                label = {
                                    Text(
                                        frequencyLabel(freq),
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                    )
                                },
                            )
                        }
                    }
                }
                error?.let {
                    Text(
                        text = when (it) {
                            NoteValidationError.START_BELOW_ONE -> stringResource(R.string.note_error_start)
                            NoteValidationError.END_BEFORE_START -> stringResource(R.string.note_error_end)
                            NoteValidationError.OUT_OF_RANGE -> stringResource(R.string.note_error_range)
                            NoteValidationError.TEXT_BLANK -> stringResource(R.string.note_error_text)
                        },
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val start = startText.toIntOrNull() ?: 0
                    val end = endText.toIntOrNull() ?: start
                    scope.launch {
                        val result = viewModel.saveNote(
                            id = note?.id ?: 0L,
                            rowStart = start,
                            rowEnd = end,
                            frequency = frequency,
                            text = text,
                        )
                        if (result == null) onDismiss() else error = result
                    }
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
