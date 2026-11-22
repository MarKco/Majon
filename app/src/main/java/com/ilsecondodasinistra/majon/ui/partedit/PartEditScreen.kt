package com.ilsecondodasinistra.majon.ui.partedit

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
            }

            FormSection(stringResource(R.string.part_measures_title)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    MeasureSketch()
                    Text(
                        stringResource(R.string.part_measures_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
                MeasureField(
                    keys = listOf(MeasureKey.WIDTH, MeasureKey.CIRCUMFERENCE),
                    selected = form.stitchMeasure,
                    value = form.stitchCm,
                    gives = stringResource(R.string.measure_gives_stitches),
                    onKey = { k -> viewModel.update { it.copy(stitchMeasure = k) } },
                    onValue = { v -> viewModel.update { it.copy(stitchCm = v) } },
                )
                MeasureField(
                    keys = listOf(MeasureKey.LENGTH, MeasureKey.HEIGHT),
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

/** A small sketch of a piece with an arrow across (the stitches) and one down (the rows). */
@Composable
private fun MeasureSketch() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.size(104.dp)) {
            val left = 26.dp.toPx()
            val top = 24.dp.toPx()
            val w = size.width - left - 6.dp.toPx()
            val h = size.height - top - 6.dp.toPx()
            drawRoundRect(
                color = color.copy(alpha = 0.25f),
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(w, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
            )
            val stroke = 2.dp.toPx()
            val head = 5.dp.toPx()
            // across: the stitches
            val ay = top - 10.dp.toPx()
            drawLine(accent, androidx.compose.ui.geometry.Offset(left, ay), androidx.compose.ui.geometry.Offset(left + w, ay), stroke)
            drawLine(accent, androidx.compose.ui.geometry.Offset(left, ay), androidx.compose.ui.geometry.Offset(left + head, ay - head), stroke)
            drawLine(accent, androidx.compose.ui.geometry.Offset(left, ay), androidx.compose.ui.geometry.Offset(left + head, ay + head), stroke)
            drawLine(accent, androidx.compose.ui.geometry.Offset(left + w, ay), androidx.compose.ui.geometry.Offset(left + w - head, ay - head), stroke)
            drawLine(accent, androidx.compose.ui.geometry.Offset(left + w, ay), androidx.compose.ui.geometry.Offset(left + w - head, ay + head), stroke)
            // down: the rows
            val ax = left - 10.dp.toPx()
            drawLine(accent, androidx.compose.ui.geometry.Offset(ax, top), androidx.compose.ui.geometry.Offset(ax, top + h), stroke)
            drawLine(accent, androidx.compose.ui.geometry.Offset(ax, top), androidx.compose.ui.geometry.Offset(ax - head, top + head), stroke)
            drawLine(accent, androidx.compose.ui.geometry.Offset(ax, top), androidx.compose.ui.geometry.Offset(ax + head, top + head), stroke)
            drawLine(accent, androidx.compose.ui.geometry.Offset(ax, top + h), androidx.compose.ui.geometry.Offset(ax - head, top + h - head), stroke)
            drawLine(accent, androidx.compose.ui.geometry.Offset(ax, top + h), androidx.compose.ui.geometry.Offset(ax + head, top + h - head), stroke)
        }
        Text(stringResource(R.string.swatch_stitches_caption), style = MaterialTheme.typography.labelSmall, color = accent, modifier = Modifier.align(Alignment.TopCenter).padding(start = 22.dp))
        Text(stringResource(R.string.swatch_rows_caption), style = MaterialTheme.typography.labelSmall, color = accent, modifier = Modifier.align(Alignment.Center).padding(start = 22.dp, top = 22.dp))
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
