package com.ilsecondodasinistra.majon.ui.finish

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilsecondodasinistra.majon.R
import com.ilsecondodasinistra.majon.domain.model.formatSkeins
import com.ilsecondodasinistra.majon.ui.components.FormSection
import com.ilsecondodasinistra.majon.ui.components.KeyboardBackHandler

/** At the end of a project: how many skeins of each linked yarn were used, filled in with the estimate. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishScreen(
    onBack: () -> Unit,
    viewModel: FinishViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.saved.collect { onBack() } }
    KeyboardBackHandler()
    val separator = java.text.DecimalFormatSymbols.getInstance(LocalLocale.current.platformLocale).decimalSeparator

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.finish_title)) },
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.finish_message), style = MaterialTheme.typography.bodyLarge)

            FormSection(null) {
                form.yarns.forEach { yarn ->
                    OutlinedTextField(
                        value = form.amounts[yarn.yarnId].orEmpty(),
                        onValueChange = { v ->
                            viewModel.setAmount(yarn.yarnId, v.filter { c -> c.isDigit() || c == '.' || c == ',' })
                        },
                        label = {
                            Text(stringResource(R.string.finish_skeins_label, yarn.name, formatSkeins(yarn.stockSkeins, separator)))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Button(
                onClick = { viewModel.save() },
                enabled = form.loaded && form.yarns.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text(stringResource(R.string.finish_confirm)) }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.finish_skip)) }
            Spacer(Modifier.height(24.dp))
        }
    }
}
