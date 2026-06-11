package com.ilsecondodasinistra.majon.ui.editproject

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilsecondodasinistra.majon.R
import com.ilsecondodasinistra.majon.domain.model.ProjectColor
import com.ilsecondodasinistra.majon.domain.model.ProjectIcon
import com.ilsecondodasinistra.majon.ui.components.emoji
import com.ilsecondodasinistra.majon.ui.theme.toColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProjectScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditProjectViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val savedEvent by viewModel.savedEvent.collectAsStateWithLifecycle()

    LaunchedEffect(savedEvent) {
        if (savedEvent != null) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (form.isEditing) R.string.edit_project else R.string.add_project))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            OutlinedTextField(
                value = form.name,
                onValueChange = viewModel::updateName,
                label = { Text(stringResource(R.string.project_name)) },
                isError = form.nameError,
                supportingText = if (form.nameError) {
                    { Text(stringResource(R.string.project_name_error)) }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Column {
                Text(stringResource(R.string.project_icon), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(ProjectIcon.entries) { icon ->
                        IconChoice(
                            icon = icon,
                            selected = icon == form.icon,
                            selectedColor = form.color.toColor(),
                            onClick = { viewModel.updateIcon(icon) },
                        )
                    }
                }
            }

            Column {
                Text(stringResource(R.string.project_color), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(ProjectColor.entries) { color ->
                        ColorChoice(
                            color = color,
                            selected = color == form.color,
                            onClick = { viewModel.updateColor(color) },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = form.yarnType,
                onValueChange = viewModel::updateYarnType,
                label = { Text(stringResource(R.string.yarn_type)) },
                placeholder = { Text(stringResource(R.string.yarn_type_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = form.needleSize,
                onValueChange = viewModel::updateNeedleSize,
                label = { Text(stringResource(R.string.needle_size)) },
                placeholder = { Text(stringResource(R.string.needle_size_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = viewModel::save,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(stringResource(R.string.save), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun IconChoice(
    icon: ProjectIcon,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (selected) selectedColor.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
        label = "iconBg",
    )
    val scale by animateFloatAsState(targetValue = if (selected) 1.1f else 1f, label = "iconScale")
    Box(
        modifier = Modifier
            .size(56.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(background)
            .then(
                if (selected) {
                    Modifier.border(2.dp, selectedColor, CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(icon.emoji, fontSize = 26.sp)
    }
}

@Composable
private fun ColorChoice(
    color: ProjectColor,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(targetValue = if (selected) 1.15f else 1f, label = "colorScale")
    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color.toColor())
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
            )
        }
    }
}
