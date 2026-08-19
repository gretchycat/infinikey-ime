package com.infinikey_ime.stt

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.File

/**
 * Whisper.cpp GGUF Offline Speech-to-Text Engine.
 * Uses unified PcmAudioRecorder for dynamic silence detection and PCM recording.
 */
class WhisperSttEngine(private val context: Context) : SttEngine {

    override val engineName: String = "Whisper (GGUF / Cloud)"
    override val isAvailable: Boolean
        get() {
            val modelsDir = File(context.getExternalFilesDir(null), "stt_models")
            if (!modelsDir.exists()) return false
            return modelsDir.walkTopDown().any { file ->
                file.isFile && (file.name.endsWith(".gguf", ignoreCase = true) || file.name.endsWith(".bin", ignoreCase = true))
            }
        }

    private var activeCallback: SttCallback? = null
    private val recorder = PcmAudioRecorder()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun startListening(callback: SttCallback) {
        destroy()
        activeCallback = callback

        if (!isAvailable) {
            mainHandler.post { 
                callback.onError("Whisper GGUF model file (.gguf / .bin) missing in stt_models directory. Tap 'Download STT Models' in settings.") 
            }
            return
        }

        recorder.start(
            callback = callback,
            onAudioFrame = { pcmShorts, readSize ->
                // Native Whisper.cpp frame processing
            },
            onRecordingComplete = { finalShorts ->
                if (finalShorts.isEmpty()) {
                    mainHandler.post { activeCallback?.onError("No speech recognized") }
                } else {
                    AudioTranscriber.transcribeAudio(
                        context = context,
                        pcmShorts = finalShorts,
                        engineName = engineName,
                        onResult = { text -> emitResult(text, isFinal = true) },
                        onError = { errorMsg -> mainHandler.post { activeCallback?.onError(errorMsg) } }
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

    fun emitResult(text: String, isFinal: Boolean) {
        val processed = ProfanityFilter.processText(context, text)
        mainHandler.post {
            if (isFinal) {
                activeCallback?.onFinalResult(processed)
            } else {
                activeCallback?.onPartialResult(processed)
            }
        }
    }
}
