package com.programmerkeyboard.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import com.programmerkeyboard.R

/**
 * In-Keyboard Voice Input Overlay.
 * Renders directly over KeyboardView at the exact keyboard position without dimming or floating windows.
 * Streams recognized voice text directly into the active InputConnection without losing editor focus.
 */
class VoiceInputOverlay(
    private val context: Context,
    private val onTextRecognized: (String) -> Unit
) {
    private var popupWindow: PopupWindow? = null
    private var popupView: VoiceInputView? = null

    fun show(anchorView: View) {
        dismiss()

        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1]
        val width = anchorView.width
        val height = anchorView.height

        popupView = VoiceInputView(context, onTextRecognized) {
            dismiss()
        }.apply {
            layoutParams = ViewGroup.LayoutParams(width, height)
            startListening()
        }

        popupWindow = PopupWindow(popupView, width, height, true).apply {
            isClippingEnabled = true
            showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y)
        }
    }

    fun dismiss() {
        popupView?.stopListening()
        popupWindow?.dismiss()
        popupWindow = null
        popupView = null
    }

    private class VoiceInputView(
        context: Context,
        private val onTextRecognized: (String) -> Unit,
        private val onCloseRequested: () -> Unit
    ) : View(context), RecognitionListener {

        private var speechRecognizer: SpeechRecognizer? = null
        private var statusMessage = "🎙 Listening..."
        private var isListening = false
        private var wavePulseRadius = 40f
        private var pulseDirection = 1.5f

        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.keyboard_background)
            style = Paint.Style.FILL
        }

        private val micBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.key_background_modifier_active)
            style = Paint.Style.FILL
        }

        private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.key_background_modifier_active)
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.key_text_primary)
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.key_text_secondary)
            textSize = 24f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }

        private val closeRect = RectF()

        fun startListening() {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                com.programmerkeyboard.PermissionRequestActivity.onPermissionResultListener = { granted ->
                    if (granted) {
                        startListening()
                    } else {
                        statusMessage = "Audio permission required"
                        invalidate()
                    }
                }
                val intent = android.content.Intent(context, com.programmerkeyboard.PermissionRequestActivity::class.java).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                statusMessage = "Voice recognition not available"
                invalidate()
                return
            }

            speechRecognizer = if (android.os.Build.VERSION.SDK_INT >= 33 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                try {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } catch (_: Exception) {
                    SpeechRecognizer.createSpeechRecognizer(context)
                }
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }.apply {
                setRecognitionListener(this@VoiceInputView)
            }

            val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra("android.speech.extra.DICTATION_MODE", true)
                putExtra("android.speech.extra.EXTRA_SEGMENTED_SESSION", true)
            }

            isListening = true
            statusMessage = "🎙 Continuous Speech Input..."
            speechRecognizer?.startListening(intent)
            postAnimationRunnable()
        }

        fun stopListening() {
            isListening = false
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            } catch (e: Exception) {}
            speechRecognizer = null
        }

        private fun postAnimationRunnable() {
            if (!isListening) return
            wavePulseRadius += pulseDirection
            if (wavePulseRadius > 70f || wavePulseRadius < 35f) {
                pulseDirection = -pulseDirection
            }
            invalidate()
            postDelayed({ postAnimationRunnable() }, 30)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val density = context.resources.displayMetrics.density

            // Background
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Header close button
            closeRect.set(width.toFloat() - (54f * density), 6f, width.toFloat() - 6f, (44f * density))
            canvas.drawRoundRect(closeRect, 10f, 10f, micBgPaint)
            val closeMetrics = textPaint.fontMetrics
            val closeBaseline = closeRect.centerY() - (closeMetrics.ascent + closeMetrics.descent) / 2
            canvas.drawText("✕", closeRect.centerX(), closeBaseline, textPaint)

            // Center Mic Circle
            val centerX = width / 2f
            val centerY = height / 2f - (10f * density)

            if (isListening) {
                canvas.drawCircle(centerX, centerY, wavePulseRadius * density / 2f, wavePaint)
            }

            canvas.drawCircle(centerX, centerY, 36f * density, micBgPaint)
            canvas.drawText("🎙", centerX, centerY - (closeMetrics.ascent + closeMetrics.descent) / 2, textPaint)

            // Status message
            canvas.drawText(statusMessage, centerX, height - (45f * density), textPaint)
            canvas.drawText("Tap anywhere to stop dictation", centerX, height - (18f * density), hintPaint)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_UP) {
                onCloseRequested()
            }
            return true
        }

        private fun restartListening() {
            if (!isListening) return
            try {
                val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra("android.speech.extra.DICTATION_MODE", true)
                    putExtra("android.speech.extra.EXTRA_SEGMENTED_SESSION", true)
                }
                speechRecognizer?.startListening(intent)
            } catch (_: Exception) {}
        }

        override fun onReadyForSpeech(params: Bundle?) {
            statusMessage = "🎙 Listening continuously..."
            invalidate()
        }

        override fun onBeginningOfSpeech() {
            statusMessage = "🗣 Recording speech..."
            invalidate()
        }

        override fun onRmsChanged(rmsdB: Float) {
            wavePulseRadius = (35f + rmsdB * 2.5f).coerceIn(35f, 75f)
            invalidate()
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            if (isListening) {
                statusMessage = "Processing speech..."
                invalidate()
            }
        }

        override fun onError(error: Int) {
            if (isListening) {
                restartListening()
            } else {
                statusMessage = "Speech error ($error)"
                invalidate()
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val textWithSpace = matches[0] + " "
                onTextRecognized(textWithSpace)
                statusMessage = "🗣 " + matches[0]
            }
            if (isListening) {
                restartListening()
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                statusMessage = "🗣 " + matches[0]
                invalidate()
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
