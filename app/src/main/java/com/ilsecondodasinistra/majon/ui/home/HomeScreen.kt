package com.ilsecondodasinistra.majon.ui.home

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilsecondodasinistra.majon.R
import com.ilsecondodasinistra.majon.domain.model.ProjectWithParts
import com.ilsecondodasinistra.majon.ui.components.LocalBottomBarHeight
import com.ilsecondodasinistra.majon.ui.components.AnimatedProgressBar
import com.ilsecondodasinistra.majon.ui.components.ConfirmDialog
import com.ilsecondodasinistra.majon.ui.components.EmptyState
import com.ilsecondodasinistra.majon.ui.components.typeLabelRes
import com.ilsecondodasinistra.majon.ui.components.ProjectIconBadge
import com.ilsecondodasinistra.majon.ui.theme.toColor
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    onProjectClick: (Long) -> Unit,
    onAddProject: () -> Unit,
    onEditProject: (Long) -> Unit,
    onSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // A copy of a project opens in the editor, so it can be varied right away
    val openCopy by rememberUpdatedState(onEditProject)
    LaunchedEffect(viewModel) { viewModel.duplicated.collect { openCopy(it) } }
    var projectToDelete by remember { mutableStateOf<ProjectWithParts?>(null) }

    Scaffold(
        topBar = {
            // Compact, like the Stash tab: the title and the settings button share one row
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.padding(bottom = LocalBottomBarHeight.current),
                onClick = onAddProject,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_project)) },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            is HomeUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
            is HomeUiState.Ready -> {
                if (state.projects.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState(
                            emoji = "🧶",
                            title = stringResource(R.string.home_empty_title),
                            subtitle = stringResource(R.string.home_empty_subtitle),
                            modifier = Modifier.padding(32.dp),
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp, top = 8.dp,
                            bottom = 8.dp + LocalBottomBarHeight.current,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.projects, key = { it.project.id }) { item ->
                            val copyName = stringResource(R.string.duplicate_copy_name, item.project.name)
                            ProjectCard(
                                item = item,
                                mainYarn = state.mainYarns[item.project.id],
                                onClick = { onProjectClick(item.project.id) },
                                onEdit = { onEditProject(item.project.id) },
                                onDuplicate = { viewModel.duplicateProject(item.project.id, copyName) },
                                onDelete = { projectToDelete = item },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }

    projectToDelete?.let { item ->
        ConfirmDialog(
            title = stringResource(R.string.delete_project_title),
            message = stringResource(R.string.delete_project_message, item.project.name),
            confirmLabel = stringResource(R.string.delete),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = {
                viewModel.deleteProject(item.project.id)
                projectToDelete = null
            },
            onDismiss = { projectToDelete = null },
        )
    }
}

@Composable
private fun ProjectCard(
    item: ProjectWithParts,
    mainYarn: String?,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().animateContentSize(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProjectIconBadge(icon = item.project.icon, color = item.project.color)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.project.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                )
                // What it is and what it is made with: tells similar projects apart at a glance
                Text(
                    listOfNotNull(stringResource(item.project.icon.typeLabelRes()), mainYarn).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(Modifier.height(8.dp))
                AnimatedProgressBar(
                    progress = item.progress,
                    color = item.project.color.toColor(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.percent_complete, (item.progress * 100).roundToInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.duplicate_project)) },
                        onClick = {
                            menuOpen = false
                            onDuplicate()
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
