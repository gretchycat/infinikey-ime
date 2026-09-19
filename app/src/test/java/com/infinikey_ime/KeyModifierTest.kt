package com.infinikey_ime

import android.view.KeyEvent
import com.infinikey_ime.model.KeyboardState
import com.infinikey_ime.model.ModifierState
import org.junit.Assert.*
import org.junit.Test

class KeyModifierTest {

    @Test
    fun testKeyboardStateMetaStateWithCtrl() {
        val state = KeyboardState(ctrlState = ModifierState.LATCHED)
        val meta = state.getMetaState()
        assertTrue((meta and KeyEvent.META_CTRL_ON) != 0)
        assertTrue((meta and KeyEvent.META_CTRL_LEFT_ON) != 0)
    }

    @Test
    fun testKeyboardStateMetaStateWithAlt() {
        val state = KeyboardState(altState = ModifierState.LATCHED)
        val meta = state.getMetaState()
        assertTrue((meta and KeyEvent.META_ALT_ON) != 0)
        assertTrue((meta and KeyEvent.META_ALT_LEFT_ON) != 0)
    }

    @Test
    fun testKeyboardStateMetaStateWithSuper() {
        val state = KeyboardState(superState = ModifierState.LATCHED)
        val meta = state.getMetaState()
        assertTrue((meta and KeyEvent.META_META_ON) != 0)
        assertTrue((meta and KeyEvent.META_META_LEFT_ON) != 0)
    }

    @Test
    fun testKeyboardStateMetaStateWithShift() {
        val state = KeyboardState(shiftState = ModifierState.LATCHED)
        val meta = state.getMetaState()
        assertTrue((meta and KeyEvent.META_SHIFT_ON) != 0)
        assertTrue((meta and KeyEvent.META_SHIFT_LEFT_ON) != 0)
    }
}
