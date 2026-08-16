package com.infinikey_ime.stt

/**
 * Unified callback interface for phrase-by-phrase Speech-to-Text engines.
 * Consumed by VoiceInputOverlay and VoiceInputContinuousActivity.
 */
interface SttCallback {
    fun onReadyForSpeech()
    fun onBeginningOfSpeech()
    fun onRmsChanged(rmsdB: Float)
    fun onPartialResult(text: String)
    fun onFinalResult(text: String)
    fun onError(errorMessage: String)
}
