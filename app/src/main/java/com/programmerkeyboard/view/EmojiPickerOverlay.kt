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
 * Native System Emoji Picker Overlay for Programmer Keyboard.
 * Uses the device's native system emoji font (Typeface.DEFAULT) to dynamically render standard Unicode emojis.
 */
class EmojiPickerOverlay(
    private val context: Context,
    private val onEmojiSelected: (String) -> Unit
) {
    private var popupWindow: PopupWindow? = null
    private var popupView: EmojiPickerView? = null

    fun show(anchorView: View) {
        dismiss()

        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1]
        val width = anchorView.width
        val height = anchorView.height

        popupView = EmojiPickerView(context, onEmojiSelected) {
            dismiss()
        }.apply {
            layoutParams = ViewGroup.LayoutParams(width, height)
        }

        popupWindow = PopupWindow(popupView, width, height, true).apply {
            isClippingEnabled = true
            showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y)
        }
    }

    fun dismiss() {
        popupWindow?.dismiss()
        popupWindow = null
        popupView = null
    }

    private class EmojiPickerView(
        context: Context,
        private val onEmojiSelected: (String) -> Unit,
        private val onCloseRequested: () -> Unit
    ) : View(context) {

        private val categories = listOf(
            Category("😀", listOf("😀","😃","😄","😁","😆","😅","😂","🤣","🥲","😊","😇","🙂","🙃","😉","😌","😍","🥰","😘","😋","😛","😜","🤪","🤨","🧐","🤓","😎","🤩","🥳","😏","😒","😞","😔","😟","😕","🙁","☹️","😣","😖","😫","😩","🥺","😢","😭","😤","😠","😡","🤬","🤯","😳","🥵","🥶","😱","😨","😰","😥","😓","🤗","🤔","🤭","🤫","🤥","😶","😐","😑","😬","🙄","😯","😦","😧","😮","😲","🥱","😴","🤤","😪","😵","🤐","🥴","🤢","🤮","🤧","😷","🤒","🤕")),
            Category("👍", listOf("👍","👎","👌","🤌","🤏","✌️","🤞","🤟","🤘","🤙","👈","👉","👆","🖕","👇","☝️","✊","👊","🤛","🤜","👏","🙌","👐","🤲","🤝","🙏","✍️","💅","🤳","💪","🦵","🦶","👂","👃","🧠","🫀","🫁","🦷","🦴","👀","👁️","👅","👄")),
            Category("❤️", listOf("❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔","❣️","💕","💞","💓","💗","💖","💘","💝","✨","⭐","🌟","💫","💥","🔥","🎉","🎊","💯","✅","❌","❓","❗","‼️","⁉️")),
            Category("💻", listOf("💻","🖥️","🖨️","🖱️","🪛","🔧","🔨","⚙️","⚖️","⛓️","⚡","🔋","🔌","📱","📞","📟","📠","📷","📹","🎥","📻","💡","🔦","🏮","📔","📕","📖","📗","📘","📙","📚","📓","📒","📝","✏️","✒️","🖊️","🖌️","🖍️","📌","📍","✂️","🔒","🔓","🔑","🗝️"))
        )

        private var selectedCategoryIndex = 0
        private val categoryRects = mutableListOf<RectF>()
        private val emojiRects = mutableListOf<Pair<String, RectF>>()
        private val closeRect = RectF()

        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.keyboard_background)
            style = Paint.Style.FILL
        }

        private val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.key_background_modifier)
            style = Paint.Style.FILL
        }

        private val tabActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.key_background_modifier_active)
            style = Paint.Style.FILL
        }

        private val emojiTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.key_text_primary)
            textSize = 44f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }

        private val closeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.key_text_primary)
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val density = context.resources.displayMetrics.density

            // Background
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Header bar
            val headerHeight = 44f * density
            canvas.drawRect(0f, 0f, width.toFloat(), headerHeight, headerBgPaint)

            categoryRects.clear()
            val tabWidth = (width.toFloat() - (60f * density)) / categories.size

            categories.forEachIndexed { idx, category ->
                val tabRect = RectF(idx * tabWidth, 0f, (idx + 1) * tabWidth, headerHeight)
                categoryRects.add(tabRect)

                if (idx == selectedCategoryIndex) {
                    canvas.drawRoundRect(RectF(tabRect.left + 6f, 4f, tabRect.right - 6f, headerHeight - 4f), 10f, 10f, tabActivePaint)
                }

                val fontMetrics = emojiTextPaint.fontMetrics
                val baseline = tabRect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2
                canvas.drawText(category.icon, tabRect.centerX(), baseline, emojiTextPaint)
            }

            // Close button (X)
            closeRect.set(width.toFloat() - (54f * density), 4f, width.toFloat() - 6f, headerHeight - 4f)
            canvas.drawRoundRect(closeRect, 10f, 10f, headerBgPaint)
            val closeMetrics = closeTextPaint.fontMetrics
            val closeBaseline = closeRect.centerY() - (closeMetrics.ascent + closeMetrics.descent) / 2
            canvas.drawText("✕", closeRect.centerX(), closeBaseline, closeTextPaint)

            // Emoji grid
            emojiRects.clear()
            val currentList = categories[selectedCategoryIndex].emojis
            val columns = 8
            val cellWidth = width.toFloat() / columns
            val cellHeight = (height.toFloat() - headerHeight) / 4

            currentList.forEachIndexed { index, emoji ->
                val row = index / columns
                val col = index % columns
                if (row < 4) {
                    val rect = RectF(
                        col * cellWidth,
                        headerHeight + row * cellHeight,
                        (col + 1) * cellWidth,
                        headerHeight + (row + 1) * cellHeight
                    )
                    emojiRects.add(Pair(emoji, rect))

                    val fontMetrics = emojiTextPaint.fontMetrics
                    val baseline = rect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2
                    canvas.drawText(emoji, rect.centerX(), baseline, emojiTextPaint)
                }
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_UP) {
                val x = event.x
                val y = event.y

                if (closeRect.contains(x, y)) {
                    onCloseRequested()
                    return true
                }

                categoryRects.forEachIndexed { idx, rect ->
                    if (rect.contains(x, y)) {
                        selectedCategoryIndex = idx
                        invalidate()
                        return true
                    }
                }

                emojiRects.forEach { (emoji, rect) ->
                    if (rect.contains(x, y)) {
                        onEmojiSelected(emoji)
                        return true
                    }
                }
            }
            return true
        }

        private data class Category(val icon: String, val emojis: List<String>)
    }
}
