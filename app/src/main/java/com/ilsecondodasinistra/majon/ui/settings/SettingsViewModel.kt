package com.ilsecondodasinistra.majon.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.data.export.BackupCodec
import com.ilsecondodasinistra.majon.data.settings.AppLanguage
import com.ilsecondodasinistra.majon.data.settings.AppSettings
import com.ilsecondodasinistra.majon.data.settings.SettingsRepository
import com.ilsecondodasinistra.majon.data.settings.ColorPalette
import com.ilsecondodasinistra.majon.data.settings.ThemeMode
import com.ilsecondodasinistra.majon.domain.repository.MajonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What an import added. */
data class ImportSummary(val projects: Int, val yarns: Int, val needles: Int)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val majonRepository: MajonRepository,
    private val backupCodec: BackupCodec,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsRepository.setLanguage(language) }
    }

    fun setTheme(theme: ThemeMode) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    fun setPalette(palette: ColorPalette) {
        viewModelScope.launch { settingsRepository.setPalette(palette) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    fun setHaptics(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHaptics(enabled) }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepScreenOn(enabled) }
    }

    fun setWavyProgress(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setWavyProgress(enabled) }
    }

    fun setVoiceFeedback(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVoiceFeedback(enabled) }
    }

    fun setVolumeKeys(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVolumeKeys(enabled) }
    }

    suspend fun exportData(): String = backupCodec.encode(
        majonRepository.getFullProjects(),
        exportedAt = System.currentTimeMillis(),
        stash = majonRepository.getStash(),
    )

    suspend fun importData(content: String): Result<ImportSummary> =
        backupCodec.decodeAll(content).mapCatching { data ->
            val projects = majonRepository.importProjects(data.projects)
            val stash = majonRepository.importStash(data.stash)
            ImportSummary(projects = projects, yarns = stash.yarns, needles = stash.needles)
        }
}
