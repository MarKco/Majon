package com.ilsecondodasinistra.majon.ui.stash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilsecondodasinistra.majon.R
import com.ilsecondodasinistra.majon.domain.model.StitchPattern
import com.ilsecondodasinistra.majon.ui.components.FormSection
import com.ilsecondodasinistra.majon.ui.components.GaugeSwatchInput

/** Adds or edits a gauge swatch: the 10 x 10 cm square is the form, as on a yarn label. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwatchEditScreen(
    onBack: () -> Unit,
    viewModel: SwatchEditViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.saved.collect { onBack() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (form.isEditing) R.string.swatch_edit else R.string.swatch_new)) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.swatch_tap_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            GaugeSwatchInput(
                stitches = form.stitches,
                rows = form.rows,
                needleMm = form.needleMm,
                onStitches = { v -> viewModel.update { it.copy(stitches = v) } },
                onRows = { v -> viewModel.update { it.copy(rows = v) } },
                onNeedleMm = { v -> viewModel.update { it.copy(needleMm = v) } },
                modifier = Modifier.fillMaxWidth(),
                squareSize = 220.dp,
                error = form.error,
            )
            if (form.error) {
                Text(
                    stringResource(R.string.swatch_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            FormSection(stringResource(R.string.swatch_pattern)) {
                ChipChoices(
                    options = StitchPattern.entries,
                    selected = form.pattern,
                    label = { stringResource(it.labelRes()) },
                    onSelect = { p -> p?.let { viewModel.update { f -> f.copy(pattern = it) } } },
                    clearable = false,
                )
            }

            FormSection(null) {
                OutlinedTextField(
                    value = form.weightG,
                    onValueChange = { v -> viewModel.update { it.copy(weightG = v.filter { c -> c.isDigit() || c == '.' || c == ',' }) } },
                    label = { Text(stringResource(R.string.swatch_weight)) },
                    supportingText = { Text(stringResource(R.string.swatch_weight_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.notes,
                    onValueChange = { v -> viewModel.update { it.copy(notes = v) } },
                    label = { Text(stringResource(R.string.swatch_notes)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Button(onClick = { viewModel.save() }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(R.string.save))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
