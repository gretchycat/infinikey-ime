package com.programmerkeyboard.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import com.programmerkeyboard.R

/**
 * Momentary visual key press preview popup overlay (magnified key label shown above pressed key).
 */
class KeyPreviewOverlay(private val context: Context) {
    private var popupWindow: PopupWindow? = null
    private var popupView: PreviewContentView? = null

    fun show(anchorView: View, anchorRect: RectF, text: String) {
        dismiss()
        if (text.isEmpty()) return

        val density = context.resources.displayMetrics.density
        val keyHeight = anchorRect.height()
        val keyWidth = anchorRect.width()

        val widthPx = maxOf(keyWidth * 1.1f, 48f * density).toInt()
        val heightPx = (keyHeight * 1.25f).toInt()
        val fontSize = keyHeight * 0.50f
        val screenWidth = anchorView.width.toFloat()

        val popupX = (anchorRect.centerX() - widthPx / 2f).coerceIn(4f, maxOf(4f, screenWidth - widthPx - 4f))
        val popupY = anchorRect.top - heightPx - (4f * density)

        popupView = PreviewContentView(context, text, fontSize).apply {
            layoutParams = ViewGroup.MarginLayoutParams(widthPx, heightPx)
        }

        popupWindow = PopupWindow(popupView, widthPx, heightPx, false).apply {
            isClippingEnabled = false
            showAtLocation(
                anchorView,
                Gravity.NO_GRAVITY,
                popupX.toInt(),
                popupY.toInt()
            )
        }
    }

    fun dismiss() {
        popupWindow?.dismiss()
        popupWindow = null
        popupView = null
    }

    private class PreviewContentView(
        context: Context,
        private val text: String,
        fontSize: Float
    ) : View(context) {

        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.key_background_pressed)
            style = Paint.Style.FILL
        }

        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.key_border)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.key_text_primary)
            textSize = fontSize
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val rect = RectF(2f, 2f, width.toFloat() - 2f, height.toFloat() - 2f)
            canvas.drawRoundRect(rect, 14f, 14f, bgPaint)
            canvas.drawRoundRect(rect, 14f, 14f, borderPaint)

            val fontMetrics = textPaint.fontMetrics
            val baseline = rect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2
            canvas.drawText(text, rect.centerX(), baseline, textPaint)
        }
    }
}
