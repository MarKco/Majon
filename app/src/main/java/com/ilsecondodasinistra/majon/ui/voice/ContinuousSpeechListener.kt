package com.ilsecondodasinistra.majon.ui.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/** Why listening stopped for good; transient errors are retried silently. */
enum class SpeechFailure { UNAVAILABLE, PERMISSION, LANGUAGE, TOO_MANY_ERRORS }

/**
 * Keeps Android's [SpeechRecognizer] listening by restarting it after every result, silence
 * or transient error. Must be created and used on the main thread.
 *
 * It prefers the on-device recognizer (Android 12+), which works offline.
 * [onHeard] gets the recognizer's alternatives, best first.
 */
class ContinuousSpeechListener(
    private val context: Context,
    private val languageTag: String,
    private val onHeard: (List<String>) -> Unit,
    private val onFailure: (SpeechFailure) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var active = false
    private var consecutiveErrors = 0

    /** Cleared when the on-device recognizer lacks the language, to fall back to the system one. */
    private var allowOnDevice = true
    private var usingOnDevice = false

    fun start() {
        if (active) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onFailure(SpeechFailure.UNAVAILABLE)
            return
        }
        active = true
        consecutiveErrors = 0
        allowOnDevice = true
        listen()
    }

    fun stop() {
        active = false
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        recognizer = null
    }

    private fun newRecognizer(): SpeechRecognizer {
        val onDevice = allowOnDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        usingOnDevice = onDevice
        val created = if (onDevice) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }
        created.setRecognitionListener(listener)
        return created
    }

    private fun listen() {
        if (!active) return
        val current = recognizer ?: newRecognizer().also { recognizer = it }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            // Only on-device is offline; the fallback must be free to use the network
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, usingOnDevice)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            // Commands are one or two words: do not wait long for more
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 700L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 500L)
        }
        current.startListening(intent)
    }

    private fun restart(delayMillis: Long, recreate: Boolean = false) {
        if (!active) return
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            if (recreate) {
                recognizer?.destroy()
                recognizer = null
            }
            listen()
        }, delayMillis)
    }

    private val listener = object : RecognitionListener {
        override fun onResults(results: Bundle?) {
            consecutiveErrors = 0
            val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            if (heard.isNotEmpty()) onHeard(heard)
            restart(120)
        }

        override fun onError(error: Int) {
            when (error) {
                // Nothing said: the normal idle case, just listen again
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> restart(120)
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> fail(SpeechFailure.PERMISSION)
                SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> {
                    if (allowOnDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        // No offline pack for this language: try the system recognizer instead
                        allowOnDevice = false
                        restart(200, recreate = true)
                    } else {
                        fail(SpeechFailure.LANGUAGE)
                    }
                }
                else -> {
                    consecutiveErrors++
                    if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                        fail(SpeechFailure.TOO_MANY_ERRORS)
                    } else {
                        // Busy, client or audio hiccup: start from a fresh recognizer after a pause
                        restart(600L * consecutiveErrors, recreate = true)
                    }
                }
            }
        }

        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onPartialResults(partialResults: Bundle?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun fail(reason: SpeechFailure) {
        stop()
        onFailure(reason)
    }

    private companion object {
        const val MAX_CONSECUTIVE_ERRORS = 5
    }
}
