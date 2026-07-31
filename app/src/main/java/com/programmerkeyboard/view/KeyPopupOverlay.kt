package com.programmerkeyboard.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import com.programmerkeyboard.R

/**
 * Overlay popup window for long-press key selection popups (accented letters & symbol variations).
 */
class KeyPopupOverlay(
    private val context: Context,
    private val onItemSelected: (String) -> Unit
) {
    private var popupWindow: PopupWindow? = null
    private var popupView: PopupContentView? = null
    private var popupX = 0f
    private var popupY = 0f

    fun show(anchorView: View, anchorRect: RectF, options: List<String>) {
        dismiss()
        if (options.isEmpty()) return

        val density = context.resources.displayMetrics.density
        val keyHeight = anchorRect.height()
        val keyWidth = anchorRect.width()

        val itemWidthPx = maxOf(keyWidth, 54f * density).toInt()
        val popupWidth = options.size * itemWidthPx + (12f * density).toInt()
        val popupHeight = keyHeight.toInt() + (8f * density).toInt()
        val fontSize = keyHeight * 0.40f
        val screenWidth = anchorView.width.toFloat()

        val idealPopupX = anchorRect.left - (6f * density)
        popupX = idealPopupX.coerceIn(10f, maxOf(10f, screenWidth - popupWidth - 10f))
        popupY = anchorRect.top - popupHeight - (8f * density)

        popupView = PopupContentView(context, options, fontSize, onItemSelected).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                popupWidth,
                popupHeight
            )
        }

        popupWindow = PopupWindow(popupView, popupWidth, popupHeight, false).apply {
            isClippingEnabled = false
            showAtLocation(
                anchorView,
                Gravity.NO_GRAVITY,
                popupX.toInt(),
                popupY.toInt()
            )
        }
    }

    fun handleTouchEvent(event: MotionEvent): Boolean {
        val localX = event.x - popupX
        popupView?.updateSelection(localX)

        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            popupView?.commitSelection()
            dismiss()
        }
        return true
    }

    fun dismiss() {
        popupWindow?.dismiss()
        popupWindow = null
        popupView = null
    }

    private class PopupContentView(
        context: Context,
        private val options: List<String>,
        fontSize: Float,
        private val onItemSelected: (String) -> Unit
    ) : View(context) {

        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.key_background_pressed)
            style = Paint.Style.FILL
        }

        private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.key_background_modifier_active)
            style = Paint.Style.FILL
        }

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.key_text_primary)
            textSize = fontSize
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        private var selectedIndex = 0
        private var initialTouchX: Float? = null

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRoundRect(rect, 16f, 16f, bgPaint)

            val itemWidth = width.toFloat() / options.size
            options.forEachIndexed { index, option ->
                val itemRect = RectF(
                    index * itemWidth + 4f,
                    4f,
                    (index + 1) * itemWidth - 4f,
                    height.toFloat() - 4f
                )
                if (index == selectedIndex) {
                    canvas.drawRoundRect(itemRect, 12f, 12f, selectedPaint)
                }
                val fontMetrics = textPaint.fontMetrics
                val baseline = itemRect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2
                canvas.drawText(option, itemRect.centerX(), baseline, textPaint)
            }
        }

        fun updateSelection(localX: Float) {
            if (initialTouchX == null) {
                initialTouchX = localX
            }
            val density = context.resources.displayMetrics.density
            val slop = 16f * density
            val deltaX = localX - (initialTouchX ?: localX)

            if (kotlin.math.abs(deltaX) < slop && localX in 0f..width.toFloat()) {
                if (selectedIndex != 0) {
                    selectedIndex = 0
                    invalidate()
                }
                return
            }

            val itemWidth = width.toFloat() / options.size
            val index = (localX / itemWidth).toInt().coerceIn(0, options.size - 1)
            if (index != selectedIndex) {
                selectedIndex = index
                invalidate()
            }
        }

        fun commitSelection() {
            if (selectedIndex in options.indices) {
                onItemSelected(options[selectedIndex])
            }
        }
    }
}
