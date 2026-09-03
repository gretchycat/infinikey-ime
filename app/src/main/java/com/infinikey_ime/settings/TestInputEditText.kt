package com.infinikey_ime.settings

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.appcompat.widget.AppCompatEditText

/**
 * Custom EditText for keyboard testing that allows intercepting and suppressing
 * text commits and key events when Listening mode is active.
 */
class TestInputEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    var isListening: Boolean = false
    var onInterceptTextCommit: ((String) -> Unit)? = null
    var onInterceptKeyEvent: ((KeyEvent) -> Unit)? = null

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val target = super.onCreateInputConnection(outAttrs) ?: return null
        return object : InputConnectionWrapper(target, true) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                if (isListening) {
                    if (!text.isNullOrEmpty()) {
                        onInterceptTextCommit?.invoke(text.toString())
                    }
                    return true // Disconnect from text box while listening
                }
                return super.commitText(text, newCursorPosition)
            }

            override fun sendKeyEvent(event: KeyEvent?): Boolean {
                if (isListening) {
                    if (event != null && event.action == KeyEvent.ACTION_DOWN) {
                        onInterceptKeyEvent?.invoke(event)
                    }
                    return true // Disconnect from text box and UI while listening
                }
                return super.sendKeyEvent(event)
            }

            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                if (isListening) {
                    onInterceptKeyEvent?.invoke(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                    return true
                }
                return super.deleteSurroundingText(beforeLength, afterLength)
            }
        }
    }
}
