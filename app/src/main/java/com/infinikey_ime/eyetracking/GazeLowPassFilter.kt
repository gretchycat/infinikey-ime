package com.infinikey_ime.eyetracking

import kotlin.math.hypot

/**
 * Adaptive Low-Pass Filter for Eye-Gaze Tracking.
 * 
 * Features:
 * - Dynamically adjusts smoothing factor (alpha) based on eye velocity:
 *   - Low velocity (staring/reading): Heavy low-pass filtering to eliminate micro-saccades and twitching.
 *   - High velocity (saccade across screen): High alpha for zero-latency response.
 * - Freeze/Hold state during blinks to prevent eyelid closure distortions.
 */
class GazeLowPassFilter(
    private var minAlpha: Float = 0.08f, // Heavy smoothing when eye is still
    private var maxAlpha: Float = 0.65f  // Fast response during rapid eye movement
) {
    private var filteredX = 0f
    private var filteredY = 0f
    private var isInitialized = false

    fun filter(rawX: Float, rawY: Float, isBlinking: Boolean): Pair<Float, Float> {
        // 1. If user is blinking, hold the last valid position
        if (isBlinking) {
            return Pair(filteredX, filteredY)
        }

        if (!isInitialized) {
            filteredX = rawX
            filteredY = rawY
            isInitialized = true
            return Pair(filteredX, filteredY)
        }

        // 2. Calculate distance moved since last frame
        val deltaX = rawX - filteredX
        val deltaY = rawY - filteredY
        val distance = hypot(deltaX.toDouble(), deltaY.toDouble()).toFloat()

        // 3. Dynamically compute alpha based on movement speed
        val speedThreshold = 40f
        val alpha = (distance / speedThreshold).coerceIn(minAlpha, maxAlpha)

        // 4. Apply Exponential Low-Pass Filter formula
        filteredX += alpha * (rawX - filteredX)
        filteredY += alpha * (rawY - filteredY)

        return Pair(filteredX, filteredY)
    }

    fun reset() {
        isInitialized = false
    }
}
