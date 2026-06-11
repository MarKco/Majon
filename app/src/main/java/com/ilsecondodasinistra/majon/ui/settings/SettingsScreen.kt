package com.ilsecondodasinistra.majon.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilsecondodasinistra.majon.R
import com.ilsecondodasinistra.majon.data.settings.AppLanguage
import com.ilsecondodasinistra.majon.data.settings.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val exportDoneMsg = stringResource(R.string.export_done)
    val exportErrorMsg = stringResource(R.string.export_error)
    val importErrorMsg = stringResource(R.string.import_error)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val json = viewModel.exportData()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                } ?: error("Cannot open output stream")
            }.onSuccess {
                snackbar.showSnackbar(exportDoneMsg)
            }.onFailure {
                snackbar.showSnackbar(exportErrorMsg)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val result = runCatching {
                val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readBytes().toString(Charsets.UTF_8)
                } ?: error("Cannot open input stream")
                viewModel.importData(content).getOrThrow()
            }
            result.onSuccess { count ->
                snackbar.showSnackbar(resources.getString(R.string.import_done, count))
            }.onFailure {
                snackbar.showSnackbar(importErrorMsg)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Language
            SettingSection(title = stringResource(R.string.language)) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    val options = listOf(
                        AppLanguage.SYSTEM to stringResource(R.string.lang_system),
                        AppLanguage.ITALIAN to stringResource(R.string.lang_it),
                        AppLanguage.ENGLISH to stringResource(R.string.lang_en),
                    )
                    options.forEachIndexed { index, (lang, label) ->
                        SegmentedButton(
                            selected = settings.language == lang,
                            onClick = {
                                viewModel.setLanguage(lang)
                                AppCompatDelegate.setApplicationLocales(
                                    lang.tag?.let { LocaleListCompat.forLanguageTags(it) }
                                        ?: LocaleListCompat.getEmptyLocaleList(),
                                )
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                            label = { Text(label, maxLines = 1) },
                        )
                    }
                }
            }

            // Theme
            SettingSection(title = stringResource(R.string.theme)) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    val options = listOf(
                        ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                        ThemeMode.LIGHT to stringResource(R.string.theme_light),
                        ThemeMode.DARK to stringResource(R.string.theme_dark),
                    )
                    options.forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            selected = settings.theme == mode,
                            onClick = { viewModel.setTheme(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                            label = { Text(label, maxLines = 1) },
                        )
                    }
                }

                ToggleItem(
                    title = stringResource(R.string.dynamic_colors),
                    subtitle = stringResource(R.string.dynamic_colors_desc),
                    checked = settings.dynamicColor,
                    onCheckedChange = viewModel::setDynamicColor,
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            ToggleItem(
                title = stringResource(R.string.haptics),
                subtitle = stringResource(R.string.haptics_desc),
                checked = settings.haptics,
                onCheckedChange = viewModel::setHaptics,
            )
            ToggleItem(
                title = stringResource(R.string.keep_screen_on),
                subtitle = stringResource(R.string.keep_screen_on_desc),
                checked = settings.keepScreenOn,
                onCheckedChange = viewModel::setKeepScreenOn,
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // Data
            SettingSection(title = stringResource(R.string.data_section)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.export_data)) },
                    supportingContent = { Text(stringResource(R.string.export_data_desc)) },
                    leadingContent = { Text("📤") },
                    modifier = Modifier.clickableItem { exportLauncher.launch("majon-backup.json") },
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.import_data)) },
                    supportingContent = { Text(stringResource(R.string.import_data_desc)) },
                    leadingContent = { Text("📥") },
                    modifier = Modifier.clickableItem {
                        importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun Modifier.clickableItem(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))

@Composable
private fun SettingSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        content()
    }
}

@Composable
private fun ToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
