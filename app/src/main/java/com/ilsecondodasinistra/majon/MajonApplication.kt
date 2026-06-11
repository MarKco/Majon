package com.ilsecondodasinistra.majon

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.ilsecondodasinistra.majon.data.settings.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class MajonApplication : Application() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        // Apply the persisted language before any activity is created.
        // The settings file is tiny, so the blocking read is negligible.
        val language = runBlocking { settingsRepository.settings.first().language }
        AppCompatDelegate.setApplicationLocales(
            language.tag?.let { LocaleListCompat.forLanguageTags(it) }
                ?: LocaleListCompat.getEmptyLocaleList(),
        )
    }
}
