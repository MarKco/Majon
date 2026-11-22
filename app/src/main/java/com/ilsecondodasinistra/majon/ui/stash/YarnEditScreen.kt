package com.ilsecondodasinistra.majon.ui.stash

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilsecondodasinistra.majon.R
import com.ilsecondodasinistra.majon.domain.model.YarnMaterial
import com.ilsecondodasinistra.majon.domain.model.YarnWeight
import com.ilsecondodasinistra.majon.ui.components.FormSection
import com.ilsecondodasinistra.majon.ui.components.rememberLocalImage
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun YarnEditScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: YarnEditViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(viewModel) { viewModel.saved.collect { onSaved() } }
    var showPicker by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    if (showPicker) {
        ColorPickerDialog(
            initialArgb = form.colorArgb,
            onDismiss = { showPicker = false },
            onConfirm = { argb ->
                viewModel.update { it.copy(colorArgb = argb) }
                showPicker = false
            },
        )
    }

    // The picked picture is copied into the app's own storage, so it survives the source going away
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val path = withContext(Dispatchers.IO) {
                runCatching {
                    val file = File(context.filesDir, "yarn_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
                    file.absolutePath
                }.getOrNull()
            }
            if (path != null) {
                form.photoPath?.let { File(it).delete() }
                viewModel.setPhoto(path)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (form.isEditing) R.string.yarn_edit else R.string.yarn_new)) },
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FormSection(stringResource(R.string.yarn_section_yarn)) {
            OutlinedTextField(
                value = form.name,
                onValueChange = { v -> viewModel.update { it.copy(name = v) } },
                label = { Text(stringResource(R.string.yarn_name)) },
                singleLine = true,
                isError = form.nameError,
                supportingText = if (form.nameError) ({ Text(stringResource(R.string.yarn_name_required)) }) else null,
                modifier = Modifier.fillMaxWidth(),
            )
            TextField2(form.brand, R.string.yarn_brand) { v -> viewModel.update { it.copy(brand = v) } }
            TextField2(form.colorName, R.string.yarn_color_name) { v -> viewModel.update { it.copy(colorName = v) } }

            Text(stringResource(R.string.yarn_color), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ColorChoice(argb = null, selected = form.colorArgb == null) { viewModel.update { it.copy(colorArgb = null) } }
                YarnColorChoices.forEach { argb ->
                    ColorChoice(argb = argb, selected = form.colorArgb == argb) { viewModel.update { it.copy(colorArgb = argb) } }
                }
                // Any other colour: a swatch ring that opens the continuous picker; shows the chosen custom colour
                val custom = form.colorArgb?.takeIf { it !in YarnColorChoices }
                CustomColorChoice(custom = custom) { showPicker = true }
            }
            TextField2(form.dyeLot, R.string.yarn_dye_lot) { v -> viewModel.update { it.copy(dyeLot = v) } }

            Text(stringResource(R.string.yarn_weight_category), style = MaterialTheme.typography.labelLarge)
            ChipChoices(
                options = YarnWeight.entries,
                selected = form.weight,
                label = { stringResource(it.labelRes()) },
                onSelect = { w -> viewModel.update { it.copy(weight = w) } },
            )
            }

            FormSection(stringResource(R.string.yarn_composition)) {
            MaterialsEditor(
                rows = form.materials,
                error = form.materialsError,
                onChange = viewModel::setMaterials,
            )
            }

            FormSection(stringResource(R.string.yarn_section_skein)) {
                Text(stringResource(R.string.yarn_skein_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(form.skeinWeightG, R.string.yarn_skein_weight, decimal = false, modifier = Modifier.weight(1f)) { v -> viewModel.update { it.copy(skeinWeightG = v) } }
                NumberField(form.skeinLengthM, R.string.yarn_skein_length, decimal = false, modifier = Modifier.weight(1f)) { v -> viewModel.update { it.copy(skeinLengthM = v) } }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(form.stockSkeins, R.string.yarn_stock, decimal = true, hint = R.string.yarn_stock_hint, modifier = Modifier.weight(1f)) { v -> viewModel.update { it.copy(stockSkeins = v) } }
                androidx.compose.material3.FilledTonalButton(onClick = { viewModel.changeStock(-0.25) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)) {
                    Text(stringResource(R.string.stock_minus_label))
                }
                androidx.compose.material3.FilledTonalButton(onClick = { viewModel.changeStock(+0.25) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)) {
                    Text(stringResource(R.string.stock_plus_label))
                }
            }
            }

            FormSection(stringResource(R.string.yarn_section_buy)) {
            NumberField(form.pricePerSkein, R.string.yarn_price, decimal = true) { v -> viewModel.update { it.copy(pricePerSkein = v) } }
            TextField2(form.shop, R.string.yarn_shop) { v -> viewModel.update { it.copy(shop = v) } }
            TextField2(form.care, R.string.yarn_care) { v -> viewModel.update { it.copy(care = v) } }
            TextField2(form.notes, R.string.yarn_notes, singleLine = false) { v -> viewModel.update { it.copy(notes = v) } }

            PhotoPicker(
                path = form.photoPath,
                onPick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onRemove = {
                    form.photoPath?.let { File(it).delete() }
                    viewModel.setPhoto(null)
                },
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
private fun TextField2(value: String, label: Int, singleLine: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NumberField(
    value: String,
    label: Int,
    decimal: Boolean,
    modifier: Modifier = Modifier,
    hint: Int? = null,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() || (decimal && (c == '.' || c == ',')) }) },
        label = { Text(stringResource(label)) },
        supportingText = hint?.let { { Text(stringResource(it)) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = if (modifier == Modifier) Modifier.fillMaxWidth() else modifier,
    )
}

@Composable
private fun ColorChoice(argb: Int?, selected: Boolean, onClick: () -> Unit) {
    val ring = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Box(
        Modifier.size(44.dp).border(3.dp, ring, CircleShape).padding(4.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ColorDot(argb, size = 34.dp, flat = true)
    }
}

/** The "other colour" choice: a rainbow ring, with the custom colour inside once one is picked. */
@Composable
private fun CustomColorChoice(custom: Int?, onClick: () -> Unit) {
    val ring = if (custom != null) MaterialTheme.colorScheme.primary else Color.Transparent
    Box(
        Modifier.size(44.dp).border(3.dp, ring, CircleShape).padding(4.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(androidx.compose.ui.graphics.Brush.sweepGradient((0..6).map { Color(com.ilsecondodasinistra.majon.domain.model.hsvToArgb(it * 60f, 0.9f, 1f)) })),
            contentAlignment = Alignment.Center,
        ) {
            if (custom != null) Box(Modifier.size(18.dp).clip(CircleShape).background(Color(custom)).border(1.dp, Color.White, CircleShape))
            else Text(stringResource(R.string.yarn_color_other).take(1), style = MaterialTheme.typography.labelSmall, color = Color.White)
        }
    }
}

@Composable
private fun MaterialsEditor(rows: List<MaterialRow>, error: Boolean, onChange: (List<MaterialRow>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEachIndexed { index, row ->
            var menu by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(row.material.labelRes()), maxLines = 1)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        YarnMaterial.entries.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(stringResource(m.labelRes())) },
                                onClick = {
                                    menu = false
                                    onChange(rows.toMutableList().also { it[index] = row.copy(material = m) })
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = row.percent,
                    onValueChange = { v -> onChange(rows.toMutableList().also { it[index] = row.copy(percent = v.filter { c -> c.isDigit() }.take(3)) }) },
                    label = { Text("%") },
                    singleLine = true,
                    isError = error,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(88.dp),
                )
                IconButton(onClick = { onChange(rows.toMutableList().also { it.removeAt(index) }) }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.yarn_remove_material))
                }
            }
            if (row.material == YarnMaterial.OTHER) {
                OutlinedTextField(
                    value = row.customName,
                    onValueChange = { v -> onChange(rows.toMutableList().also { it[index] = row.copy(customName = v) }) },
                    label = { Text(stringResource(R.string.yarn_material_custom)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (error) Text(stringResource(R.string.yarn_materials_over), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { onChange(rows + MaterialRow(if (rows.isEmpty()) YarnMaterial.WOOL else YarnMaterial.COTTON)) }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.yarn_add_material))
        }
    }
}

@Composable
private fun PhotoPicker(path: String?, onPick: () -> Unit, onRemove: () -> Unit) {
    val image = rememberLocalImage(path, maxPx = 600)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = stringResource(R.string.yarn_photo_desc),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(180.dp).clip(MaterialTheme.shapes.large),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPick) { Text(stringResource(if (path == null) R.string.yarn_photo_add else R.string.yarn_photo_change)) }
            if (path != null) TextButton(onClick = onRemove) { Text(stringResource(R.string.yarn_photo_remove)) }
        }
    }
}
