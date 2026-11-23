package com.ilsecondodasinistra.majon.ui.notes

import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilsecondodasinistra.majon.R
import com.ilsecondodasinistra.majon.domain.model.NoteFrequency
import com.ilsecondodasinistra.majon.domain.model.NoteValidationError
import com.ilsecondodasinistra.majon.ui.components.FormSection
import com.ilsecondodasinistra.majon.ui.components.KeyboardBackHandler

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditScreen(
    onBack: () -> Unit,
    viewModel: NoteEditViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.saved.collect { onBack() } }
    KeyboardBackHandler()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (form.isEditing) R.string.edit_note else R.string.add_note)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
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
            FormSection(null) {
                OutlinedTextField(
                    value = form.text,
                    onValueChange = { v -> viewModel.update { it.copy(text = v) } },
                    label = { Text(stringResource(R.string.note_text)) },
                    isError = form.error == NoteValidationError.TEXT_BLANK,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            FormSection(stringResource(R.string.note_scope_title)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        NoteScope.RANGE to R.string.note_scope_range,
                        NoteScope.ROWS to R.string.note_scope_rows,
                        NoteScope.EVERY to R.string.note_scope_every,
                    ).forEach { (scope, label) ->
                        FilterChip(
                            selected = form.scope == scope,
                            onClick = { viewModel.update { it.copy(scope = scope) } },
                            label = { Text(stringResource(label)) },
                        )
                    }
                }
                when (form.scope) {
                    NoteScope.RANGE -> RangeFields(form, viewModel)
                    NoteScope.ROWS -> OutlinedTextField(
                        value = form.rowsText,
                        onValueChange = { v -> viewModel.update { it.copy(rowsText = v.filter { c -> c.isDigit() || c in " ,;-\u2013" }) } },
                        label = { Text(stringResource(R.string.note_rows_field)) },
                        supportingText = { Text(stringResource(R.string.note_rows_hint)) },
                        isError = form.error == NoteValidationError.ROWS_EMPTY || form.error == NoteValidationError.ROWS_INVALID ||
                            form.error == NoteValidationError.OUT_OF_RANGE || form.error == NoteValidationError.START_BELOW_ONE,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    NoteScope.EVERY -> EveryFields(form, viewModel)
                }
                if (form.totalRows > 0) {
                    Text(
                        "1–${form.totalRows}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            form.error?.let {
                Text(
                    text = when (it) {
                        NoteValidationError.START_BELOW_ONE -> stringResource(R.string.note_error_start)
                        NoteValidationError.END_BEFORE_START -> stringResource(R.string.note_error_end)
                        NoteValidationError.OUT_OF_RANGE -> stringResource(R.string.note_error_range)
                        NoteValidationError.TEXT_BLANK -> stringResource(R.string.note_error_text)
                        NoteValidationError.ROWS_EMPTY -> stringResource(R.string.note_error_rows_empty)
                        NoteValidationError.ROWS_INVALID -> stringResource(R.string.note_error_rows_invalid)
                        NoteValidationError.STEP_INVALID -> stringResource(R.string.note_error_step)
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(onClick = { viewModel.save() }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(R.string.save))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RangeFields(form: NoteForm, viewModel: NoteEditViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = form.rowStart,
            onValueChange = { v -> viewModel.update { it.copy(rowStart = v.filter { c -> c.isDigit() }) } },
            label = { Text(stringResource(R.string.from_row)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = form.error == NoteValidationError.START_BELOW_ONE,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = form.rowEnd,
            onValueChange = { v -> viewModel.update { it.copy(rowEnd = v.filter { c -> c.isDigit() }) } },
            label = { Text(stringResource(R.string.to_row)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = form.error == NoteValidationError.END_BEFORE_START || form.error == NoteValidationError.OUT_OF_RANGE,
            modifier = Modifier.weight(1f),
        )
    }
    Column(Modifier.selectableGroup()) {
        listOf(NoteFrequency.EVERY_ROW, NoteFrequency.ODD_ROWS, NoteFrequency.EVEN_ROWS).forEach { freq ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = form.frequency == freq,
                        onClick = { viewModel.update { it.copy(frequency = freq) } },
                        role = Role.RadioButton,
                    )
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = form.frequency == freq, onClick = null)
                Spacer(Modifier.width(12.dp))
                Text(frequencyLabel(freq), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun EveryFields(form: NoteForm, viewModel: NoteEditViewModel) {
    OutlinedTextField(
        value = form.step,
        onValueChange = { v -> viewModel.update { it.copy(step = v.filter { c -> c.isDigit() }) } },
        label = { Text(stringResource(R.string.note_every_field)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = form.error == NoteValidationError.STEP_INVALID,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = form.rowStart,
            onValueChange = { v -> viewModel.update { it.copy(rowStart = v.filter { c -> c.isDigit() }) } },
            label = { Text(stringResource(R.string.from_row)) },
            supportingText = { Text(stringResource(R.string.note_every_from_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = form.error == NoteValidationError.START_BELOW_ONE,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = form.rowEnd,
            onValueChange = { v -> viewModel.update { it.copy(rowEnd = v.filter { c -> c.isDigit() }) } },
            label = { Text(stringResource(R.string.to_row)) },
            supportingText = { Text(stringResource(R.string.note_every_to_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = form.error == NoteValidationError.END_BEFORE_START || form.error == NoteValidationError.OUT_OF_RANGE,
            modifier = Modifier.weight(1f),
        )
    }
}
