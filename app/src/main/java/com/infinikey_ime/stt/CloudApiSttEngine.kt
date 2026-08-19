package com.infinikey_ime.stt

import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * Cloud API Speech-to-Text Engine (REST / WebSocket endpoints like Groq / OpenAI / Vosk Cloud).
 * Uses unified PcmAudioRecorder for dynamic silence detection and PCM recording.
 */
class CloudApiSttEngine(private val context: Context) : SttEngine {

    override val engineName: String = "Cloud API (Groq / OpenAI / Vosk)"
    override val isAvailable: Boolean
        get() {
            val prefs = context.getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
            return (prefs.getString("pref_stt_cloud_url", "") ?: "").isNotEmpty()
        }

    private var activeCallback: SttCallback? = null
    private val recorder = PcmAudioRecorder()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun startListening(callback: SttCallback) {
        destroy()
        activeCallback = callback

        val prefs = context.getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        val cloudUrl = prefs.getString("pref_stt_cloud_url", "") ?: ""

        if (cloudUrl.isEmpty()) {
            mainHandler.post { 
                callback.onError("Cloud API REST URL not configured. Tap 'Configure Cloud API Endpoint' in settings.") 
            }
            return
        }

        recorder.start(
            callback = callback,
            onAudioFrame = null,
            onRecordingComplete = { finalShorts ->
                if (finalShorts.isEmpty()) {
                    mainHandler.post { activeCallback?.onError("No speech recognized") }
                } else {
                    AudioTranscriber.transcribeAudio(
                        context = context,
                        pcmShorts = finalShorts,
                        engineName = engineName,
                        onResult = { text ->
                            mainHandler.post { activeCallback?.onFinalResult(text) }
                        },
                        onError = { errorMsg ->
                            mainHandler.post { activeCallback?.onError(errorMsg) }
                        }
                    )
                }
            }
        )
    }

    override fun stopListening() {
        recorder.stop()
    }

    override fun destroy() {
        stopListening()
        activeCallback = null
    }
}
