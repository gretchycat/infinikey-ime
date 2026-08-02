package com.programmerkeyboard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Transparent activity proxy for triggering system Voice Recognition via startActivityForResult.
 * Automatically checks and requests RECORD_AUDIO runtime permission when the microphone button is pressed.
 */
class VoiceInputActivity : Activity() {

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 2001
        private const val REQUEST_SPEECH_INPUT = 1001
        var onSpeechResultListener: ((String) -> Unit)? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        } else {
            startSpeechRecognition()
        }
    }

    private fun startSpeechRecognition() {
        val isContinuous = intent.getBooleanExtra("IS_CONTINUOUS", false)
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, if (isContinuous) "Dictate speech continuously..." else "Speak now...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            if (isContinuous) {
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra("android.speech.extra.DICTATION_MODE", true)
            }
        }

        try {
            startActivityForResult(speechIntent, REQUEST_SPEECH_INPUT)
        } catch (e: Exception) {
            Toast.makeText(this, "Google Voice Typing or Speech Recognizer is not installed", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSpeechRecognition()
            } else {
                Toast.makeText(this, "Microphone permission is required for voice input", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val textWithSpace = results[0] + " "
                onSpeechResultListener?.invoke(textWithSpace)
            }
        }
        finish()
    }
}
