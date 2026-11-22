package com.ilsecondodasinistra.majon.ui.stash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilsecondodasinistra.majon.R
import kotlin.math.roundToInt
import com.ilsecondodasinistra.majon.domain.model.StitchPattern
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.domain.model.formatDecimalInput
import com.ilsecondodasinistra.majon.domain.model.formatMillimeters
import com.ilsecondodasinistra.majon.domain.model.parseDecimal
import com.ilsecondodasinistra.majon.ui.components.ConfirmDialog
import com.ilsecondodasinistra.majon.ui.components.GaugeSwatchDiagram
import com.ilsecondodasinistra.majon.ui.components.rememberLocalImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YarnDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    /** Opens the swatch editor: a swatch id to edit it, null for a new swatch. */
    onEditSwatch: (yarnId: Long, swatchId: Long?) -> Unit,
    viewModel: YarnDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var useDialog by remember { mutableStateOf(false) }
    var swatchToDelete by remember { mutableStateOf<Swatch?>(null) }

    LaunchedEffect(state) {
        if (state is YarnDetailUiState.Deleted) onBack()
    }
    val ready = state as? YarnDetailUiState.Ready

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ready?.yarn?.name.orEmpty(), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (ready != null) {
                        Box {
                            IconButton(onClick = { menu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
                            }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.edit)) },
                                    onClick = {
                                        menu = false
                                        onEdit(ready.yarn.id)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete)) },
                                    onClick = {
                                        menu = false
                                        confirmDelete = true
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (ready == null) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        val yarn = ready.yarn
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            YarnHeader(yarn)
            StockCard(
                yarn = yarn,
                onMinus = { viewModel.adjustStock(-0.25) },
                onPlus = { viewModel.adjustStock(+0.25) },
                onUse = { useDialog = true },
            )
            if (yarn.materials.isNotEmpty() || yarn.weight != null) {
                InfoCard(stringResource(R.string.yarn_composition)) {
                    if (yarn.materials.isNotEmpty()) Text(compositionText(yarn), style = MaterialTheme.typography.bodyLarge)
                    yarn.weight?.let { Text(stringResource(it.labelRes()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            val details = listOfNotNull(
                yarn.skeinWeightG?.let { "$it g" },
                yarn.skeinLengthM?.let { "$it m" },
                yarn.pricePerSkein?.let { formatDecimalInput(it) + " €" },
            ).joinToString(" · ")
            if (details.isNotEmpty() || listOf(yarn.shop, yarn.care, yarn.notes).any { !it.isNullOrBlank() }) {
                InfoCard(stringResource(R.string.yarn_details)) {
                    if (details.isNotEmpty()) Text(details, style = MaterialTheme.typography.bodyLarge)
                    listOf(yarn.shop, yarn.care, yarn.notes).filter { !it.isNullOrBlank() }.forEach {
                        Text(it!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            SwatchesSection(
                swatches = ready.swatches,
                onAdd = { onEditSwatch(ready.yarn.id, null) },
                onEdit = { onEditSwatch(ready.yarn.id, it.id) },
                onDelete = { swatchToDelete = it },
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (useDialog) {
        UseSkeinsDialog(
            onDismiss = { useDialog = false },
            onConfirm = { amount ->
                viewModel.adjustStock(-amount)
                useDialog = false
            },
        )
    }
    swatchToDelete?.let { swatch ->
        ConfirmDialog(
            title = stringResource(R.string.delete_swatch_title),
            message = stringResource(R.string.swatch_summary, swatch.stitches, swatch.rows),
            confirmLabel = stringResource(R.string.delete),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = {
                viewModel.deleteSwatch(swatch.id)
                swatchToDelete = null
            },
            onDismiss = { swatchToDelete = null },
        )
    }
    if (confirmDelete && ready != null) {
        ConfirmDialog(
            title = stringResource(R.string.delete_yarn_title),
            message = stringResource(R.string.delete_yarn_message, ready.yarn.name),
            confirmLabel = stringResource(R.string.delete),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = {
                confirmDelete = false
                ready.yarn.photoPath?.let { java.io.File(it).delete() }
                viewModel.deleteYarn()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun YarnHeader(yarn: Yarn) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val photo = rememberLocalImage(yarn.photoPath, maxPx = 400)
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = stringResource(R.string.yarn_photo_desc),
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(96.dp).height(96.dp).clip(MaterialTheme.shapes.large),
            )
        } else {
            ColorDot(yarn.colorArgb, size = 72.dp)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            listOfNotNull(yarn.brand, yarn.colorName, yarn.dyeLot?.let { "#$it" }).forEach {
                Text(it, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun StockCard(yarn: Yarn, onMinus: () -> Unit, onPlus: () -> Unit, onUse: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.yarn_stock_section), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                skeinsText(yarn.stockSkeins),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            val sub = listOfNotNull(
                yarn.stockGrams?.let { "${it.roundToInt()} g" },
                yarn.stockMeters?.let { "${it.roundToInt()} m" },
            ).joinToString(" · ")
            if (sub.isNotEmpty() && yarn.stockSkeins > 0) {
                Text(sub, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalIconButton(onClick = onMinus, enabled = yarn.stockSkeins > 0) {
                    Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.stock_minus))
                }
                FilledTonalIconButton(onClick = onPlus) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.stock_plus))
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = onUse, enabled = yarn.stockSkeins > 0) { Text(stringResource(R.string.yarn_use)) }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun SwatchesSection(swatches: List<Swatch>, onAdd: () -> Unit, onEdit: (Swatch) -> Unit, onDelete: (Swatch) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.yarn_swatches), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_swatch))
            }
        }
        if (swatches.isEmpty()) {
            Text(stringResource(R.string.swatches_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        swatches.forEach { swatch -> SwatchCard(swatch, onEdit = { onEdit(swatch) }, onDelete = { onDelete(swatch) }) }
    }
}

@Composable
private fun SwatchCard(swatch: Swatch, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val separator = decimalSeparator()
    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            GaugeSwatchDiagram(
                stitches = swatch.stitches,
                rows = swatch.rows,
                needleSize = swatch.needleSizeMm?.let { formatMillimeters(it, separator) },
                squareSize = 84.dp,
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(swatch.pattern.labelRes()), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.swatch_summary, swatch.stitches, swatch.rows), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                swatch.weightG?.let { Text("${formatDecimalInput(it)} g", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                swatch.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.edit)) }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

/** Asks how many skeins were used; [initial] is a suggested starting value (e.g. what the project needs). */
@Composable
fun UseSkeinsDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    yarnName: String? = null,
    initial: String = "",
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    val amount = parseDecimal(text)?.takeIf { it > 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (yarnName == null) stringResource(R.string.yarn_use_title) else "$yarnName · " + stringResource(R.string.yarn_use_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                label = { Text(stringResource(R.string.yarn_use_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        confirmButton = {
            TextButton(onClick = { amount?.let(onConfirm) }, enabled = amount != null) {
                Text(stringResource(R.string.yarn_use_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
