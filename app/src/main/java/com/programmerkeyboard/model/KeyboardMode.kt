package com.programmerkeyboard.model

/**
 * Alignment Mode defining physical key position alignment.
 */
enum class AlignmentMode {
    STAGGERED,     // Standard QWERTY staggered offset
    RECTANGULAR    // Ortholinear uniform grid alignment
}

/**
 * Screen Form Factor mode.
 */
enum class FormFactorMode {
    FULL_WIDTH_DOCKED, // Standard bottom docked full width
    SPLIT,             // Left and right thumb clusters
    LEFT_DOCKED,       // Docked to left screen border
    RIGHT_DOCKED,      // Docked to right screen border
    SIDE_DOCKED,       // Left docked alias
    FLOATING           // Moveable overlay window
}

/**
 * Unified Modifier State enum for Shift, Ctrl, Alt, and Super modifiers.
 */
enum class ModifierState {
    OFF,        // Inactive
    LATCHED,    // Active for the next single keypress (One-shot)
    LOCKED      // Locked active state (Caps lock / Ctrl lock)
}

enum class ShiftLockMode {
    CAPS_LOCK,
    SHIFT_LOCK
}

/**
 * Current operational state of the keyboard layout.
 */
data class KeyboardState(
    var alignmentMode: AlignmentMode = AlignmentMode.STAGGERED,
    var formFactorMode: FormFactorMode = FormFactorMode.FULL_WIDTH_DOCKED,
    var shiftState: ModifierState = ModifierState.OFF,
    var ctrlState: ModifierState = ModifierState.OFF,
    var altState: ModifierState = ModifierState.OFF,
    var superState: ModifierState = ModifierState.OFF,
    var isFnActive: Boolean = false,
    var shiftLockMode: ShiftLockMode = ShiftLockMode.CAPS_LOCK
) {
    val isShiftActive: Boolean get() = shiftState != ModifierState.OFF
    val isCtrlActive: Boolean get() = ctrlState != ModifierState.OFF
    val isAltActive: Boolean get() = altState != ModifierState.OFF
    val isSuperActive: Boolean get() = superState != ModifierState.OFF

    fun shouldShiftKey(key: KeyDefinition): Boolean {
        return when (shiftState) {
            ModifierState.LATCHED -> true
            ModifierState.LOCKED -> {
                if (shiftLockMode == ShiftLockMode.SHIFT_LOCK) {
                    true
                } else {
                    key.primaryLabel.length == 1 && key.primaryLabel[0].isLetter()
                }
            }
            ModifierState.OFF -> false
        }
    }

    fun getMetaState(): Int {
        var meta = 0
        if (shiftState == ModifierState.LATCHED || (shiftState == ModifierState.LOCKED && shiftLockMode == ShiftLockMode.SHIFT_LOCK)) {
            meta = meta or android.view.KeyEvent.META_SHIFT_ON
        } else if (shiftState == ModifierState.LOCKED && shiftLockMode == ShiftLockMode.CAPS_LOCK) {
            meta = meta or android.view.KeyEvent.META_CAPS_LOCK_ON
        }
        if (isCtrlActive) meta = meta or android.view.KeyEvent.META_CTRL_ON
        if (isAltActive) meta = meta or android.view.KeyEvent.META_ALT_ON
        if (isSuperActive) meta = meta or android.view.KeyEvent.META_META_ON
        return meta
    }

    /**
     * Resets any latched (one-shot) modifiers after a character/code emission.
     * Returns true if any modifier state was changed.
     */
    fun consumeOneShotModifiers(): Boolean {
        var changed = false
        if (shiftState == ModifierState.LATCHED) {
            shiftState = ModifierState.OFF
            changed = true
        }
        if (ctrlState == ModifierState.LATCHED) {
            ctrlState = ModifierState.OFF
            changed = true
        }
        if (altState == ModifierState.LATCHED) {
            altState = ModifierState.OFF
            changed = true
        }
        if (superState == ModifierState.LATCHED) {
            superState = ModifierState.OFF
            changed = true
        }
        return changed
    }
}
