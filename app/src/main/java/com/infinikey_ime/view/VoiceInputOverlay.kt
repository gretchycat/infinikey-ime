package com.infinikey_ime.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import com.infinikey_ime.R
import com.infinikey_ime.stt.SttCallback
import com.infinikey_ime.stt.SttEngine
import com.infinikey_ime.stt.SttEngineFactory

/**
 * Minimal Pill-based In-Keyboard Voice Input Overlay.
 * Renders a compact, sleek floating capsule pill at the top of the keyboard.
 * Dynamically expands multi-line up to 4 lines when text is long.
 * Streams recognized voice text directly into the active InputConnection without losing editor focus.
 */
class VoiceInputOverlay(
    private val context: Context,
    private val onTextRecognized: (String) -> Unit
) {
    private var popupWindow: PopupWindow? = null
    private var popupView: MinimalVoicePillView? = null
    private var currentPillWidth = 0
    private var currentPillHeight = 0
    private var currentPosX = 0
    private var currentPosY = 0

    fun show(anchorView: View) {
        dismiss()

        val density = context.resources.displayMetrics.density
        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)

        currentPillWidth = (anchorView.width * 0.92f).toInt().coerceAtLeast((260f * density).toInt())
        currentPillHeight = (50f * density).toInt()

        currentPosX = location[0] + (anchorView.width - currentPillWidth) / 2
        currentPosY = location[1] + (6f * density).toInt()

        popupView = MinimalVoicePillView(context, onTextRecognized, {
            dismiss()
        }) { newHeight ->
            updateHeight(newHeight)
        }.apply {
            layoutParams = ViewGroup.LayoutParams(currentPillWidth, currentPillHeight)
            startListening()
        }

        popupWindow = PopupWindow(popupView, currentPillWidth, currentPillHeight, false).apply {
            isOutsideTouchable = true
            isFocusable = false
            showAtLocation(anchorView, Gravity.NO_GRAVITY, currentPosX, currentPosY)
        }
    }

    private fun updateHeight(newHeight: Int) {
        if (newHeight != currentPillHeight && popupWindow != null && popupView != null) {
            currentPillHeight = newHeight
            popupWindow?.update(currentPosX, currentPosY, currentPillWidth, currentPillHeight)
        }
    }

    fun dismiss() {
        popupView?.stopListening()
        popupWindow?.dismiss()
        popupWindow = null
        popupView = null
    }

    private class MinimalVoicePillView(
        context: Context,
        private val onTextRecognized: (String) -> Unit,
        private val onCloseRequested: () -> Unit,
        private val onHeightChanged: (Int) -> Unit
    ) : View(context), SttCallback {

        private var sttEngine: SttEngine? = null
        private var statusText = "Listening..."
        private var isListening = false
        private var micPulseRadius = 14f
        private var pulseDirection = 0.4f

        private val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A") // Sleek Slate Midnight
            style = Paint.Style.FILL
        }

        private val pillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8") // Cyan accent border
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        private val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            textSize = 15f * context.resources.displayMetrics.density
            typeface = Typeface.DEFAULT_BOLD
        }

        private val closeBtnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            style = Paint.Style.FILL
        }

        private val closeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 14f * context.resources.displayMetrics.density
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        private val pillBounds = RectF()
        private val closeRect = RectF()

        fun startListening() {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                com.infinikey_ime.PermissionRequestActivity.onPermissionResultListener = { granted ->
                    if (granted) {
                        startListening()
                    } else {
                        post { onCloseRequested() }
                    }
                }
                val intent = android.content.Intent(context, com.infinikey_ime.PermissionRequestActivity::class.java).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }

            sttEngine = SttEngineFactory.getActiveEngine(context)
            if (sttEngine?.isAvailable != true && sttEngine !is com.infinikey_ime.stt.AndroidSystemSttEngine) {
                sttEngine = SttEngineFactory.createEngine(context, "ANDROID_SYSTEM")
            }

            isListening = true
            updateStatusText("Listening...")
            sttEngine?.startListening(this)
            postAnimationRunnable()
        }

        fun stopListening() {
            isListening = false
            try {
                sttEngine?.stopListening()
                sttEngine?.destroy()
            } catch (_: Exception) {}
            sttEngine = null
        }

        private fun updateStatusText(newText: String) {
            statusText = newText
            post {
                val density = context.resources.displayMetrics.density
                val maxTextWidth = getMaxTextWidth()
                val requiredHeight = computeRequiredHeight(density, maxTextWidth)
                onHeightChanged(requiredHeight)
                invalidate()
            }
        }

        fun getMaxTextWidth(): Int {
            val density = context.resources.displayMetrics.density
            val ledAreaWidth = 56f * density
            val closeAreaWidth = 46f * density
            return (width - ledAreaWidth - closeAreaWidth).toInt().coerceAtLeast(100)
        }

        fun computeRequiredHeight(density: Float, maxTextWidth: Int): Int {
            if (statusText.isEmpty()) return (56f * density).toInt()
            val layout = createStaticLayout(statusText, textPaint, maxTextWidth)
            val lineCount = layout.lineCount.coerceIn(1, 4)
            val extraPadding = (lineCount - 1) * (24f * density).toInt()
            return ((56f * density).toInt() + extraPadding).coerceIn((56f * density).toInt(), (160f * density).toInt())
        }

        private fun createStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
            val safeWidth = width.coerceAtLeast(50)
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(text, 0, text.length, paint, safeWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.15f)
                    .setIncludePad(false)
                    .setMaxLines(4)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(text, 0, text.length, paint, safeWidth, Layout.Alignment.ALIGN_NORMAL, 1.15f, 0f, false)
            }
        }

        private fun postAnimationRunnable() {
            if (!isListening) return
            micPulseRadius += pulseDirection
            if (micPulseRadius > 18f || micPulseRadius < 11f) {
                pulseDirection = -pulseDirection
            }
            invalidate()
            postDelayed({ postAnimationRunnable() }, 40)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val density = context.resources.displayMetrics.density

            pillBounds.set(2f, 2f, width - 2f, height - 2f)
            val cornerRadius = (25f * density).coerceAtMost(height / 2f)

            // Draw Capsule Pill Background & Border
            canvas.drawRoundRect(pillBounds, cornerRadius, cornerRadius, pillBgPaint)
            canvas.drawRoundRect(pillBounds, cornerRadius, cornerRadius, pillBorderPaint)

            // Left Side: Status LED & Mic Icon
            val isNativeEngine = sttEngine?.isAvailable == true && sttEngine !is com.infinikey_ime.stt.AndroidSystemSttEngine
            val isSystemFallback = sttEngine is com.infinikey_ime.stt.AndroidSystemSttEngine

            val ledColor = when {
                isNativeEngine || isSystemFallback -> Color.parseColor("#10B981") // Green
                else -> Color.parseColor("#EF4444") // Red
            }

            val ledPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ledColor
                style = Paint.Style.FILL
            }

            val ledX = 20f * density
            val ledY = height / 2f
            canvas.drawCircle(ledX, ledY, 4f * density, ledPaint)

            // Pulsing Mic Ring
            if (isListening) {
                canvas.drawCircle(ledX + (18f * density), ledY, micPulseRadius * density / 1.6f, pulsePaint)
            }

            canvas.drawText("🎙", ledX + (11f * density), ledY + (4f * density), closeTextPaint)

            // Right Side: Close Touch Button
            val closeBtnSize = 30f * density
            closeRect.set(
                width - (38f * density),
                (height - closeBtnSize) / 2f,
                width - (8f * density),
                (height + closeBtnSize) / 2f
            )
            canvas.drawRoundRect(closeRect, 15f * density, 15f * density, closeBtnPaint)
            val closeMetrics = closeTextPaint.fontMetrics
            val closeBaseline = closeRect.centerY() - (closeMetrics.ascent + closeMetrics.descent) / 2
            canvas.drawText("✕", closeRect.centerX(), closeBaseline, closeTextPaint)

            // Center Multi-Line Text Layout
            val startTextX = ledX + (36f * density)
            val maxTextWidth = getMaxTextWidth()
            val staticLayout = createStaticLayout(statusText, textPaint, maxTextWidth)

            canvas.save()
            val textY = (height - staticLayout.height) / 2f
            canvas.translate(startTextX, textY)
            staticLayout.draw(canvas)
            canvas.restore()
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_UP) {
                onCloseRequested()
            }
            return true
        }

        // SttCallback Implementation
        override fun onReadyForSpeech() {
            updateStatusText("Listening...")
        }

        override fun onBeginningOfSpeech() {
            updateStatusText("Recording...")
        }

        override fun onRmsChanged(rmsdB: Float) {
            micPulseRadius = (11f + rmsdB * 0.8f).coerceIn(11f, 20f)
            invalidate()
        }

        override fun onPartialResult(text: String) {
            updateStatusText(text)
        }

        override fun onFinalResult(text: String) {
            val textWithSpace = text + " "
            onTextRecognized(textWithSpace)
            updateStatusText(text)

            val prefs = context.getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
            val isContinuous = prefs.getBoolean("pref_stt_continuous_mode", false)
            if (!isContinuous) {
                onCloseRequested()
            }
        }

        override fun onError(errorMessage: String) {
            post {
                onCloseRequested()
            }
        }
    }
}
