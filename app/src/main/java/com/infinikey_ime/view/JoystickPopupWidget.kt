package com.infinikey_ime.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import com.infinikey_ime.R
import com.infinikey_ime.engine.KeyRepeatEngine
import kotlin.math.atan2
import kotlin.math.hypot

private data class JoystickPosition(
    val popupX: Float,
    val popupY: Float,
    val initialThumbX: Float,
    val initialThumbY: Float
)

/**
 * Interactive 360° Analog Joystick Widget for Arrow Navigation.
 * Dynamically aligns over the key extents based on direction (Left, Right, Up, Down).
 * Features dynamic 4-way direction switching, speed acceleration, and glowing direction indicators.
 */
class JoystickPopupWidget(
    private val context: Context,
    private val onSendKeyCode: (Int) -> Unit
) {
    private var popupWindow: PopupWindow? = null
    private var joystickView: JoystickView? = null
    private val repeatEngine = KeyRepeatEngine { keyCode -> onSendKeyCode(keyCode) }

    fun show(anchorView: View, keyCenterX: Float, keyCenterY: Float, initialKeyCode: Int) {
        dismiss()

        val size = 320

        // Calculate dynamic popup position & initial thumb offset based on pressed arrow key extent
        val pos = when (initialKeyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> JoystickPosition(keyCenterX - 230f, keyCenterY - 160f, 230f, 160f)
            KeyEvent.KEYCODE_DPAD_RIGHT -> JoystickPosition(keyCenterX - 90f, keyCenterY - 160f, 90f, 160f)
            KeyEvent.KEYCODE_DPAD_UP -> JoystickPosition(keyCenterX - 160f, keyCenterY - 230f, 160f, 230f)
            KeyEvent.KEYCODE_DPAD_DOWN -> JoystickPosition(keyCenterX - 160f, keyCenterY - 90f, 160f, 90f)
            else -> JoystickPosition(keyCenterX - 160f, keyCenterY - 160f, 160f, 160f)
        }

        joystickView = JoystickView(context, initialKeyCode, pos.initialThumbX, pos.initialThumbY, repeatEngine).apply {
            layoutParams = ViewGroup.MarginLayoutParams(size, size)
        }

        popupWindow = PopupWindow(joystickView, size, size, false).apply {
            isClippingEnabled = false
            isTouchable = true
            showAtLocation(anchorView, Gravity.NO_GRAVITY, pos.popupX.toInt(), pos.popupY.toInt())
        }

        repeatEngine.startRepeat(initialKeyCode)
    }

    fun handleTouchEvent(event: MotionEvent): Boolean {
        joystickView?.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            dismiss()
        }
        return true
    }

    fun dismiss() {
        repeatEngine.stopRepeat()
        popupWindow?.dismiss()
        popupWindow = null
        joystickView = null
    }

    private class JoystickView(
        context: Context,
        initialKeyCode: Int,
        initialThumbX: Float,
        initialThumbY: Float,
        private val repeatEngine: KeyRepeatEngine
    ) : View(context) {

        private val outerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.key_background_modifier_active)
            style = Paint.Style.STROKE
            strokeWidth = 6f
            alpha = 200
        }

        private val centerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.keyboard_background)
            style = Paint.Style.FILL
            alpha = 240
        }

        private val thumbNubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.key_text_accent)
            style = Paint.Style.FILL
        }

        private val activeDirectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.key_text_accent)
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        private val inactiveDirectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.key_text_secondary)
            textSize = 24f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
            alpha = 150
        }

        private var activeKeyCode = initialKeyCode
        private val centerPos = PointF(160f, 160f)
        private val thumbPos = PointF(initialThumbX, initialThumbY)
        private val maxRadius = 110f

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            setMeasuredDimension(320, 320)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            // Outer Ring Background
            canvas.drawCircle(centerPos.x, centerPos.y, maxRadius, centerBgPaint)
            canvas.drawCircle(centerPos.x, centerPos.y, maxRadius, outerRingPaint)

            // Direction Indicators (▲ Top, ▼ Bottom, ◀ Left, ▶ Right)
            val topPaint = if (activeKeyCode == KeyEvent.KEYCODE_DPAD_UP) activeDirectionPaint else inactiveDirectionPaint
            val bottomPaint = if (activeKeyCode == KeyEvent.KEYCODE_DPAD_DOWN) activeDirectionPaint else inactiveDirectionPaint
            val leftPaint = if (activeKeyCode == KeyEvent.KEYCODE_DPAD_LEFT) activeDirectionPaint else inactiveDirectionPaint
            val rightPaint = if (activeKeyCode == KeyEvent.KEYCODE_DPAD_RIGHT) activeDirectionPaint else inactiveDirectionPaint

            canvas.drawText("▲", centerPos.x, centerPos.y - maxRadius + 32f, topPaint)
            canvas.drawText("▼", centerPos.x, centerPos.y + maxRadius - 14f, bottomPaint)
            canvas.drawText("◀", centerPos.x - maxRadius + 22f, centerPos.y + 10f, leftPaint)
            canvas.drawText("▶", centerPos.x + maxRadius - 22f, centerPos.y + 10f, rightPaint)

            // Central Thumb Nub
            canvas.drawCircle(thumbPos.x, thumbPos.y, 32f, thumbNubPaint)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_MOVE, MotionEvent.ACTION_DOWN -> {
                    val dx = event.x - centerPos.x
                    val dy = event.y - centerPos.y
                    val dist = hypot(dx, dy)

                    val clampedDist = dist.coerceAtMost(maxRadius)
                    val angleRad = atan2(dy, dx)
                    val angleDeg = Math.toDegrees(angleRad.toDouble())

                    thumbPos.x = centerPos.x + clampedDist * kotlin.math.cos(angleRad)
                    thumbPos.y = centerPos.y + clampedDist * kotlin.math.sin(angleRad)

                    // 4-Way Direction Switching based on angle
                    if (dist > 15f) {
                        activeKeyCode = when {
                            angleDeg >= -135.0 && angleDeg < -45.0 -> KeyEvent.KEYCODE_DPAD_UP
                            angleDeg >= 45.0 && angleDeg < 135.0 -> KeyEvent.KEYCODE_DPAD_DOWN
                            angleDeg >= -45.0 && angleDeg < 45.0 -> KeyEvent.KEYCODE_DPAD_RIGHT
                            else -> KeyEvent.KEYCODE_DPAD_LEFT
                        }
                        repeatEngine.updateKeyCode(activeKeyCode)
                    }

                    // Dynamic speed acceleration proportional to displacement distance
                    repeatEngine.updateAnalogRate(clampedDist, maxRadius)
                    invalidate()
                }
            }
            return true
        }
    }
}
