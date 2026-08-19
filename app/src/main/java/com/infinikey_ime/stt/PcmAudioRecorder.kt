package com.infinikey_ime.stt

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.ArrayDeque
import kotlin.concurrent.thread

/**
 * Unified PCM 16kHz Mono Audio Recorder for Infinikey IME STT Engines.
 * Features sliding window average silence detection and 4.5s hard phrase limit.
 */
class PcmAudioRecorder {

    companion object {
        private const val TAG = "PcmAudioRecorder"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val WINDOW_SIZE = 18 // ~1150ms at 64ms/frame
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordThread: Thread? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun start(
        callback: SttCallback,
        onAudioFrame: ((ShortArray, Int) -> Unit)? = null,
        onRecordingComplete: (ShortArray) -> Unit
    ) {
        stop()

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBufferSize <= 0) {
            mainHandler.post { callback.onError("Invalid audio record buffer size") }
            return
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                minBufferSize.coerceAtLeast(SAMPLE_RATE * 2)
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                mainHandler.post { callback.onError("Failed to initialize microphone audio stream") }
                return
            }

            audioRecord?.startRecording()
            isRecording = true
            mainHandler.post { callback.onReadyForSpeech() }

            recordThread = thread(start = true) {
                val buffer = ShortArray(1024)
                val recordedShorts = ArrayList<Short>()
                val recentRmsQueue = ArrayDeque<Float>()
                var hasStartedSpeech = false
                var silenceFrameCount = 0
                val startTime = System.currentTimeMillis()

                var noiseFloorRmsDb = 1.5f

                while (isRecording && !Thread.currentThread().isInterrupted) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        for (i in 0 until readSize) {
                            recordedShorts.add(buffer[i])
                        }
                        onAudioFrame?.invoke(buffer, readSize)

                        var sum = 0.0
                        for (i in 0 until readSize) {
                            val sample = buffer[i].toDouble()
                            sum += sample * sample
                        }
                        val rms = Math.sqrt(sum / readSize)
                        val rmsdB = (20 * Math.log10(rms.coerceAtLeast(1.0))).toFloat()
                        val currentRmsDb = ((rmsdB - 30f) / 4.0f).coerceIn(0f, 10f)

                        recentRmsQueue.addLast(currentRmsDb)
                        if (recentRmsQueue.size > WINDOW_SIZE) {
                            recentRmsQueue.removeFirst()
                        }

                        val avgRmsInWindow = recentRmsQueue.average().toFloat()

                        // Dynamic noise floor tracking: only track ambient noise when not in active speech
                        if (currentRmsDb < noiseFloorRmsDb) {
                            noiseFloorRmsDb = noiseFloorRmsDb * 0.7f + currentRmsDb * 0.3f
                        } else if (currentRmsDb < noiseFloorRmsDb + 1.2f) {
                            noiseFloorRmsDb = noiseFloorRmsDb * 0.98f + currentRmsDb * 0.02f
                        }

                        val silenceCutoff = (noiseFloorRmsDb + 1.0f).coerceIn(1.2f, 7.0f)
                        val speechThreshold = (noiseFloorRmsDb + 1.8f).coerceIn(2.0f, 8.0f)

                        mainHandler.post { callback.onRmsChanged(currentRmsDb) }

                        val elapsed = System.currentTimeMillis() - startTime

                        if (!hasStartedSpeech) {
                            if (currentRmsDb >= speechThreshold) {
                                hasStartedSpeech = true
                                silenceFrameCount = 0
                                mainHandler.post { callback.onBeginningOfSpeech() }
                            } else if (elapsed >= 5000L) {
                                // 5.0s initial timeout if user never spoke (matches system recognizer)
                                Log.d(TAG, "No speech detected within 5.0s initial timeout -> Stopping recording")
                                break
                            }
                        } else {
                            // Speech has started: check for post-speech silence (~1.2s matching system speech engine)
                            if (currentRmsDb <= silenceCutoff || avgRmsInWindow <= silenceCutoff) {
                                silenceFrameCount++
                                if (silenceFrameCount >= 19 || (silenceFrameCount >= 15 && avgRmsInWindow <= silenceCutoff)) {
                                    Log.d(TAG, "Post-speech silence detected (silenceFrames=$silenceFrameCount, windowAvg=$avgRmsInWindow, floor=$noiseFloorRmsDb, cutoff=$silenceCutoff) -> Stopping recording")
                                    break
                                }
                            } else {
                                silenceFrameCount = 0
                            }
                        }

                        // Hard Maximum Phrase Limit (10.0 seconds)
                        if (elapsed >= 10000L) {
                            Log.d(TAG, "Phrase hard limit 10.0s reached -> Stopping recording")
                            break
                        }
                    }
                }

                val finalShorts = ShortArray(recordedShorts.size) { recordedShorts[it] }
                isRecording = false
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (_: Exception) {}
                audioRecord = null

                if (finalShorts.isNotEmpty()) {
                    onRecordingComplete(finalShorts)
                }
            }
        } catch (e: Exception) {
            mainHandler.post { callback.onError("Microphone recording error: ${e.message}") }
        }
    }

    fun stop() {
        isRecording = false
        recordThread?.interrupt()
        recordThread = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }
}
