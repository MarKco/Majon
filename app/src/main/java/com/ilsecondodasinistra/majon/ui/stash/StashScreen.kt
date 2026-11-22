package com.ilsecondodasinistra.majon.ui.stash

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilsecondodasinistra.majon.R
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.NeedleMaterial
import com.ilsecondodasinistra.majon.domain.model.NeedleType
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.domain.model.formatDecimalInput
import com.ilsecondodasinistra.majon.ui.components.ConfirmDialog
import com.ilsecondodasinistra.majon.ui.components.EmptyState
import com.ilsecondodasinistra.majon.ui.components.LocalBottomBarHeight
import com.ilsecondodasinistra.majon.ui.components.rememberLocalImage
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale

private enum class StashTab { YARNS, NEEDLES }

private data class NeedleDialogState(val needle: Needle? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StashScreen(
    onYarnClick: (Long) -> Unit,
    onAddYarn: () -> Unit,
    viewModel: StashViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tabName by rememberSaveable { mutableStateOf(StashTab.YARNS.name) }
    val tab = StashTab.valueOf(tabName)
    var needleDialog by remember { mutableStateOf<NeedleDialogState?>(null) }
    var needleToDelete by remember { mutableStateOf<Needle?>(null) }
    var yarnToDelete by remember { mutableStateOf<Yarn?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val barHeight = LocalBottomBarHeight.current

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.stash_title)) },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.padding(bottom = barHeight),
                onClick = { if (tab == StashTab.YARNS) onAddYarn() else needleDialog = NeedleDialogState() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(if (tab == StashTab.YARNS) R.string.add_yarn else R.string.add_needle)) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SecondaryTabRow(selectedTabIndex = tab.ordinal) {
                Tab(
                    selected = tab == StashTab.YARNS,
                    onClick = { tabName = StashTab.YARNS.name },
                    text = { Text(stringResource(R.string.stash_tab_yarns)) },
                )
                Tab(
                    selected = tab == StashTab.NEEDLES,
                    onClick = { tabName = StashTab.NEEDLES.name },
                    text = { Text(stringResource(R.string.stash_tab_needles)) },
                )
            }
            val ready = state as? StashUiState.Ready
            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "stashTab",
                modifier = Modifier.weight(1f),
            ) { current ->
                if (ready == null) {
                    Box(Modifier.fillMaxSize())
                } else if (current == StashTab.YARNS) {
                    YarnList(ready.yarns, onYarnClick, onDelete = { yarnToDelete = it })
                } else {
                    NeedleList(ready.needles, onEdit = { needleDialog = NeedleDialogState(it) }, onDelete = { needleToDelete = it })
                }
            }
        }
    }

    needleDialog?.let { dialog ->
        NeedleEditorDialog(
            needle = dialog.needle,
            onDismiss = { needleDialog = null },
            onSave = { type, material, size, length, quantity, brand, notes ->
                val ok = viewModel.saveNeedle(dialog.needle?.id, type, material, size, length, quantity, brand, notes)
                if (ok) needleDialog = null
                ok
            },
        )
    }
    needleToDelete?.let { needle ->
        ConfirmDialog(
            title = stringResource(R.string.delete_needle_title),
            message = stringResource(R.string.delete_needle_message),
            confirmLabel = stringResource(R.string.delete),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = {
                viewModel.deleteNeedle(needle.id)
                needleToDelete = null
            },
            onDismiss = { needleToDelete = null },
        )
    }
    yarnToDelete?.let { yarn ->
        ConfirmDialog(
            title = stringResource(R.string.delete_yarn_title),
            message = stringResource(R.string.delete_yarn_message, yarn.name),
            confirmLabel = stringResource(R.string.delete),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = {
                yarn.photoPath?.let { java.io.File(it).delete() }
                viewModel.deleteYarn(yarn.id)
                yarnToDelete = null
            },
            onDismiss = { yarnToDelete = null },
        )
    }
}

@Composable
private fun YarnList(yarns: List<Yarn>, onClick: (Long) -> Unit, onDelete: (Yarn) -> Unit) {
    if (yarns.isEmpty()) {
        EmptyState(
            emoji = "🧶",
            title = stringResource(R.string.empty_yarns_title),
            subtitle = stringResource(R.string.empty_yarns_subtitle),
            modifier = Modifier.fillMaxSize().padding(32.dp),
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 12.dp,
            bottom = 96.dp + LocalBottomBarHeight.current,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(yarns, key = { it.id }) { yarn ->
            YarnCard(yarn, onClick = { onClick(yarn.id) }, onDelete = { onDelete(yarn) }, modifier = Modifier.animateItem())
        }
    }
}

@Composable
private fun YarnCard(yarn: Yarn, onClick: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    var menu by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val photo = rememberLocalImage(yarn.photoPath, maxPx = 160)
            if (photo != null) {
                Image(
                    bitmap = photo,
                    contentDescription = stringResource(R.string.yarn_photo_desc),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(CircleShape),
                )
            } else {
                ColorDot(yarn.colorArgb, size = 56.dp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(yarn.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                val subtitle = listOfNotNull(yarn.brand, yarn.colorName).joinToString(" · ")
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                if (yarn.materials.isNotEmpty()) {
                    Text(compositionText(yarn), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Text(
                    stockText(yarn),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (yarn.stockSkeins > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                )
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = {
                            menu = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NeedleList(needles: List<Needle>, onEdit: (Needle) -> Unit, onDelete: (Needle) -> Unit) {
    if (needles.isEmpty()) {
        EmptyState(
            emoji = "🪡",
            title = stringResource(R.string.empty_needles_title),
            subtitle = stringResource(R.string.empty_needles_subtitle),
            modifier = Modifier.fillMaxSize().padding(32.dp),
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 12.dp,
            bottom = 96.dp + LocalBottomBarHeight.current,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(needles, key = { it.id }) { needle ->
            NeedleCard(needle, onClick = { onEdit(needle) }, onDelete = { onDelete(needle) }, modifier = Modifier.animateItem())
        }
    }
}

@Composable
private fun NeedleCard(needle: Needle, onClick: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    var menu by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(88.dp)) {
                Text(needleSizeText(needle), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                val line = listOfNotNull(
                    stringResource(needle.type.labelRes()),
                    needle.material?.let { stringResource(it.labelRes()) },
                    needle.lengthCm?.let { "$it cm" },
                ).joinToString(" · ")
                Text(line, style = MaterialTheme.typography.titleMedium)
                val extra = listOfNotNull(needle.brand, if (needle.quantity > 1) "×${needle.quantity}" else null).joinToString(" · ")
                if (extra.isNotEmpty()) {
                    Text(extra, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        onClick = {
                            menu = false
                            onClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = {
                            menu = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NeedleEditorDialog(
    needle: Needle?,
    onDismiss: () -> Unit,
    onSave: (NeedleType, NeedleMaterial?, String, String, String, String, String) -> Boolean,
) {
    var type by rememberSaveable { mutableStateOf((needle?.type ?: NeedleType.CIRCULAR).name) }
    var material by rememberSaveable { mutableStateOf(needle?.material?.name) }
    var size by rememberSaveable { mutableStateOf(needle?.sizeMm?.let(::formatDecimalInput).orEmpty()) }
    var length by rememberSaveable { mutableStateOf(needle?.lengthCm?.toString().orEmpty()) }
    var quantity by rememberSaveable { mutableStateOf((needle?.quantity ?: 1).toString()) }
    var brand by rememberSaveable { mutableStateOf(needle?.brand.orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(needle?.notes.orEmpty()) }
    var error by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (needle == null) R.string.needle_new else R.string.needle_edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.needle_type), style = MaterialTheme.typography.labelLarge)
                ChipChoices(
                    options = NeedleType.entries,
                    selected = NeedleType.valueOf(type),
                    label = { stringResource(it.labelRes()) },
                    onSelect = { it?.let { t -> type = t.name } },
                    clearable = false,
                )
                OutlinedTextField(
                    value = size,
                    onValueChange = { size = it.filter { c -> c.isDigit() || c == '.' || c == ',' }; error = false },
                    label = { Text(stringResource(R.string.needle_size_mm)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = length,
                    onValueChange = { length = it.filter { c -> c.isDigit() }; error = false },
                    label = { Text(stringResource(R.string.needle_length)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.needle_material), style = MaterialTheme.typography.labelLarge)
                ChipChoices(
                    options = NeedleMaterial.entries,
                    selected = material?.let { NeedleMaterial.valueOf(it) },
                    label = { stringResource(it.labelRes()) },
                    onSelect = { material = it?.name },
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter { c -> c.isDigit() }; error = false },
                    label = { Text(stringResource(R.string.needle_quantity)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text(stringResource(R.string.needle_brand)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.needle_notes)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error) {
                    Text(stringResource(R.string.needle_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val ok = onSave(NeedleType.valueOf(type), material?.let { NeedleMaterial.valueOf(it) }, size, length, quantity, brand, notes)
                    if (!ok) error = true
                },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
