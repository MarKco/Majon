package com.ilsecondodasinistra.majon

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.ilsecondodasinistra.majon.ui.components.LocalWavyProgress
import com.ilsecondodasinistra.majon.ui.input.VolumeKeyHost
import com.ilsecondodasinistra.majon.ui.navigation.MajonNavGraph
import com.ilsecondodasinistra.majon.ui.settings.SettingsViewModel
import com.ilsecondodasinistra.majon.ui.theme.MajonTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), VolumeKeyHost {

    override var volumeKeyHandler: ((up: Boolean) -> Unit)? = null

    /** While a screen claims the volume keys, they drive the app instead of the system volume. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handler = volumeKeyHandler
        val isVolumeKey = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (handler != null && isVolumeKey) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                handler(event.keyCode == KeyEvent.KEYCODE_VOLUME_UP)
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

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
                CompositionLocalProvider(LocalWavyProgress provides settings.wavyProgress) {
                    MajonNavGraph(navController = rememberNavController())
                }
            }
        }
    }
}
