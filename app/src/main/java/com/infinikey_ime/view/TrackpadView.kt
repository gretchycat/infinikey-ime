package com.infinikey_ime.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.infinikey_ime.R

/**
 * Side margin trackpad view that provides pointer movement and touch mouse emulation
 * for Termux, X11, VNC, and desktop Android apps.
 */
class TrackpadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val trackpadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.trackpad_background)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.key_text_secondary)
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    var onPointerMoveListener: ((dx: Float, dy: Float) -> Unit)? = null
    var onLeftClickListener: (() -> Unit)? = null

    private var lastX: Float = 0f
    private var lastY: Float = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), trackpadPaint)
        canvas.drawText("TRACKPAD", width / 2f, height / 2f, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                onPointerMoveListener?.invoke(dx, dy)
                lastX = event.x
                lastY = event.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                onLeftClickListener?.invoke()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
