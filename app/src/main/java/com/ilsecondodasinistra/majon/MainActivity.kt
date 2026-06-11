package com.ilsecondodasinistra.majon

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.ilsecondodasinistra.majon.ui.navigation.MajonNavGraph
import com.ilsecondodasinistra.majon.ui.settings.SettingsViewModel
import com.ilsecondodasinistra.majon.ui.theme.MajonTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            MajonTheme(
                themeMode = settings.theme,
                dynamicColor = settings.dynamicColor,
            ) {
                MajonNavGraph(navController = rememberNavController())
            }
        }
    }
}
