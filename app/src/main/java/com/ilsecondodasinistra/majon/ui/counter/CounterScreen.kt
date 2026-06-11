package com.ilsecondodasinistra.majon.ui.counter

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilsecondodasinistra.majon.R
import com.ilsecondodasinistra.majon.domain.model.RowNote
import com.ilsecondodasinistra.majon.ui.components.AnimatedProgressBar
import com.ilsecondodasinistra.majon.ui.components.ConfirmDialog
import com.ilsecondodasinistra.majon.ui.settings.SettingsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(
    onBack: () -> Unit,
    onNotes: () -> Unit,
    viewModel: CounterViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val activity = LocalActivity.current

    var resetDialog by rememberSaveable { mutableStateOf(false) }
    var goToRowDialog by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(settings.keepScreenOn) {
        val window = activity?.window
        if (settings.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val state = uiState as? CounterUiState.Ready ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.partName, maxLines = 1) },
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
                                text = { Text(stringResource(R.string.notes)) },
                                onClick = {
                                    menuOpen = false
                                    onNotes()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.set_row)) },
                                onClick = {
                                    menuOpen = false
                                    goToRowDialog = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.reset_counter)) },
                                onClick = {
                                    menuOpen = false
                                    resetDialog = true
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))

            // Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedProgressBar(
                    progress = state.progress,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "${(state.progress * 100).roundToInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(24.dp))

            if (state.isComplete) {
                CompletionCelebration(totalRows = state.totalRows)
            } else {
                // Row counter
                AnimatedContent(
                    targetState = state.currentRow,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInVertically { it / 2 } + fadeIn()) togetherWith
                                (slideOutVertically { -it / 2 } + fadeOut())
                        } else {
                            (slideInVertically { -it / 2 } + fadeIn()) togetherWith
                                (slideOutVertically { it / 2 } + fadeOut())
                        }
                    },
                    label = "rowNumber",
                ) { row ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.row_current, row),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "$row",
                            style = MaterialTheme.typography.displayLarge,
                            fontSize = 110.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            stringResource(R.string.row_of_total, state.totalRows),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Current row notes
                AnimatedVisibility(
                    visible = state.currentNotes.isNotEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    NoteCard(
                        title = stringResource(R.string.current_row_note),
                        notes = state.currentNotes,
                        highlighted = true,
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Next row preview
                if (state.currentRow < state.totalRows) {
                    NoteCard(
                        title = stringResource(R.string.next_row_label, state.currentRow + 1),
                        notes = state.nextNotes,
                        highlighted = false,
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(24.dp))

            // Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                FilledIconButton(
                    onClick = {
                        if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.decrement()
                    },
                    modifier = Modifier.size(64.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = stringResource(R.string.row_undo),
                        modifier = Modifier.size(28.dp),
                    )
                }

                PlusButton(
                    enabled = !state.isComplete,
                    onClick = {
                        if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        viewModel.increment()
                    },
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (resetDialog) {
        ConfirmDialog(
            title = stringResource(R.string.reset_confirm_title),
            message = stringResource(R.string.reset_confirm_message),
            confirmLabel = stringResource(R.string.confirm),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = {
                resetDialog = false
                viewModel.reset()
            },
            onDismiss = { resetDialog = false },
        )
    }

    if (goToRowDialog) {
        GoToRowDialog(
            totalRows = state.totalRows,
            onDismiss = { goToRowDialog = false },
            onConfirm = { row ->
                goToRowDialog = false
                viewModel.setCurrentRow(row)
            },
        )
    }
}

@Composable
private fun PlusButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "plusScale",
    )
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier
            .size(104.dp)
            .scale(scale),
        shape = CircleShape,
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = stringResource(R.string.row_done),
            modifier = Modifier.size(52.dp),
        )
    }
}

@Composable
private fun NoteCard(
    title: String,
    notes: List<RowNote>,
    highlighted: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            },
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = if (highlighted) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (notes.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                notes.forEach { note ->
                    Text(
                        "📌 ${note.text}",
                        style = if (highlighted) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        color = if (highlighted) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletionCelebration(totalRows: Int) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "celebrate",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 24.dp),
    ) {
        Text("🎉", fontSize = (88 * scale).sp)
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.part_complete),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.part_complete_subtitle, totalRows),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GoToRowDialog(
    totalRows: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_row_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() } },
                label = { Text(stringResource(R.string.row_number)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { Text("1–$totalRows") },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { text.toIntOrNull()?.let(onConfirm) },
                enabled = text.toIntOrNull() != null,
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
