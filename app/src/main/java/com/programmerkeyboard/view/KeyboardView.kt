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
        val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        val effectiveHeightPercent = if (isLandscape) minOf(heightPercentage, 35) else heightPercentage

        val calculatedHeight = if (keyboardState.formFactorMode == com.programmerkeyboard.model.FormFactorMode.FLOATING) {
            (displayMetrics.heightPixels * 0.45f).toInt()
        } else {
            (displayMetrics.heightPixels * (effectiveHeightPercent / 100f)).toInt()
        }

        val aspectRatio = getKeyboardAspectRatio()
        val idealWidth = (calculatedHeight * aspectRatio).toInt()
        val isPrimaryFullWidth = isPrimaryFullWidthLayout()

        val measuredWidth = when (keyboardState.formFactorMode) {
            com.programmerkeyboard.model.FormFactorMode.LEFT_DOCKED,
            com.programmerkeyboard.model.FormFactorMode.RIGHT_DOCKED,
            com.programmerkeyboard.model.FormFactorMode.SIDE_DOCKED,
            com.programmerkeyboard.model.FormFactorMode.SPLIT,
            com.programmerkeyboard.model.FormFactorMode.FLOATING -> width
            com.programmerkeyboard.model.FormFactorMode.FULL_WIDTH_DOCKED -> {
                if (isPrimaryFullWidth) width else minOf(width, idealWidth)
            }
        }

        setMeasuredDimension(measuredWidth, calculatedHeight)
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

    // Long press and popup overlay handlers
    private val handler = Handler(Looper.getMainLooper())
    private var isLongPressTriggered = false
    private var keyPopupOverlay: KeyPopupOverlay? = null
    private var keyPreviewOverlay: KeyPreviewOverlay? = null
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

    // Floating Window Draggable Position Offsets
    private var floatingOffsetX = 0f
    private var floatingOffsetY = 0f
    private var isDraggingFloatingWindow = false
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var initialFloatingOffsetX = 0f
    private var initialFloatingOffsetY = 0f

    private fun isInTrackpadZone(x: Float, y: Float): Boolean {
        return false
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
            if (key.onLongPressAction is KeyAction.None) {
                return@Runnable
            }
            isLongPressTriggered = true
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
        return prefs.getFloat("pref_keyboard_aspect_ratio", 2.2f).coerceIn(1.0f, 5.0f)
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
        val aspectRatio = getKeyboardAspectRatio()
        val idealWidth = h * aspectRatio
        val isPrimaryFullWidth = isPrimaryFullWidthLayout()

        val targetWidth = when (formFactor) {
            com.programmerkeyboard.model.FormFactorMode.FULL_WIDTH_DOCKED -> {
                if (isPrimaryFullWidth) w else minOf(w, idealWidth)
            }
            com.programmerkeyboard.model.FormFactorMode.LEFT_DOCKED,
            com.programmerkeyboard.model.FormFactorMode.SIDE_DOCKED,
            com.programmerkeyboard.model.FormFactorMode.RIGHT_DOCKED,
            com.programmerkeyboard.model.FormFactorMode.SPLIT,
            com.programmerkeyboard.model.FormFactorMode.FLOATING -> minOf(w, idealWidth)
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
                        val sx = (w - targetWidth) / 2f
                        Triple(sx, vSpacingPx, rowHeight)
                    }
                    com.programmerkeyboard.model.FormFactorMode.RIGHT_DOCKED -> {
                        Triple(w - targetWidth, vSpacingPx, rowHeight)
                    }
                    com.programmerkeyboard.model.FormFactorMode.FLOATING -> {
                        val baseStartX = (w - activeWidth) / 2f
                        val sx = (baseStartX + floatingOffsetX).coerceIn(4f, maxOf(4f, w - activeWidth - 4f))
                        val topHandlePadding = 20f * density
                        val floatCardHeight = (activeWidth / aspectRatio).coerceIn(120f * density, h * 0.70f)
                        val baseStartY = (h - floatCardHeight) / 2f
                        val cardTop = (baseStartY + floatingOffsetY).coerceIn(4f, maxOf(4f, h - floatCardHeight - 4f))
                        val sy = cardTop + topHandlePadding
                        val floatAvailableHeight = floatCardHeight - topHandlePadding - 12f * density
                        val rh = maxOf(10f, floatAvailableHeight / currentRows.size)
                        Triple(sx, sy, rh)
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
                        val effectiveWeight = baseW + (if (key.isFlexible) flexBonus else 0f)

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
                val dims = computeSplitClusterDimensions(activeWidth, w)

                val maxLeftRatioWeight = currentRows.maxOfOrNull { row ->
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
                }?.toFloat() ?: 6.0f

                val maxRightRatioWeight = currentRows.maxOfOrNull { row ->
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
                }?.toFloat() ?: 6.0f

                currentRows.forEachIndexed { rowIndex, row ->
                    val splitIdx = row.splitIndex ?: row.keys.indexOfFirst { it.isSplitKey }.takeIf { it >= 0 } ?: ((row.keys.size + 1) / 2)
                    val isSplitKey = row.splitKey || (splitIdx in row.keys.indices && row.keys[splitIdx].isSplitKey)

                    val leftKeys: List<KeyDefinition>
                    val rightKeys: List<KeyDefinition>

                    if (isSplitKey && splitIdx in row.keys.indices) {
                        val targetKey = row.keys[splitIdx]
                        val leftWeight = targetKey.splitLeftWeight ?: when (val weight = targetKey.widthWeight) {
                            is DimensionValue.Ratio -> DimensionValue.Ratio(weight.value / 2f)
                            is DimensionValue.Absolute -> DimensionValue.Absolute(weight.value / 2)
                        }
                        val rightWeight = targetKey.splitRightWeight ?: when (val weight = targetKey.widthWeight) {
                            is DimensionValue.Ratio -> {
                                val lw = (leftWeight as? DimensionValue.Ratio)?.value ?: (weight.value / 2f)
                                DimensionValue.Ratio(maxOf(0.1f, weight.value - lw))
                            }
                            is DimensionValue.Absolute -> {
                                val lw = (leftWeight as? DimensionValue.Absolute)?.value ?: (weight.value / 2)
                                DimensionValue.Absolute(maxOf(1, weight.value - lw))
                            }
                        }
                        val leftHalf = targetKey.copy(widthWeight = leftWeight)
                        val rightHalf = targetKey.copy(widthWeight = rightWeight)

                        leftKeys = row.keys.take(splitIdx) + leftHalf
                        rightKeys = listOf(rightHalf) + row.keys.drop(splitIdx + 1)
                    } else {
                        leftKeys = row.keys.take(splitIdx)
                        rightKeys = row.keys.drop(splitIdx)
                    }

                    val leftRatioWeight = leftKeys.sumOf { (it.widthWeight as? DimensionValue.Ratio)?.value?.toDouble() ?: 1.0 }.toFloat()
                    val leftDeficit = maxOf(0f, maxLeftRatioWeight - leftRatioWeight)
                    val leftFlexCount = leftKeys.count { it.isFlexible }
                    val leftFlexBonus = if (leftFlexCount > 0 && leftDeficit > 0f) (leftDeficit / leftFlexCount) else 0f

                    val rightRatioWeight = rightKeys.sumOf { (it.widthWeight as? DimensionValue.Ratio)?.value?.toDouble() ?: 1.0 }.toFloat()
                    val rightDeficit = maxOf(0f, maxRightRatioWeight - rightRatioWeight)
                    val rightFlexCount = rightKeys.count { it.isFlexible }
                    val rightFlexBonus = if (rightFlexCount > 0 && rightDeficit > 0f) (rightDeficit / rightFlexCount) else 0f

                    val leftClusterWidth = row.splitRatio?.let { (activeWidth * it).coerceIn(activeWidth * 0.2f, activeWidth * 0.8f) } ?: dims.leftClusterWidth
                    val rightClusterWidth = activeWidth - leftClusterWidth

                    val leftAvailable = (leftClusterWidth - (hSpacingPx * (maxLeftRatioWeight + 1)))
                    val globalLeftBaseUnit = if (maxLeftRatioWeight > 0) maxOf(0f, leftAvailable / maxLeftRatioWeight) else 0f

                    val rightAvailable = (rightClusterWidth - (hSpacingPx * (maxRightRatioWeight + 1)))
                    val globalRightBaseUnit = if (maxRightRatioWeight > 0) maxOf(0f, rightAvailable / maxRightRatioWeight) else 0f

                    // Offsets for Left and Right Clusters
                    val leftRowOffsetPx = when (val off = row.leftOffset) {
                        is DimensionValue.Ratio -> leftClusterWidth * off.value
                        is DimensionValue.Absolute -> off.value * density
                        else -> 0f
                    }
                    val rightRowOffsetPx = when (val off = row.rightOffset) {
                        is DimensionValue.Ratio -> rightClusterWidth * off.value
                        is DimensionValue.Absolute -> off.value * density
                        else -> 0f
                    }

                    // Render Left Cluster Keys
                    var currentX = hSpacingPx + leftRowOffsetPx
                    val currentY = vSpacingPx + rowIndex * (rowHeight + vSpacingPx)

                    leftKeys.forEach { key ->
                        val keyOffsetPx = when (val off = key.startOffset) {
                            is DimensionValue.Ratio -> leftClusterWidth * off.value
                            is DimensionValue.Absolute -> off.value * density
                            else -> 0f
                        }
                        currentX += keyOffsetPx

                        val baseW = (key.widthWeight as? DimensionValue.Ratio)?.value ?: 1.0f
                        val effectiveWeight = baseW + (if (key.isFlexible) leftFlexBonus else 0f)

                        val keyWidth = when (key.widthWeight) {
                            is DimensionValue.Ratio -> (globalLeftBaseUnit * effectiveWeight) + ((effectiveWeight - 1.0f) * hSpacingPx)
                            is DimensionValue.Absolute -> key.widthWeight.value * density
                        }
                        val rect = RectF(currentX, currentY, currentX + keyWidth, currentY + rowHeight)
                        keyBoundsList.add(KeyBounds(key, rect))
                        currentX += keyWidth + hSpacingPx
                    }

                    // Render Right Cluster Keys (Right-Justified against right cluster edge)
                    val totalRightKeysWidth = rightKeys.sumOf { key ->
                        val keyOffsetPx = when (val off = key.startOffset) {
                            is DimensionValue.Ratio -> rightClusterWidth * off.value
                            is DimensionValue.Absolute -> off.value * density
                            else -> 0f
                        }
                        val baseW = (key.widthWeight as? DimensionValue.Ratio)?.value ?: 1.0f
                        val effectiveWeight = baseW + (if (key.isFlexible) rightFlexBonus else 0f)

                        val kw = when (key.widthWeight) {
                            is DimensionValue.Ratio -> (globalRightBaseUnit * effectiveWeight) + ((effectiveWeight - 1.0f) * hSpacingPx)
                            is DimensionValue.Absolute -> key.widthWeight.value * density
                        }
                        (keyOffsetPx + kw).toDouble()
                    }.toFloat() + (hSpacingPx * maxOf(0, rightKeys.size - 1))

                    currentX = w - hSpacingPx - rightRowOffsetPx - totalRightKeysWidth

                    rightKeys.forEach { key ->
                        val keyOffsetPx = when (val off = key.startOffset) {
                            is DimensionValue.Ratio -> rightClusterWidth * off.value
                            is DimensionValue.Absolute -> off.value * density
                            else -> 0f
                        }
                        currentX += keyOffsetPx

                        val baseW = (key.widthWeight as? DimensionValue.Ratio)?.value ?: 1.0f
                        val effectiveWeight = baseW + (if (key.isFlexible) rightFlexBonus else 0f)

                        val keyWidth = when (key.widthWeight) {
                            is DimensionValue.Ratio -> (globalRightBaseUnit * effectiveWeight) + ((effectiveWeight - 1.0f) * hSpacingPx)
                            is DimensionValue.Absolute -> key.widthWeight.value * density
                        }
                        val rect = RectF(currentX, currentY, currentX + keyWidth, currentY + rowHeight)
                        keyBoundsList.add(KeyBounds(key, rect))
                        currentX += keyWidth + hSpacingPx
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
        val themeBg = layoutDefinition?.theme?.backgroundColor
        val bgColor = themeBg ?: ContextCompat.getColor(context, R.color.keyboard_background)

        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }

        when (keyboardState.formFactorMode) {
            com.programmerkeyboard.model.FormFactorMode.FLOATING -> {
                val baseStartX = (w - activeWidth) / 2f
                val startX = (baseStartX + floatingOffsetX).coerceIn(4f, maxOf(4f, w - activeWidth - 4f))
                val floatCardHeight = (activeWidth / aspectRatio).coerceIn(120f * density, h * 0.70f)
                val baseStartY = (h - floatCardHeight) / 2f
                val cardTop = (baseStartY + floatingOffsetY).coerceIn(4f, maxOf(4f, h - floatCardHeight - 4f))
                val cardBottom = cardTop + floatCardHeight

                val cardRect = RectF(startX + 4f, cardTop, startX + activeWidth - 4f, cardBottom)
                canvas.drawRoundRect(cardRect, 20f * density, 20f * density, cardBgPaint)

                val handleWidth = 40f * density
                val handleX = cardRect.centerX() - handleWidth / 2f
                val handleY = cardRect.top + 8f * density
                val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#475569")
                    style = Paint.Style.FILL
                }
                val handleRect = RectF(handleX, handleY, handleX + handleWidth, handleY + 4f * density)
                canvas.drawRoundRect(handleRect, 2f * density, 2f * density, handlePaint)
            }
            else -> {
                // Draw full unsplit background box across the entire keyboard area
                canvas.drawRect(0f, 0f, w, h, cardBgPaint)
            }
        }

        if (keyBoundsList.isEmpty()) {
            recalculateKeyBounds()
        }

        if (keyBoundsList.isEmpty()) return

        val currentRows = (layoutDefinition?.rows ?: emptyList()).filter { isRowVisible(it) }
        val rowCount = if (currentRows.isNotEmpty()) currentRows.size else 5
        val availableHeight = height.toFloat() - (resolveDimension(layoutDefinition?.metadata?.verticalSpacing, height.toFloat(), density, 4f) * (rowCount + 1))
        val rowHeight = availableHeight / rowCount

        val isVerticalScroll = layoutDefinition?.metadata?.scrollDirection.equals("VERTICAL", ignoreCase = true)
        val maxVisRows = layoutDefinition?.metadata?.maxVisibleRows ?: 4
        val effectiveRowCount = maxVisRows + 2
        val availableH = h - (resolveDimension(layoutDefinition?.metadata?.verticalSpacing, h, density, 4f) * (effectiveRowCount + 1))
        val rHeight = availableH / effectiveRowCount
        val vSpacing = resolveDimension(layoutDefinition?.metadata?.verticalSpacing, h, density, 4f)

        val topBarBottom = vSpacing + rHeight + (vSpacing / 2f)
        val bottomNavTop = h - rHeight - (vSpacing * 1.5f)

        keyBoundsList.forEach { keyBounds ->
            val key = keyBounds.key
            if (key.isSpacer) return@forEach
            val rect = keyBounds.rect

            val isMiddleScrollableKey = isVerticalScroll && (rect.top >= topBarBottom - 10f && rect.bottom <= bottomNavTop + 10f)
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
                val lpAct = key.onLongPressAction
                val upAct = key.onSwipeUpAction
                when {
                    !key.secondaryLabel.isNullOrEmpty() -> if (key.secondaryLabel.any { it.isLowerCase() }) key.secondaryLabel.uppercase() else key.secondaryLabel
                    upAct is KeyAction.SendText -> if (upAct.text.any { it.isLowerCase() }) upAct.text.uppercase() else upAct.text
                    lpAct is KeyAction.ShowPopup && lpAct.options.isNotEmpty() -> {
                        val opt = lpAct.options.first()
                        if (opt.any { it.isLowerCase() }) opt.uppercase() else opt
                    }
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

            val baseline = rect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2
            canvas.drawText(displayLabel, rect.centerX(), baseline, textPaintToUse)

            // Top-Left Corner Text Drawing
            val topLeft = key.topLeftLabel
            if (!topLeft.isNullOrEmpty() && !isModifierKey(key)) {
                val topLeftPaint = Paint(secondaryTextPaint).apply { textAlign = Paint.Align.LEFT }
                key.secondaryFgColor?.let { topLeftPaint.color = it }
                canvas.drawText(topLeft, rect.left + (6f * density), rect.top + (14f * density), topLeftPaint)
            }

            // Top-Right Corner Text Drawing (First Alternate)
            val lpAction = key.onLongPressAction
            val upAction = key.onSwipeUpAction
            val rawSecToDraw = key.topRightLabel ?: firstAlt ?: when {
                lpAction is KeyAction.ShowPopup && lpAction.options.isNotEmpty() -> lpAction.options.first()
                !key.secondaryLabel.isNullOrEmpty() -> key.secondaryLabel
                upAction is KeyAction.SendText -> upAction.text
                lpAction is KeyAction.SendText -> lpAction.text
                else -> null
            }

            if (!rawSecToDraw.isNullOrEmpty() && rawSecToDraw != displayLabel && !isModifierKey(key)) {
                val secPaint = Paint(secondaryTextPaint).apply { textAlign = Paint.Align.RIGHT }
                key.secondaryFgColor?.let { secPaint.color = it }
                canvas.drawText(rawSecToDraw, rect.right - (6f * density), rect.top + (14f * density), secPaint)
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
    }

    private fun resolveDimension(value: DimensionValue?, parentSize: Float, density: Float, fallbackPx: Float): Float {
        return when (value) {
            is DimensionValue.Ratio -> parentSize * value.value
            is DimensionValue.Absolute -> value.value * density
            null -> fallbackPx
        }
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
        val actionToExecute = if (sourceKey != null && keyboardState.shouldShiftKey(sourceKey)) {
            val isLetter = sourceKey.primaryLabel.length == 1 && sourceKey.primaryLabel[0].isLowerCase()
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
            is KeyAction.ShowPopup -> {
                if (layoutDefinition?.id == "phone") return
                val rect = (sourceKey?.let { k -> keyBoundsList.firstOrNull { it.key == k } } ?: pressedKeyBounds)?.rect ?: return
                val optionsToDisplay = if (keyboardState.isShiftActive) {
                    actionToExecute.options.map { opt ->
                        if (opt.length == 1 && opt[0].isLowerCase()) opt.uppercase() else opt
                    }
                } else {
                    actionToExecute.options
                }
                keyPopupOverlay = KeyPopupOverlay(context) { selected ->
                    onKeyActionListener?.invoke(KeyAction.SendText(selected))
                }.also {
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
            is KeyAction.ToggleModifier -> {
                onKeyActionListener?.invoke(actionToExecute)
            }
            is KeyAction.SelectAll, is KeyAction.Copy, is KeyAction.Cut, is KeyAction.Paste, is KeyAction.SwitchIme -> {
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

    private fun dismissKeyPreview() {
        keyPreviewOverlay?.dismiss()
        keyPreviewOverlay = null
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
                .setMaxStreams(5)
                .setAudioAttributes(audioAttrs)
                .build()

            // Mechvibes Recorded Switch Sound Packs
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
        }
    }

    private fun playKeyClickSound() {
        val prefs = context.getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        val isSoundEnabled = prefs.getBoolean("pref_key_click_sound_enabled", true)
        if (!isSoundEnabled) return

        val switchType = prefs.getString("pref_switch_type", "REC_EG_PURPLE") ?: "REC_EG_PURPLE"
        val volumePercent = prefs.getInt("pref_key_click_volume", 80).coerceIn(0, 100)
        val vol = volumePercent / 100f
        if (vol <= 0f) return

        if (soundPool == null) {
            initSoundPool()
        }
        val soundId = soundMap[switchType] ?: soundMap["REC_EG_PURPLE"] ?: soundMap.values.firstOrNull() ?: 0
        var played = false
        if (soundId != 0) {
            val streamId = soundPool?.play(soundId, vol, vol, 1, 0, 1.0f) ?: 0
            if (streamId != 0) {
                played = true
            }
        }

        if (!played) {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                audioManager?.playSoundEffect(android.media.AudioManager.FX_KEYPRESS_STANDARD, vol)
            } catch (_: Exception) {}
        }
    }

    private fun findHitKeyBounds(x: Float, y: Float): KeyBounds? {
        val isVerticalScroll = layoutDefinition?.metadata?.scrollDirection.equals("VERTICAL", ignoreCase = true)
        if (isVerticalScroll) {
            val fixedHit = keyBoundsList.firstOrNull { it.isFixedRow && !it.key.isSpacer && it.rect.contains(x, y) }
            if (fixedHit != null) return fixedHit

            val density = context.resources.displayMetrics.density
            val h = height.toFloat()
            val vSpacingPx = resolveDimension(layoutDefinition?.metadata?.verticalSpacing, h, density, 4f)
            val maxVisRows = layoutDefinition?.metadata?.maxVisibleRows ?: 4
            val effectiveRowCount = maxVisRows + 2
            val availableHeight = h - (vSpacingPx * (effectiveRowCount + 1))
            val rowHeight = availableHeight / effectiveRowCount

            val topBarBottom = vSpacingPx + rowHeight + (vSpacingPx / 2f)
            val bottomNavTop = h - rowHeight - (vSpacingPx * 1.5f)

            if (y >= topBarBottom && y <= bottomNavTop) {
                return keyBoundsList.firstOrNull { !it.isFixedRow && !it.key.isSpacer && it.rect.contains(x, y) }
            }
            return null
        }
        return keyBoundsList.firstOrNull { !it.key.isSpacer && it.rect.contains(x, y) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
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
                        initialPointersCenterX = centerX
                        initialPointersCenterY = centerY
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
            keyPopupOverlay?.handleTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                keyPopupOverlay = null
                pressedKeyBounds = null
                dismissKeyPreview()
                invalidate()
            }
            return true
        }

        if (!isTrackpadActive && !isSpacebarTrackpad && gestureDetector.onTouchEvent(event)) {
            dismissKeyPreview()
            handler.removeCallbacks(longPressRunnable)
            stopAutoRepeat()
            return true
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isLongPressTriggered = false
                isScrollDragging = false
                lastScrollTouchY = event.y

                if (isEditorPreviewMode) {
                    val hitGear = rowGearBoundsList.firstOrNull { it.rect.contains(event.x, event.y) }
                    if (hitGear != null) {
                        performKeypressHapticFeedback()
                        return true
                    }
                }
                
                if (isInTrackpadZone(event.x, event.y)) {
                    isTrackpadActive = true
                    isSpacebarTrackpad = false
                    pressedKeyBounds = null
                    trackpadLastX = event.x
                    trackpadLastY = event.y
                    trackpadTouchX = event.x
                    trackpadTouchY = event.y
                    trackpadAccumulatedDx = 0f
                    trackpadAccumulatedDy = 0f
                    performKeypressHapticFeedback()
                    invalidate()
                    return true
                }

                pressedKeyBounds = findHitKeyBounds(event.x, event.y)
                if (pressedKeyBounds != null) {
                    performKeypressHapticFeedback()
                    playKeyClickSound()
                    showKeyPreview(pressedKeyBounds!!.key, pressedKeyBounds!!.rect)
                    handler.postDelayed(longPressRunnable, getLongPressTimeoutMs())
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
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
                if (hoveredBounds != pressedKeyBounds) {
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
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isScrollDragging) {
                    isScrollDragging = false
                    pressedKeyBounds = null
                    invalidate()
                    return true
                }
                dismissKeyPreview()
                handler.removeCallbacks(longPressRunnable)
                stopAutoRepeat()

                if (isEditorPreviewMode) {
                    val hitGear = rowGearBoundsList.firstOrNull { it.rect.contains(event.x, event.y) }
                    if (hitGear != null) {
                        onRowTapForEditingListener?.invoke(hitGear.rowIdx, hitGear.row)
                        pressedKeyBounds = null
                        isLongPressTriggered = false
                        return true
                    }
                }

                val releasedBounds = findHitKeyBounds(event.x, event.y) ?: pressedKeyBounds

                if (!isLongPressTriggered) {
                    releasedBounds?.key?.let { key ->
                        executeAction(key.onPressAction, key)
                    }
                }

                pressedKeyBounds = null
                isLongPressTriggered = false
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun isModifierKey(key: KeyDefinition): Boolean {
        if (key.onPressAction is KeyAction.ToggleModifier || key.onPressAction is KeyAction.SwitchLayout) return true
        return key.primaryLabel in listOf("Ctrl", "Control", "Shift", "⇧", "Alt", "Option", "Super", "Win", "Cmd", "⌘", "❖", "Fn")
    }

    private fun getModifierState(key: KeyDefinition): com.programmerkeyboard.model.ModifierState {
        val action = key.onPressAction
        if (action is KeyAction.ToggleModifier) {
            return when (action.modifier.uppercase()) {
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
            "UpArrow", "↑" -> KeyEvent.KEYCODE_DPAD_UP
            "DownArrow", "↓" -> KeyEvent.KEYCODE_DPAD_DOWN
            "LeftArrow", "←" -> KeyEvent.KEYCODE_DPAD_LEFT
            "RightArrow", "→" -> KeyEvent.KEYCODE_DPAD_RIGHT
            else -> KeyEvent.KEYCODE_DPAD_CENTER
        }
    }
}
