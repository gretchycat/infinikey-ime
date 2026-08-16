package com.infinikey_ime.stt

/**
 * Common interface for all Speech-to-Text engines (Android System, Sherpa-onnx, Whisper, Cloud API).
 * Keeps the phrase-by-phrase listener UI identical regardless of backend engine.
 */
interface SttEngine {
    val engineName: String
    val isAvailable: Boolean

    fun startListening(callback: SttCallback)
    fun stopListening()
    fun destroy()
}
