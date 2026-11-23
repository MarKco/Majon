package com.ilsecondodasinistra.majon

import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import com.ilsecondodasinistra.majon.ui.importer.ImportFiles
import com.ilsecondodasinistra.majon.data.export.ImportInbox
import androidx.lifecycle.lifecycleScope
import android.content.Intent
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

    @Inject lateinit var importInbox: ImportInbox

    /** A .majon or .majon-notes file opened from a file manager, a chat or an email waits for the import screen. */
    private fun handleIncoming(intent: Intent?) {
        val uri = intent?.data ?: return
        if (intent.action != Intent.ACTION_VIEW) return
        lifecycleScope.launch {
            val text = withContext(Dispatchers.IO) { ImportFiles.readText(this@MainActivity, uri) }
            if (text != null) importInbox.offer(text)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncoming(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Not again after a rotation: the file was already taken the first time
        if (savedInstanceState == null) handleIncoming(intent)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            MajonTheme(
                themeMode = settings.theme,
                palette = settings.palette,
                dynamicColor = settings.dynamicColor,
            ) {
                CompositionLocalProvider(LocalWavyProgress provides settings.wavyProgress) {
                    MajonNavGraph(navController = rememberNavController())
                }
            }
        }
    }
}
