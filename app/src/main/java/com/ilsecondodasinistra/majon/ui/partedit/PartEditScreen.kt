package com.ilsecondodasinistra.majon.ui.partedit

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.layout.imePadding
import com.ilsecondodasinistra.majon.ui.components.KeyboardBackHandler
import com.ilsecondodasinistra.majon.ui.components.SidewaysSwitch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.ilsecondodasinistra.majon.domain.model.MeasureKey
import com.ilsecondodasinistra.majon.domain.model.parseDecimal
import com.ilsecondodasinistra.majon.ui.components.FormSection
import com.ilsecondodasinistra.majon.ui.components.labelRes

/** Adds or edits a part, one section at a time: name and rows, measures, extra rows. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PartEditScreen(
    onBack: () -> Unit,
    viewModel: PartEditViewModel = hiltViewModel(),
) {
    KeyboardBackHandler()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val context by viewModel.context.collectAsStateWithLifecycle()
    val estimate by viewModel.estimate.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.saved.collect { onBack() } }

    val rows = form.rows.toIntOrNull() ?: 0
    // Rows left empty are taken from the measures when they give an estimate
    val rowsFromMeasures = rows <= 0 && estimate.rows != null
    val reduceWarning = form.isEditing && rows in 1 until form.rowsDone

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (form.isEditing) R.string.edit_part else R.string.add_part)) },
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
            if (!form.isEditing && context.presets.isNotEmpty()) {
                FormSection(stringResource(R.string.part_suggested)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        context.presets.forEach { preset ->
                            val label = stringResource(preset.name.labelRes())
                            FilterChip(
                                selected = form.name == label,
                                onClick = { viewModel.applyPreset(preset, label) },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }

            FormSection(null) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = { v -> viewModel.update { it.copy(name = v) } },
                    label = { Text(stringResource(R.string.part_name)) },
                    singleLine = true,
                    isError = form.nameError,
                    supportingText = if (form.nameError) ({ Text(stringResource(R.string.project_name_error)) }) else null,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            FormSection(stringResource(R.string.part_section_rows)) {
                OutlinedTextField(
                    value = form.rows,
                    onValueChange = { v -> viewModel.update { it.copy(rows = v.filter { c -> c.isDigit() }) } },
                    label = { Text(stringResource(R.string.total_rows)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = form.rowsError,
                    supportingText = when {
                        rowsFromMeasures -> ({ Text(stringResource(R.string.rows_from_measures, estimate.rows!!)) })
                        form.rowsError -> ({ Text(stringResource(R.string.rows_invalid)) })
                        else -> null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (reduceWarning) {
                    Text(
                        stringResource(R.string.part_reduce_warning, form.rowsDone),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                val copies = (form.copies.toIntOrNull() ?: 1).coerceIn(1, MAX_COPIES)
                Stepper(
                    label = stringResource(R.string.part_copies),
                    value = copies.toString(),
                    onMinus = { viewModel.update { it.copy(copies = (copies - 1).coerceAtLeast(1).toString()) } },
                    onPlus = { viewModel.update { it.copy(copies = (copies + 1).coerceAtMost(MAX_COPIES).toString()) } },
                    minusDescription = stringResource(R.string.part_copies_minus),
                    plusDescription = stringResource(R.string.part_copies_plus),
                    hint = stringResource(R.string.part_copies_hint),
                )
                if (form.isEditing && copies > 1) {
                    val inHand = form.currentCopy.coerceIn(1, copies)
                    Stepper(
                        label = stringResource(R.string.part_copy_current, inHand, copies),
                        value = inHand.toString(),
                        onMinus = { viewModel.update { it.copy(currentCopy = (inHand - 1).coerceAtLeast(1)) } },
                        onPlus = { viewModel.update { it.copy(currentCopy = (inHand + 1).coerceAtMost(copies)) } },
                        minusDescription = stringResource(R.string.part_copies_minus),
                        plusDescription = stringResource(R.string.part_copies_plus),
                        hint = stringResource(R.string.part_copy_current_hint),
                    )
                }
                OutlinedTextField(
                    value = form.castOn,
                    onValueChange = { v -> viewModel.update { it.copy(castOn = v.filter { c -> c.isDigit() }) } },
                    label = { Text(stringResource(R.string.part_cast_on)) },
                    supportingText = { Text(stringResource(R.string.part_cast_on_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (estimate.stitches != null && estimate.stitches.toString() != form.castOn) {
                    TextButton(onClick = viewModel::useEstimatedCastOn) {
                        Text(stringResource(R.string.part_use_cast_on, estimate.stitches!!))
                    }
                }
            }

            FormSection(stringResource(R.string.part_measures_title)) {
                // The part's own shape, with the two measures to take drawn on it
                val named = context.presets.firstOrNull { preset -> form.name.trim().equals(stringResource(preset.name.labelRes()), ignoreCase = true) }?.name
                val shape = com.ilsecondodasinistra.majon.domain.model.partShape(context.projectIcon, named)
                val sideways = form.sideways
                val stitchesCaption = stringResource(R.string.swatch_stitches_caption)
                val rowsCaption = stringResource(R.string.swatch_rows_caption)
                val stitchLabel = stringResource(form.stitchMeasure.labelRes()).substringBefore(" (") + " → " + stitchesCaption
                val rowLabel = stringResource(form.rowMeasure.labelRes()).substringBefore(" (") + " → " + rowsCaption
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    // The arrows stay where they are (across / along the piece); the labels follow the orientation
                    PartSketch(
                        shape = shape,
                        acrossLabel = if (sideways) rowLabel else stitchLabel,
                        alongLabel = if (sideways) stitchLabel else rowLabel,
                    )
                }
                Text(
                    stringResource(R.string.part_measures_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SidewaysSwitch(checked = sideways, onCheckedChange = viewModel::setSideways)
                MeasureField(
                    keys = if (sideways) listOf(MeasureKey.LENGTH, MeasureKey.HEIGHT) else listOf(MeasureKey.WIDTH, MeasureKey.CIRCUMFERENCE),
                    selected = form.stitchMeasure,
                    value = form.stitchCm,
                    gives = stringResource(R.string.measure_gives_stitches),
                    onKey = { k -> viewModel.update { it.copy(stitchMeasure = k) } },
                    onValue = { v -> viewModel.update { it.copy(stitchCm = v) } },
                )
                OutlinedTextField(
                    value = form.endStitchCm,
                    onValueChange = { v -> viewModel.update { it.copy(endStitchCm = v.filter { c -> c.isDigit() || c == '.' || c == ',' }) } },
                    label = { Text(stringResource(R.string.measure_end)) },
                    supportingText = { Text(stringResource(R.string.measure_end_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                MeasureField(
                    keys = if (sideways) listOf(MeasureKey.WIDTH, MeasureKey.CIRCUMFERENCE) else listOf(MeasureKey.LENGTH, MeasureKey.HEIGHT),
                    selected = form.rowMeasure,
                    value = form.rowCm,
                    gives = stringResource(R.string.measure_gives_rows),
                    onKey = { k -> viewModel.update { it.copy(rowMeasure = k) } },
                    onValue = { v -> viewModel.update { it.copy(rowCm = v) } },
                )
                if (context.swatches.size > 1) {
                    Text(stringResource(R.string.part_swatch), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = form.swatchId == null,
                            onClick = { viewModel.update { it.copy(swatchId = null) } },
                            label = { Text(stringResource(R.string.part_swatch_default)) },
                        )
                        context.swatches.forEach { sw ->
                            FilterChip(
                                selected = form.swatchId == sw.id,
                                onClick = { viewModel.update { it.copy(swatchId = sw.id) } },
                                label = { Text(listOfNotNull(context.yarnNames[sw.yarnId], "${sw.stitches}×${sw.rows}").joinToString(" · ")) },
                            )
                        }
                    }
                }
                val measured = parseDecimal(form.stitchCm) != null || parseDecimal(form.rowCm) != null
                val hasSwatch = context.defaultSwatch != null || form.swatchId != null
                when {
                    estimate.stitches != null && estimate.rows != null -> EstimateText(stringResource(R.string.part_estimate, estimate.stitches!!, estimate.rows!!))
                    estimate.stitches != null -> EstimateText(stringResource(R.string.part_estimate_stitches, estimate.stitches!!))
                    estimate.rows != null -> EstimateText(stringResource(R.string.part_estimate_rows, estimate.rows!!))
                    measured && !hasSwatch -> Text(
                        stringResource(R.string.part_estimate_needs_swatch),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                estimate.endStitches?.let { EstimateText(stringResource(R.string.part_estimate_end, it)) }
                if (estimate.rows != null && estimate.rows != rows) {
                    TextButton(onClick = viewModel::useEstimatedRows) {
                        Text(stringResource(R.string.part_use_rows, estimate.rows!!))
                    }
                }
            }

            FormSection(stringResource(R.string.part_section_extra)) {
                ExtraRowOption(
                    label = stringResource(R.string.prep_row_toggle),
                    checked = form.prepRow,
                    onCheckedChange = { v -> viewModel.update { it.copy(prepRow = v) } },
                    noteLabel = stringResource(R.string.prep_row_note),
                    note = form.prepNote,
                    onNoteChange = { v -> viewModel.update { it.copy(prepNote = v) } },
                )
                ExtraRowOption(
                    label = stringResource(R.string.final_row_toggle),
                    checked = form.finalRow,
                    onCheckedChange = { v -> viewModel.update { it.copy(finalRow = v) } },
                    noteLabel = stringResource(R.string.final_row_note),
                    note = form.finalNote,
                    onNoteChange = { v -> viewModel.update { it.copy(finalNote = v) } },
                )
            }

            Button(onClick = { viewModel.save() }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(R.string.save))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** A number with a minus and a plus button (copies of a part), a line of explanation under it. */
@Composable
private fun Stepper(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusDescription: String,
    plusDescription: String,
    hint: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            FilledTonalIconButton(onClick = onMinus) { Icon(Icons.Default.Remove, contentDescription = minusDescription) }
            Text(value, style = MaterialTheme.typography.titleMedium, modifier = Modifier.widthIn(min = 24.dp), textAlign = TextAlign.Center)
            FilledTonalIconButton(onClick = onPlus) { Icon(Icons.Default.Add, contentDescription = plusDescription) }
        }
        Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EstimateText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
}

/** One measure of a part: the choice of what is measured (a width or a circumference...) and the cm. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MeasureField(
    keys: List<MeasureKey>,
    selected: MeasureKey,
    value: String,
    gives: String,
    onKey: (MeasureKey) -> Unit,
    onValue: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            keys.forEach { key ->
                FilterChip(
                    selected = key == selected,
                    onClick = { onKey(key) },
                    label = { Text(stringResource(key.labelRes()).substringBefore(" (")) },
                )
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = { onValue(it.filter { c -> c.isDigit() || c == '.' || c == ',' }) },
            label = { Text(stringResource(selected.labelRes())) },
            supportingText = { Text(gives) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** A switch for an optional extra row (preparation / final) with its note shown while it is on. */
@Composable
private fun ExtraRowOption(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    noteLabel: String,
    note: String,
    onNoteChange: (String) -> Unit,
) {
    Column {
        Row(
            // The whole row toggles, not only the small switch
            modifier = Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = checked, onCheckedChange = null)
        }
        AnimatedVisibility(visible = checked) {
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                label = { Text(noteLabel) },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
    }
}
