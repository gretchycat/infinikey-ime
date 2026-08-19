package com.infinikey_ime.stt

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.infinikey_ime.util.SttArchiveUnpacker
import java.io.File

/**
 * Sherpa-onnx Offline Streaming Speech-to-Text Engine.
 * Uses unified PcmAudioRecorder for dynamic silence detection and PCM recording.
 */
class SherpaOnnxSttEngine(private val context: Context) : SttEngine {

    companion object {
        private const val TAG = "SherpaSttEngine"

        fun findOnnxFilesRecursively(baseDir: File): List<File> {
            if (!baseDir.exists()) return emptyList()
            val result = mutableListOf<File>()
            try {
                baseDir.walkTopDown()
                    .onFail { file, exc ->
                        Log.w(TAG, "Skipping unreadable path ${file.name}: ${exc.message}")
                        kotlin.io.OnErrorAction.SKIP
                    }
                    .forEach { file ->
                        if (file.isFile && file.name.endsWith(".onnx", ignoreCase = true)) {
                            result.add(file)
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error walking model directory tree: ${e.message}")
            }
            return result
        }
    }

    override val engineName: String = "Sherpa-onnx (Offline)"
    override val isAvailable: Boolean
        get() {
            val prefs = context.getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
            val configuredPath = prefs.getString("pref_sherpa_onnx_path", "") ?: ""
            if (configuredPath.isNotEmpty() && File(configuredPath).exists()) {
                Log.i(TAG, "Sherpa-onnx isAvailable=true (via linked model path: $configuredPath)")
                return true
            }

            val cloudUrl = prefs.getString("pref_stt_cloud_url", "") ?: ""
            if (cloudUrl.isNotEmpty()) {
                Log.i(TAG, "Sherpa-onnx isAvailable=true (via Cloud API Endpoint: $cloudUrl)")
                return true
            }

            val modelsDir = File(context.getExternalFilesDir(null), "stt_models")
            val onnxFiles = findOnnxFilesRecursively(modelsDir)
            val available = onnxFiles.isNotEmpty()
            Log.i(TAG, "Sherpa-onnx recursive check: isAvailable=$available, foundOnnxCount=${onnxFiles.size}")
            return available
        }

    private var activeCallback: SttCallback? = null
    private val recorder = PcmAudioRecorder()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun startListening(callback: SttCallback) {
        Log.i(TAG, "startListening() invoked for Sherpa-onnx STT engine")
        destroy()
        activeCallback = callback

        Log.i(TAG, "Sherpa-onnx startListening: Starting PcmAudioRecorder...")
        recorder.start(
            callback = callback,
            onAudioFrame = { _, _ ->
                // Native JNI frame streaming
            },
            onRecordingComplete = { finalShorts ->
                val durationSec = finalShorts.size / 16000.0
                Log.i(TAG, "onRecordingComplete: recorded ${finalShorts.size} samples (~${String.format("%.2f", durationSec)}s of speech)")
                if (finalShorts.isEmpty()) {
                    Log.w(TAG, "Audio buffer is empty (0 samples)")
                    mainHandler.post { activeCallback?.onError("No speech recognized") }
                } else {
                    Log.i(TAG, "Passing ${finalShorts.size} PCM samples to AudioTranscriber...")
                    AudioTranscriber.transcribeAudio(
                        context = context,
                        pcmShorts = finalShorts,
                        engineName = engineName,
                        onResult = { text ->
                            Log.i(TAG, "AudioTranscriber result received for Sherpa: '$text'")
                            emitResult(text, isFinal = true)
                        },
                        onError = { errorMsg ->
                            Log.e(TAG, "AudioTranscriber error for Sherpa: '$errorMsg'")
                            mainHandler.post { activeCallback?.onError(errorMsg) }
                        }
                    )
                }
            }
        )
    }

    override fun stopListening() {
        Log.d(TAG, "stopListening() called")
        recorder.stop()
    }

    override fun destroy() {
        Log.d(TAG, "destroy() called")
        stopListening()
        activeCallback = null
    }

    fun emitResult(text: String, isFinal: Boolean) {
        val processed = ProfanityFilter.processText(context, text)
        Log.i(TAG, "emitResult(raw='$text', processed='$processed', isFinal=$isFinal)")
        mainHandler.post {
            if (isFinal) {
                activeCallback?.onFinalResult(processed)
            } else {
                activeCallback?.onPartialResult(processed)
            }
        }
    }
}
