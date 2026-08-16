package com.infinikey_ime.stt

import android.content.Context

/**
 * Factory class to instantiate the configured phrase-by-phrase Speech-to-Text engine.
 * Reads the engine selection preference and returns a unified SttEngine instance.
 */
object SttEngineFactory {

    fun getActiveEngine(context: Context): SttEngine {
        val prefs = context.getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        val selectedKey = prefs.getString("pref_stt_engine", "ANDROID_SYSTEM") ?: "ANDROID_SYSTEM"
        val selectedModel = prefs.getString("pref_stt_model", "SYSTEM_BUILTIN") ?: "SYSTEM_BUILTIN"
        val filterProfanity = prefs.getBoolean("pref_stt_filter_profanity", false)
        android.util.Log.d("STT_DEBUG", "SttEngineFactory: engineKey=$selectedKey, modelKey=$selectedModel, filterProfanity=$filterProfanity")
        return createEngine(context, selectedKey)
    }

    fun createEngine(context: Context, engineKey: String): SttEngine {
        return when (engineKey) {
            "SHERPA_ONNX" -> SherpaOnnxSttEngine(context)
            "WHISPER_CPP" -> WhisperSttEngine(context)
            "CLOUD_API" -> CloudApiSttEngine(context)
            else -> AndroidSystemSttEngine(context)
        }
    }
}
