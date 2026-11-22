package com.ilsecondodasinistra.majon.ui.projectdetail

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
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
import androidx.compose.material3.Switch
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import com.ilsecondodasinistra.majon.domain.model.MeasureKey
import com.ilsecondodasinistra.majon.domain.model.PresetPart
import com.ilsecondodasinistra.majon.domain.model.ProjectIcon
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.YarnCheck
import com.ilsecondodasinistra.majon.domain.model.YarnPlan
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.estimate
import com.ilsecondodasinistra.majon.domain.model.formatDecimalInput
import com.ilsecondodasinistra.majon.domain.model.formatMillimeters
import com.ilsecondodasinistra.majon.domain.model.formatSkeins
import com.ilsecondodasinistra.majon.domain.model.parseDecimal
import com.ilsecondodasinistra.majon.domain.model.presetParts
import com.ilsecondodasinistra.majon.ui.components.labelRes
import com.ilsecondodasinistra.majon.ui.stash.ColorDot
import com.ilsecondodasinistra.majon.ui.stash.UseSkeinsDialog
import com.ilsecondodasinistra.majon.ui.stash.decimalSeparator
import com.ilsecondodasinistra.majon.ui.stash.labelRes as stashLabelRes
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
    /** Opens the part editor: a part id to edit it, null for a new part. */
    onEditPart: (Long?) -> Unit,
    /** Opens the screen that links yarn and needles from the stash to this project. */
    onEditLinks: () -> Unit,
    onDuplicated: (Long) -> Unit = {},
    viewModel: ProjectDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val openCopy by rememberUpdatedState(onDuplicated)
    LaunchedEffect(viewModel) { viewModel.duplicated.collect { openCopy(it) } }
    var useDialogFor by remember { mutableStateOf<YarnPlan?>(null) }
    var partToDelete by remember { mutableStateOf<Part?>(null) }
    var deleteProjectDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is ProjectDetailUiState.Deleted) onBack()
    }

    val state = uiState as? ProjectDetailUiState.Ready ?: return
    val project = state.projectWithParts.project
    val parts = state.projectWithParts.parts
    val copyName = stringResource(R.string.duplicate_copy_name, project.name)

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
                                text = { Text(stringResource(R.string.duplicate_project)) },
                                onClick = {
                                    menuOpen = false
                                    viewModel.duplicateProject(copyName)
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
                onClick = { onEditPart(null) },
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
            if (state.plans.isNotEmpty() || state.needles.isNotEmpty()) {
                item { YarnPlanCard(state.plans, state.needles, onUse = { useDialogFor = it }, onChange = onEditLinks) }
            } else {
                item { LinksInviteCard(onEditLinks) }
            }
            if (parts.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        EmptyState(
                            emoji = "🪡",
                            title = stringResource(R.string.parts_empty_title),
                            subtitle = stringResource(R.string.parts_empty_subtitle),
                        )
                        val presets = project.icon.presetParts()
                        // Both lists are read here (in the composition) because stringResource is not allowed in a callback
                        val labels = presets.map { stringResource(it.name.labelRes()) }
                        if (presets.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            androidx.compose.material3.FilledTonalButton(
                                onClick = { viewModel.addSuggestedParts(presets.zip(labels)) },
                            ) {
                                Text(stringResource(R.string.parts_suggest_button, labels.joinToString(", ")), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.parts_suggest_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp),
                            )
                        }
                    }
                }
            } else {
                items(parts, key = { it.id }) { part ->
                    val swatch = state.swatches.firstOrNull { it.id == part.swatchId } ?: state.defaultSwatch
                    val estimate = part.estimate(swatch)
                    PartCard(
                        part = part,
                        estimate = if (estimate.stitches != null && estimate.rows != null) {
                            stringResource(R.string.part_estimate_line, estimate.stitches, estimate.rows)
                        } else {
                            null
                        },
                        accentColor = project.color.toColor(),
                        onClick = { if (part.totalRows == 0) onEditPart(part.id) else onPartClick(part.id) },
                        onEdit = { onEditPart(part.id) },
                        onNotes = { onPartNotes(part.id) },
                        onDelete = { partToDelete = part },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    useDialogFor?.let { plan ->
        UseSkeinsDialog(
            yarnName = plan.yarn.name,
            initial = plan.neededSkeins?.let { formatDecimalInput(kotlin.math.ceil(it * 4) / 4) }.orEmpty(),
            onDismiss = { useDialogFor = null },
            onConfirm = { amount ->
                viewModel.useSkeins(plan.yarn.id, amount)
                useDialogFor = null
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
    estimate: String?,
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
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
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
                    if (part.totalRows == 0) {
                        stringResource(R.string.part_rows_to_define)
                    } else {
                        stringResource(R.string.rows_progress, part.rowsDone, part.totalRows)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (part.totalRows == 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (estimate != null) {
                    Text(estimate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
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

@Composable
private fun YarnPlanCard(plans: List<YarnPlan>, needles: List<Needle>, onUse: (YarnPlan) -> Unit, onChange: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.project_stash_title), style = MaterialTheme.typography.titleMedium)
            val separator = decimalSeparator()
            plans.forEach { plan ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ColorDot(plan.yarn.colorArgb, size = 28.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(plan.yarn.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        if (plans.size > 1 && plan.link.isPrimary) {
                            Text(stringResource(R.string.plan_main_yarn), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    val have = formatSkeins(plan.yarn.stockSkeins, separator)
                    when (val check = plan.check) {
                        is YarnCheck.Enough -> {
                            Text(
                                stringResource(R.string.plan_need_have, formatSkeins(check.needed, separator), have),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(stringResource(R.string.plan_enough), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                        }
                        is YarnCheck.Missing -> {
                            Text(
                                stringResource(R.string.plan_need_have, formatSkeins(check.needed, separator), have),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                stringResource(R.string.plan_missing, formatSkeins(check.missingRoundedUp, separator)),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        YarnCheck.Unknown -> {
                            Text(stringResource(R.string.plan_have, have), style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.plan_unknown), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (plan.neededSkeins != null) {
                        Text(
                            stringResource(if (plan.link.plannedSkeins != null) R.string.plan_planned_note else R.string.plan_estimate_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    androidx.compose.material3.TextButton(onClick = { onUse(plan) }, enabled = plan.yarn.stockSkeins > 0) {
                        Text(stringResource(R.string.plan_use))
                    }
                }
            }
            if (needles.isNotEmpty()) {
                // map is inline, so the type's string resource can be read inside it
                val needleTexts = needles.map { n ->
                    listOfNotNull(n.sizeMm?.let { formatMillimeters(it, separator) + " mm" }, stringResource(n.type.stashLabelRes()))
                        .joinToString(" ")
                }
                Text(
                    "🪡 " + needleTexts.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            androidx.compose.material3.TextButton(onClick = onChange) { Text(stringResource(R.string.project_links_edit)) }
        }
    }
}

/** Shown while nothing is linked: says what linking gives, and opens the screen that does it. */
@Composable
private fun LinksInviteCard(onLink: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.project_links_invite_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(stringResource(R.string.project_links_invite_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            androidx.compose.material3.FilledTonalButton(onClick = onLink) { Text(stringResource(R.string.project_links_invite_button)) }
        }
    }
}
