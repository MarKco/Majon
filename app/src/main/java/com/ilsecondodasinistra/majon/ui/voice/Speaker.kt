package com.ilsecondodasinistra.majon.ui.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** Thin wrapper over [TextToSpeech] that queues nothing until the engine is ready. */
class Speaker(context: Context, private val locale: Locale) {
    private var ready = false
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val result = engine.setLanguage(locale)
                ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    /** Speaks [text], replacing whatever is being said. Silently ignored if TTS is unavailable. */
    fun speak(text: String) {
        if (ready) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "majon")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}

/** A [Speaker] tied to the composition, in the current app language; null when [enabled] is false. */
@Composable
fun rememberSpeaker(enabled: Boolean): Speaker? {
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    val speaker = remember(enabled, locale) { if (enabled) Speaker(context, locale) else null }
    DisposableEffect(speaker) { onDispose { speaker?.shutdown() } }
    return speaker
}
