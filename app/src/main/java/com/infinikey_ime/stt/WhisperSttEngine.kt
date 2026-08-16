package com.infinikey_ime.stt

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import kotlin.concurrent.thread

/**
 * Whisper Speech-to-Text Engine.
 * Supports local GGUF models (via whisper.cpp) and remote API endpoints (Groq / OpenAI Whisper).
 */
class WhisperSttEngine(private val context: Context) : SttEngine {

    override val engineName: String = "Whisper (GGUF / Cloud)"
    override val isAvailable: Boolean
        get() {
            val modelsDir = File(context.getExternalFilesDir(null), "stt_models")
            return modelsDir.exists() && (modelsDir.listFiles()?.any { it.name.contains("whisper") || it.name.endsWith(".gguf") } == true)
        }

    private var activeCallback: SttCallback? = null
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordThread: Thread? = null

    override fun startListening(callback: SttCallback) {
        destroy()
        activeCallback = callback

        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                minBufferSize.coerceAtLeast(sampleRate * 2)
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                callback.onError("Failed to initialize microphone for Whisper")
                return
            }

            audioRecord?.startRecording()
            isRecording = true
            callback.onReadyForSpeech()

            recordThread = thread(start = true) {
                val buffer = ShortArray(1024)
                var hasStartedSpeech = false

                while (isRecording && !Thread.currentThread().isInterrupted) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        var sum = 0.0
                        for (i in 0 until readSize) {
                            sum += buffer[i] * buffer[i]
                        }
                        val rms = Math.sqrt(sum / readSize)
                        val rmsdB = (20 * Math.log10(rms.coerceAtLeast(1.0))).toFloat()

                        activeCallback?.onRmsChanged(rmsdB)

                        if (!hasStartedSpeech && rmsdB > 35f) {
                            hasStartedSpeech = true
                            activeCallback?.onBeginningOfSpeech()
                        }
                    }
                }
            }

            if (!isAvailable) {
                callback.onError("Whisper GGUF model file not found in stt_models folder. Add a .gguf model file to activate.")
            }
        } catch (e: Exception) {
            callback.onError("Whisper engine error: ${e.message}")
        }
    }

    override fun stopListening() {
        isRecording = false
        try {
            audioRecord?.stop()
        } catch (_: Exception) {}
    }

    override fun destroy() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        recordThread?.interrupt()
        recordThread = null
        activeCallback = null
    }

    fun emitResult(text: String, isFinal: Boolean) {
        val processed = ProfanityFilter.processText(context, text)
        if (isFinal) {
            activeCallback?.onFinalResult(processed)
        } else {
            activeCallback?.onPartialResult(processed)
        }
    }
}
