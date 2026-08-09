package com.programmerkeyboard.model

import com.google.gson.annotations.SerializedName

/**
 * Dimensioning value that can be either a Ratio (float) relative to parent size
 * or an Absolute value (int) in pixels/dp.
 */
sealed class DimensionValue {
    data class Ratio(val value: Float) : DimensionValue()
    data class Absolute(val value: Int) : DimensionValue()
}

/**
 * Visual style definition for keys.
 */
data class KeyStyle(
    val bgColor: String? = null,
    val pressedBgColor: String? = null,
    val activeBgColor: String? = null,
    val fgColor: String? = null,
    val secondaryFgColor: String? = null,
    val activeFgColor: String? = null,
    val borderColor: String? = null,
    val borderWidth: DimensionValue? = null,
    val cornerRadius: DimensionValue? = null,
    val fontSize: DimensionValue? = null,
    val secondaryFontSize: DimensionValue? = null,
    val backgroundImage: String? = null,
    val showPreview: Boolean? = null
)

/**
 * Supported Key Action Types in JSON Layout Descriptors
 */
enum class KeyActionType {
    @SerializedName("SEND_TEXT") SEND_TEXT,
    @SerializedName("SEND_CODE") SEND_CODE,
    @SerializedName("SWITCH_LAYOUT") SWITCH_LAYOUT,
    @SerializedName("SET_SCREEN_MODE") SET_SCREEN_MODE,
    @SerializedName("ADJUST_HEIGHT") ADJUST_HEIGHT,
    @SerializedName("SHOW_POPUP") SHOW_POPUP,
    @SerializedName("SHOW_WIDGET") SHOW_WIDGET,
    @SerializedName("AUTO_REPEAT") AUTO_REPEAT,
    @SerializedName("TOGGLE_ROW") TOGGLE_ROW,
    @SerializedName("TOGGLE_MODIFIER") TOGGLE_MODIFIER,
    @SerializedName("SELECT_ALL") SELECT_ALL,
    @SerializedName("COPY") COPY,
    @SerializedName("CUT") CUT,
    @SerializedName("PASTE") PASTE,
    @SerializedName("PASTE_ECHO") PASTE_ECHO,
    @SerializedName("SWITCH_IME") SWITCH_IME,
    @SerializedName("NONE") NONE
}

/**
 * Strongly-typed action triggered by key events or full-surface gestures.
 */
sealed class KeyAction {
    data class SendText(val text: String) : KeyAction()
    data class SendCode(val code: Int) : KeyAction()
    data class SwitchLayout(val target: String) : KeyAction()
    data class SetScreenMode(val mode: String) : KeyAction()
    data class AdjustHeight(val delta: Int? = null, val percentage: Int? = null) : KeyAction()
    data class ShowPopup(val options: List<String>, val actions: List<KeyAction> = emptyList()) : KeyAction()
    data class ShowWidget(val widget: String) : KeyAction()
    data class AutoRepeat(val code: Int, val intervalMs: Long = 50L) : KeyAction()
    data class ToggleRow(val rowId: Any) : KeyAction()
    data class ToggleModifier(val modifier: String) : KeyAction()
    object SelectAll : KeyAction()
    object Copy : KeyAction()
    object Cut : KeyAction()
    object Paste : KeyAction()
    object PasteEcho : KeyAction()
    object SwitchIme : KeyAction()
    data class ShowZoomPreview(val text: String? = null) : KeyAction()
    object None : KeyAction()
}

/**
 * Raw key descriptor model for JSON parsing.
 */
data class KeyDescriptor(
    val label: String,
    val secondaryLabel: String? = null,
    val style: String? = null,
    val weight: Any? = null,
    val height: Any? = null,
    val isSplitKey: Boolean = false,
    val splitLeftWeight: Any? = null,
    val splitRightWeight: Any? = null,
    val flexible: Boolean = false,
    val grow: Boolean = false,
    val canGrow: Boolean = false,
    val spacer: Boolean = false,
    val showPreview: Boolean? = null,
    val showKeyPreview: Boolean? = null,
    val alternates: List<String>? = null,
    val alternateKeys: List<String>? = null,
    val topLeftLabel: String? = null,
    val topRightLabel: String? = null,
    val startOffset: Any? = null,
    val fgColor: String? = null,
    val secondaryFgColor: String? = null,
    val bgColor: String? = null,
    val pressedBgColor: String? = null,
    val activeBgColor: String? = null,
    val borderColor: String? = null,
    val borderWidth: Any? = null,
    val cornerRadius: Any? = null,
    val fontSize: Any? = null,
    val maxFontSize: Any? = null,
    val secondaryFontSize: Any? = null,
    val icon: String? = null,
    val backgroundImage: String? = null,
    val onPress: Map<String, Any>? = null,
    val onLongPress: Map<String, Any>? = null,
    val onSwipeUp: Map<String, Any>? = null,
    val onSwipeDown: Map<String, Any>? = null,
    val onSwipeLeft: Map<String, Any>? = null,
    val onSwipeRight: Map<String, Any>? = null
)

/**
 * Internal definition of a single key on the keyboard.
 */
data class KeyDefinition(
    val primaryLabel: String,
    val secondaryLabel: String? = null,
    val styleName: String? = null,
    val widthWeight: DimensionValue = DimensionValue.Ratio(1.0f),
    val heightRatio: DimensionValue? = null,
    val isSplitKey: Boolean = false,
    val splitLeftWeight: DimensionValue? = null,
    val splitRightWeight: DimensionValue? = null,
    val isFlexible: Boolean = false,
    val maxWeight: Float? = null,
    val isSpacer: Boolean = false,
    val showPreview: Boolean? = null,
    val alternates: List<String> = emptyList(),
    val topLeftLabel: String? = null,
    val topRightLabel: String? = null,
    val startOffset: DimensionValue? = null,
    val fgColor: Int? = null,
    val secondaryFgColor: Int? = null,
    val bgColor: Int? = null,
    val pressedBgColor: Int? = null,
    val activeBgColor: Int? = null,
    val borderColor: Int? = null,
    val borderWidth: DimensionValue? = null,
    val cornerRadius: DimensionValue? = null,
    val fontSize: DimensionValue? = null,
    val maxFontSize: DimensionValue? = null,
    val secondaryFontSize: DimensionValue? = null,
    val iconName: String? = null,
    val backgroundImage: String? = null,
    val onPressAction: KeyAction = KeyAction.SendText(primaryLabel),
    val onLongPressAction: KeyAction = KeyAction.None,
    val onSwipeUpAction: KeyAction = KeyAction.None,
    val onSwipeDownAction: KeyAction = KeyAction.None,
    val onSwipeLeftAction: KeyAction = KeyAction.None,
    val onSwipeRightAction: KeyAction = KeyAction.None
)
