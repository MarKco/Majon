package com.ilsecondodasinistra.majon.ui.stash

import androidx.compose.foundation.layout.imePadding
import com.ilsecondodasinistra.majon.ui.components.KeyboardBackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilsecondodasinistra.majon.R
import com.ilsecondodasinistra.majon.domain.model.NeedleMaterial
import com.ilsecondodasinistra.majon.domain.model.NeedleType
import com.ilsecondodasinistra.majon.ui.components.FormSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeedleEditScreen(
    onBack: () -> Unit,
    viewModel: NeedleEditViewModel = hiltViewModel(),
) {
    KeyboardBackHandler()
    val form by viewModel.form.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.saved.collect { onBack() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (form.isEditing) R.string.needle_edit else R.string.needle_new)) },
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
            FormSection(stringResource(R.string.needle_type)) {
                ChipChoices(
                    options = NeedleType.entries,
                    selected = form.type,
                    label = { stringResource(it.labelRes()) },
                    onSelect = { t -> t?.let { viewModel.update { f -> f.copy(type = it) } } },
                    clearable = false,
                )
            }

            FormSection(null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = form.sizeMm,
                        onValueChange = { v -> viewModel.update { it.copy(sizeMm = v.filter { c -> c.isDigit() || c == '.' || c == ',' }) } },
                        label = { Text(stringResource(R.string.needle_size_mm)) },
                        singleLine = true,
                        isError = form.error,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = form.lengthCm,
                        onValueChange = { v -> viewModel.update { it.copy(lengthCm = v.filter { c -> c.isDigit() }) } },
                        label = { Text(stringResource(R.string.needle_length)) },
                        singleLine = true,
                        isError = form.error,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = form.quantity,
                        onValueChange = { v -> viewModel.update { it.copy(quantity = v.filter { c -> c.isDigit() }) } },
                        label = { Text(stringResource(R.string.needle_quantity)) },
                        singleLine = true,
                        isError = form.error,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    FilledTonalIconButton(
                        onClick = { viewModel.update { f -> f.copy(quantity = ((f.quantity.toIntOrNull() ?: 1) - 1).coerceAtLeast(1).toString()) } },
                    ) { Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.needle_quantity_minus)) }
                    FilledTonalIconButton(
                        onClick = { viewModel.update { f -> f.copy(quantity = ((f.quantity.toIntOrNull() ?: 0) + 1).toString()) } },
                    ) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.needle_quantity_plus)) }
                }
                if (form.error) {
                    Text(stringResource(R.string.needle_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }

            FormSection(stringResource(R.string.needle_material)) {
                ChipChoices(
                    options = NeedleMaterial.entries,
                    selected = form.material,
                    label = { stringResource(it.labelRes()) },
                    onSelect = { m -> viewModel.update { it.copy(material = m) } },
                )
            }

            FormSection(null) {
                OutlinedTextField(
                    value = form.brand,
                    onValueChange = { v -> viewModel.update { it.copy(brand = v) } },
                    label = { Text(stringResource(R.string.needle_brand)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.notes,
                    onValueChange = { v -> viewModel.update { it.copy(notes = v) } },
                    label = { Text(stringResource(R.string.needle_notes)) },
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
