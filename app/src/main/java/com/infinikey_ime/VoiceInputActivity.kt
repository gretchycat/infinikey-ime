package com.infinikey_ime

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
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)

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
        val prefs = getSharedPreferences("programmer_keyboard_prefs", MODE_PRIVATE)
        val shouldMaskProfanity = prefs.getBoolean("pref_stt_filter_profanity", false)
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra("android.speech.extra.MASK_OFFENSIVE_WORDS", shouldMaskProfanity)
            putExtra("mask_offensive_words", shouldMaskProfanity)
            putExtra("android.speech.extras.SPEECH_INPUT_MASK_OFFENSIVE_WORDS", shouldMaskProfanity)
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
                val processed = com.infinikey_ime.stt.ProfanityFilter.processText(this, results[0])
                val isCapsLock = intent.getBooleanExtra("IS_CAPS_LOCK_ACTIVE", false)
                val rawText = processed + " "
                val textWithSpace = if (isCapsLock) rawText.uppercase() else rawText
                onSpeechResultListener?.invoke(textWithSpace)
            }
        }
        finish()
    }
}
