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

    @Test
    fun testGetMetaState() {
        val emptyState = KeyboardState()
        assertEquals(0, emptyState.getMetaState())

        val altState = KeyboardState(altState = ModifierState.LATCHED)
        val expectedAlt = android.view.KeyEvent.META_ALT_ON or android.view.KeyEvent.META_ALT_LEFT_ON
        assertEquals(expectedAlt, altState.getMetaState())

        val ctrlState = KeyboardState(ctrlState = ModifierState.LOCKED)
        val expectedCtrl = android.view.KeyEvent.META_CTRL_ON or android.view.KeyEvent.META_CTRL_LEFT_ON
        assertEquals(expectedCtrl, ctrlState.getMetaState())

        val shiftState = KeyboardState(shiftState = ModifierState.LATCHED)
        val expectedShift = android.view.KeyEvent.META_SHIFT_ON or android.view.KeyEvent.META_SHIFT_LEFT_ON
        assertEquals(expectedShift, shiftState.getMetaState())

        val rightShiftState = KeyboardState(shiftState = ModifierState.LATCHED, isRightShift = true)
        val expectedRightShift = android.view.KeyEvent.META_SHIFT_ON or android.view.KeyEvent.META_SHIFT_RIGHT_ON
        assertEquals(expectedRightShift, rightShiftState.getMetaState())

        val superState = KeyboardState(superState = ModifierState.LATCHED)
        val expectedSuper = android.view.KeyEvent.META_META_ON or android.view.KeyEvent.META_META_LEFT_ON
        assertEquals(expectedSuper, superState.getMetaState())

        val combinedState = KeyboardState(
            altState = ModifierState.LATCHED,
            ctrlState = ModifierState.LATCHED,
            shiftState = ModifierState.LATCHED
        )
        val expectedCombined = expectedAlt or expectedCtrl or expectedShift
        assertEquals(expectedCombined, combinedState.getMetaState())
    }

    @Test
    fun testParseModifierComponents() {
        assertEquals(listOf("CTRL", "ALT"), parseModifierComponents("CTRL+ALT"))
        assertEquals(listOf("CTRL", "ALT"), parseModifierComponents("ctrl_alt"))
        assertEquals(listOf("CTRL", "SHIFT"), parseModifierComponents("Control + Shift"))
        assertEquals(listOf("SUPER", "ALT"), parseModifierComponents("META+OPTION"))
        assertEquals(listOf("CTRL", "ALT", "SHIFT"), parseModifierComponents("CTRL+ALT+SHIFT"))
        assertEquals(listOf("SHIFT_RIGHT"), parseModifierComponents("SHIFT_RIGHT"))
        assertEquals(listOf("SHIFT_RIGHT"), parseModifierComponents("RIGHT_SHIFT"))
        assertEquals(listOf("CTRL", "SHIFT_RIGHT"), parseModifierComponents("CTRL+SHIFT_RIGHT"))
    }
}
