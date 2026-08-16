package com.infinikey_ime.view

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
 * Renders individual tactile 3D button caps with native vector SVG icons for clipboard actions.
 * Supports both drag-selection and direct tap-to-click selections.
 */
class KeyPopupOverlay(
    private val context: Context,
    private val onItemSelected: (Int, String) -> Unit,
    private val onHoverChanged: ((Int, String) -> Unit)? = null,
    var onDismissListener: (() -> Unit)? = null
) {
    private var popupWindow: PopupWindow? = null
    private var popupView: PopupContentView? = null
    private var popupX = 0f
    private var popupY = 0f

    fun isShowing(): Boolean = popupWindow?.isShowing == true

    fun getItemRect(index: Int): RectF? {
        val view = popupView ?: return null
        val density = context.resources.displayMetrics.density
        val padding = 6f * density
        val availableWidth = view.width.toFloat() - (padding * 2f)
        val availableHeight = view.height.toFloat() - (padding * 2f)
        if (availableWidth <= 0f || availableHeight <= 0f || index !in view.options.indices) return null
        
        val itemWidth = availableWidth / view.numCols
        val itemHeight = availableHeight / view.numRows
        val row = index / view.numCols
        val col = index % view.numCols

        val left = popupX + padding + (col * itemWidth)
        val right = left + itemWidth
        val top = popupY + padding + (row * itemHeight)
        val bottom = top + itemHeight
        
        return RectF(left, top, right, bottom)
    }

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
        val containerPaddingPx = (16f * density).toInt()
        val singleRowWidth = options.size * itemWidthPx + containerPaddingPx

        val screenWidth = if (anchorView.width > 0) anchorView.width.toFloat() else context.resources.displayMetrics.widthPixels.toFloat()

        val (numRows, numCols, actualItemWidthPx) = if (singleRowWidth <= screenWidth) {
            Triple(1, options.size, itemWidthPx)
        } else {
            val maxColsPerRow = maxOf(1, ((screenWidth - containerPaddingPx) / itemWidthPx).toInt())
            val rowsNeeded = (options.size + maxColsPerRow - 1) / maxColsPerRow
            val colsPerRow = (options.size + rowsNeeded - 1) / rowsNeeded
            val adjustedItemWidth = minOf(itemWidthPx, maxOf((30f * density).toInt(), ((screenWidth - containerPaddingPx) / colsPerRow).toInt()))
            Triple(rowsNeeded, colsPerRow, adjustedItemWidth)
        }

        val popupWidth = (numCols * actualItemWidthPx + containerPaddingPx).coerceAtMost(screenWidth.toInt())
        val singleRowHeight = (keyHeight * 1.25f).toInt().coerceAtLeast((56f * density).toInt())
        val popupHeight = singleRowHeight * numRows
        val fontSize = 13f * density

        val idealPopupX = anchorRect.left - (8f * density)
        popupX = idealPopupX.coerceIn(10f, maxOf(10f, screenWidth - popupWidth - 10f))

        val idealPopupY = anchorRect.top - popupHeight - (10f * density)
        val screenHeight = if (anchorView.height > 0) anchorView.height.toFloat() else context.resources.displayMetrics.heightPixels.toFloat()
        popupY = if (idealPopupY < 10f && anchorRect.bottom + popupHeight + (10f * density) <= screenHeight) {
            anchorRect.bottom + (10f * density)
        } else {
            idealPopupY.coerceAtLeast(10f)
        }

        popupView = PopupContentView(
            context = context,
            options = options,
            numRows = numRows,
            numCols = numCols,
            fontSize = fontSize,
            onItemSelected = onItemSelected,
            onDismissRequest = { dismiss() },
            onHoverChanged = onHoverChanged
        ).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                popupWidth,
                popupHeight
            )
        }

        popupWindow = PopupWindow(popupView, popupWidth, popupHeight, false).apply {
            isClippingEnabled = false
            isTouchable = true
            isOutsideTouchable = true
            setOnDismissListener {
                onDismissListener?.invoke()
            }
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
        val localY = event.y - popupY
        popupView?.updateSelection(localX, localY, event.action)

        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            popupView?.commitSelection()
            dismiss()
        }
        return true
    }

    fun dismiss() {
        val pw = popupWindow
        popupWindow = null
        popupView = null
        pw?.dismiss()
        onDismissListener?.invoke()
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
        val options: List<String>,
        val numRows: Int,
        val numCols: Int,
        private val fontSize: Float,
        private val onItemSelected: (Int, String) -> Unit,
        private val onDismissRequest: () -> Unit,
        private val onHoverChanged: ((Int, String) -> Unit)? = null
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
        private var initialTouchY: Float? = null

        private fun drawSvgIcon(canvas: Canvas, iconName: String, rect: RectF, paint: Paint) {
            val iconSize = minOf(rect.width(), rect.height())
            val scale = iconSize / 24f
            val offsetX = rect.centerX() - (iconSize / 2f)
            val offsetY = rect.centerY() - (iconSize / 2f)

            val saveCount = canvas.save()
            canvas.translate(offsetX, offsetY)
            canvas.scale(scale, scale)

            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = paint.color
                style = Paint.Style.STROKE
                strokeWidth = 2f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            when (iconName) {
                "select_all" -> {
                    canvas.drawLine(3f, 3f, 7f, 3f, strokePaint)
                    canvas.drawLine(3f, 3f, 3f, 7f, strokePaint)
                    canvas.drawLine(21f, 3f, 17f, 3f, strokePaint)
                    canvas.drawLine(21f, 3f, 21f, 7f, strokePaint)
                    canvas.drawLine(3f, 21f, 7f, 21f, strokePaint)
                    canvas.drawLine(3f, 21f, 3f, 17f, strokePaint)
                    canvas.drawLine(21f, 21f, 17f, 21f, strokePaint)
                    canvas.drawLine(21f, 21f, 21f, 17f, strokePaint)
                    canvas.drawRoundRect(RectF(8f, 8f, 16f, 16f), 1f, 1f, strokePaint)
                }
                "copy" -> {
                    canvas.drawRoundRect(RectF(8f, 8f, 22f, 22f), 2f, 2f, strokePaint)
                    val backCard = android.graphics.Path().apply {
                        moveTo(4f, 16f)
                        cubicTo(2.9f, 16f, 2f, 15.1f, 2f, 14f)
                        lineTo(2f, 4f)
                        cubicTo(2f, 2.9f, 2.9f, 2f, 4f, 2f)
                        lineTo(14f, 2f)
                        cubicTo(15.1f, 2f, 16f, 2.9f, 16f, 4f)
                    }
                    canvas.drawPath(backCard, strokePaint)
                }
                "cut" -> {
                    canvas.drawCircle(6f, 6f, 3f, strokePaint)
                    canvas.drawCircle(6f, 18f, 3f, strokePaint)
                    canvas.drawLine(20f, 4f, 8.12f, 15.88f, strokePaint)
                    canvas.drawLine(14.47f, 14.48f, 20f, 20f, strokePaint)
                    canvas.drawLine(8.12f, 8.12f, 12f, 12f, strokePaint)
                }
                "paste" -> {
                    val boardPath = android.graphics.Path().apply {
                        moveTo(16f, 4f)
                        lineTo(18f, 4f)
                        cubicTo(19.1f, 4f, 20f, 4.9f, 20f, 6f)
                        lineTo(20f, 20f)
                        cubicTo(20f, 21.1f, 19.1f, 22f, 18f, 22f)
                        lineTo(6f, 22f)
                        cubicTo(4.9f, 22f, 4f, 21.1f, 4f, 20f)
                        lineTo(4f, 6f)
                        cubicTo(4f, 4.9f, 4.9f, 4f, 6f, 4f)
                        lineTo(8f, 4f)
                    }
                    canvas.drawPath(boardPath, strokePaint)
                    canvas.drawRoundRect(RectF(8f, 2f, 16f, 6f), 1f, 1f, strokePaint)
                    canvas.drawLine(12f, 11f, 12f, 17f, strokePaint)
                    val arrowHead = android.graphics.Path().apply {
                        moveTo(9f, 14f)
                        lineTo(12f, 17f)
                        lineTo(15f, 14f)
                    }
                    canvas.drawPath(arrowHead, strokePaint)
                }
                "echo", "paperclip" -> {
                    val clipPath = android.graphics.Path().apply {
                        moveTo(21.44f, 11.05f)
                        lineTo(12.25f, 20.24f)
                        cubicTo(9.9f, 22.59f, 6.1f, 22.59f, 3.76f, 20.24f)
                        cubicTo(1.41f, 17.89f, 1.41f, 14.09f, 3.76f, 11.75f)
                        lineTo(12.33f, 3.18f)
                        cubicTo(13.89f, 1.62f, 16.42f, 1.62f, 17.99f, 3.18f)
                        cubicTo(19.55f, 4.74f, 19.55f, 7.27f, 17.99f, 8.84f)
                        lineTo(9.4f, 17.42f)
                        cubicTo(8.62f, 18.2f, 7.35f, 18.2f, 6.57f, 17.42f)
                        cubicTo(5.79f, 16.64f, 5.79f, 15.37f, 6.57f, 14.59f)
                        lineTo(14.45f, 6.71f)
                    }
                    canvas.drawPath(clipPath, strokePaint)
                }
            }
            canvas.restoreToCount(saveCount)
        }

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
            val availableHeight = height.toFloat() - (padding * 2f)
            val itemWidth = availableWidth / numCols
            val itemHeight = availableHeight / numRows
            val buttonRadius = 10f * density

            options.forEachIndexed { index, option ->
                val row = index / numCols
                val col = index % numCols

                val buttonRect = RectF(
                    padding + (col * itemWidth) + (3f * density),
                    padding + (row * itemHeight) + (3f * density),
                    padding + ((col + 1) * itemWidth) - (3f * density),
                    padding + ((row + 1) * itemHeight) - (3f * density)
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

                // Check for SVG Icon
                val iconName = when (option.trim().uppercase()) {
                    "SELECT ALL", "SELECT_ALL" -> "select_all"
                    "COPY" -> "copy"
                    "CUT" -> "cut"
                    "PASTE" -> "paste"
                    "PASTE ECHO", "PASTE_ECHO", "ECHO" -> "echo"
                    else -> null
                }

                val displayOption = getDisplayOption(option)
                if (iconName != null) {
                    val iconRect = RectF(
                        buttonRect.centerX() - (11f * density),
                        buttonRect.top + (4f * density),
                        buttonRect.centerX() + (11f * density),
                        buttonRect.top + (26f * density)
                    )
                    drawSvgIcon(canvas, iconName, iconRect, textPaintToUse)

                    val labelPaint = Paint(textPaintToUse).apply {
                        textSize = fontSize * 0.78f
                    }
                    com.infinikey_ime.util.FontFallbackManager.applyToPaint(labelPaint, displayOption)
                    val fontMetrics = labelPaint.fontMetrics
                    val baseline = buttonRect.bottom - (4f * density) - fontMetrics.descent
                    canvas.drawText(displayOption, buttonRect.centerX(), baseline, labelPaint)
                } else {
                    com.infinikey_ime.util.FontFallbackManager.applyToPaint(textPaintToUse, displayOption)
                    val fontMetrics = textPaintToUse.fontMetrics
                    val baseline = buttonRect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2f
                    canvas.drawText(displayOption, buttonRect.centerX(), baseline, textPaintToUse)
                }
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val density = context.resources.displayMetrics.density
            val padding = 6f * density
            val availableWidth = width.toFloat() - (padding * 2f)
            val availableHeight = height.toFloat() - (padding * 2f)
            if (availableWidth > 0f && availableHeight > 0f && options.isNotEmpty()) {
                val itemWidth = availableWidth / numCols
                val itemHeight = availableHeight / numRows

                val col = ((event.x - padding) / itemWidth).toInt().coerceIn(0, numCols - 1)
                val row = ((event.y - padding) / itemHeight).toInt().coerceIn(0, numRows - 1)
                val tappedIdx = (row * numCols + col).coerceIn(0, options.size - 1)

                when (event.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        if (tappedIdx != selectedIndex) {
                            selectedIndex = tappedIdx
                            invalidate()
                            onHoverChanged?.invoke(selectedIndex, options[selectedIndex])
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        selectedIndex = tappedIdx
                        commitSelection()
                        onDismissRequest()
                    }
                }
            }
            return true
        }

        fun updateSelection(localX: Float, localY: Float, action: Int? = null) {
            val density = context.resources.displayMetrics.density
            val padding = 6f * density
            val availableWidth = width.toFloat() - (padding * 2f)
            val availableHeight = height.toFloat() - (padding * 2f)
            if (availableWidth <= 0f || availableHeight <= 0f || options.isEmpty()) return

            val itemWidth = availableWidth / numCols
            val itemHeight = availableHeight / numRows

            if (initialTouchX == null || initialTouchY == null || action == MotionEvent.ACTION_DOWN) {
                initialTouchX = localX
                initialTouchY = localY
                selectedIndex = 0
                invalidate()
                onHoverChanged?.invoke(selectedIndex, options[selectedIndex])
                return
            }

            val startX = initialTouchX ?: localX
            val startY = initialTouchY ?: localY
            val touchDelta = Math.hypot((localX - startX).toDouble(), (localY - startY).toDouble())
            val dragThreshold = 28f * density

            val newIndex = if (touchDelta < dragThreshold) {
                0
            } else {
                val rawCol = ((localX - padding) / itemWidth).toInt().coerceIn(0, numCols - 1)
                val rawRow = ((localY - padding) / itemHeight).toInt().coerceIn(0, numRows - 1)
                val rawIdx = (rawRow * numCols + rawCol).coerceIn(0, options.size - 1)

                if (rawIdx != selectedIndex) {
                    val currentCol = selectedIndex % numCols
                    val currentRow = selectedIndex / numCols
                    val currentCenterX = padding + (currentCol + 0.5f) * itemWidth
                    val currentCenterY = padding + (currentRow + 0.5f) * itemHeight
                    val deltaFromCurrentCenter = Math.hypot((localX - currentCenterX).toDouble(), (localY - currentCenterY).toDouble())
                    if (deltaFromCurrentCenter > minOf(itemWidth, itemHeight) * 0.4f) {
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
                onHoverChanged?.invoke(selectedIndex, options[selectedIndex])
            }
        }

        fun commitSelection() {
            if (selectedIndex in options.indices) {
                onItemSelected(selectedIndex, options[selectedIndex])
            }
        }
    }
}
