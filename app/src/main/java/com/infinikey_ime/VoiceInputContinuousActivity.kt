package com.infinikey_ime

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.infinikey_ime.stt.SttCallback
import com.infinikey_ime.stt.SttEngine
import com.infinikey_ime.stt.SttEngineFactory

/**
 * Translucent activity proxy for continuous real-time voice recognition.
 * Designed for mobile layouts. Uses unified SttEngine backend (Android System, Sherpa-onnx, Whisper, Cloud).
 */
class VoiceInputContinuousActivity : Activity(), SttCallback {

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 3001
        private const val REQUEST_SPEECH_INPUT = 3002
        var onContinuousSpeechResultListener: ((String) -> Unit)? = null
    }

    private var sttEngine: SttEngine? = null
    private var isListening = false
    private lateinit var tvStatus: TextView
    private lateinit var tvPreview: TextView
    private lateinit var btnStop: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            setBackgroundColor(0xCC0F172A.toInt())
        }

        tvStatus = TextView(this).apply {
            text = "🎙 Mobile Voice Dictation"
            setTextColor(0xFF38BDF8.toInt())
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        rootLayout.addView(tvStatus)

        tvPreview = TextView(this).apply {
            text = "Listening... Speak your text..."
            setTextColor(0xFFE2E8F0.toInt())
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        rootLayout.addView(tvPreview)

        btnStop = Button(this).apply {
            text = "⏹ Stop / Done"
            setBackgroundColor(0xFFDC2626.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
            setOnClickListener {
                stopListening()
                finish()
            }
        }
        rootLayout.addView(btnStop, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        setContentView(rootLayout)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        } else {
            startDictation()
        }
    }

    private fun startDictation() {
        sttEngine = SttEngineFactory.getActiveEngine(this)
        try {
            isListening = true
            tvStatus.text = "🎙 Listening (${sttEngine?.engineName})..."
            sttEngine?.startListening(this)
        } catch (e: Exception) {
            tvStatus.text = "Failed to start ${sttEngine?.engineName}: ${e.message}"
        }
    }

    private fun stopListening() {
        isListening = false
        try {
            sttEngine?.stopListening()
            sttEngine?.destroy()
        } catch (_: Exception) {}
        sttEngine = null
    }

    // SttCallback Implementation
    override fun onReadyForSpeech() {
        tvStatus.text = "🎙 Listening continuously (${sttEngine?.engineName})..."
    }

    override fun onBeginningOfSpeech() {
        tvStatus.text = "🗣 Recording speech..."
    }

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onPartialResult(text: String) {
        val isCapsLock = intent.getBooleanExtra("IS_CAPS_LOCK_ACTIVE", false)
        val partialText = if (isCapsLock) text.uppercase() else text
        tvPreview.text = partialText
    }

    override fun onFinalResult(text: String) {
        val isCapsLock = intent.getBooleanExtra("IS_CAPS_LOCK_ACTIVE", false)
        val rawText = text + " "
        val recognizedText = if (isCapsLock) rawText.uppercase() else rawText
        tvPreview.text = recognizedText
        onContinuousSpeechResultListener?.invoke(recognizedText)
    }

    override fun onError(errorMessage: String) {
        tvStatus.text = errorMessage
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startDictation()
            } else {
                Toast.makeText(this, "Microphone permission required for voice input", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val isCapsLock = intent.getBooleanExtra("IS_CAPS_LOCK_ACTIVE", false)
                val rawText = results[0] + " "
                val text = if (isCapsLock) rawText.uppercase() else rawText
                onContinuousSpeechResultListener?.invoke(text)
            }
        }
        finish()
    }

    override fun onDestroy() {
        stopListening()
        super.onDestroy()
    }
}
