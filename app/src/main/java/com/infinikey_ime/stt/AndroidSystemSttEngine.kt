package com.infinikey_ime.stt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Robust default STT Engine using native Android SpeechRecognizer API.
 * Gracefully handles speech timeouts, silence, speech engine busy states, and partial result commits.
 */
class AndroidSystemSttEngine(private val context: Context) : SttEngine, RecognitionListener {

    private companion object {
        private const val TAG = "AndroidSystemSttEngine"
    }

    override val engineName: String = "Android System STT"
    override val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    private var speechRecognizer: SpeechRecognizer? = null
    private var activeCallback: SttCallback? = null
    private var isListening = false
    private var lastPartialText: String = ""
    private var isRetryPending = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun startListening(callback: SttCallback) {
        activeCallback = callback
        lastPartialText = ""

        if (!isAvailable) {
            callback.onError("Speech recognition is not available on this device")
            return
        }

        if (speechRecognizer == null) {
            initializeSpeechRecognizer()
        } else {
            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {}
        }
        isListening = true
        restartListening()
    }

    private fun initializeSpeechRecognizer() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}

        speechRecognizer = try {
            SpeechRecognizer.createSpeechRecognizer(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating SpeechRecognizer: ${e.message}")
            null
        }
        speechRecognizer?.setRecognitionListener(this)
    }

    private fun restartListening() {
        if (!isListening) return
        lastPartialText = ""
        try {
            val shouldMaskProfanity = ProfanityFilter.isSystemProfanityFilterEnabled(context) == true
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra("android.speech.extra.MASK_OFFENSIVE_WORDS", shouldMaskProfanity)
                putExtra("mask_offensive_words", shouldMaskProfanity)
                putExtra("android.speech.extras.SPEECH_INPUT_MASK_OFFENSIVE_WORDS", shouldMaskProfanity)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting listening: ${e.message}")
            activeCallback?.onError(e.message ?: "Failed to start speech input")
        }
    }

    override fun stopListening() {
        isListening = false
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
    }

    override fun destroy() {
        isListening = false
        isRetryPending = false
        mainHandler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        activeCallback = null
        lastPartialText = ""
    }

    // RecognitionListener Callbacks
    override fun onReadyForSpeech(params: Bundle?) {
        activeCallback?.onReadyForSpeech()
    }

    override fun onBeginningOfSpeech() {
        activeCallback?.onBeginningOfSpeech()
    }

    override fun onRmsChanged(rmsdB: Float) {
        activeCallback?.onRmsChanged(rmsdB)
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {}

    override fun onError(error: Int) {
        Log.w(TAG, "SpeechRecognizer onError callback: $error")
        val prefs = context.getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        val isContinuous = prefs.getBoolean("pref_stt_continuous_mode", false)

        when (error) {
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                if (lastPartialText.isNotEmpty()) {
                    // Commit any pending transcribed text before ending or restarting
                    activeCallback?.onFinalResult(lastPartialText)
                    lastPartialText = ""
                }

                if (isListening && isContinuous) {
                    mainHandler.postDelayed({ restartListening() }, 300)
                } else {
                    if (lastPartialText.isEmpty()) {
                        activeCallback?.onError("No speech detected")
                    }
                    stopListening()
                }
            }

            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                if (isListening && !isRetryPending) {
                    isRetryPending = true
                    initializeSpeechRecognizer()
                    mainHandler.postDelayed({
                        isRetryPending = false
                        restartListening()
                    }, 400)
                } else {
                    activeCallback?.onError("Speech recognizer busy, please try again")
                    stopListening()
                }
            }

            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            SpeechRecognizer.ERROR_NETWORK -> {
                if (isListening && isContinuous) {
                    mainHandler.postDelayed({ restartListening() }, 1000)
                } else {
                    activeCallback?.onError(getHumanReadableErrorMessage(error))
                    stopListening()
                }
            }

            SpeechRecognizer.ERROR_CLIENT -> {
                // Re-initialize recognizer on client error
                initializeSpeechRecognizer()
                if (isListening && isContinuous) {
                    mainHandler.postDelayed({ restartListening() }, 500)
                } else {
                    activeCallback?.onError("Speech engine error, please tap microphone to try again")
                    stopListening()
                }
            }

            else -> {
                val errorMsg = getHumanReadableErrorMessage(error)
                activeCallback?.onError(errorMsg)
                stopListening()
            }
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val text = ProfanityFilter.processText(context, matches[0])
            activeCallback?.onFinalResult(text)
            lastPartialText = ""
        } else if (lastPartialText.isNotEmpty()) {
            activeCallback?.onFinalResult(lastPartialText)
            lastPartialText = ""
        }

        val prefs = context.getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        val isContinuous = prefs.getBoolean("pref_stt_continuous_mode", false)
        if (isListening && isContinuous) {
            mainHandler.postDelayed({ restartListening() }, 200)
        } else {
            stopListening()
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val text = ProfanityFilter.processText(context, matches[0])
            lastPartialText = text
            activeCallback?.onPartialResult(text)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun getHumanReadableErrorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Speech engine error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
            SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network connection timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy"
            SpeechRecognizer.ERROR_SERVER -> "Speech server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Speech server disconnected"
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language not supported"
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Language currently unavailable"
            else -> "Speech recognition error ($error)"
        }
    }
}
