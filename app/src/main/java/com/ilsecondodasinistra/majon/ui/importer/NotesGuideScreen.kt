package com.ilsecondodasinistra.majon.ui.importer

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilsecondodasinistra.majon.R
import com.ilsecondodasinistra.majon.domain.model.buildNotesGuide
import com.ilsecondodasinistra.majon.domain.model.guidePartLines
import com.ilsecondodasinistra.majon.ui.components.FormSection
import kotlinx.coroutines.launch

/**
 * The text to give an AI assistant so it writes a `.majon-notes` file from a pattern's instructions: copy it, share it,
 * and the three steps of what to do with it. For a project it already lists the project's parts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesGuideScreen(
    onBack: () -> Unit,
    onImport: () -> Unit,
    viewModel: NotesGuideViewModel = hiltViewModel(),
) {
    val guideContext by viewModel.context.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val copiedMessage = stringResource(R.string.guide_copied)

    val resources = LocalResources.current
    // The guide is in the language of the app (res/raw for Italian, res/raw-en for English)
    val template = remember(resources) { resources.openRawResource(R.raw.notes_guide).bufferedReader().use { it.readText() } }
    val partsHeading = stringResource(R.string.guide_parts_heading)
    val guide = buildNotesGuide(
        template,
        partsBlock = if (guideContext.parts.isEmpty()) {
            ""
        } else {
            partsHeading + "\n" + guidePartLines(guideContext.parts) { resources.getString(R.string.guide_rows_label, it) }
        },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.guide_title)) },
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
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.guide_intro), style = MaterialTheme.typography.bodyLarge)

            FormSection(stringResource(R.string.guide_step1_title)) {
                Text(stringResource(R.string.guide_step1_body), style = MaterialTheme.typography.bodyMedium)
                guideContext.projectName?.let {
                    Text(
                        stringResource(R.string.guide_project_hint, it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            clipboard.setText(AnnotatedString(guide))
                            scope.launch { snackbar.showSnackbar(copiedMessage) }
                        },
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(Modifier.padding(start = 8.dp))
                        Text(stringResource(R.string.guide_copy))
                    }
                    OutlinedButton(
                        onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, guide)
                            }
                            context.startActivity(Intent.createChooser(send, null))
                        },
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.padding(start = 8.dp))
                        Text(stringResource(R.string.guide_share))
                    }
                }
            }

            FormSection(stringResource(R.string.guide_step2_title)) {
                Text(stringResource(R.string.guide_step2_body), style = MaterialTheme.typography.bodyMedium)
            }

            FormSection(stringResource(R.string.guide_step3_title)) {
                Text(stringResource(R.string.guide_step3_body), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onImport) { Text(stringResource(R.string.guide_go_import)) }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.guide_text_label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    SelectionContainer {
                        Text(guide, fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
