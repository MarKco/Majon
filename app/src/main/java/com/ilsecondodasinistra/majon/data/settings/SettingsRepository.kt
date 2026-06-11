package com.ilsecondodasinistra.majon.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppLanguage(val tag: String?) {
    SYSTEM(null), ITALIAN("it"), ENGLISH("en")
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

data class AppSettings(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val haptics: Boolean = true,
    val keepScreenOn: Boolean = true,
)

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val language = stringPreferencesKey("language")
        val theme = stringPreferencesKey("theme")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val haptics = booleanPreferencesKey("haptics")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            language = prefs[Keys.language]?.let { stored ->
                AppLanguage.entries.find { it.name == stored }
            } ?: AppLanguage.SYSTEM,
            theme = prefs[Keys.theme]?.let { stored ->
                ThemeMode.entries.find { it.name == stored }
            } ?: ThemeMode.SYSTEM,
            dynamicColor = prefs[Keys.dynamicColor] ?: false,
            haptics = prefs[Keys.haptics] ?: true,
            keepScreenOn = prefs[Keys.keepScreenOn] ?: true,
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[Keys.language] = language.name }
    }

    suspend fun setTheme(theme: ThemeMode) {
        dataStore.edit { it[Keys.theme] = theme.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.dynamicColor] = enabled }
    }

    suspend fun setHaptics(enabled: Boolean) {
        dataStore.edit { it[Keys.haptics] = enabled }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        dataStore.edit { it[Keys.keepScreenOn] = enabled }
    }
}
