package com.ilsecondodasinistra.majon.ui.counter

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.runtime.LaunchedEffect
import com.ilsecondodasinistra.majon.ui.components.Confetti
import com.ilsecondodasinistra.majon.ui.components.PulseRing
import com.ilsecondodasinistra.majon.ui.components.YarnThreadBackground
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
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

    var pulseKey by rememberSaveable { mutableStateOf(0) }
    var confettiKey by rememberSaveable { mutableStateOf(0) }
    // Celebrate only on the transition to complete, not when reopening a finished part
    var wasComplete by rememberSaveable { mutableStateOf(state.isComplete) }
    LaunchedEffect(state.isComplete) {
        if (state.isComplete && !wasComplete) {
            confettiKey++
            if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        wasComplete = state.isComplete
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.partName, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            YarnThreadBackground(progress = state.progress, color = MaterialTheme.colorScheme.primary)

            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                    RowNumber(row = state.currentRow, totalRows = state.totalRows)

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

                CounterButtons(
                    canIncrement = !state.isComplete,
                    pulseKey = pulseKey,
                    onDecrement = {
                        if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.decrement()
                    },
                    onIncrement = {
                        if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        pulseKey++
                        viewModel.increment()
                    },
                )

                Spacer(Modifier.height(20.dp))

                CounterToolbar(
                    onNotes = onNotes,
                    onSetRow = { goToRowDialog = true },
                    onReset = { resetDialog = true },
                )

                Spacer(Modifier.height(24.dp))
            }

            Confetti(burstKey = confettiKey)
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
private fun RowNumber(row: Int, totalRows: Int) {
    // Springy pop each time the row changes
    val pop = remember { Animatable(1f) }
    LaunchedEffect(row) {
        pop.snapTo(0.85f)
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium))
    }
    AnimatedContent(
        targetState = row,
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
    ) { value ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.row_current, value),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "$value",
                style = MaterialTheme.typography.displayLarge,
                fontSize = 110.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.scale(pop.value),
            )
            Text(
                stringResource(R.string.row_of_total, totalRows),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CounterButtons(
    canIncrement: Boolean,
    pulseKey: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    val minusSource = remember { MutableInteractionSource() }
    val plusSource = remember { MutableInteractionSource() }
    val plusPressed by plusSource.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (plusPressed) 36.dp else 56.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "plusCorner",
    )
    ButtonGroup(overflowIndicator = {}) {
        customItem(
            buttonGroupContent = {
                Box(
                    modifier = Modifier.size(width = 72.dp, height = 112.dp).animateWidth(minusSource),
                    contentAlignment = Alignment.Center,
                ) {
                    FilledIconButton(
                        onClick = onDecrement,
                        interactionSource = minusSource,
                        modifier = Modifier.fillMaxWidth().height(72.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = stringResource(R.string.row_undo),
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            },
            menuContent = {},
        )
        customItem(
            buttonGroupContent = {
                Box(
                    modifier = Modifier.size(width = 112.dp, height = 112.dp).animateWidth(plusSource),
                    contentAlignment = Alignment.Center,
                ) {
                    PulseRing(pulseKey = pulseKey, color = MaterialTheme.colorScheme.primary)
                    FilledIconButton(
                        onClick = onIncrement,
                        enabled = canIncrement,
                        interactionSource = plusSource,
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(corner),
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.row_done),
                            modifier = Modifier.size(52.dp),
                        )
                    }
                }
            },
            menuContent = {},
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CounterToolbar(
    onNotes: () -> Unit,
    onSetRow: () -> Unit,
    onReset: () -> Unit,
) {
    HorizontalFloatingToolbar(
        expanded = true,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        content = {
            IconButton(onClick = onNotes) {
                Icon(Icons.Default.PushPin, contentDescription = stringResource(R.string.notes))
            }
            IconButton(onClick = onSetRow) {
                Icon(Icons.Default.Pin, contentDescription = stringResource(R.string.set_row))
            }
            IconButton(onClick = onReset) {
                Icon(Icons.Default.RestartAlt, contentDescription = stringResource(R.string.reset_counter))
            }
        },
    )
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
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = null,
                            modifier = Modifier.padding(top = 3.dp, end = 8.dp).size(18.dp),
                            tint = if (highlighted) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Text(
                            note.text,
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
