package com.infinikey_ime.engine

import android.os.Handler
import android.os.Looper

/**
 * Engine responsible for key-repeat acceleration, long-press detection,
 * and the Analog Arrow Joystick mode.
 */
class KeyRepeatEngine(private val onRepeatCallback: (Int) -> Unit) {

    private val handler = Handler(Looper.getMainLooper())
    private var isRepeating = false
    private var currentKeyCode: Int = 0
    private var repeatDelayMs: Long = INITIAL_DELAY_MS

    private val repeatRunnable = object : Runnable {
        override fun run() {
            if (isRepeating) {
                onRepeatCallback(currentKeyCode)
                handler.postDelayed(this, repeatDelayMs)
            }
        }
    }

    fun startRepeat(keyCode: Int) {
        stopRepeat()
        currentKeyCode = keyCode
        isRepeating = true
        repeatDelayMs = INITIAL_DELAY_MS
        handler.postDelayed(repeatRunnable, repeatDelayMs)
    }

    fun updateKeyCode(keyCode: Int) {
        currentKeyCode = keyCode
    }

    fun updateAnalogRate(displacementDistancePx: Float, maxDistancePx: Float) {
        val normalized = (displacementDistancePx / maxDistancePx).coerceIn(0.1f, 1.0f)
        // Interpolate repeat delay from INITIAL_DELAY_MS down to FASTEST_DELAY_MS proportionally
        repeatDelayMs = (INITIAL_DELAY_MS - (normalized * (INITIAL_DELAY_MS - FASTEST_DELAY_MS))).toLong()
    }

    fun stopRepeat() {
        isRepeating = false
        handler.removeCallbacks(repeatRunnable)
    }

    companion object {
        private const val INITIAL_DELAY_MS = 400L
        private const val FASTEST_DELAY_MS = 30L
    }
}
