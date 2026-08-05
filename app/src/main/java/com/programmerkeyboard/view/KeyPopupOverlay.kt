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

/**
 * Overlay popup window for long-press & tap action selection popups (accented letters, symbols, & action buttons).
 * Renders individual tactile 3D button caps for each action option with relaxed drag tracking.
 */
class KeyPopupOverlay(
    private val context: Context,
    private val onItemSelected: (Int, String) -> Unit
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

        val testPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 15f * density
            typeface = Typeface.DEFAULT_BOLD
        }

        val maxTextWidth = options.maxOfOrNull { testPaint.measureText(getDisplayOption(it)) } ?: (40f * density)
        val itemWidthPx = maxOf(keyWidth * 1.05f, maxTextWidth + (22f * density), 62f * density).toInt()
        val popupWidth = options.size * itemWidthPx + (16f * density).toInt()
        val popupHeight = (keyHeight * 1.15f).toInt().coerceAtLeast((52f * density).toInt())
        val fontSize = 14f * density
        val screenWidth = anchorView.width.toFloat()

        val idealPopupX = anchorRect.left - (8f * density)
        popupX = idealPopupX.coerceIn(10f, maxOf(10f, screenWidth - popupWidth - 10f))
        popupY = anchorRect.top - popupHeight - (10f * density)

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
        popupView?.updateSelection(localX, event.action)

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

    companion object {
        fun getDisplayOption(option: String): String {
            return when (option.trim().uppercase()) {
                "SELECT ALL", "SELECT_ALL" -> "All"
                "COPY" -> "Copy"
                "CUT" -> "Cut"
                "PASTE" -> "Paste"
                "PASTE ECHO", "PASTE_ECHO", "ECHO" -> "Echo"
                "SWITCH IME", "SWITCH_IME" -> "IME"
                else -> option
            }
        }
    }

    private class PopupContentView(
        context: Context,
        private val options: List<String>,
        private val fontSize: Float,
        private val onItemSelected: (Int, String) -> Unit
    ) : View(context) {

        private val containerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#0F172A")
            style = Paint.Style.FILL
            alpha = 235
        }

        private val containerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#334155")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        private val buttonBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#1E293B")
            style = Paint.Style.FILL
        }

        private val buttonBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#475569")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        private val selectedButtonBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#0284C7")
            style = Paint.Style.FILL
        }

        private val selectedButtonBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#00F0FF")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#F8FAFC")
            textSize = fontSize
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        private val selectedTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = fontSize * 1.05f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        private var selectedIndex = 0
        private var initialTouchX: Float? = null

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val density = context.resources.displayMetrics.density
            val containerRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            val containerRadius = 16f * density

            // 1. Draw Outer Glassmorphic Container Card
            canvas.drawRoundRect(containerRect, containerRadius, containerRadius, containerBgPaint)
            canvas.drawRoundRect(containerRect, containerRadius, containerRadius, containerBorderPaint)

            // 2. Draw Individual Tactile Key Button Caps
            val padding = 6f * density
            val availableWidth = width.toFloat() - (padding * 2f)
            val itemWidth = availableWidth / options.size
            val buttonRadius = 10f * density

            options.forEachIndexed { index, option ->
                val buttonRect = RectF(
                    padding + (index * itemWidth) + (3f * density),
                    padding,
                    padding + ((index + 1) * itemWidth) - (3f * density),
                    height.toFloat() - padding
                )

                val isSelected = (index == selectedIndex)
                val bgPaintToUse = if (isSelected) selectedButtonBgPaint else buttonBgPaint
                val borderPaintToUse = if (isSelected) selectedButtonBorderPaint else buttonBorderPaint
                val textPaintToUse = if (isSelected) selectedTextPaint else textPaint

                // Draw Button Cap Background & Border
                canvas.drawRoundRect(buttonRect, buttonRadius, buttonRadius, bgPaintToUse)
                canvas.drawRoundRect(buttonRect, buttonRadius, buttonRadius, borderPaintToUse)

                // Top highlight line for 3D tactile button cap look
                if (isSelected) {
                    val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.argb(100, 255, 255, 255)
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5f * density
                    }
                    canvas.drawLine(
                        buttonRect.left + (buttonRadius / 2f),
                        buttonRect.top + (2f * density),
                        buttonRect.right - (buttonRadius / 2f),
                        buttonRect.top + (2f * density),
                        highlightPaint
                    )
                }

                // Draw Button Title Label
                val displayOption = getDisplayOption(option)
                val fontMetrics = textPaintToUse.fontMetrics
                val baseline = buttonRect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2f
                canvas.drawText(displayOption, buttonRect.centerX(), baseline, textPaintToUse)
            }
        }

        fun updateSelection(localX: Float, action: Int? = null) {
            val density = context.resources.displayMetrics.density
            val padding = 6f * density
            val availableWidth = width.toFloat() - (padding * 2f)
            if (availableWidth <= 0f || options.isEmpty()) return

            val itemWidth = availableWidth / options.size

            if (initialTouchX == null || action == MotionEvent.ACTION_DOWN) {
                initialTouchX = localX
                selectedIndex = 0
                invalidate()
                return
            }

            val startX = initialTouchX ?: localX
            val touchDelta = Math.abs(localX - startX)
            val dragThreshold = 28f * density

            val newIndex = if (touchDelta < dragThreshold) {
                0
            } else {
                val rawIdx = ((localX - padding) / itemWidth).toInt().coerceIn(0, options.size - 1)
                if (rawIdx != selectedIndex) {
                    val currentCenter = padding + (selectedIndex + 0.5f) * itemWidth
                    val deltaFromCurrentCenter = Math.abs(localX - currentCenter)
                    if (deltaFromCurrentCenter > itemWidth * 0.4f) {
                        rawIdx
                    } else {
                        selectedIndex
                    }
                } else {
                    rawIdx
                }
            }

            if (newIndex != selectedIndex) {
                selectedIndex = newIndex
                invalidate()
            }
        }

        fun commitSelection() {
            if (selectedIndex in options.indices) {
                onItemSelected(selectedIndex, options[selectedIndex])
            }
        }
    }
}
