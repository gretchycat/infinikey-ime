package com.programmerkeyboard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Translucent activity proxy for continuous real-time voice recognition.
 * Designed for mobile layouts. Tries native SpeechRecognizer first, falling back to Google Voice Typing / System Voice Intent.
 */
class VoiceInputContinuousActivity : Activity() {

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 3001
        private const val REQUEST_SPEECH_INPUT = 3002
        var onContinuousSpeechResultListener: ((String) -> Unit)? = null
    }

    private var speechRecognizer: SpeechRecognizer? = null
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
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            try {
                startSpeechRecognizerEngine()
                return
            } catch (_: Exception) {}
        }
        launchSystemVoiceIntent()
    }

    private fun startSpeechRecognizerEngine() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    tvStatus.text = "🎙 Listening continuously..."
                }

                override fun onBeginningOfSpeech() {
                    tvStatus.text = "🗣 Recording speech..."
                }

                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    if (isListening) {
                        tvStatus.text = "Processing speech..."
                    }
                }

                override fun onError(error: Int) {
                    if (isListening && (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                        restartListening()
                    } else {
                        // Fallback to System Voice Intent if engine errors
                        launchSystemVoiceIntent()
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val isCapsLock = intent.getBooleanExtra("IS_CAPS_LOCK_ACTIVE", false)
                        val rawText = matches[0] + " "
                        val recognizedText = if (isCapsLock) rawText.uppercase() else rawText
                        tvPreview.text = recognizedText
                        onContinuousSpeechResultListener?.invoke(recognizedText)
                    }
                    if (isListening) {
                        restartListening()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val isCapsLock = intent.getBooleanExtra("IS_CAPS_LOCK_ACTIVE", false)
                        val partialText = if (isCapsLock) matches[0].uppercase() else matches[0]
                        tvPreview.text = partialText
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        isListening = true
        restartListening()
    }

    private fun restartListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun launchSystemVoiceIntent() {
        stopListening()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to dictate text...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            startActivityForResult(intent, REQUEST_SPEECH_INPUT)
        } catch (e: Exception) {
            Toast.makeText(this, "Google Voice Typing or Speech Recognizer is required for voice input", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun stopListening() {
        isListening = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
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
