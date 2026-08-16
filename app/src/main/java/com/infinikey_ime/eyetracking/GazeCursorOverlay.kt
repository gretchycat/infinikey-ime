package com.infinikey_ime.eyetracking

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.util.Log
import kotlin.math.abs

/**
 * Floating Cursor Overlay View rendered via WindowManager TYPE_APPLICATION_OVERLAY.
 * 
 * Features:
 * - 60 FPS Linear Interpolation (LERP) smooth motion ticker so cursor glides smoothly between points.
 * - FLAG_NOT_TOUCHABLE so hardware touch events pass through cleanly.
 * - Visual pulse animation on click trigger.
 */
class GazeCursorOverlay(private val context: Context) {

    companion object {
        private const val TAG = "GazeCursorOverlay"
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var cursorView: CursorPointerView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isShowing = false

    private var targetX = 500f
    private var targetY = 1000f
    private var currentXFloat = 500f
    private var currentYFloat = 1000f

    // LERP smoothing factor (0.22 gives silky smooth 60 FPS gliding motion)
    private val smoothFactor = 0.22f

    var currentX: Int = 500
        private set
    var currentY: Int = 1000
        private set

    private val smoothAnimationRunnable = object : Runnable {
        override fun run() {
            if (!isShowing) return
            val dx = targetX - currentXFloat
            val dy = targetY - currentYFloat

            if (abs(dx) > 0.5f || abs(dy) > 0.5f) {
                currentXFloat += dx * smoothFactor
                currentYFloat += dy * smoothFactor

                currentX = currentXFloat.toInt()
                currentY = currentYFloat.toInt()

                layoutParams?.x = currentX - 60 // Center cursor on point
                layoutParams?.y = currentY - 60
                try {
                    windowManager.updateViewLayout(cursorView, layoutParams)
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating layout: ${e.message}")
                }
            }
            mainHandler.postDelayed(this, 16) // 60 FPS ticker loop
        }
    }

    init {
        initLayoutParams()
    }

    private fun initLayoutParams() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            120, // width in px
            120, // height in px
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = currentX
            y = currentY
        }
    }

    @SuppressLint("InflateParams")
    fun show() {
        if (isShowing) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Cannot show cursor overlay: SYSTEM_ALERT_WINDOW permission missing.")
            return
        }

        try {
            cursorView = CursorPointerView(context)
            windowManager.addView(cursorView, layoutParams)
            isShowing = true
            mainHandler.post(smoothAnimationRunnable)
            Log.d(TAG, "Gaze cursor overlay displayed successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding cursor overlay view to WindowManager: ${e.message}", e)
        }
    }

    fun updatePosition(x: Int, y: Int) {
        targetX = x.toFloat()
        targetY = y.toFloat()
    }

    fun triggerClickAnimation() {
        cursorView?.pulseClick()
    }

    fun hide() {
        if (!isShowing || cursorView == null) return
        try {
            mainHandler.removeCallbacks(smoothAnimationRunnable)
            windowManager.removeView(cursorView)
            isShowing = false
            cursorView = null
            Log.d(TAG, "Gaze cursor overlay hidden.")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing cursor overlay view: ${e.message}", e)
        }
    }

    /**
     * Custom Canvas View that draws a modern glowing target cursor.
     */
    private class CursorPointerView(context: Context) : View(context) {

        private val outerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF") // Neon Cyan
            style = Paint.Style.STROKE
            strokeWidth = 5f
            setShadowLayer(8f, 0f, 0f, Color.parseColor("#00E5FF"))
        }

        private val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF0055") // Neon Pink/Red Center Dot
            style = Paint.Style.FILL
            setShadowLayer(6f, 0f, 0f, Color.parseColor("#FF0055"))
        }

        private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeWidth = 3f
        }

        private var pulseScale = 1.0f

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val radius = 24f * pulseScale

            // Draw outer glowing target ring
            canvas.drawCircle(cx, cy, radius, outerRingPaint)

            // Draw crosshair ticks
            canvas.drawLine(cx - radius - 8, cy, cx - radius + 4, cy, crosshairPaint)
            canvas.drawLine(cx + radius - 4, cy, cx + radius + 8, cy, crosshairPaint)
            canvas.drawLine(cx, cy - radius - 8, cx, cy - radius + 4, crosshairPaint)
            canvas.drawLine(cx, cy + radius - 4, cx, cy + radius + 8, crosshairPaint)

            // Draw inner center dot
            canvas.drawCircle(cx, cy, 7f, centerDotPaint)
        }

        fun pulseClick() {
            pulseScale = 1.6f
            invalidate()
            postDelayed({
                pulseScale = 1.0f
                invalidate()
            }, 250)
        }
    }
}
