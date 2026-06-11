package com.ilsecondodasinistra.majon.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.data.export.BackupCodec
import com.ilsecondodasinistra.majon.data.settings.AppLanguage
import com.ilsecondodasinistra.majon.data.settings.AppSettings
import com.ilsecondodasinistra.majon.data.settings.SettingsRepository
import com.ilsecondodasinistra.majon.data.settings.ThemeMode
import com.ilsecondodasinistra.majon.domain.repository.MajonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    fun setHaptics(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHaptics(enabled) }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepScreenOn(enabled) }
    }

    suspend fun exportData(): String =
        backupCodec.encode(majonRepository.getFullProjects(), exportedAt = System.currentTimeMillis())

    suspend fun importData(content: String): Result<Int> =
        backupCodec.decode(content).mapCatching { majonRepository.importProjects(it) }
}
