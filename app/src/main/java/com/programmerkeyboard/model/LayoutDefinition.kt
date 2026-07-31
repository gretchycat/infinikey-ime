package com.programmerkeyboard.model

/**
 * Metadata associated with a layout descriptor.
 */
data class LayoutMetadata(
    val horizontalSpacing: DimensionValue = DimensionValue.Absolute(4),
    val verticalSpacing: DimensionValue = DimensionValue.Absolute(4),
    val defaultScreenMode: String = "FULL_WIDTH_DOCKED",
    val defaultHeightPercentage: Int = 30,
    val longPressTimeoutMs: Long = 350L,
    val autoRepeatIntervalMs: Long = 50L,
    val splitClusterRatio: Float? = null,
    val showKeyPreview: Boolean = true,
    val maxFontSize: DimensionValue? = null
)

/**
 * Global theme configuration for a layout.
 */
data class LayoutTheme(
    val backgroundColor: Int? = null,
    val fontFamily: String? = null,
    val modifierOffDotColor: Int? = null,
    val modifierLatchedDotColor: Int? = null,
    val modifierLockedDotColor: Int? = null
)

/**
 * Representation of a single row of keys.
 */
data class KeyRow(
    val id: Any,
    val hidden: Boolean = false,
    val splitIndex: Int? = null,
    val splitKey: Boolean = false,
    val splitRatio: Float? = null,
    val leftOffset: DimensionValue? = null,
    val rightOffset: DimensionValue? = null,
    val keys: List<KeyDefinition>
)

/**
 * Full layout definition holding metadata, theme, styles, surface gestures, and rows.
 */
data class LayoutDefinition(
    val id: String,
    val name: String,
    val version: String = "1.0",
    val author: String = "System",
    val description: String = "",
    val metadata: LayoutMetadata = LayoutMetadata(),
    val theme: LayoutTheme = LayoutTheme(),
    val styles: Map<String, KeyStyle> = emptyMap(),
    val gestures: Map<String, KeyAction> = emptyMap(),
    val rows: List<KeyRow> = emptyList()
)
