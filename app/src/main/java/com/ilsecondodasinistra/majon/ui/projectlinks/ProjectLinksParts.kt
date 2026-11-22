package com.ilsecondodasinistra.majon.ui.projectlinks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.ilsecondodasinistra.majon.ui.components.FormSection
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilsecondodasinistra.majon.R
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.ProjectYarnLink
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.domain.model.formatDecimalInput
import com.ilsecondodasinistra.majon.domain.model.formatMillimeters
import com.ilsecondodasinistra.majon.ui.stash.ColorDot
import com.ilsecondodasinistra.majon.ui.stash.decimalSeparator
import com.ilsecondodasinistra.majon.ui.stash.labelRes

/** Which yarns and needles of the stash a project is made with: the base of the "do I have enough?" check. */
@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ProjectLinksScreen(
    onBack: () -> Unit,
    viewModel: ProjectLinksViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val yarns by viewModel.yarns.collectAsStateWithLifecycle()
    val needles by viewModel.needles.collectAsStateWithLifecycle()
    val links by viewModel.links.collectAsStateWithLifecycle()
    val swatches by viewModel.swatchesByYarn.collectAsStateWithLifecycle()
    val usage by viewModel.needleUsage.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(viewModel) { viewModel.saved.collect { onBack() } }

    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(stringResource(R.string.project_stash_title)) },
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
                stringResource(R.string.project_links_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FormSection(stringResource(R.string.project_yarns_title)) {
                if (yarns.isEmpty()) {
                    Text(
                        stringResource(R.string.project_stash_empty_yarns),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(stringResource(R.string.project_yarns_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        yarns.forEach { yarn ->
                            FilterChip(
                                selected = links.yarns.any { it.yarnId == yarn.id },
                                onClick = { viewModel.toggleYarn(yarn.id) },
                                label = { Text(listOfNotNull(yarn.name, yarn.colorName).joinToString(" · ")) },
                                leadingIcon = { ColorDot(yarn.colorArgb, size = 20.dp) },
                            )
                        }
                    }
                    links.yarns.forEach { link ->
                        val yarn = yarns.firstOrNull { it.id == link.yarnId } ?: return@forEach
                        YarnLinkCard(
                            yarn = yarn,
                            link = link,
                            swatches = swatches[yarn.id].orEmpty(),
                            onMakeMain = { viewModel.setPrimaryYarn(yarn.id) },
                            onSwatch = { viewModel.setYarnSwatch(yarn.id, it) },
                            onPlanned = { viewModel.setPlannedSkeins(yarn.id, it) },
                        )
                    }
                }
            }

            FormSection(stringResource(R.string.project_needles_title)) {
                if (needles.isEmpty()) {
                    Text(
                        stringResource(R.string.project_stash_empty_needles),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    needles.forEach { needle ->
                        NeedleRow(
                            needle = needle,
                            checked = needle.id in links.needleIds,
                            inUseBy = usage[needle.id].orEmpty(),
                            onToggle = { viewModel.toggleNeedle(needle.id) },
                        )
                    }
                }
            }

            androidx.compose.material3.Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text(stringResource(R.string.save)) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun YarnLinkCard(
    yarn: Yarn,
    link: ProjectYarnLink,
    swatches: List<Swatch>,
    onMakeMain: () -> Unit,
    onSwatch: (Long?) -> Unit,
    onPlanned: (String) -> Unit,
) {
    var planned by rememberSaveable(yarn.id) { mutableStateOf(link.plannedSkeins?.let(::formatDecimalInput).orEmpty()) }
    val separator = decimalSeparator()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorDot(yarn.colorArgb, size = 28.dp)
                Spacer(Modifier.width(10.dp))
                Text(yarn.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (link.isPrimary) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.project_yarn_main), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                } else {
                    IconButton(onClick = onMakeMain) {
                        Icon(Icons.Outlined.StarOutline, contentDescription = stringResource(R.string.project_yarn_make_main))
                    }
                }
            }
            if (swatches.isEmpty()) {
                Text(stringResource(R.string.project_yarn_no_swatch), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(stringResource(R.string.project_yarn_swatch), style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    swatches.forEach { swatch ->
                        FilterChip(
                            selected = link.swatchId == swatch.id,
                            onClick = { onSwatch(if (link.swatchId == swatch.id) null else swatch.id) },
                            label = {
                                Text(
                                    listOfNotNull(
                                        "${swatch.stitches}×${swatch.rows}",
                                        swatch.needleSizeMm?.let { formatMillimeters(it, separator) + " mm" },
                                    ).joinToString(" · "),
                                )
                            },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = planned,
                onValueChange = { v ->
                    planned = v.filter { c -> c.isDigit() || c == '.' || c == ',' }
                    onPlanned(planned)
                },
                label = { Text(stringResource(R.string.project_yarn_planned)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun NeedleRow(needle: Needle, checked: Boolean, inUseBy: List<String>, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Column(Modifier.weight(1f)) {
            val size = needle.sizeMm?.let { formatMillimeters(it, decimalSeparator()) + " mm" }
            Text(
                listOfNotNull(size, stringResource(needle.type.labelRes()), needle.lengthCm?.let { "$it cm" }).joinToString(" · "),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (inUseBy.isNotEmpty()) {
                Text(
                    stringResource(R.string.needle_in_use, inUseBy.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}
