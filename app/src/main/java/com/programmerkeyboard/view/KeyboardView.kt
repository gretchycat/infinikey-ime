package com.programmerkeyboard.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.programmerkeyboard.R
import com.programmerkeyboard.model.DimensionValue
import com.programmerkeyboard.model.KeyAction
import com.programmerkeyboard.model.KeyDefinition
import com.programmerkeyboard.model.KeyRow
import com.programmerkeyboard.model.KeyboardState
import com.programmerkeyboard.model.LayoutDefinition
import kotlin.math.abs

class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Layout Scrolling State
    private var scrollOffsetY = 0f
    private var scrollOffsetX = 0f
    private var maxScrollOffsetY = 0f
    private var maxScrollOffsetX = 0f
    private var isScrollDragging = false
    private var lastScrollTouchY = 0f
    private var lastScrollTouchX = 0f
    private var scrollViewportHeight = 0f
    private var totalScrollContentHeight = 0f
    private var isTouchInScrollableRegion = false
    private var velocityTracker: android.view.VelocityTracker? = null
    private var editorDownX = 0f
    private var editorDownY = 0f
    private var isEditorDragging = false

    private val inertialScrollRunnable = object : Runnable {
        var velocityY = 0f
        override fun run() {
            if (Math.abs(velocityY) < 50f) return
            velocityY *= 0.90f
            val dy = -velocityY * 0.016f
            val newOffset = (scrollOffsetY + dy).coerceIn(0f, maxScrollOffsetY)
            if (newOffset != scrollOffsetY) {
                scrollOffsetY = newOffset
                recalculateKeyBounds()
                invalidate()
                handler.postDelayed(this, 16)
            }
        }
    }

    var layoutDefinition: LayoutDefinition? = null
        set(value) {
            val isFirstLoad = (field == null)
            field = value
            scrollOffsetY = 0f
            scrollOffsetX = 0f
            value?.let { def ->
                if (isFirstLoad) {
                    heightPercentage = def.metadata.defaultHeightPercentage
                }
            }
            recalculateKeyBounds()
            requestLayout()
            invalidate()
        }

    var keyboardState: KeyboardState = KeyboardState()
        set(value) {
            field = value
            invalidate()
        }

    var heightPercentage: Int = 30
        set(value) {
            field = value.coerceIn(15, 60)
            requestLayout()
            invalidate()
        }

    var onKeyActionListener: ((KeyAction) -> Unit)? = null
    var onLayoutChangeListener: ((String) -> Unit)? = null
    var onScreenModeChangeListener: ((String) -> Unit)? = null
    var onRowToggleListener: ((String, Boolean) -> Unit)? = null

    // Track dynamic row visibility overrides
    private val rowVisibilityMap = mutableMapOf<Any, Boolean>()

    fun setRowVisibilityMap(map: Map<String, Boolean>) {
        rowVisibilityMap.clear()
        map.forEach { (k, v) -> rowVisibilityMap[k] = v }
        recalculateKeyBounds()
        requestLayout()
        invalidate()
    }

    fun getRowVisibilityMap(): Map<String, Boolean> {
        return rowVisibilityMap.mapKeys { it.key.toString() }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val displayMetrics = context.resources.displayMetrics
        val prefs = context.getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        val defaultHeight = if (isLandscape) 45 else 30
        val heightKey = if (isLandscape) "pref_keyboard_height_percent_landscape" else "pref_keyboard_height_percent_portrait"
        val fallbackHeight = prefs.getInt("pref_keyboard_height_percent", defaultHeight)
        val effectiveHeightPercent = prefs.getInt(heightKey, fallbackHeight).coerceIn(15, 65)

        val calculatedHeight = if (keyboardState.formFactorMode == com.programmerkeyboard.model.FormFactorMode.FLOATING) {
            (displayMetrics.heightPixels * 0.45f).toInt()
        } else {
            (displayMetrics.heightPixels * (effectiveHeightPercent / 100f)).toInt()
        }

        setMeasuredDimension(width, calculatedHeight)
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.keyboard_background)
    }

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.key_background)
        style = Paint.Style.FILL
    }

    private val keyModifierPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.key_background_modifier)
        style = Paint.Style.FILL
    }

    private val keyModifierActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.key_background_modifier_active)
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.key_border)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.key_text_primary)
        textSize = 36f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private val secondaryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.key_text_secondary)
        textSize = 22f
        textAlign = Paint.Align.RIGHT
        isFakeBoldText = true
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private val customBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val customBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val indicatorDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#38BDF8")
        style = Paint.Style.FILL
    }

    private val lockedDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#F59E0B")
        style = Paint.Style.FILL
    }

    data class KeyBounds(val key: KeyDefinition, val rect: RectF, val rowIndex: Int = 0, val isFixedRow: Boolean = false)

    private val keyBoundsList = mutableListOf<KeyBounds>()
    private var pressedKeyBounds: KeyBounds? = null
    private var lastKeyTapTimeMs: Long = 0L
    private var lastKeyTapLabel: String = ""

    // Long press and popup overlay handlers
    private val handler = Handler(Looper.getMainLooper())
    private var isLongPressTriggered = false
    private var keyPopupOverlay: KeyPopupOverlay? = null
    private var keyPreviewOverlay: KeyPreviewOverlay? = null
    private var activeZoomedText: String? = null
    private var joystickPopupWidget: JoystickPopupWidget? = null
    private var emojiPickerOverlay: EmojiPickerOverlay? = null
    private var voiceInputOverlay: VoiceInputOverlay? = null
    private var autoRepeatRunnable: Runnable? = null

    var activeListeningStatus: String? = null
        set(value) {
            field = value
            invalidate()
        }

    fun showEmojiPicker() {
        emojiPickerOverlay?.dismiss()
        emojiPickerOverlay = EmojiPickerOverlay(context) { selectedEmoji ->
            onKeyActionListener?.invoke(KeyAction.SendText(selectedEmoji))
        }.also {
            it.show(this)
        }
    }

    // Multi-touch tracking
    private var initialPointersDistance = 0f
    private var initialPointersCenterX = 0f
    private var initialPointersCenterY = 0f

    // Trackpad gesture state
    private var isTrackpadActive = false
    private var isSpacebarTrackpad = false
    private var trackpadLastX = 0f
    private var trackpadLastY = 0f
    private var trackpadAccumulatedDx = 0f
    private var trackpadAccumulatedDy = 0f
    private var trackpadTouchX = 0f
    private var trackpadTouchY = 0f
    private var trackpadBlinkStep = 0
    private var trackpadBlinkTime = 0L
    private val trackpadBlinkColors = intArrayOf(
        android.graphics.Color.parseColor("#00F0FF"),
        android.graphics.Color.parseColor("#10B981"),
        android.graphics.Color.parseColor("#A855F7"),
        android.graphics.Color.parseColor("#F59E0B"),
        android.graphics.Color.parseColor("#EC4899"),
        android.graphics.Color.parseColor("#3B82F6")
    )

    // Floating Window Draggable Position Offsets
    private var floatingOffsetX = 0f
    private var floatingOffsetY = 0f
    private var isDraggingFloatingWindow = false
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var initialFloatingOffsetX = 0f
    private var initialFloatingOffsetY = 0f

    var onFloatingBoundsChangedListener: (() -> Unit)? = null

    init {
        val prefs = context.getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        floatingOffsetX = prefs.getFloat("pref_floating_offset_x", 0f)
        floatingOffsetY = prefs.getFloat("pref_floating_offset_y", 0f)
    }

    fun getFloatingCardBounds(): RectF? {
        if (keyboardState.formFactorMode != com.programmerkeyboard.model.FormFactorMode.FLOATING) return null
        if (keyBoundsList.isNotEmpty()) {
            val minLeft = keyBoundsList.minOf { it.rect.left }
            val maxRight = keyBoundsList.maxOf { it.rect.right }
            val minTop = keyBoundsList.minOf { it.rect.top }
            val maxBottom = keyBoundsList.maxOf { it.rect.bottom }
            val d = resources.displayMetrics.density
            val topHandleH = 26f * d
            val pad = 8f * d
            return RectF(minLeft - pad, minTop - topHandleH, maxRight + pad, maxBottom + pad)
        }

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return null

        val d = resources.displayMetrics.density
        val aspectRatio = getKeyboardAspectRatio()
        val idealWidth = h * aspectRatio
        val activeWidth = minOf(w, idealWidth)
        val baseStartX = (w - activeWidth) / 2f
        val startX = (baseStartX + floatingOffsetX).coerceIn(4f, maxOf(4f, w - activeWidth - 4f))
        val floatCardHeight = (activeWidth / aspectRatio).coerceIn(120f * d, h * 0.70f)
        val baseStartY = (h - floatCardHeight) / 2f
        val cardTop = (baseStartY + floatingOffsetY).coerceIn(4f, maxOf(4f, h - floatCardHeight - 4f))
        val cardBottom = cardTop + floatCardHeight

        return RectF(startX + 4f, cardTop, startX + activeWidth - 4f, cardBottom)
    }

    var onFormFactorModeChangeListener: ((com.programmerkeyboard.model.FormFactorMode) -> Unit)? = null

    fun setFormFactorMode(mode: com.programmerkeyboard.model.FormFactorMode) {
        keyboardState.formFactorMode = mode
        val prefs = context.getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        val modeStr = when (mode) {
            com.programmerkeyboard.model.FormFactorMode.SPLIT -> "SPLIT"
            com.programmerkeyboard.model.FormFactorMode.LEFT_DOCKED, com.programmerkeyboard.model.FormFactorMode.SIDE_DOCKED -> "LEFT_DOCKED"
            com.programmerkeyboard.model.FormFactorMode.RIGHT_DOCKED -> "RIGHT_DOCKED"
            com.programmerkeyboard.model.FormFactorMode.FLOATING -> "FLOATING"
            else -> "FULL_WIDTH_DOCKED"
        }
        prefs.edit().putString("pref_form_factor", modeStr).apply()
        dismissKeyPreview()
        recalculateKeyBounds()
        requestLayout()
        invalidate()
        onFormFactorModeChangeListener?.invoke(mode)
    }

    fun setLayout(layout: LayoutDefinition) {
        layoutDefinition = layout
        recalculateKeyBounds()
        requestLayout()
        invalidate()
    }

    private val longPressRunnable = Runnable {
        if (layoutDefinition?.id == "phone") return@Runnable
        pressedKeyBounds?.let { bounds ->
            val key = bounds.key
            val isEmoji = isEmojiKey(key)
            if (isEmoji && (key.onLongPressAction is KeyAction.None || key.onLongPressAction == null)) {
                isLongPressTriggered = true
                performKeypressHapticFeedback()
                showZoomPreview(key.primaryLabel, bounds.rect)
                return@Runnable
            }
            if (key.onLongPressAction is KeyAction.None) {
                return@Runnable
            }
            isLongPressTriggered = true
            performKeypressHapticFeedback()
            executeAction(key.onLongPressAction, key)
        }
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) return false
            val diffY = e2.y - e1.y
            val diffX = e2.x - e1.x

            if (pressedKeyBounds != null) {
                val key = pressedKeyBounds!!.key
                if (abs(diffY) > abs(diffX) && abs(diffY) > 80) {
                    if (diffY < 0 && key.onSwipeUpAction !is KeyAction.None) {
                        executeAction(key.onSwipeUpAction, key)
                        return true
                    } else if (diffY > 0 && key.onSwipeDownAction !is KeyAction.None) {
                        executeAction(key.onSwipeDownAction, key)
                        return true
                    }
                } else if (abs(diffX) > 80) {
                    if (diffX < 0 && key.onSwipeLeftAction !is KeyAction.None) {
                        executeAction(key.onSwipeLeftAction, key)
                        return true
                    } else if (diffX > 0 && key.onSwipeRightAction !is KeyAction.None) {
                        executeAction(key.onSwipeRightAction, key)
                        return true
                    }
                }
            }
            return false
        }
    })

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalculateKeyBounds()
    }

    private fun isPrimaryFullWidthLayout(): Boolean {
        val id = layoutDefinition?.id ?: ""
        return id == "main" || id == "mobile" || id == "phone" || id == "full"
    }

    private fun getKeyboardAspectRatio(): Float {
        val prefs = context.getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val aspectKey = if (isLandscape) "pref_keyboard_aspect_ratio_landscape" else "pref_keyboard_aspect_ratio_portrait"
        val defaultRatio = if (isLandscape) 3.0f else 1.75f
        val fallbackRatio = prefs.getFloat("pref_keyboard_aspect_ratio", defaultRatio)
        return prefs.getFloat(aspectKey, fallbackRatio).coerceIn(1.0f, 5.0f)
    }

    data class RowGearBounds(val rowIdx: Int, val row: com.programmerkeyboard.model.KeyRow, val rect: RectF)
    val rowGearBoundsList = mutableListOf<RowGearBounds>()
    var onRowTapForEditingListener: ((rowIdx: Int, row: com.programmerkeyboard.model.KeyRow) -> Unit)? = null

    private fun isRowVisible(row: com.programmerkeyboard.model.KeyRow): Boolean {
        if (isEditorPreviewMode) return true
        val layoutId = layoutDefinition?.id ?: ""
        val qualifiedKey = "$layoutId:${row.id}"
        val override = rowVisibilityMap[qualifiedKey]
        if (override != null) return override
        if (layoutId == "main" || layoutId == "function") {
            return !row.hidden || keyboardState.isFnActive
        }
        return !row.hidden
    }

    fun recalculateKeyBounds() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val allRows = layoutDefinition?.rows ?: emptyList()
        if (allRows.isEmpty()) {
            keyBoundsList.clear()
            return
        }

        val currentRows = allRows.filter { isRowVisible(it) }

        if (currentRows.isEmpty()) {
            keyBoundsList.clear()
            return
        }

        val density = context.resources.displayMetrics.density
        val hSpacingPx = resolveDimension(layoutDefinition?.metadata?.horizontalSpacing, w, density, 4f)
        val vSpacingPx = resolveDimension(layoutDefinition?.metadata?.verticalSpacing, h, density, 4f)

        val isVerticalScroll = layoutDefinition?.metadata?.scrollDirection.equals("VERTICAL", ignoreCase = true)
        val maxVisRows = layoutDefinition?.metadata?.maxVisibleRows ?: 4
        val fixedTopRowsCount = if (isVerticalScroll && currentRows.size > 2) 1 else 0
        val fixedBottomRowsCount = if (isVerticalScroll && currentRows.size > 2) 1 else 0
        val scrollingRowsCount = if (isVerticalScroll) maxOf(0, currentRows.size - fixedTopRowsCount - fixedBottomRowsCount) else currentRows.size
        val effectiveVisibleRowCount = if (isVerticalScroll) (maxVisRows + fixedTopRowsCount + fixedBottomRowsCount) else currentRows.size

        val availableHeight = h - (vSpacingPx * (effectiveVisibleRowCount + 1))
        val rowHeight = availableHeight / effectiveVisibleRowCount

        if (isVerticalScroll) {
            scrollViewportHeight = maxVisRows * (rowHeight + vSpacingPx)
            totalScrollContentHeight = scrollingRowsCount * (rowHeight + vSpacingPx)
            maxScrollOffsetY = (totalScrollContentHeight - scrollViewportHeight).coerceAtLeast(0f)
            scrollOffsetY = scrollOffsetY.coerceIn(0f, maxScrollOffsetY)
        } else {
            maxScrollOffsetY = 0f
            scrollOffsetY = 0f
        }

        keyBoundsList.clear()
        rowGearBoundsList.clear()

        val formFactor = keyboardState.formFactorMode
        val widthRatio = getKeyboardAspectRatio()
        val ratioWidth = (widthRatio * h).coerceIn(120f * density, w)

        val targetWidth = when (formFactor) {
            com.programmerkeyboard.model.FormFactorMode.FULL_WIDTH_DOCKED -> w
            else -> ratioWidth
        }
        val activeWidth = targetWidth

        when (formFactor) {
            com.programmerkeyboard.model.FormFactorMode.FULL_WIDTH_DOCKED,
            com.programmerkeyboard.model.FormFactorMode.LEFT_DOCKED,
            com.programmerkeyboard.model.FormFactorMode.SIDE_DOCKED,
            com.programmerkeyboard.model.FormFactorMode.RIGHT_DOCKED,
            com.programmerkeyboard.model.FormFactorMode.FLOATING -> {
                val maxRowRatioWeight = currentRows.maxOfOrNull { row ->
                    row.keys.sumOf { (it.widthWeight as? DimensionValue.Ratio)?.value?.toDouble() ?: 1.0 }.toFloat()
                } ?: 1.0f

                val editorGearWidth = if (isEditorPreviewMode) 40f * density else 0f
                val availableWidthForRatio = targetWidth - editorGearWidth - (hSpacingPx * (maxRowRatioWeight + 1))
                val globalBaseUnit = if (maxRowRatioWeight > 0) maxOf(0f, availableWidthForRatio / maxRowRatioWeight) else 0f

                val (startX, keysStartY, floatRowHeight) = when (formFactor) {
                    com.programmerkeyboard.model.FormFactorMode.FULL_WIDTH_DOCKED -> {
                        Triple(0f, vSpacingPx, rowHeight)
                    }
                    com.programmerkeyboard.model.FormFactorMode.LEFT_DOCKED,
                    com.programmerkeyboard.model.FormFactorMode.SIDE_DOCKED -> {
                        Triple(0f, vSpacingPx, rowHeight)
                    }
                    com.programmerkeyboard.model.FormFactorMode.RIGHT_DOCKED -> {
                        Triple(w - targetWidth, vSpacingPx, rowHeight)
                    }
                    com.programmerkeyboard.model.FormFactorMode.FLOATING -> {
                        val baseStartX = (w - activeWidth) / 2f
                        val sx = (baseStartX + floatingOffsetX).coerceIn(4f, maxOf(4f, w - activeWidth - 4f))
                        val topHandlePadding = 26f * density
                        val totalKeysHeight = (rowHeight + vSpacingPx) * currentRows.size
                        val baseStartY = (h - totalKeysHeight) / 2f
                        val cardTop = (baseStartY + floatingOffsetY).coerceIn(4f, maxOf(4f, h - totalKeysHeight - topHandlePadding - 4f))
                        val sy = cardTop + topHandlePadding
                        Triple(sx, sy, rowHeight)
                    }
                    else -> Triple(0f, vSpacingPx, rowHeight)
                }

                currentRows.forEachIndexed { rowIndex, row ->
                    val rowRatioWeight = row.keys.sumOf { (it.widthWeight as? DimensionValue.Ratio)?.value?.toDouble() ?: 1.0 }.toFloat()
                    val deficit = maxOf(0f, maxRowRatioWeight - rowRatioWeight)
                    val flexCount = row.keys.count { it.isFlexible }
                    val flexBonus = if (flexCount > 0 && deficit > 0f) (deficit / flexCount) else 0f

                    val rowOffsetPx = when (val off = row.leftOffset) {
                        is DimensionValue.Ratio -> targetWidth * off.value
                        is DimensionValue.Absolute -> off.value * density
                        else -> 0f
                    }

                    val currentY = if (isVerticalScroll) {
                        if (rowIndex < fixedTopRowsCount) {
                            vSpacingPx
                        } else if (rowIndex < fixedTopRowsCount + scrollingRowsCount) {
                            val scrollRowIdx = rowIndex - fixedTopRowsCount
                            vSpacingPx + (rowHeight + vSpacingPx) + (scrollRowIdx * (rowHeight + vSpacingPx)) - scrollOffsetY
                        } else {
                            h - rowHeight - vSpacingPx
                        }
                    } else if (formFactor == com.programmerkeyboard.model.FormFactorMode.FLOATING) {
                        keysStartY + rowIndex * (floatRowHeight + vSpacingPx)
                    } else {
                        vSpacingPx + rowIndex * (rowHeight + vSpacingPx)
                    }
                    val curRowHeight = if (formFactor == com.programmerkeyboard.model.FormFactorMode.FLOATING) floatRowHeight else rowHeight

                    var currentX = startX + hSpacingPx + rowOffsetPx

                    if (isEditorPreviewMode) {
                        val gearW = 38f * density
                        val gearRect = RectF(startX + hSpacingPx, currentY, startX + hSpacingPx + gearW, currentY + curRowHeight)
                        rowGearBoundsList.add(RowGearBounds(rowIndex, row, gearRect))
                        currentX += gearW + hSpacingPx
                    }

                    row.keys.forEach { key ->
                        val keyOffsetPx = when (val off = key.startOffset) {
                            is DimensionValue.Ratio -> targetWidth * off.value
                            is DimensionValue.Absolute -> off.value * density
                            else -> 0f
                        }
                        currentX += keyOffsetPx

                        val baseW = (key.widthWeight as? DimensionValue.Ratio)?.value ?: 1.0f
                        val rawWeight = baseW + (if (key.isFlexible) flexBonus else 0f)
                        val effectiveWeight = key.maxWeight?.let { rawWeight.coerceAtMost(it) } ?: rawWeight

                        val keyWidth = when (key.widthWeight) {
                            is DimensionValue.Ratio -> (globalBaseUnit * effectiveWeight) + ((effectiveWeight - 1.0f) * hSpacingPx)
                            is DimensionValue.Absolute -> key.widthWeight.value * density
                        }
                        val isFixed = isVerticalScroll && (rowIndex == 0 || rowIndex == currentRows.lastIndex)
                        val rect = RectF(currentX, currentY, currentX + keyWidth, currentY + curRowHeight)
                        keyBoundsList.add(KeyBounds(key, rect, rowIndex, isFixed))
                        currentX += keyWidth + hSpacingPx
                    }
                }
            }
            com.programmerkeyboard.model.FormFactorMode.SPLIT -> {
                val maxRatioWeight = currentRows.maxOfOrNull { row ->
                    row.keys.sumOf { (it.widthWeight as? DimensionValue.Ratio)?.value?.toDouble() ?: 1.0 }
                }?.toFloat() ?: 10.0f

                val editorGearWidth = if (isEditorPreviewMode) 40f * density else 0f
                val totalAvailableWidth = (targetWidth - editorGearWidth - (hSpacingPx * (maxRatioWeight + 1)))
                val globalBaseUnit = if (maxRatioWeight > 0) maxOf(0f, totalAvailableWidth / maxRatioWeight) else 0f

                currentRows.forEachIndexed { rowIndex, row ->
                    val totalRatioWeight = row.keys.sumOf { (it.widthWeight as? DimensionValue.Ratio)?.value?.toDouble() ?: 1.0 }.toFloat()
                    val deficitWeight = maxOf(0f, maxRatioWeight - totalRatioWeight)
                    val flexCount = row.keys.count { it.isFlexible }
                    val flexBonus = if (flexCount > 0 && deficitWeight > 0f) (deficitWeight / flexCount) else 0f

                    val currentY = if (isVerticalScroll) {
                        if (rowIndex < fixedTopRowsCount) {
                            vSpacingPx
                        } else if (rowIndex < fixedTopRowsCount + scrollingRowsCount) {
                            val scrollRowIdx = rowIndex - fixedTopRowsCount
                            vSpacingPx + (rowHeight + vSpacingPx) + (scrollRowIdx * (rowHeight + vSpacingPx)) - scrollOffsetY
                        } else {
                            h - rowHeight - vSpacingPx
                        }
                    } else {
                        vSpacingPx + rowIndex * (rowHeight + vSpacingPx)
                    }

                    val splitIdx = row.splitIndex ?: row.keys.indexOfFirst { it.isSplitKey }.takeIf { it >= 0 } ?: ((row.keys.size + 1) / 2)
                    val isSplitKey = row.splitKey || (splitIdx in row.keys.indices && row.keys[splitIdx].isSplitKey)

                    fun getKeyWidth(key: KeyDefinition): Float {
                        val baseW = (key.widthWeight as? DimensionValue.Ratio)?.value ?: 1.0f
                        val rawWeight = baseW + (if (key.isFlexible) flexBonus else 0f)
                        val effectiveWeight = key.maxWeight?.let { rawWeight.coerceAtMost(it) } ?: rawWeight
                        return when (key.widthWeight) {
                            is DimensionValue.Ratio -> (globalBaseUnit * effectiveWeight) + ((effectiveWeight - 1.0f) * hSpacingPx)
                            is DimensionValue.Absolute -> key.widthWeight.value * density
                        }
                    }

                    val leftKeys = mutableListOf<KeyDefinition>()
                    val rightKeys = mutableListOf<KeyDefinition>()

                    row.keys.forEachIndexed { keyIdx, key ->
                        if (isSplitKey && keyIdx == splitIdx) {
                            val fullW = getKeyWidth(key)
                            val leftRatio = (key.splitLeftWeight as? DimensionValue.Ratio)?.value?.toDouble() ?: 0.5
                            val rightRatio = (key.splitRightWeight as? DimensionValue.Ratio)?.value?.toDouble() ?: 0.5
                            val totalRatio = maxOf(0.01, leftRatio + rightRatio)
                            val leftFraction = (leftRatio / totalRatio).toFloat()

                            val leftPx = maxOf(1f, fullW * leftFraction)
                            val rightPx = maxOf(1f, fullW * (1f - leftFraction))

                            val leftHalfKey = key.copy(widthWeight = DimensionValue.Absolute((leftPx / density).toInt()))
                            val rightHalfKey = key.copy(widthWeight = DimensionValue.Absolute((rightPx / density).toInt()))

                            leftKeys.add(leftHalfKey)
                            rightKeys.add(rightHalfKey)
                        } else if (keyIdx < splitIdx) {
                            leftKeys.add(key)
                        } else {
                            rightKeys.add(key)
                        }
                    }

                    val leftRowOffsetPx = when (val off = row.leftOffset) {
                        is DimensionValue.Ratio -> targetWidth * off.value
                        is DimensionValue.Absolute -> off.value * density
                        else -> 0f
                    }
                    val rightRowOffsetPx = when (val off = row.rightOffset) {
                        is DimensionValue.Ratio -> targetWidth * off.value
                        is DimensionValue.Absolute -> off.value * density
                        else -> 0f
                    }

                    val isFixed = isVerticalScroll && (rowIndex == 0 || rowIndex == currentRows.lastIndex)

                    // Render Left Cluster (Left-Justified starting at left edge of screen)
                    var currentX = hSpacingPx + leftRowOffsetPx
                    if (isEditorPreviewMode) {
                        val gearW = 38f * density
                        val gearRect = RectF(hSpacingPx, currentY, hSpacingPx + gearW, currentY + rowHeight)
                        rowGearBoundsList.add(RowGearBounds(rowIndex, row, gearRect))
                        currentX += gearW + hSpacingPx
                    }

                    leftKeys.forEach { key ->
                        val keyOffsetPx = when (val off = key.startOffset) {
                            is DimensionValue.Ratio -> targetWidth * off.value
                            is DimensionValue.Absolute -> off.value * density
                            else -> 0f
                        }
                        currentX += keyOffsetPx

                        val kw = getKeyWidth(key)
                        val rect = RectF(currentX, currentY, currentX + kw, currentY + rowHeight)
                        keyBoundsList.add(KeyBounds(key, rect, rowIndex, isFixed))
                        currentX += kw + hSpacingPx
                    }

                    // Render Right Cluster (Right-Justified to Right Screen Edge)
                    val totalRightWidth = rightKeys.sumOf { key ->
                        val keyOffsetPx = when (val off = key.startOffset) {
                            is DimensionValue.Ratio -> targetWidth * off.value
                            is DimensionValue.Absolute -> off.value * density
                            else -> 0f
                        }
                        (keyOffsetPx + getKeyWidth(key)).toDouble()
                    }.toFloat() + (hSpacingPx * maxOf(0, rightKeys.size - 1))

                    currentX = w - hSpacingPx - rightRowOffsetPx - totalRightWidth
                    rightKeys.forEach { key ->
                        val keyOffsetPx = when (val off = key.startOffset) {
                            is DimensionValue.Ratio -> targetWidth * off.value
                            is DimensionValue.Absolute -> off.value * density
                            else -> 0f
                        }
                        currentX += keyOffsetPx

                        val kw = getKeyWidth(key)
                        val rect = RectF(currentX, currentY, currentX + kw, currentY + rowHeight)
                        keyBoundsList.add(KeyBounds(key, rect, rowIndex, isFixed))
                        currentX += kw + hSpacingPx
                    }
                }
            }
        }
    }
    private data class SplitClusterDimensions(
        val leftClusterWidth: Float,
        val rightClusterWidth: Float,
        val rightClusterStartX: Float,
        val splitRatio: Float
    )

    private fun computeSplitClusterDimensions(activeWidth: Float, w: Float): SplitClusterDimensions {
        val currentRows = (layoutDefinition?.rows ?: emptyList()).filter { isRowVisible(it) }

        val customRatio = layoutDefinition?.metadata?.splitClusterRatio
        val splitRatio = customRatio ?: run {
            val maxLeft = currentRows.maxOfOrNull { row ->
                val splitIdx = row.splitIndex ?: row.keys.indexOfFirst { it.isSplitKey }.takeIf { it >= 0 } ?: ((row.keys.size + 1) / 2)
                val isSplitKey = row.splitKey || (splitIdx in row.keys.indices && row.keys[splitIdx].isSplitKey)
                if (isSplitKey && splitIdx in row.keys.indices) {
                    val targetKey = row.keys[splitIdx]
                    val leftW = (targetKey.splitLeftWeight as? DimensionValue.Ratio)?.value?.toDouble()
                        ?: ((targetKey.widthWeight as? DimensionValue.Ratio)?.value?.toDouble()?.div(2.0))
                        ?: 1.0
                    row.keys.take(splitIdx).sumOf { (it.widthWeight as? DimensionValue.Ratio)?.value?.toDouble() ?: 1.0 } + leftW
                } else {
                    row.keys.take(splitIdx).sumOf { (it.widthWeight as? DimensionValue.Ratio)?.value?.toDouble() ?: 1.0 }
                }
            } ?: 6.0
            val maxRight = currentRows.maxOfOrNull { row ->
                val splitIdx = row.splitIndex ?: row.keys.indexOfFirst { it.isSplitKey }.takeIf { it >= 0 } ?: ((row.keys.size + 1) / 2)
                val isSplitKey = row.splitKey || (splitIdx in row.keys.indices && row.keys[splitIdx].isSplitKey)
                if (isSplitKey && splitIdx in row.keys.indices) {
                    val targetKey = row.keys[splitIdx]
                    val totalW = (targetKey.widthWeight as? DimensionValue.Ratio)?.value?.toDouble() ?: 2.0
                    val leftW = (targetKey.splitLeftWeight as? DimensionValue.Ratio)?.value?.toDouble()
                        ?: (totalW / 2.0)
                    val rightW = (targetKey.splitRightWeight as? DimensionValue.Ratio)?.value?.toDouble()
                        ?: maxOf(0.1, totalW - leftW)
                    row.keys.drop(splitIdx + 1).sumOf { (it.widthWeight as? DimensionValue.Ratio)?.value?.toDouble() ?: 1.0 } + rightW
                } else {
                    row.keys.drop(splitIdx).sumOf { (it.widthWeight as? DimensionValue.Ratio)?.value?.toDouble() ?: 1.0 }
                }
            } ?: 6.0
            if (maxLeft + maxRight > 0) {
                (maxLeft / (maxLeft + maxRight)).toFloat()
            } else 0.5f
        }

        val leftClusterWidth = (activeWidth * splitRatio).coerceIn(activeWidth * 0.2f, activeWidth * 0.8f)
        val rightClusterWidth = activeWidth - leftClusterWidth
        val rightClusterStartX = w - rightClusterWidth

        return SplitClusterDimensions(leftClusterWidth, rightClusterWidth, rightClusterStartX, splitRatio)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val density = context.resources.displayMetrics.density
        val aspectRatio = getKeyboardAspectRatio()
        val activeWidth = (w * (aspectRatio / 2.0f)).coerceIn(150f, w)
        if (keyBoundsList.isEmpty()) {
            recalculateKeyBounds()
        }

        if (keyBoundsList.isEmpty()) return

        val themeBg = layoutDefinition?.theme?.backgroundColor
        val bgColor = themeBg ?: ContextCompat.getColor(context, R.color.keyboard_background)

        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }

        when (keyboardState.formFactorMode) {
            com.programmerkeyboard.model.FormFactorMode.FLOATING -> {
                val cardRect = getFloatingCardBounds()
                if (cardRect != null) {
                    canvas.drawRoundRect(cardRect, 20f * density, 20f * density, cardBgPaint)

                    // Prominent Grab Handle Pill & Grip Header
                    val handleWidth = 64f * density
                    val handleHeight = 6f * density
                    val handleX = cardRect.centerX() - handleWidth / 2f
                    val handleY = cardRect.top + 8f * density

                    val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.parseColor("#38BDF8")
                        style = Paint.Style.FILL
                    }
                    val handleRect = RectF(handleX, handleY, handleX + handleWidth, handleY + handleHeight)
                    canvas.drawRoundRect(handleRect, 3f * density, 3f * density, handlePaint)

                    val gripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.parseColor("#0284C7")
                        style = Paint.Style.FILL
                    }
                    val gripRadius = 1.5f * density
                    val gripSpacing = 8f * density
                    val cX = handleRect.centerX()
                    val cY = handleRect.centerY()
                    canvas.drawCircle(cX - gripSpacing, cY, gripRadius, gripPaint)
                    canvas.drawCircle(cX, cY, gripRadius, gripPaint)
                    canvas.drawCircle(cX + gripSpacing, cY, gripRadius, gripPaint)
                }
            }
            else -> {
                // Draw full unsplit background box across the entire keyboard area
                canvas.drawRect(0f, 0f, w, h, cardBgPaint)
            }
        }

        val currentRows = (layoutDefinition?.rows ?: emptyList()).filter { isRowVisible(it) }
        val rowCount = if (currentRows.isNotEmpty()) currentRows.size else 5
        val isVerticalScroll = layoutDefinition?.metadata?.scrollDirection.equals("VERTICAL", ignoreCase = true)
        val maxVisRows = layoutDefinition?.metadata?.maxVisibleRows ?: 4
        val fixedTopRowsCount = if (isVerticalScroll && currentRows.size > 2) 1 else 0
        val fixedBottomRowsCount = if (isVerticalScroll && currentRows.size > 2) 1 else 0
        val effectiveVisibleRowCount = if (isVerticalScroll) (maxVisRows + fixedTopRowsCount + fixedBottomRowsCount) else rowCount

        val availableHeight = height.toFloat() - (resolveDimension(layoutDefinition?.metadata?.verticalSpacing, height.toFloat(), density, 4f) * (effectiveVisibleRowCount + 1))
        val rowHeight = availableHeight / effectiveVisibleRowCount

        val vSpacing = resolveDimension(layoutDefinition?.metadata?.verticalSpacing, h, density, 4f)
        val topBarBottom = vSpacing + rowHeight + (vSpacing / 2f)
        val bottomNavTop = h - rowHeight - (vSpacing * 1.5f)

        keyBoundsList.forEach { keyBounds ->
            val key = keyBounds.key
            if (key.isSpacer) {
                if (isEditorPreviewMode) {
                    val rect = keyBounds.rect
                    val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        color = android.graphics.Color.parseColor("#334155")
                        pathEffect = android.graphics.DashPathEffect(floatArrayOf(6f, 6f), 0f)
                        strokeWidth = 2f
                    }
                    canvas.drawRoundRect(rect, 8f, 8f, dashPaint)
                }
                return@forEach
            }
            val rect = keyBounds.rect

            val isMiddleScrollableKey = isVerticalScroll && !keyBounds.isFixedRow
            val saveCount = if (isVerticalScroll && isMiddleScrollableKey) {
                val sc = canvas.save()
                canvas.clipRect(0f, topBarBottom, w, bottomNavTop)
                sc
            } else -1

            val isPressed = (pressedKeyBounds == keyBounds)
            val isMod = isModifierKey(key)
            val modState = getModifierState(key)
            val isModActive = modState != com.programmerkeyboard.model.ModifierState.OFF

            val isReadTextKey = key.primaryLabel == "🔊" || (key.onPressAction as? KeyAction.ShowWidget)?.widget == "READ_TEXT"
            val paintToUse = when {
                isPressed && key.pressedBgColor != null -> customBgPaint.apply { color = key.pressedBgColor }
                isReadTextKey && isTextSelected -> keyModifierActivePaint
                isModActive && key.activeBgColor != null -> customBgPaint.apply { color = key.activeBgColor }
                key.bgColor != null -> customBgPaint.apply { color = key.bgColor }
                modState == com.programmerkeyboard.model.ModifierState.LOCKED -> keyModifierActivePaint
                isModActive -> keyModifierActivePaint
                isMod -> keyModifierPaint
                else -> keyPaint
            }

            val cornerRadiusPx = resolveDimension(key.cornerRadius, rowHeight, density, 12f)
            canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paintToUse)

            // Border rendering
            val strokeColor = key.borderColor
            val strokeWidthPx = resolveDimension(key.borderWidth, rowHeight, density, 2f)
            val borderPaintToUse = if (strokeColor != null) {
                customBorderPaint.apply {
                    color = strokeColor
                    strokeWidth = strokeWidthPx
                }
            } else borderPaint

            canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, borderPaintToUse)

            // Active LED Indicator Dots & Lock Badges
            if (isMod) {
                val theme = layoutDefinition?.theme
                val offColor = theme?.modifierOffDotColor ?: android.graphics.Color.parseColor("#000000")
                val latchedColor = theme?.modifierLatchedDotColor ?: android.graphics.Color.parseColor("#22C55E")
                val lockedColor = theme?.modifierLockedDotColor ?: android.graphics.Color.parseColor("#EF4444")

                val dotColor = when (modState) {
                    com.programmerkeyboard.model.ModifierState.LOCKED -> lockedColor
                    com.programmerkeyboard.model.ModifierState.LATCHED -> latchedColor
                    else -> offColor
                }

                val dotRadius = 3f * density
                val dotMargin = 6f * density
                val dotCenterX = rect.left + dotMargin
                val dotCenterY = rect.top + dotMargin

                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = dotColor
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(dotCenterX, dotCenterY, dotRadius, dotPaint)

                val dotBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb(128, 255, 255, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 1f * density
                }
                canvas.drawCircle(dotCenterX, dotCenterY, dotRadius, dotBorderPaint)
            }

            // Key Labels & Icons
            val firstAlt = key.alternates.firstOrNull()
            val displayLabel = if (keyboardState.isShiftActive && key.primaryLabel.length == 1 && key.primaryLabel[0].isLetter()) {
                key.primaryLabel.uppercase()
            } else if (keyboardState.isShiftActive) {
                val upAct = key.onSwipeUpAction
                when {
                    !key.secondaryLabel.isNullOrEmpty() -> if (key.secondaryLabel.any { it.isLowerCase() }) key.secondaryLabel.uppercase() else key.secondaryLabel
                    upAct is KeyAction.SendText -> if (upAct.text.any { it.isLowerCase() }) upAct.text.uppercase() else upAct.text
                    else -> key.primaryLabel
                }
            } else {
                key.primaryLabel
            }

            val textPaintToUse = when {
                isModActive -> Paint(textPaint).apply { color = android.graphics.Color.WHITE }
                key.fgColor != null -> Paint(textPaint).apply { color = key.fgColor }
                else -> textPaint
            }

            val maxAvailableWidth = rect.width() - (12f * density)
            val maxAvailableHeight = rect.height() - (8f * density)

            val defaultMaxFontSize = 18f * density
            val layoutMaxFont = layoutDefinition?.metadata?.maxFontSize
            val keyMaxFont = key.maxFontSize
            val maxFontCap = resolveDimension(keyMaxFont ?: layoutMaxFont, rowHeight, density, defaultMaxFontSize)

            var targetFontSize = resolveDimension(key.fontSize, rowHeight, density, rowHeight * 0.40f).coerceAtMost(maxFontCap)
            textPaintToUse.textSize = targetFontSize

            var measuredWidth = textPaintToUse.measureText(displayLabel)
            var fontMetrics = textPaintToUse.fontMetrics
            var measuredHeight = fontMetrics.descent - fontMetrics.ascent

            if (measuredWidth > maxAvailableWidth || measuredHeight > maxAvailableHeight) {
                val scaleWidth = if (measuredWidth > 0) maxAvailableWidth / measuredWidth else 1.0f
                val scaleHeight = if (measuredHeight > 0) maxAvailableHeight / measuredHeight else 1.0f
                val scaleFactor = kotlin.math.min(scaleWidth, scaleHeight).coerceIn(0.25f, 1.0f)
                targetFontSize *= scaleFactor
                textPaintToUse.textSize = targetFontSize
                fontMetrics = textPaintToUse.fontMetrics
            }

            drawKeyIconOrLabel(canvas, key, displayLabel, rect, textPaintToUse)

            val secFontSize = (targetFontSize * 0.55f).coerceIn(9f * density, 14f * density)
            val secY = rect.top + secFontSize + (2f * density)

            // Top-Left Corner Text Drawing
            val topLeft = key.topLeftLabel
            if (!topLeft.isNullOrEmpty() && !isModifierKey(key)) {
                val topLeftPaint = Paint(secondaryTextPaint).apply {
                    textSize = secFontSize
                    textAlign = Paint.Align.LEFT
                }
                key.secondaryFgColor?.let { topLeftPaint.color = it }
                canvas.drawText(topLeft, rect.left + (5f * density), secY, topLeftPaint)
            }

            // Top-Right Corner Text Drawing (First Alternate Sorted by Usage Frequency)
            val lpAction = key.onLongPressAction
            val upAction = key.onSwipeUpAction
            val isKeyPrimaryLetter = key.primaryLabel.length == 1 && key.primaryLabel[0].isLetter()

            val altList = if (key.alternates.isNotEmpty()) {
                key.alternates
            } else {
                (key.onLongPressAction as? KeyAction.ShowPopup)?.options ?: emptyList()
            }

            val mostUsedAlt = if (altList.isNotEmpty()) {
                val currentLayoutId = layoutDefinition?.id ?: "main"
                val usageCounts = com.programmerkeyboard.engine.AlternatePriorityManager.getAlternateUsageCounts(context, currentLayoutId)
                altList.maxByOrNull { usageCounts[if (it.length == 1 && it[0].isLetter()) it.lowercase() else it] ?: 0 } ?: altList.firstOrNull()
            } else null

            val formattedMostUsedAlt = if (!mostUsedAlt.isNullOrEmpty() && mostUsedAlt.length == 1 && mostUsedAlt[0].isLetter()) {
                if (keyboardState.isShiftActive) mostUsedAlt.uppercase() else mostUsedAlt.lowercase()
            } else mostUsedAlt

            val rawSecToDraw = if (isKeyPrimaryLetter) {
                formattedMostUsedAlt
            } else {
                key.topRightLabel ?: key.secondaryLabel ?: formattedMostUsedAlt ?: when {
                    upAction is KeyAction.SendText -> upAction.text
                    lpAction is KeyAction.SendText -> lpAction.text
                    else -> null
                }
            }

            if (!rawSecToDraw.isNullOrEmpty() && rawSecToDraw != displayLabel && !isModifierKey(key)) {
                val secPaint = Paint(secondaryTextPaint).apply {
                    textSize = secFontSize
                    textAlign = Paint.Align.RIGHT
                }
                key.secondaryFgColor?.let { secPaint.color = it }
                canvas.drawText(rawSecToDraw, rect.right - (5f * density), secY, secPaint)
            }

            if (saveCount != -1) {
                canvas.restoreToCount(saveCount)
            }
        }

        // Draw Minimal In-Keyboard Listening Toast/Badge if active
        activeListeningStatus?.let { status ->
            val pillWidth = 220f * density
            val pillHeight = 36f * density
            val pillX = (w - pillWidth) / 2f
            val pillY = 12f * density
            val pillRect = RectF(pillX, pillY, pillX + pillWidth, pillY + pillHeight)

            val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.argb(235, 15, 23, 42)
                style = Paint.Style.FILL
            }
            val pillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#38BDF8")
                style = Paint.Style.STROKE
                strokeWidth = 2f * density
            }
            val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = 14f * density
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }

            canvas.drawRoundRect(pillRect, 18f * density, 18f * density, pillBgPaint)
            canvas.drawRoundRect(pillRect, 18f * density, 18f * density, pillBorderPaint)
            val fontMetrics = pillTextPaint.fontMetrics
            val baseline = pillRect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText(status, pillRect.centerX(), baseline, pillTextPaint)
        }

        // Draw Row Gear Buttons in Layout Editor Mode
        if (isEditorPreviewMode && rowGearBoundsList.isNotEmpty()) {
            val gearBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }
            val gearBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f * density
            }
            val gearTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = 11f * density
                textAlign = Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            rowGearBoundsList.forEach { gBounds ->
                val r = gBounds.rect
                val cornerRadius = 6f * density
                if (gBounds.row.hidden) {
                    gearBgPaint.color = android.graphics.Color.parseColor("#334155")
                    gearBorderPaint.color = android.graphics.Color.parseColor("#94A3B8")
                } else {
                    gearBgPaint.color = android.graphics.Color.parseColor("#0F766E")
                    gearBorderPaint.color = android.graphics.Color.parseColor("#2DD4BF")
                }
                canvas.drawRoundRect(r, cornerRadius, cornerRadius, gearBgPaint)
                canvas.drawRoundRect(r, cornerRadius, cornerRadius, gearBorderPaint)
                
                val label = if (gBounds.row.hidden) "🙈 R${gBounds.rowIdx + 1}" else "⚙️ R${gBounds.rowIdx + 1}"
                val fontMetrics = gearTextPaint.fontMetrics
                val textY = r.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2f
                canvas.drawText(label, r.centerX(), textY, gearTextPaint)
            }
        }

        // Draw Scrollbar Indicator if scrollable
        if (maxScrollOffsetY > 0f && scrollViewportHeight > 0f) {
            val trackWidth = 6f * density
            val trackRight = w - 4f * density
            val trackLeft = trackRight - trackWidth
            val trackTop = 8f * density
            val trackBottom = scrollViewportHeight - 8f * density
            val trackHeight = trackBottom - trackTop

            if (trackHeight > 30f) {
                val thumbHeight = (trackHeight * (scrollViewportHeight / totalScrollContentHeight)).coerceAtLeast(20f * density)
                val thumbProgress = (scrollOffsetY / maxScrollOffsetY).coerceIn(0f, 1f)
                val thumbTop = trackTop + (thumbProgress * (trackHeight - thumbHeight))
                val thumbBottom = thumbTop + thumbHeight

                val scrollbarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = ContextCompat.getColor(context, R.color.key_border)
                    style = Paint.Style.FILL
                    alpha = 180
                }
                val thumbRect = RectF(trackLeft, thumbTop, trackRight, thumbBottom)
                canvas.drawRoundRect(thumbRect, trackWidth / 2f, trackWidth / 2f, scrollbarPaint)
            }
        }

        // Draw Interactive Trackpad Joystick Widget Feedback Overlay
        drawTrackpadJoystickOverlay(canvas)
    }

    private fun drawTrackpadJoystickOverlay(canvas: Canvas) {
        if (!isSpacebarTrackpad) return

        val density = context.resources.displayMetrics.density
        val outerRadius = 52f * density
        val maxKnobOffset = 34f * density
        val knobRadius = 18f * density

        val originX = trackpadTouchX
        val originY = trackpadTouchY

        // Extract theme colors from the target touched key or theme fallback
        val currentKey = pressedKeyBounds?.key
        val keyBgColor = currentKey?.bgColor ?: keyPaint.color
        val keyPressedBgColor = currentKey?.pressedBgColor ?: ContextCompat.getColor(context, R.color.key_background_pressed)
        val keyFgColor = currentKey?.fgColor ?: textPaint.color

        // Calculate 360-degree knob offset relative to touch origin
        val deltaX = trackpadLastX - originX
        val deltaY = trackpadLastY - originY
        val dist = kotlin.math.hypot(deltaX, deltaY)
        val angle = kotlin.math.atan2(deltaY, deltaX)
        val clampedDist = kotlin.math.min(dist, maxKnobOffset)
        val knobX = originX + clampedDist * kotlin.math.cos(angle).toFloat()
        val knobY = originY + clampedDist * kotlin.math.sin(angle).toFloat()

        // Position shift blink state (flashes key's Pressed background color on each step move tick)
        val now = System.currentTimeMillis()
        val timeSinceBlink = now - trackpadBlinkTime
        val isBlinking = timeSinceBlink < 160L

        // 1. Outer Ring Glassmorphic Background Circle (uses same background color as the key)
        val outerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = keyBgColor
            style = Paint.Style.FILL
            alpha = 230
        }
        canvas.drawCircle(originX, originY, outerRadius, outerBgPaint)

        // 2. Outer Ring Accent Border (flashes to Pressed background color on shift ticks)
        val outerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isBlinking) keyPressedBgColor else keyFgColor
            style = Paint.Style.STROKE
            strokeWidth = if (isBlinking) 4.5f * density else 2.5f * density
            alpha = if (isBlinking) 255 else 180
        }
        canvas.drawCircle(originX, originY, outerRadius, outerBorderPaint)

        // 3. Directional Arrow Indicators (▲ ▼ ◀ ▶)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isBlinking) keyPressedBgColor else keyFgColor
            textSize = 14f * density
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            alpha = if (isBlinking) 255 else 160
        }
        val arrowDist = outerRadius - (14f * density)
        canvas.drawText("▲", originX, originY - arrowDist + (5f * density), textPaint)
        canvas.drawText("▼", originX, originY + arrowDist + (5f * density), textPaint)
        canvas.drawText("◀", originX - arrowDist, originY + (5f * density), textPaint)
        canvas.drawText("▶", originX + arrowDist, originY + (5f * density), textPaint)

        // 4. Connecting vector line from origin to knob
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isBlinking) keyPressedBgColor else keyFgColor
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
            alpha = if (isBlinking) 220 else 120
        }
        canvas.drawLine(originX, originY, knobX, knobY, linePaint)

        // 5. Inner Joystick Knob Circle (same background color as key, blinks to Pressed background color)
        val knobFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isBlinking) keyPressedBgColor else keyBgColor
            style = Paint.Style.FILL
            alpha = if (isBlinking) 255 else 220
        }
        canvas.drawCircle(knobX, knobY, knobRadius, knobFillPaint)

        val knobBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isBlinking) keyFgColor else keyPressedBgColor
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
        }
        canvas.drawCircle(knobX, knobY, knobRadius, knobBorderPaint)

        // Inner core dot
        val coreDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = keyFgColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(knobX, knobY, 4f * density, coreDotPaint)

        if (isBlinking) {
            postInvalidateDelayed(30L)
        }
    }

    private fun resolveDimension(value: DimensionValue?, parentSize: Float, density: Float, fallbackPx: Float): Float {
        return when (value) {
            is DimensionValue.Ratio -> parentSize * value.value
            is DimensionValue.Absolute -> value.value * density
            null -> fallbackPx
        }
    }

    private fun drawSvgMicIcon(canvas: Canvas, rect: RectF, paint: Paint) {
        val iconSize = minOf(rect.width(), rect.height()) * 0.48f
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

        // 1. Capsule (rect x="9" y="3" width="6" height="11" rx="3")
        val capsuleRect = RectF(9f, 3f, 15f, 14f)
        canvas.drawRoundRect(capsuleRect, 3f, 3f, strokePaint)

        // 2. Cradle Arc (path d="M5 11a7 7 0 0 0 14 0")
        val arcRect = RectF(5f, 4f, 19f, 18f)
        canvas.drawArc(arcRect, 0f, 180f, false, strokePaint)

        // 3. Stem Base (line x1="12" y1="18" x2="12" y2="21")
        canvas.drawLine(12f, 18f, 12f, 21f, strokePaint)

        // 4. Base Line
        canvas.drawLine(9f, 21f, 15f, 21f, strokePaint)

        canvas.restoreToCount(saveCount)
    }

    private fun drawSvgTtsIcon(canvas: Canvas, rect: RectF, paint: Paint) {
        val iconH = minOf(rect.width(), rect.height()) * 0.48f
        val scale = iconH / 100f
        val iconW = 110f * scale
        val offsetX = rect.centerX() - (iconW / 2f)
        val offsetY = rect.centerY() - (iconH / 2f)

        val saveCount = canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paint.color
            style = Paint.Style.STROKE
            strokeWidth = 8f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        // Geometric Profile Head (Shallow Mouth)
        val ttsPath = android.graphics.Path().apply {
            moveTo(22f, 85f)
            lineTo(22f, 62f)
            cubicTo(7f, 48f, 7f, 22f, 25f, 12f)
            cubicTo(45f, 2f, 64f, 16f, 64f, 35f)
            lineTo(69f, 50f)
            lineTo(62f, 50f)
            lineTo(62f, 56f)
            lineTo(53f, 58f)
            lineTo(62f, 64f)
            lineTo(62f, 70f)
            lineTo(47f, 70f)
            lineTo(47f, 85f)
            close()
        }
        canvas.drawPath(ttsPath, strokePaint)

        // Three Center-Outward Rays
        canvas.drawLine(70f, 56f, 84f, 48f, strokePaint)
        canvas.drawLine(71f, 60f, 86f, 60f, strokePaint)
        canvas.drawLine(70f, 64f, 84f, 72f, strokePaint)

        canvas.restoreToCount(saveCount)
    }

    private fun drawSvgPaperclipIcon(canvas: Canvas, rect: RectF, paint: Paint) {
        val iconSize = minOf(rect.width(), rect.height()) * 0.48f
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
        canvas.restoreToCount(saveCount)
    }

    private fun drawSvgClipboardIcon(canvas: Canvas, rect: RectF, paint: Paint) {
        val iconSize = minOf(rect.width(), rect.height()) * 0.48f
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

        canvas.drawRoundRect(RectF(8f, 2f, 16f, 6f), 1f, 1f, strokePaint)
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
        canvas.drawLine(12f, 11f, 16f, 11f, strokePaint)
        canvas.drawLine(12f, 16f, 16f, 16f, strokePaint)
        canvas.drawLine(8f, 11f, 8.1f, 11f, strokePaint)
        canvas.drawLine(8f, 16f, 8.1f, 16f, strokePaint)

        canvas.restoreToCount(saveCount)
    }

    private fun drawSvgCopyIcon(canvas: Canvas, rect: RectF, paint: Paint) {
        val iconSize = minOf(rect.width(), rect.height()) * 0.48f
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

        canvas.restoreToCount(saveCount)
    }

    private fun drawSvgCutIcon(canvas: Canvas, rect: RectF, paint: Paint) {
        val iconSize = minOf(rect.width(), rect.height()) * 0.48f
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

        canvas.drawCircle(6f, 6f, 3f, strokePaint)
        canvas.drawCircle(6f, 18f, 3f, strokePaint)
        canvas.drawLine(20f, 4f, 8.12f, 15.88f, strokePaint)
        canvas.drawLine(14.47f, 14.48f, 20f, 20f, strokePaint)
        canvas.drawLine(8.12f, 8.12f, 12f, 12f, strokePaint)

        canvas.restoreToCount(saveCount)
    }

    private fun drawSvgPasteIcon(canvas: Canvas, rect: RectF, paint: Paint) {
        val iconSize = minOf(rect.width(), rect.height()) * 0.48f
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

        // Arrow pointing down into board
        canvas.drawLine(12f, 11f, 12f, 17f, strokePaint)
        val arrowHead = android.graphics.Path().apply {
            moveTo(9f, 14f)
            lineTo(12f, 17f)
            lineTo(15f, 14f)
        }
        canvas.drawPath(arrowHead, strokePaint)

        canvas.restoreToCount(saveCount)
    }

    private fun drawSvgSelectAllIcon(canvas: Canvas, rect: RectF, paint: Paint) {
        val iconSize = minOf(rect.width(), rect.height()) * 0.48f
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

        // Selection Corners
        canvas.drawLine(3f, 3f, 7f, 3f, strokePaint)
        canvas.drawLine(3f, 3f, 3f, 7f, strokePaint)

        canvas.drawLine(21f, 3f, 17f, 3f, strokePaint)
        canvas.drawLine(21f, 3f, 21f, 7f, strokePaint)

        canvas.drawLine(3f, 21f, 7f, 21f, strokePaint)
        canvas.drawLine(3f, 21f, 3f, 17f, strokePaint)

        canvas.drawLine(21f, 21f, 17f, 21f, strokePaint)
        canvas.drawLine(21f, 21f, 21f, 17f, strokePaint)

        canvas.drawRoundRect(RectF(8f, 8f, 16f, 16f), 1f, 1f, strokePaint)

        canvas.restoreToCount(saveCount)
    }

    private fun drawKeyIconOrLabel(canvas: Canvas, key: KeyDefinition, displayLabel: String, rect: RectF, paint: Paint) {
        val iconType = key.iconName
        if (!iconType.isNullOrEmpty()) {
            val isVector = com.programmerkeyboard.util.IconRenderer.drawVectorIcon(canvas, iconType, rect, paint)
            if (isVector) return

            if (iconType.startsWith("content://") || iconType.startsWith("file://") || iconType.startsWith("/")) {
                try {
                    val uri = android.net.Uri.parse(iconType)
                    val bitmap = if (iconType.startsWith("content://")) {
                        context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) }
                    } else {
                        android.graphics.BitmapFactory.decodeFile(iconType)
                    }
                    if (bitmap != null) {
                        val iconSize = minOf(rect.width(), rect.height()) * 0.42f
                        val half = iconSize * 0.8f
                        val dstRect = RectF(rect.centerX() - half, rect.centerY() - half, rect.centerX() + half, rect.centerY() + half)
                        canvas.drawBitmap(bitmap, null, dstRect, paint)
                        return
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val fontMetrics = paint.fontMetrics
        val baseline = rect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2
        canvas.drawText(displayLabel, rect.centerX(), baseline, paint)
    }

    var isTextSelected: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var isEditorPreviewMode: Boolean = false
    var onKeyTapForEditingListener: ((rowIdx: Int, keyIdx: Int, key: KeyDefinition) -> Unit)? = null
    var onSpacingTapForEditingListener: (() -> Unit)? = null

    private fun executeAction(action: KeyAction, sourceKey: KeyDefinition? = null) {
        if (isEditorPreviewMode) {
            sourceKey?.let { targetKey ->
                val rows = layoutDefinition?.rows ?: emptyList()
                for (rIdx in rows.indices) {
                    val kIdx = rows[rIdx].keys.indexOf(targetKey)
                    if (kIdx != -1) {
                        onKeyTapForEditingListener?.invoke(rIdx, kIdx, targetKey)
                        return
                    }
                }
            }
            return
        }
        val actionToExecute = if (sourceKey != null && keyboardState.shouldShiftKey(sourceKey) && action !is KeyAction.ShowPopup && action !is KeyAction.ShowWidget) {
            val isLetter = sourceKey.primaryLabel.length == 1 && sourceKey.primaryLabel[0].isLetter()
            val firstAlt = sourceKey.alternates.firstOrNull()
            val upAct = sourceKey.onSwipeUpAction
            val lpAct = sourceKey.onLongPressAction
            when {
                isLetter -> KeyAction.SendText(sourceKey.primaryLabel.uppercase())
                firstAlt != null -> KeyAction.SendText(if (firstAlt.any { it.isLowerCase() }) firstAlt.uppercase() else firstAlt)
                !sourceKey.secondaryLabel.isNullOrEmpty() -> KeyAction.SendText(if (sourceKey.secondaryLabel.any { it.isLowerCase() }) sourceKey.secondaryLabel.uppercase() else sourceKey.secondaryLabel)
                upAct is KeyAction.SendText -> KeyAction.SendText(if (upAct.text.any { it.isLowerCase() }) upAct.text.uppercase() else upAct.text)
                lpAct is KeyAction.ShowPopup && lpAct.options.isNotEmpty() -> {
                    val opt = lpAct.options.first()
                    KeyAction.SendText(if (opt.any { it.isLowerCase() }) opt.uppercase() else opt)
                }
                else -> action
            }
        } else {
            action
        }

        when (actionToExecute) {
            is KeyAction.SendText -> onKeyActionListener?.invoke(actionToExecute)
            is KeyAction.SendCode -> onKeyActionListener?.invoke(actionToExecute)
            is KeyAction.SwitchLayout -> {
                onLayoutChangeListener?.invoke(actionToExecute.target)
            }
            is KeyAction.SetScreenMode -> {
                onScreenModeChangeListener?.invoke(actionToExecute.mode)
            }
            is KeyAction.AdjustHeight -> {
                actionToExecute.percentage?.let { heightPercentage = it }
                actionToExecute.delta?.let { heightPercentage = (heightPercentage + it).coerceIn(15, 60) }
            }
            is KeyAction.ShowZoomPreview -> {
                val text = actionToExecute.text ?: sourceKey?.primaryLabel ?: return
                val bounds = pressedKeyBounds ?: sourceKey?.let { k -> keyBoundsList.firstOrNull { it.key == k } } ?: return
                showZoomPreview(text, bounds.rect)
            }
            is KeyAction.ShowPopup -> {
                if (layoutDefinition?.id == "phone") return
                val rect = (sourceKey?.let { k -> keyBoundsList.firstOrNull { it.key == k } } ?: pressedKeyBounds)?.rect ?: return
                val currentLayoutId = layoutDefinition?.id ?: "main"
                val usageCounts = com.programmerkeyboard.engine.AlternatePriorityManager.getAlternateUsageCounts(context, currentLayoutId)

                val baseOptions = actionToExecute.options.map { opt ->
                    if (opt.length == 1 && opt[0].isLetter()) {
                        if (keyboardState.isShiftActive) opt.uppercase() else opt.lowercase()
                    } else {
                        opt
                    }
                }

                val baseActions = actionToExecute.actions
                val pairedList = baseOptions.mapIndexed { idx, opt ->
                    val act = if (idx in baseActions.indices) baseActions[idx] else null
                    Pair(opt, act)
                }

                val sortedPairs = if (pairedList.size > 1) {
                    val firstPair = pairedList[0]
                    val remainingPairs = pairedList.subList(1, pairedList.size)
                    val sortedRemaining = remainingPairs.sortedByDescending { pair ->
                        val opt = pair.first
                        val normOpt = if (opt.length == 1 && opt[0].isLetter()) opt.lowercase() else opt
                        usageCounts[normOpt] ?: 0
                    }
                    listOf(firstPair) + sortedRemaining
                } else {
                    pairedList
                }

                val optionsToDisplay = sortedPairs.map { it.first }
                val sortedActions = sortedPairs.mapNotNull { it.second }

                keyPopupOverlay = KeyPopupOverlay(
                    context = context,
                    onItemSelected = { selectedIndex, selectedLabel ->
                        val keyLabel = sourceKey?.primaryLabel ?: pressedKeyBounds?.key?.primaryLabel
                        com.programmerkeyboard.engine.AlternatePriorityManager.recordAlternateSelection(
                            context = context,
                            layoutId = currentLayoutId,
                            sourceKeyLabel = keyLabel,
                            selectedLabel = selectedLabel
                        )
                        val actionToRun = if (selectedIndex in sortedActions.indices && sortedActions[selectedIndex] !is KeyAction.SendText) {
                            sortedActions[selectedIndex]
                        } else {
                            resolveActionFromLabel(selectedLabel)
                        }
                        executeAction(actionToRun, sourceKey)
                        playKeyClickSound(isKeyDown = false)
                    },
                    onHoverChanged = { hoveredIndex, hoveredLabel ->
                        val isEmoji = isEmojiKey(sourceKey ?: pressedKeyBounds?.key ?: return@KeyPopupOverlay)
                        if (isEmoji) {
                            val itemRect = keyPopupOverlay?.getItemRect(hoveredIndex)
                            if (itemRect != null) {
                                showZoomPreview(hoveredLabel, itemRect)
                            }
                        }
                    },
                    onDismissListener = {
                        keyPopupOverlay = null
                        pressedKeyBounds = null
                        dismissKeyPreview()
                        invalidate()
                    }
                ).also {
                    it.show(this, rect, optionsToDisplay)
                }
            }
            is KeyAction.ShowWidget -> {
                when (actionToExecute.widget) {
                    "JOYSTICK" -> {
                        val bounds = pressedKeyBounds ?: sourceKey?.let { k -> keyBoundsList.firstOrNull { it.key == k } }
                        val key = bounds?.key ?: sourceKey
                        val rect = bounds?.rect
                        val keyCode = getArrowKeyCode(key?.primaryLabel ?: "")
                        val touchLocation = IntArray(2)
                        getLocationOnScreen(touchLocation)
                        val keyCenterX = (rect?.centerX() ?: (width / 2f)) + touchLocation[0]
                        val keyCenterY = (rect?.centerY() ?: (height / 2f)) + touchLocation[1]

                        joystickPopupWidget = JoystickPopupWidget(context) { code ->
                            onKeyActionListener?.invoke(KeyAction.SendCode(code))
                        }.also {
                            it.show(this, keyCenterX, keyCenterY, keyCode)
                        }
                    }
                    "VOICE_INPUT", "VOICE_INPUT_ONESHOT", "VOICE_INPUT_TERMINAL" -> {
                        val prefs = context.getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
                        val useMinimalToast = prefs.getBoolean("pref_minimal_voice_feedback", true)
                        if (useMinimalToast) {
                            onKeyActionListener?.invoke(actionToExecute)
                        } else {
                            voiceInputOverlay = VoiceInputOverlay(context) { recognizedText ->
                                onKeyActionListener?.invoke(KeyAction.SendText(recognizedText))
                            }.also {
                                it.show(this)
                            }
                        }
                    }
                    "VOICE_INPUT_EMBEDDED" -> {
                        voiceInputOverlay = VoiceInputOverlay(context) { recognizedText ->
                            onKeyActionListener?.invoke(KeyAction.SendText(recognizedText))
                        }.also {
                            it.show(this)
                        }
                    }
                    "EMOJI_PICKER", "EMOJI_PICKER_EMBEDDED", "EMOJI", "EMOJI_KEYBOARD" -> {
                        showEmojiPicker()
                    }
                    else -> {
                        onKeyActionListener?.invoke(actionToExecute)
                    }
                }
            }
            is KeyAction.AutoRepeat -> {
                autoRepeatRunnable = object : Runnable {
                    override fun run() {
                        onKeyActionListener?.invoke(KeyAction.SendCode(actionToExecute.code))
                        handler.postDelayed(this, getAutoRepeatIntervalMs())
                    }
                }.also { handler.post(it) }
            }
            is KeyAction.ToggleRow -> {
                val layoutId = layoutDefinition?.id ?: ""
                val rawRowId = actionToExecute.rowId.toString()
                if (rawRowId == "all_hidden") {
                    keyboardState.isFnActive = !keyboardState.isFnActive
                } else {
                    val qualifiedKey = "$layoutId:$rawRowId"
                    val targetRow = layoutDefinition?.rows?.firstOrNull { it.id.toString() == rawRowId }
                    val isCurrentlyVisible = targetRow?.let { isRowVisible(it) } ?: (rowVisibilityMap[qualifiedKey] ?: true)
                    val newVis = !isCurrentlyVisible
                    rowVisibilityMap[qualifiedKey] = newVis
                    onRowToggleListener?.invoke(qualifiedKey, newVis)
                }
                recalculateKeyBounds()
                requestLayout()
                invalidate()
            }
            is KeyAction.ToggleModifier, is KeyAction.LockModifier -> {
                onKeyActionListener?.invoke(actionToExecute)
            }
            is KeyAction.SelectAll, is KeyAction.Copy, is KeyAction.Cut, is KeyAction.Paste, is KeyAction.PasteEcho, is KeyAction.SwitchIme, is KeyAction.LaunchApp -> {
                onKeyActionListener?.invoke(actionToExecute)
            }
            is KeyAction.None -> {}
        }
    }

    private fun stopAutoRepeat() {
        autoRepeatRunnable?.let { handler.removeCallbacks(it) }
        autoRepeatRunnable = null
    }

    private fun getLongPressTimeoutMs(): Long {
        val prefs = context.getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        val prefTimeout = prefs.getLong("pref_long_press_timeout_ms", -1L)
        if (prefTimeout > 0L) return prefTimeout
        return layoutDefinition?.metadata?.longPressTimeoutMs ?: 350L
    }

    private fun getAutoRepeatIntervalMs(): Long {
        val prefs = context.getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        val prefInterval = prefs.getLong("pref_auto_repeat_interval_ms", -1L)
        if (prefInterval > 0L) return prefInterval
        return layoutDefinition?.metadata?.autoRepeatIntervalMs ?: 50L
    }

    private fun showKeyPreview(key: KeyDefinition, rect: RectF) {
        val layoutId = layoutDefinition?.id ?: ""
        if (layoutId == "phone" ||
            layoutId == "mobile_number" ||
            layoutId == "meta" ||
            layoutDefinition?.metadata?.showKeyPreview == false ||
            key.showPreview == false ||
            key.onPressAction is KeyAction.SwitchLayout) {
            return
        }
        if (keyPreviewOverlay == null) {
            keyPreviewOverlay = KeyPreviewOverlay(context)
        }
        val upAct = key.onSwipeUpAction
        val lpAct = key.onLongPressAction
        val label = if (keyboardState.shouldShiftKey(key)) {
            when {
                key.primaryLabel.length == 1 && key.primaryLabel[0].isLowerCase() -> key.primaryLabel.uppercase()
                !key.secondaryLabel.isNullOrEmpty() -> key.secondaryLabel
                upAct is KeyAction.SendText -> upAct.text
                lpAct is KeyAction.ShowPopup && lpAct.options.isNotEmpty() -> lpAct.options.first()
                else -> key.primaryLabel
            }
        } else {
            key.primaryLabel
        }
        keyPreviewOverlay?.show(this, rect, label)
    }

    private fun showZoomPreview(text: String, rect: RectF) {
        if (layoutDefinition?.metadata?.showKeyPreview == false) return
        if (keyPreviewOverlay == null) {
            keyPreviewOverlay = KeyPreviewOverlay(context)
        }
        keyPreviewOverlay?.show(this, rect, text, isLarge = true)
        activeZoomedText = text
    }

    private fun dismissKeyPreview() {
        keyPreviewOverlay?.dismiss()
        keyPreviewOverlay = null
        activeZoomedText = null
    }

    private fun performKeypressHapticFeedback() {
        val prefs = context.getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        val isHapticEnabled = prefs.getBoolean("pref_haptic_feedback_enabled", true)
        if (!isHapticEnabled) return

        val vibStyle = prefs.getString("pref_vibration_style", "SHARP_CLICK") ?: "SHARP_CLICK"
        val durationMs = prefs.getLong("pref_vibration_duration_ms", 40L).coerceIn(5L, 200L)
        val amplitudePercent = prefs.getInt("pref_vibration_amplitude", 100).coerceIn(1, 100)
        val rawAmplitude = (amplitudePercent / 100f * 255f).toInt().coerceIn(1, 255)

        performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)

        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }

            vibrator?.let {
                if (it.hasVibrator()) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && vibStyle != "CUSTOM_PULSE") {
                        val effectId = when (vibStyle) {
                            "HEAVY_CLICK" -> android.os.VibrationEffect.EFFECT_HEAVY_CLICK
                            "CRISP_TICK" -> android.os.VibrationEffect.EFFECT_TICK
                            "DOUBLE_CLICK" -> android.os.VibrationEffect.EFFECT_DOUBLE_CLICK
                            else -> android.os.VibrationEffect.EFFECT_CLICK
                        }
                        it.vibrate(android.os.VibrationEffect.createPredefined(effectId))
                    } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        if (it.hasAmplitudeControl()) {
                            it.vibrate(android.os.VibrationEffect.createOneShot(durationMs, rawAmplitude))
                        } else {
                            it.vibrate(android.os.VibrationEffect.createOneShot(durationMs, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(durationMs)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private var soundPool: android.media.SoundPool? = null
    private val soundMap = mutableMapOf<String, Int>()
    private val soundMapDown = mutableMapOf<String, Int>()
    private val soundMapUp = mutableMapOf<String, Int>()

    private fun loadAssetSound(assetPath: String): Int {
        return try {
            val afd = context.assets.openFd(assetPath)
            val soundId = soundPool?.load(afd, 1) ?: 0
            afd.close()
            soundId
        } catch (e: Exception) {
            0
        }
    }

    private fun initSoundPool() {
        if (soundPool == null) {
            val audioAttrs = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            soundPool = android.media.SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(audioAttrs)
                .build()

            // Standard Mechvibes Recorded Switch Sound Packs
            soundMap["REC_BLUE_PBT"] = loadAssetSound("audio/sound_cherry_blue_pbt.wav")
            soundMap["REC_BLUE_ABS"] = loadAssetSound("audio/sound_cherry_blue_abs.wav")
            soundMap["REC_BROWN_PBT"] = loadAssetSound("audio/sound_cherry_brown_pbt.wav")
            soundMap["REC_BROWN_ABS"] = loadAssetSound("audio/sound_cherry_brown_abs.wav")
            soundMap["REC_RED_PBT"] = loadAssetSound("audio/sound_cherry_red_pbt.wav")
            soundMap["REC_RED_ABS"] = loadAssetSound("audio/sound_cherry_red_abs.wav")
            soundMap["REC_BLACK_PBT"] = loadAssetSound("audio/sound_cherry_black_pbt.wav")
            soundMap["REC_BLACK_ABS"] = loadAssetSound("audio/sound_cherry_black_abs.wav")
            soundMap["REC_NK_CREAM"] = loadAssetSound("audio/sound_nk_cream.wav")
            soundMap["REC_EG_OREO"] = loadAssetSound("audio/sound_eg_oreo.wav")
            soundMap["REC_EG_PURPLE"] = loadAssetSound("audio/sound_eg_crystal_purple.wav")
            soundMap["REC_TOPRE"] = loadAssetSound("audio/sound_topre_purple.wav")

            // Synthesized / Generated Audio Sounds
            soundMap["SYNTH_CLICKY"] = loadAssetSound("audio/switch_cherry_blue.wav")
            soundMap["SYNTH_TACTILE"] = loadAssetSound("audio/switch_cherry_brown.wav")
            soundMap["SYNTH_LINEAR"] = loadAssetSound("audio/switch_cherry_red.wav")
            soundMap["SYNTH_THOCK"] = loadAssetSound("audio/switch_cherry_black.wav")
            soundMap["SYNTH_SPRING"] = loadAssetSound("audio/switch_ibm_buckling.wav")

            // Split Key Down Sound Packs
            soundMapDown["REC_BLUE_PBT"] = loadAssetSound("audio_split/sound_cherry_blue_pbt_down.wav")
            soundMapDown["REC_BLUE_ABS"] = loadAssetSound("audio_split/sound_cherry_blue_abs_down.wav")
            soundMapDown["REC_BROWN_PBT"] = loadAssetSound("audio_split/sound_cherry_brown_pbt_down.wav")
            soundMapDown["REC_BROWN_ABS"] = loadAssetSound("audio_split/sound_cherry_brown_abs_down.wav")
            soundMapDown["REC_RED_PBT"] = loadAssetSound("audio_split/sound_cherry_red_pbt_down.wav")
            soundMapDown["REC_RED_ABS"] = loadAssetSound("audio_split/sound_cherry_red_abs_down.wav")
            soundMapDown["REC_BLACK_PBT"] = loadAssetSound("audio_split/sound_cherry_black_pbt_down.wav")
            soundMapDown["REC_BLACK_ABS"] = loadAssetSound("audio_split/sound_cherry_black_abs_down.wav")
            soundMapDown["REC_NK_CREAM"] = loadAssetSound("audio_split/sound_nk_cream_down.wav")
            soundMapDown["REC_EG_OREO"] = loadAssetSound("audio_split/sound_eg_oreo_down.wav")
            soundMapDown["REC_EG_PURPLE"] = loadAssetSound("audio_split/sound_eg_crystal_purple_down.wav")
            soundMapDown["REC_TOPRE"] = loadAssetSound("audio_split/sound_topre_purple_down.wav")

            soundMapDown["SYNTH_CLICKY"] = loadAssetSound("audio_split/switch_cherry_blue_down.wav")
            soundMapDown["SYNTH_TACTILE"] = loadAssetSound("audio_split/switch_cherry_brown_down.wav")
            soundMapDown["SYNTH_LINEAR"] = loadAssetSound("audio_split/switch_cherry_red_down.wav")
            soundMapDown["SYNTH_THOCK"] = loadAssetSound("audio_split/switch_cherry_black_down.wav")
            soundMapDown["SYNTH_SPRING"] = loadAssetSound("audio_split/switch_ibm_buckling_down.wav")

            // Split Key Up Sound Packs
            soundMapUp["REC_BLUE_PBT"] = loadAssetSound("audio_split/sound_cherry_blue_pbt_up.wav")
            soundMapUp["REC_BLUE_ABS"] = loadAssetSound("audio_split/sound_cherry_blue_abs_up.wav")
            soundMapUp["REC_BROWN_PBT"] = loadAssetSound("audio_split/sound_cherry_brown_pbt_up.wav")
            soundMapUp["REC_BROWN_ABS"] = loadAssetSound("audio_split/sound_cherry_brown_abs_up.wav")
            soundMapUp["REC_RED_PBT"] = loadAssetSound("audio_split/sound_cherry_red_pbt_up.wav")
            soundMapUp["REC_RED_ABS"] = loadAssetSound("audio_split/sound_cherry_red_abs_up.wav")
            soundMapUp["REC_BLACK_PBT"] = loadAssetSound("audio_split/sound_cherry_black_pbt_up.wav")
            soundMapUp["REC_BLACK_ABS"] = loadAssetSound("audio_split/sound_cherry_black_abs_up.wav")
            soundMapUp["REC_NK_CREAM"] = loadAssetSound("audio_split/sound_nk_cream_up.wav")
            soundMapUp["REC_EG_OREO"] = loadAssetSound("audio_split/sound_eg_oreo_up.wav")
            soundMapUp["REC_EG_PURPLE"] = loadAssetSound("audio_split/sound_eg_crystal_purple_up.wav")
            soundMapUp["REC_TOPRE"] = loadAssetSound("audio_split/sound_topre_purple_up.wav")

            soundMapUp["SYNTH_CLICKY"] = loadAssetSound("audio_split/switch_cherry_blue_up.wav")
            soundMapUp["SYNTH_TACTILE"] = loadAssetSound("audio_split/switch_cherry_brown_up.wav")
            soundMapUp["SYNTH_LINEAR"] = loadAssetSound("audio_split/switch_cherry_red_up.wav")
            soundMapUp["SYNTH_THOCK"] = loadAssetSound("audio_split/switch_cherry_black_up.wav")
            soundMapUp["SYNTH_SPRING"] = loadAssetSound("audio_split/switch_ibm_buckling_up.wav")
        }
    }

    private fun playKeyClickSound(isKeyDown: Boolean = true) {
        val prefs = context.getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        val isSoundEnabled = prefs.getBoolean("pref_key_click_sound_enabled", true)
        if (!isSoundEnabled) return

        val isSplitAudioEnabled = prefs.getBoolean("pref_split_key_click_sound_enabled", true)
        val switchType = prefs.getString("pref_switch_type", "REC_EG_PURPLE") ?: "REC_EG_PURPLE"
        val volumePercent = prefs.getInt("pref_key_click_volume", 80).coerceIn(0, 100)
        val vol = volumePercent / 100f
        if (vol <= 0f) return

        if (soundPool == null) {
            initSoundPool()
        }

        val soundId = if (isSplitAudioEnabled) {
            val targetMap = if (isKeyDown) soundMapDown else soundMapUp
            targetMap[switchType] ?: targetMap["REC_EG_PURPLE"] ?: targetMap.values.firstOrNull() ?: 0
        } else {
            if (!isKeyDown) return
            soundMap[switchType] ?: soundMap["REC_EG_PURPLE"] ?: soundMap.values.firstOrNull() ?: 0
        }

        var played = false
        if (soundId != 0) {
            val streamId = soundPool?.play(soundId, vol, vol, 1, 0, 1.0f) ?: 0
            if (streamId != 0) {
                played = true
            }
        }

        if (!played && isKeyDown) {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                audioManager?.playSoundEffect(android.media.AudioManager.FX_KEYPRESS_STANDARD, vol)
            } catch (_: Exception) {}
        }
    }


    private fun findHitKeyBounds(x: Float, y: Float): KeyBounds? {
        val density = context.resources.displayMetrics.density
        val expansion = 6f * density

        val isVerticalScroll = layoutDefinition?.metadata?.scrollDirection.equals("VERTICAL", ignoreCase = true)
        if (isVerticalScroll) {
            val fixedHit = keyBoundsList.firstOrNull { it.isFixedRow && !it.key.isSpacer && it.rect.contains(x, y) }
            if (fixedHit != null) return fixedHit

            val h = height.toFloat()
            val vSpacingPx = resolveDimension(layoutDefinition?.metadata?.verticalSpacing, h, density, 4f)
            val maxVisRows = layoutDefinition?.metadata?.maxVisibleRows ?: 4
            val effectiveRowCount = maxVisRows + 2
            val availableHeight = h - (vSpacingPx * (effectiveRowCount + 1))
            val rowHeight = availableHeight / effectiveRowCount

            val topBarBottom = vSpacingPx + rowHeight + (vSpacingPx / 2f)
            val bottomNavTop = h - rowHeight - (vSpacingPx * 1.5f)

            if (y >= topBarBottom && y <= bottomNavTop) {
                val scrollHit = keyBoundsList.firstOrNull { !it.isFixedRow && !it.key.isSpacer && it.rect.contains(x, y) }
                if (scrollHit != null) return scrollHit

                return keyBoundsList
                    .filter { !it.isFixedRow && !it.key.isSpacer }
                    .filter {
                        val expanded = RectF(it.rect).apply { inset(-expansion, -expansion) }
                        expanded.contains(x, y)
                    }
                    .minByOrNull {
                        val dx = x - it.rect.centerX()
                        val dy = y - it.rect.centerY()
                        dx * dx + dy * dy
                    }
            }

            return keyBoundsList
                .filter { it.isFixedRow && !it.key.isSpacer }
                .filter {
                    val expanded = RectF(it.rect).apply { inset(-expansion, -expansion) }
                    expanded.contains(x, y)
                }
                .minByOrNull {
                    val dx = x - it.rect.centerX()
                    val dy = y - it.rect.centerY()
                    dx * dx + dy * dy
                }
        }

        val directHit = keyBoundsList.firstOrNull { (isEditorPreviewMode || !it.key.isSpacer) && it.rect.contains(x, y) }
        if (directHit != null) return directHit

        return keyBoundsList
            .filter { isEditorPreviewMode || !it.key.isSpacer }
            .filter {
                val expanded = RectF(it.rect).apply { inset(-expansion, -expansion) }
                expanded.contains(x, y)
            }
            .minByOrNull {
                val dx = x - it.rect.centerX()
                val dy = y - it.rect.centerY()
                dx * dx + dy * dy
            }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isDraggingFloatingWindow) {
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - dragStartX
                    val dy = event.rawY - dragStartY
                    floatingOffsetX = initialFloatingOffsetX + dx
                    floatingOffsetY = initialFloatingOffsetY + dy
                    recalculateKeyBounds()
                    invalidate()
                    onFloatingBoundsChangedListener?.invoke()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDraggingFloatingWindow = false
                    val prefs = context.getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putFloat("pref_floating_offset_x", floatingOffsetX)
                        .putFloat("pref_floating_offset_y", floatingOffsetY)
                        .apply()
                    onFloatingBoundsChangedListener?.invoke()
                }
            }
            return true
        }

        // Multi-touch two-finger surface gesture
        if (event.pointerCount > 1) {
            if (isTrackpadActive || isSpacebarTrackpad) {
                return true
            }
            val p0X = event.getX(0)
            val p0Y = event.getY(0)
            val p1X = event.getX(1)
            val p1Y = event.getY(1)
            val centerX = (p0X + p1X) / 2f
            val centerY = (p0Y + p1Y) / 2f
            val dx = p0X - p1X
            val dy = p0Y - p1Y
            val currentDist = kotlin.math.sqrt(dx * dx + dy * dy)

            when (event.actionMasked) {
                MotionEvent.ACTION_POINTER_DOWN -> {
                    initialPointersDistance = currentDist
                    initialPointersCenterX = centerX
                    initialPointersCenterY = centerY
                }
                MotionEvent.ACTION_MOVE -> {
                    val distDiff = currentDist - initialPointersDistance
                    val deltaX = centerX - initialPointersCenterX
                    val deltaY = centerY - initialPointersCenterY

                    if (distDiff > 80f) {
                        setFormFactorMode(com.programmerkeyboard.model.FormFactorMode.SPLIT)
                        initialPointersDistance = currentDist
                        initialPointersCenterX = centerX
                        initialPointersCenterY = centerY
                    } else if (distDiff < -80f) {
                        setFormFactorMode(com.programmerkeyboard.model.FormFactorMode.FULL_WIDTH_DOCKED)
                        initialPointersDistance = currentDist
                    } else if (deltaY > 80f) {
                        setFormFactorMode(com.programmerkeyboard.model.FormFactorMode.FULL_WIDTH_DOCKED)
                        initialPointersCenterY = centerY
                        initialPointersCenterX = centerX
                    } else if (deltaX < -80f) {
                        setFormFactorMode(com.programmerkeyboard.model.FormFactorMode.LEFT_DOCKED)
                        initialPointersCenterX = centerX
                        initialPointersCenterY = centerY
                    } else if (deltaX > 80f) {
                        setFormFactorMode(com.programmerkeyboard.model.FormFactorMode.RIGHT_DOCKED)
                        initialPointersCenterX = centerX
                        initialPointersCenterY = centerY
                    }
                }
            }
        }

        if (joystickPopupWidget != null) {
            joystickPopupWidget?.handleTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                joystickPopupWidget = null
                pressedKeyBounds = null
                invalidate()
            }
            return true
        }

        if (keyPopupOverlay != null) {
            if (keyPopupOverlay?.isShowing() == false) {
                keyPopupOverlay = null
            } else {
                keyPopupOverlay?.handleTouchEvent(event)
                if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                    playKeyClickSound(isKeyDown = false)
                    keyPopupOverlay?.dismiss()
                    keyPopupOverlay = null
                    pressedKeyBounds = null
                    dismissKeyPreview()
                    invalidate()
                }
                return true
            }
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                handler.removeCallbacks(inertialScrollRunnable)
                isLongPressTriggered = false
                isScrollDragging = false
                isSpacebarTrackpad = false
                lastScrollTouchY = event.y
                isTouchInScrollableRegion = false

                velocityTracker?.recycle()
                velocityTracker = android.view.VelocityTracker.obtain()
                velocityTracker?.addMovement(event)

                if (maxScrollOffsetY > 0f) {
                    val density = resources.displayMetrics.density
                    val h = height.toFloat()
                    val vSpacingPx = resolveDimension(layoutDefinition?.metadata?.verticalSpacing, h, density, 4f)
                    val maxVisRows = layoutDefinition?.metadata?.maxVisibleRows ?: 4
                    val effectiveRowCount = maxVisRows + 2
                    val availableHeight = h - (vSpacingPx * (effectiveRowCount + 1))
                    val rowHeight = availableHeight / effectiveRowCount

                    val topBarBottom = vSpacingPx + rowHeight + (vSpacingPx / 2f)
                    val bottomNavTop = h - rowHeight - (vSpacingPx * 1.5f)

                    if (event.y >= topBarBottom && event.y <= bottomNavTop) {
                        isTouchInScrollableRegion = true
                    }
                }

                if (keyboardState.formFactorMode == com.programmerkeyboard.model.FormFactorMode.FLOATING) {
                    val cardBounds = getFloatingCardBounds()
                    if (cardBounds != null) {
                        val d = resources.displayMetrics.density
                        val topHandleZone = RectF(cardBounds.left, cardBounds.top, cardBounds.right, cardBounds.top + 36f * d)
                        val hitKey = findHitKeyBounds(event.x, event.y)
                        if (topHandleZone.contains(event.x, event.y) || (hitKey == null && cardBounds.contains(event.x, event.y))) {
                            isDraggingFloatingWindow = true
                            dragStartX = event.rawX
                            dragStartY = event.rawY
                            initialFloatingOffsetX = floatingOffsetX
                            initialFloatingOffsetY = floatingOffsetY
                            performKeypressHapticFeedback()
                            return true
                        }
                    }
                }

                if (isEditorPreviewMode) {
                    editorDownX = event.rawX
                    editorDownY = event.rawY
                    isEditorDragging = false
                    val hitGear = rowGearBoundsList.firstOrNull { it.rect.contains(event.x, event.y) }
                    if (hitGear != null) {
                        performKeypressHapticFeedback()
                        return true
                    }
                }
                
                pressedKeyBounds = findHitKeyBounds(event.x, event.y)
                if (pressedKeyBounds != null) {
                    val targetKeyLabel = pressedKeyBounds!!.key.primaryLabel
                    val prefs = context.getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
                    val debounceMs = prefs.getInt("pref_key_debounce_ms", 35)
                    val now = System.currentTimeMillis()
                    if (debounceMs > 0 && targetKeyLabel == lastKeyTapLabel && (now - lastKeyTapTimeMs) < debounceMs) {
                        return true
                    }
                    lastKeyTapTimeMs = now
                    lastKeyTapLabel = targetKeyLabel

                    performKeypressHapticFeedback()
                    playKeyClickSound(isKeyDown = true)
                    showKeyPreview(pressedKeyBounds!!.key, pressedKeyBounds!!.rect)
                    handler.postDelayed(longPressRunnable, getLongPressTimeoutMs())

                    if (isTrackpadEligibleKey(pressedKeyBounds?.key)) {
                        trackpadTouchX = event.x
                        trackpadTouchY = event.y
                        trackpadLastX = event.x
                        trackpadLastY = event.y
                        trackpadAccumulatedDx = 0f
                        trackpadAccumulatedDy = 0f
                    }
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)
                if (isDraggingFloatingWindow) {
                    val dx = event.rawX - dragStartX
                    val dy = event.rawY - dragStartY
                    floatingOffsetX = initialFloatingOffsetX + dx
                    floatingOffsetY = initialFloatingOffsetY + dy
                    recalculateKeyBounds()
                    invalidate()
                    onFloatingBoundsChangedListener?.invoke()
                    return true
                }

                if (isEditorPreviewMode) {
                    val dx = event.rawX - editorDownX
                    val dy = event.rawY - editorDownY
                    val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop
                    if (!isEditorDragging && (dx * dx + dy * dy > touchSlop * touchSlop)) {
                        isEditorDragging = true
                        pressedKeyBounds = null
                        dismissKeyPreview()
                        handler.removeCallbacks(longPressRunnable)
                        invalidate()
                    }
                    if (isEditorDragging) {
                        return false
                    }
                }

                if (isLongPressTriggered) {
                    return true
                }

                if (isTrackpadEligibleKey(pressedKeyBounds?.key)) {
                    val density = context.resources.displayMetrics.density
                    val threshold = 12f * density
                    val totalDx = event.x - trackpadTouchX
                    val totalDy = event.y - trackpadTouchY

                    if (!isSpacebarTrackpad && (kotlin.math.abs(totalDx) > threshold || kotlin.math.abs(totalDy) > threshold)) {
                        isSpacebarTrackpad = true
                        handler.removeCallbacks(longPressRunnable)
                        stopAutoRepeat()
                        dismissKeyPreview()
                    }

                    if (isSpacebarTrackpad) {
                        val dx = event.x - trackpadLastX
                        val dy = event.y - trackpadLastY
                        trackpadLastX = event.x
                        trackpadLastY = event.y

                        trackpadAccumulatedDx += dx
                        trackpadAccumulatedDy += dy

                        val step = 14f * density
                        while (trackpadAccumulatedDx >= step) {
                            onKeyActionListener?.invoke(KeyAction.SendCode(KeyEvent.KEYCODE_DPAD_RIGHT))
                            performKeypressHapticFeedback()
                            trackpadBlinkStep = (trackpadBlinkStep + 1) % trackpadBlinkColors.size
                            trackpadBlinkTime = System.currentTimeMillis()
                            trackpadAccumulatedDx -= step
                        }
                        while (trackpadAccumulatedDx <= -step) {
                            onKeyActionListener?.invoke(KeyAction.SendCode(KeyEvent.KEYCODE_DPAD_LEFT))
                            performKeypressHapticFeedback()
                            trackpadBlinkStep = (trackpadBlinkStep + 1) % trackpadBlinkColors.size
                            trackpadBlinkTime = System.currentTimeMillis()
                            trackpadAccumulatedDx += step
                        }
                        while (trackpadAccumulatedDy >= step) {
                            onKeyActionListener?.invoke(KeyAction.SendCode(KeyEvent.KEYCODE_DPAD_DOWN))
                            performKeypressHapticFeedback()
                            trackpadBlinkStep = (trackpadBlinkStep + 1) % trackpadBlinkColors.size
                            trackpadBlinkTime = System.currentTimeMillis()
                            trackpadAccumulatedDy -= step
                        }
                        while (trackpadAccumulatedDy <= -step) {
                            onKeyActionListener?.invoke(KeyAction.SendCode(KeyEvent.KEYCODE_DPAD_UP))
                            performKeypressHapticFeedback()
                            trackpadBlinkStep = (trackpadBlinkStep + 1) % trackpadBlinkColors.size
                            trackpadBlinkTime = System.currentTimeMillis()
                            trackpadAccumulatedDy += step
                        }
                        invalidate()
                        return true
                    }
                }

                if (maxScrollOffsetY > 0f) {
                    val density = context.resources.displayMetrics.density
                    val h = height.toFloat()
                    val vSpacingPx = resolveDimension(layoutDefinition?.metadata?.verticalSpacing, h, density, 4f)
                    val maxVisRows = layoutDefinition?.metadata?.maxVisibleRows ?: 4
                    val effectiveRowCount = maxVisRows + 2
                    val availableHeight = h - (vSpacingPx * (effectiveRowCount + 1))
                    val rowHeight = availableHeight / effectiveRowCount

                    val topBarBottom = vSpacingPx + rowHeight + (vSpacingPx / 2f)
                    val bottomNavTop = h - rowHeight - (vSpacingPx * 1.5f)

                    val dy = lastScrollTouchY - event.y
                    if (event.y >= topBarBottom && event.y <= bottomNavTop && (kotlin.math.abs(dy) > 12f || isScrollDragging)) {
                        isScrollDragging = true
                        pressedKeyBounds = null
                        scrollOffsetY = (scrollOffsetY + dy).coerceIn(0f, maxScrollOffsetY)
                        lastScrollTouchY = event.y
                        dismissKeyPreview()
                        handler.removeCallbacks(longPressRunnable)
                        stopAutoRepeat()
                        recalculateKeyBounds()
                        invalidate()
                        return true
                    }
                }

                if (pressedKeyBounds != null && pressedKeyBounds!!.rect.contains(event.x, event.y)) {
                    return true
                }
                val hoveredBounds = findHitKeyBounds(event.x, event.y)
                if (hoveredBounds?.key != pressedKeyBounds?.key) {
                    dismissKeyPreview()
                    handler.removeCallbacks(longPressRunnable)
                    stopAutoRepeat()
                    isLongPressTriggered = false
                    pressedKeyBounds = hoveredBounds
                    if (pressedKeyBounds != null) {
                        showKeyPreview(pressedKeyBounds!!.key, pressedKeyBounds!!.rect)
                        handler.postDelayed(longPressRunnable, getLongPressTimeoutMs())
                    }
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.recycle()
                velocityTracker = null
                pressedKeyBounds = null
                isLongPressTriggered = false
                isEditorDragging = false
                isSpacebarTrackpad = false
                isDraggingFloatingWindow = false
                isScrollDragging = false
                dismissKeyPreview()
                handler.removeCallbacks(longPressRunnable)
                stopAutoRepeat()
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker?.addMovement(event)
                velocityTracker?.computeCurrentVelocity(1000)
                val initialVelocityY = velocityTracker?.yVelocity ?: 0f
                velocityTracker?.recycle()
                velocityTracker = null

                if (isSpacebarTrackpad) {
                    isSpacebarTrackpad = false
                    pressedKeyBounds = null
                    isLongPressTriggered = false
                    dismissKeyPreview()
                    handler.removeCallbacks(longPressRunnable)
                    stopAutoRepeat()
                    invalidate()
                    return true
                }
                if (isDraggingFloatingWindow) {
                    isDraggingFloatingWindow = false
                    val prefs = context.getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putFloat("pref_floating_offset_x", floatingOffsetX)
                        .putFloat("pref_floating_offset_y", floatingOffsetY)
                        .apply()
                    onFloatingBoundsChangedListener?.invoke()
                    return true
                }
                if (isScrollDragging) {
                    isScrollDragging = false
                    pressedKeyBounds = null
                    if (kotlin.math.abs(initialVelocityY) > 150f) {
                        startInertialScroll(initialVelocityY)
                    }
                    invalidate()
                    return true
                }
                
                val zoomedText = activeZoomedText
                dismissKeyPreview()
                handler.removeCallbacks(longPressRunnable)
                stopAutoRepeat()

                if (isEditorPreviewMode) {
                    val wasDragging = isEditorDragging
                    isEditorDragging = false
                    pressedKeyBounds = null
                    isLongPressTriggered = false
                    dismissKeyPreview()

                    if (wasDragging) {
                        invalidate()
                        return true
                    }

                    val hitGear = rowGearBoundsList.firstOrNull { it.rect.contains(event.x, event.y) }
                    if (hitGear != null) {
                        onRowTapForEditingListener?.invoke(hitGear.rowIdx, hitGear.row)
                        invalidate()
                        return true
                    }
                    val releasedBounds = findHitKeyBounds(event.x, event.y)
                    if (releasedBounds != null) {
                        executeAction(releasedBounds.key.onPressAction, releasedBounds.key)
                    } else {
                        onSpacingTapForEditingListener?.invoke()
                    }
                    invalidate()
                    return true
                }

                val releasedBounds = findHitKeyBounds(event.x, event.y) ?: pressedKeyBounds

                if (!isLongPressTriggered) {
                    releasedBounds?.key?.let { key ->
                        executeAction(key.onPressAction, key)
                    }
                } else if (zoomedText != null) {
                    executeAction(KeyAction.SendText(zoomedText), pressedKeyBounds?.key)
                }

                playKeyClickSound(isKeyDown = false)
                pressedKeyBounds = null
                isLongPressTriggered = false
                invalidate()
                return true

            }
        }
        return super.onTouchEvent(event)
    }

    private fun isModifierKey(key: KeyDefinition): Boolean {
        if (key.onPressAction is KeyAction.ToggleModifier || key.onPressAction is KeyAction.LockModifier || key.onLongPressAction is KeyAction.LockModifier || key.onPressAction is KeyAction.SwitchLayout) return true
        return key.primaryLabel in listOf("Ctrl", "Control", "Shift", "⇧", "Alt", "Option", "Super", "Win", "Cmd", "⌘", "❖", "Fn")
    }

    private fun getModifierState(key: KeyDefinition): com.programmerkeyboard.model.ModifierState {
        val action = key.onPressAction
        val lpAction = key.onLongPressAction
        val modName = when {
            action is KeyAction.ToggleModifier -> action.modifier
            action is KeyAction.LockModifier -> action.modifier
            lpAction is KeyAction.LockModifier -> lpAction.modifier
            else -> null
        }
        if (modName != null) {
            return when (modName.uppercase()) {
                "SHIFT" -> keyboardState.shiftState
                "CTRL" -> keyboardState.ctrlState
                "ALT" -> keyboardState.altState
                "SUPER" -> keyboardState.superState
                else -> com.programmerkeyboard.model.ModifierState.OFF
            }
        }
        return when (key.primaryLabel) {
            "Fn" -> if (keyboardState.isFnActive) com.programmerkeyboard.model.ModifierState.LATCHED else com.programmerkeyboard.model.ModifierState.OFF
            "Shift", "⇧" -> keyboardState.shiftState
            "Ctrl" -> keyboardState.ctrlState
            "Alt" -> keyboardState.altState
            "Super", "Win", "Cmd", "⌘", "❖" -> keyboardState.superState
            else -> com.programmerkeyboard.model.ModifierState.OFF
        }
    }

    private fun getArrowKeyCode(label: String): Int {
        return when (label) {
            "UpArrow", "↑", "△" -> KeyEvent.KEYCODE_DPAD_UP
            "DownArrow", "↓", "▽" -> KeyEvent.KEYCODE_DPAD_DOWN
            "LeftArrow", "←", "◁" -> KeyEvent.KEYCODE_DPAD_LEFT
            "RightArrow", "→", "▷" -> KeyEvent.KEYCODE_DPAD_RIGHT
            else -> KeyEvent.KEYCODE_DPAD_CENTER
        }
    }

    private fun resolveActionFromLabel(label: String): KeyAction {
        val clean = label.replace("✨", "").replace("📄", "").replace("✂️", "").replace("📥", "").replace("📢", "").replace("⌨", "").trim().uppercase()
        return when (clean) {
            "ALL", "SELECT ALL", "SELECT_ALL", "全选" -> KeyAction.SelectAll
            "COPY", "复制" -> KeyAction.Copy
            "CUT", "剪切" -> KeyAction.Cut
            "PASTE", "粘贴" -> KeyAction.Paste
            "ECHO", "PASTE ECHO", "ECHO_CLIPBOARD", "PASTE_ECHO", "PASTE_TEXT", "📋", "📎" -> KeyAction.PasteEcho
            "IME", "SWITCH IME", "SWITCH_IME", "KEYBOARD" -> KeyAction.SwitchIme
            else -> KeyAction.SendText(label)
        }
    }

    private fun isTrackpadEligibleKey(key: KeyDefinition?): Boolean {
        if (key == null) return false
        val prefs = context.getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        val isSpacebarTrackpadEnabled = prefs.getBoolean("pref_enable_spacebar_trackpad", true)
        val isArrowTrackpadEnabled = prefs.getBoolean("pref_enable_arrow_trackpad", false)

        val label = key.primaryLabel
        val isSpace = label == "␣" || label.equals("space", ignoreCase = true) || key.isSplitKey
        if (isSpace) return isSpacebarTrackpadEnabled

        val isArrowLabel = label in listOf("←", "↑", "↓", "→", "UpArrow", "DownArrow", "LeftArrow", "RightArrow")
        val press = key.onPressAction
        val isArrowCode = (press is KeyAction.SendCode) && (press.code == KeyEvent.KEYCODE_DPAD_UP ||
                press.code == KeyEvent.KEYCODE_DPAD_DOWN || press.code == KeyEvent.KEYCODE_DPAD_LEFT ||
                press.code == KeyEvent.KEYCODE_DPAD_RIGHT)

        if (isArrowLabel || isArrowCode) return isArrowTrackpadEnabled

        return false
    }

    private fun isEmojiKey(key: KeyDefinition?): Boolean {
        if (key == null) return false
        if (layoutDefinition?.id?.startsWith("emoji") == true) return true
        val label = key.primaryLabel
        if (label.isEmpty()) return false
        val firstCodePoint = label.codePointAt(0)
        return Character.getType(firstCodePoint).toByte() == Character.SURROGATE ||
               firstCodePoint in 0x1F300..0x1F9FF ||
               firstCodePoint in 0x1F600..0x1F64F ||
               firstCodePoint in 0x1F680..0x1F6FF ||
               firstCodePoint in 0x2600..0x27BF ||
               firstCodePoint in 0x1F1E6..0x1F1FF ||
               firstCodePoint in 0x1FA70..0x1FAFF
    }

    private fun startInertialScroll(initialVelocityY: Float) {
        handler.removeCallbacks(inertialScrollRunnable)
        inertialScrollRunnable.velocityY = initialVelocityY
        handler.post(inertialScrollRunnable)
    }
}
