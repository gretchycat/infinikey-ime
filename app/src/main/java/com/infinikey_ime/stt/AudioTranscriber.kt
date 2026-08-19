package com.infinikey_ime.stt

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread

/**
 * Utility helper to convert recorded PCM 16kHz Mono audio buffers into valid WAV format
 * and transmit audio to STT REST endpoints (Groq / OpenAI / Vosk / Whisper Cloud).
 */
object AudioTranscriber {

    private const val TAG = "AudioTranscriber"

    fun pcmToWav(pcmShorts: ShortArray, sampleRate: Int = 16000): ByteArray {
        val pcmByteCount = pcmShorts.size * 2
        val wavByteCount = pcmByteCount + 44
        val buffer = ByteBuffer.allocate(wavByteCount)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(pcmByteCount + 36)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))

        // fmt subchunk
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16) // Subchunk1Size for PCM
        buffer.putShort(1.toShort()) // AudioFormat (1 = PCM)
        buffer.putShort(1.toShort()) // NumChannels (1 = Mono)
        buffer.putInt(sampleRate) // SampleRate (16000)
        buffer.putInt(sampleRate * 2) // ByteRate (SampleRate * NumChannels * BitsPerSample / 8)
        buffer.putShort(2.toShort()) // BlockAlign (NumChannels * BitsPerSample / 8)
        buffer.putShort(16.toShort()) // BitsPerSample (16)

        // data subchunk
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(pcmByteCount)

        for (sample in pcmShorts) {
            buffer.putShort(sample)
        }

        return buffer.array()
    }

    fun transcribeAudio(
        context: Context,
        pcmShorts: ShortArray,
        engineName: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val durationSec = pcmShorts.size / 16000.0
        Log.i(TAG, "transcribeAudio() called for engine: '$engineName' | PCM samples: ${pcmShorts.size} (~${String.format("%.2f", durationSec)}s)")

        if (pcmShorts.isEmpty()) {
            Log.w(TAG, "transcribeAudio failed: PCM audio array is empty")
            onError("No audio recorded")
            return
        }

        val prefs = context.getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        val cloudUrl = prefs.getString("pref_stt_cloud_url", "") ?: ""
        val apiKey = prefs.getString("pref_stt_cloud_api_key", "") ?: ""
        Log.d(TAG, "AudioTranscriber configuration: cloudUrl='$cloudUrl', apiKeyLength=${apiKey.length}")

        if (cloudUrl.isNotEmpty()) {
            thread {
                try {
                    Log.i(TAG, "Converting ${pcmShorts.size} PCM samples to WAV binary data...")
                    val wavBytes = pcmToWav(pcmShorts)
                    Log.i(TAG, "WAV generated: ${wavBytes.size} bytes. Transmitting via HTTP POST to '$cloudUrl'...")

                    val resultText = sendWavToRestApi(cloudUrl, apiKey, wavBytes)
                    Log.i(TAG, "REST API response text: '$resultText'")

                    if (!resultText.isNullOrBlank()) {
                        val processed = ProfanityFilter.processText(context, resultText)
                        Log.i(TAG, "Profanity-filtered transcription: '$processed'. Delivering result to $engineName...")
                        onResult(processed)
                    } else {
                        val errorMsg = "No text returned from $engineName endpoint"
                        Log.e(TAG, errorMsg)
                        onError(errorMsg)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Audio transcription exception ($engineName): ${e.message}", e)
                    onError("Transcription error ($engineName): ${e.message}")
                }
            }
        } else {
            val errorMsg = "$engineName endpoint URL not configured. Tap Settings -> Speech to Text to set API endpoint."
            Log.e(TAG, errorMsg)
            onError(errorMsg)
        }
    }

    private fun sendWavToRestApi(cloudUrl: String, apiKey: String, wavBytes: ByteArray): String? {
        val url = URL(cloudUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 10000
        conn.readTimeout = 15000

        if (apiKey.isNotEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
        }

        val boundary = "---InfinikeyAudioBoundary" + System.currentTimeMillis()
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

        val os = conn.outputStream
        val writer = PrintWriter(OutputStreamWriter(os, Charsets.UTF_8), true)

        // model parameter
        writer.append("--$boundary\r\n")
        writer.append("Content-Disposition: form-data; name=\"model\"\r\n\r\n")
        writer.append("whisper-1\r\n")
        writer.flush()

        // audio file parameter
        writer.append("--$boundary\r\n")
        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"speech.wav\"\r\n")
        writer.append("Content-Type: audio/wav\r\n\r\n")
        writer.flush()

        os.write(wavBytes)
        os.flush()

        writer.append("\r\n--$boundary--\r\n")
        writer.flush()
        writer.close()

        val responseCode = conn.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            return parseResponseText(responseText)
        } else {
            val errorText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
            Log.e(TAG, "Server error response ($responseCode): $errorText")
            return null
        }
    }

    private fun parseResponseText(responseText: String): String {
        return try {
            val json = JSONObject(responseText)
            when {
                json.has("text") -> json.getString("text").trim()
                json.has("transcript") -> json.getString("transcript").trim()
                json.has("result") -> json.getString("result").trim()
                else -> responseText.trim()
            }
        } catch (_: Exception) {
            responseText.trim()
        }
    }
}
