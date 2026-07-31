package com.programmerkeyboard.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.programmerkeyboard.model.DimensionValue
import com.programmerkeyboard.model.KeyAction
import com.programmerkeyboard.model.KeyDefinition
import com.programmerkeyboard.model.KeyRow
import com.programmerkeyboard.model.LayoutDefinition
import kotlin.math.max

class InteractiveLayoutEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class KeyBounds(
        val rowIdx: Int,
        val keyIdx: Int,
        val key: KeyDefinition,
        val rect: RectF
    )

    data class AddKeyBounds(
        val rowIdx: Int,
        val rect: RectF
    )

    var layoutDefinition: LayoutDefinition? = null
        set(value) {
            field = value
            recalculateBounds()
            invalidate()
        }

    var onKeyTappedListener: ((rowIdx: Int, keyIdx: Int, key: KeyDefinition) -> Unit)? = null
    var onKeyReorderedListener: ((fromRow: Int, fromKey: Int, toRow: Int, toKey: Int) -> Unit)? = null
    var onAddKeyToRowListener: ((rowIdx: Int) -> Unit)? = null
    var onAddRowListener: (() -> Unit)? = null

    private val keyBoundsList = mutableListOf<KeyBounds>()
    private val addKeyBoundsList = mutableListOf<AddKeyBounds>()
    private var addRowRect: RectF? = null

    // Touch & Drag state
    private var touchedKeyBounds: KeyBounds? = null
    private var isDragging = false
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var currentTouchX = 0f
    private var currentTouchY = 0f
    private val dragSlopPx = 16f

    // Paints
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        style = Paint.Style.FILL
    }

    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#161923")
        style = Paint.Style.FILL
    }

    private val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#334155")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F8FAFC")
        textSize = 28f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        textSize = 18f
        textAlign = Paint.Align.CENTER
    }

    private val addBtnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E3A5F")
        style = Paint.Style.FILL
    }

    private val addBtnTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8")
        textSize = 32f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val ghostCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8")
        alpha = 180
        style = Paint.Style.FILL
    }

    private fun parseColor(hex: String?, defaultColor: Int): Int {
        if (hex.isNullOrEmpty()) return defaultColor
        return try {
            Color.parseColor(hex)
        } catch (_: Exception) {
            defaultColor
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalculateBounds()
    }

    private fun recalculateBounds() {
        keyBoundsList.clear()
        addKeyBoundsList.clear()
        addRowRect = null

        val currentLayout = layoutDefinition ?: return
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val density = context.resources.displayMetrics.density
        val padding = 12f * density
        val spacing = 6f * density
        val availableW = w - (padding * 2)

        val rows = currentLayout.rows
        val totalRows = rows.size + 1 // extra slot for "Add Row" button
        val availableH = h - (padding * 2) - (spacing * (totalRows - 1))
        val rowH = (availableH / totalRows).coerceIn(36f * density, 60f * density)

        var currentY = padding

        rows.forEachIndexed { rIdx, row ->
            val addBtnW = 44f * density
            val rowKeysW = availableW - addBtnW - spacing
            val keys = row.keys.filter { !it.isSpacer }

            val totalWeight = keys.sumOf {
                (it.widthWeight as? DimensionValue.Ratio)?.value?.toDouble() ?: 1.0
            }.toFloat().coerceAtLeast(0.1f)

            var currentX = padding

            keys.forEachIndexed { kIdx, key ->
                val weight = (key.widthWeight as? DimensionValue.Ratio)?.value ?: 1.0f
                val keyW = (rowKeysW * (weight / totalWeight)) - spacing
                val rect = RectF(currentX, currentY, currentX + keyW, currentY + rowH)
                keyBoundsList.add(KeyBounds(rIdx, kIdx, key, rect))
                currentX += keyW + spacing
            }

            // Add Key button on right of row
            val addKeyRect = RectF(padding + rowKeysW, currentY, padding + availableW, currentY + rowH)
            addKeyBoundsList.add(AddKeyBounds(rIdx, addKeyRect))

            currentY += rowH + spacing
        }

        // Add Row button at bottom
        addRowRect = RectF(padding, currentY, padding + availableW, currentY + rowH)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(parseColor(layoutDefinition?.theme?.backgroundColor, Color.parseColor("#0F172A")))

        val density = context.resources.displayMetrics.density

        // Draw Keys
        keyBoundsList.forEach { kb ->
            if (isDragging && touchedKeyBounds == kb) {
                // Don't draw original card if being dragged (or draw placeholder)
                val placeholderPaint = Paint(cardPaint).apply { alpha = 60 }
                canvas.drawRoundRect(kb.rect, 8f * density, 8f * density, placeholderPaint)
                return@forEach
            }

            val bgCol = kb.key.bgColor ?: Color.parseColor("#161923")
            val fgCol = kb.key.fgColor ?: Color.parseColor("#F8FAFC")
            val keyCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = bgCol
                style = Paint.Style.FILL
            }

            canvas.drawRoundRect(kb.rect, 8f * density, 8f * density, keyCardPaint)
            canvas.drawRoundRect(kb.rect, 8f * density, 8f * density, cardBorderPaint)

            // Primary Label
            textPaint.color = fgCol
            textPaint.textSize = (kb.rect.height() * 0.38f).coerceIn(14f, 26f)
            val textY = kb.rect.centerY() + (textPaint.textSize / 3f)
            canvas.drawText(kb.key.primaryLabel, kb.rect.centerX(), textY, textPaint)

            // Secondary label (top right)
            if (!kb.key.secondaryLabel.isNullOrEmpty()) {
                subTextPaint.color = fgCol
                subTextPaint.alpha = 180
                subTextPaint.textSize = (kb.rect.height() * 0.22f).coerceIn(10f, 16f)
                canvas.drawText(kb.key.secondaryLabel, kb.rect.right - 12f * density, kb.rect.top + 16f * density, subTextPaint)
            }
        }

        // Draw Add Key buttons
        addKeyBoundsList.forEach { akb ->
            canvas.drawRoundRect(akb.rect, 8f * density, 8f * density, addBtnPaint)
            canvas.drawRoundRect(akb.rect, 8f * density, 8f * density, cardBorderPaint)
            addBtnTextPaint.textSize = (akb.rect.height() * 0.5f).coerceIn(16f, 32f)
            val y = akb.rect.centerY() + (addBtnTextPaint.textSize / 3f)
            canvas.drawText("+", akb.rect.centerX(), y, addBtnTextPaint)
        }

        // Draw Add Row button
        addRowRect?.let { rect ->
            val rowBtnPaint = Paint(addBtnPaint).apply { color = Color.parseColor("#0F766E") }
            canvas.drawRoundRect(rect, 8f * density, 8f * density, rowBtnPaint)
            canvas.drawRoundRect(rect, 8f * density, 8f * density, cardBorderPaint)

            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = (rect.height() * 0.36f).coerceIn(14f, 22f)
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            val y = rect.centerY() + (labelPaint.textSize / 3f)
            canvas.drawText("➕ Add New Key Row", rect.centerX(), y, labelPaint)
        }

        // Draw Ghost Card during drag
        if (isDragging && touchedKeyBounds != null) {
            val ghostW = touchedKeyBounds!!.rect.width()
            val ghostH = touchedKeyBounds!!.rect.height()
            val ghostRect = RectF(
                currentTouchX - (ghostW / 2f),
                currentTouchY - (ghostH / 2f),
                currentTouchX + (ghostW / 2f),
                currentTouchY + (ghostH / 2f)
            )

            canvas.drawRoundRect(ghostRect, 10f * density, 10f * density, ghostCardPaint)
            textPaint.color = Color.WHITE
            val textY = ghostRect.centerY() + (textPaint.textSize / 3f)
            canvas.drawText(touchedKeyBounds!!.key.primaryLabel, ghostRect.centerX(), textY, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.x
                touchDownY = event.y
                currentTouchX = event.x
                currentTouchY = event.y
                isDragging = false

                touchedKeyBounds = keyBoundsList.find { it.rect.contains(event.x, event.y) }
                if (touchedKeyBounds != null) {
                    invalidate()
                    return true
                }

                val addKeyTarget = addKeyBoundsList.find { it.rect.contains(event.x, event.y) }
                if (addKeyTarget != null) {
                    onAddKeyToRowListener?.invoke(addKeyTarget.rowIdx)
                    return true
                }

                if (addRowRect?.contains(event.x, event.y) == true) {
                    onAddRowListener?.invoke()
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                currentTouchX = event.x
                currentTouchY = event.y

                if (touchedKeyBounds != null) {
                    val dist = Math.hypot((event.x - touchDownX).toDouble(), (event.y - touchDownY).toDouble())
                    if (dist > dragSlopPx) {
                        isDragging = true
                    }
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (touchedKeyBounds != null) {
                    if (isDragging) {
                        // Find drop target key
                        val targetKeyBounds = keyBoundsList.find { it.rect.contains(event.x, event.y) }
                        if (targetKeyBounds != null && targetKeyBounds != touchedKeyBounds) {
                            onKeyReorderedListener?.invoke(
                                touchedKeyBounds!!.rowIdx,
                                touchedKeyBounds!!.keyIdx,
                                targetKeyBounds.rowIdx,
                                targetKeyBounds.keyIdx
                            )
                        }
                    } else {
                        // Tap event
                        onKeyTappedListener?.invoke(
                            touchedKeyBounds!!.rowIdx,
                            touchedKeyBounds!!.keyIdx,
                            touchedKeyBounds!!.key
                        )
                    }
                }
                touchedKeyBounds = null
                isDragging = false
                invalidate()
            }
        }
        return true
    }
}
