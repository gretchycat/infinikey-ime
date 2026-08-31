package com.infinikey_ime.model

import org.junit.Assert.*
import org.junit.Test

class KeyboardModeTest {

    private fun createKey(label: String): KeyDefinition {
        return KeyDefinition(primaryLabel = label)
    }

    @Test
    fun testShouldShiftKeyWhenShiftOff() {
        val state = KeyboardState(shiftState = ModifierState.OFF)
        val letterKey = createKey("a")
        val numberKey = createKey("1")
        val symbolKey = createKey(";")

        assertFalse(state.shouldShiftKey(letterKey))
        assertFalse(state.shouldShiftKey(numberKey))
        assertFalse(state.shouldShiftKey(symbolKey))
    }

    @Test
    fun testShouldShiftKeyWhenShiftLatched() {
        val state = KeyboardState(shiftState = ModifierState.LATCHED)
        val letterKey = createKey("a")
        val numberKey = createKey("1")
        val symbolKey = createKey(";")

        assertTrue(state.shouldShiftKey(letterKey))
        assertTrue(state.shouldShiftKey(numberKey))
        assertTrue(state.shouldShiftKey(symbolKey))
    }

    @Test
    fun testShouldShiftKeyWhenCapsLockLocked() {
        val state = KeyboardState(
            shiftState = ModifierState.LOCKED,
            shiftLockMode = ShiftLockMode.CAPS_LOCK
        )
        val letterKey = createKey("a")
        val upperLetterKey = createKey("A")
        val numberKey = createKey("1")
        val symbolKey = createKey(";")

        assertTrue(state.shouldShiftKey(letterKey))
        assertTrue(state.shouldShiftKey(upperLetterKey))
        assertFalse(state.shouldShiftKey(numberKey))
        assertFalse(state.shouldShiftKey(symbolKey))
    }

    @Test
    fun testShouldShiftKeyWhenShiftLockLocked() {
        val state = KeyboardState(
            shiftState = ModifierState.LOCKED,
            shiftLockMode = ShiftLockMode.SHIFT_LOCK
        )
        val letterKey = createKey("a")
        val numberKey = createKey("1")
        val symbolKey = createKey(";")

        assertTrue(state.shouldShiftKey(letterKey))
        assertTrue(state.shouldShiftKey(numberKey))
        assertTrue(state.shouldShiftKey(symbolKey))
    }
}
