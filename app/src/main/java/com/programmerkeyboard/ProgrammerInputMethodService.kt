package com.programmerkeyboard

import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.programmerkeyboard.engine.LayoutParser
import com.programmerkeyboard.model.KeyAction
import com.programmerkeyboard.model.KeyboardState
import com.programmerkeyboard.model.ModifierState
import com.programmerkeyboard.settings.SettingsActivity
import com.programmerkeyboard.view.KeyboardView
import com.programmerkeyboard.view.TrackpadView

/**
 * Main Android InputMethodService implementation for Programmer Keyboard.
 */
class ProgrammerInputMethodService : InputMethodService() {

    private lateinit var keyboardView: KeyboardView
    private var trackpadView: TrackpadView? = null
    private var mainContainer: LinearLayout? = null
    private val keyboardState = KeyboardState()
    private data class AppProfile(
        var layoutTarget: String = "mobile",
        val rowVisibility: MutableMap<String, Boolean> = mutableMapOf()
    )

    private val appProfiles = mutableMapOf<String, AppProfile>()
    private var currentPackageName: String = "default"

    private var lastNonMetaLayout: String = "mobile"

    private val prefChangeListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "pref_custom_theme_json" || key == "pref_theme_preset_idx" || key == "pref_keyboard_height_percent") {
            reloadKeyboardLayoutAndTheme()
        }
    }

    private fun reloadKeyboardLayoutAndTheme() {
        if (::keyboardView.isInitialized) {
            val currentLayout = keyboardView.layoutDefinition
            if (currentLayout != null) {
                val updatedLayout = com.programmerkeyboard.engine.LayoutParser.applyThemeOverrides(this, currentLayout)
                keyboardView.layoutDefinition = updatedLayout
                keyboardView.recalculateKeyBounds()
                keyboardView.invalidate()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(prefChangeListener)
    }

    override fun onCreateInputView(): View {
        val prefs = getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        val heightPercent = prefs.getInt("pref_keyboard_height_percent", 30)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
        window.window?.setBackgroundDrawableResource(android.R.color.transparent)
        mainContainer = rootLayout

        keyboardView = KeyboardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
            )
            heightPercentage = heightPercent
            layoutDefinition = LayoutParser.loadLayoutFromAsset(this@ProgrammerInputMethodService, "mobile.json")
            keyboardState = this@ProgrammerInputMethodService.keyboardState
            onKeyActionListener = { action -> handleKeyAction(action) }
            onLayoutChangeListener = { targetLayout ->
                val currentId = keyboardView.layoutDefinition?.id?.removeSuffix(".json") ?: "main"
                if (!currentId.equals("meta", ignoreCase = true)) {
                    lastNonMetaLayout = currentId
                }
                val profile = appProfiles.getOrPut(currentPackageName) { AppProfile() }
                profile.layoutTarget = targetLayout
                val layoutFile = if (targetLayout.endsWith(".json")) targetLayout else "$targetLayout.json"
                layoutDefinition = LayoutParser.loadLayoutFromAsset(this@ProgrammerInputMethodService, layoutFile, lastNonMetaLayout)
            }
            onRowToggleListener = { rowId, isVisible ->
                val profile = appProfiles.getOrPut(currentPackageName) { AppProfile() }
                profile.rowVisibility[rowId] = isVisible
            }
            onScreenModeChangeListener = { mode ->
                // Mode change handler (FULL_WIDTH_DOCKED, SPLIT, DOCK_LEFT, DOCK_RIGHT, FLOAT)
            }
        }

        rootLayout.addView(keyboardView)
        return rootLayout
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val prefs = getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        val isShiftLock = prefs.getBoolean("pref_is_shift_lock", false)
        val heightPercent = prefs.getInt("pref_keyboard_height_percent", 30)
        val formFactorStr = prefs.getString("pref_form_factor", "FULL_WIDTH_DOCKED") ?: "FULL_WIDTH_DOCKED"

        val pkgName = info?.packageName ?: "default"
        currentPackageName = pkgName

        val isNewProfile = !appProfiles.containsKey(pkgName)
        val profile = appProfiles.getOrPut(pkgName) { AppProfile() }

        if (isNewProfile) {
            val lowerPkg = pkgName.lowercase()
            val inputType = info?.inputType ?: 0
            val isUriField = (inputType and android.text.InputType.TYPE_TEXT_VARIATION_URI) == android.text.InputType.TYPE_TEXT_VARIATION_URI

            if (lowerPkg.contains("termux") || lowerPkg.contains("terminal") || lowerPkg.contains("ide") || lowerPkg.contains("code")) {
                profile.layoutTarget = "main"
                profile.rowVisibility["1"] = true
            } else if (lowerPkg.contains("chrome") || lowerPkg.contains("browser") || lowerPkg.contains("search") || isUriField) {
                profile.layoutTarget = "mobile"
                profile.rowVisibility["1"] = false
            }
        }

        val inputType = info?.inputType ?: 0
        val inputClass = inputType and android.text.InputType.TYPE_MASK_CLASS

        val targetLayout = when (inputClass) {
            android.text.InputType.TYPE_CLASS_PHONE -> "phone"
            android.text.InputType.TYPE_CLASS_NUMBER,
            android.text.InputType.TYPE_CLASS_DATETIME -> "mobile_number"
            else -> profile.layoutTarget
        }

        val layoutFile = if (targetLayout.endsWith(".json")) targetLayout else "$targetLayout.json"
        keyboardView.layoutDefinition = LayoutParser.loadLayoutFromAsset(this, layoutFile, lastNonMetaLayout)
        keyboardView.setRowVisibilityMap(profile.rowVisibility)

        keyboardState.formFactorMode = when (formFactorStr) {
            "SPLIT" -> com.programmerkeyboard.model.FormFactorMode.SPLIT
            "LEFT_DOCKED", "SIDE_DOCKED" -> com.programmerkeyboard.model.FormFactorMode.LEFT_DOCKED
            "RIGHT_DOCKED" -> com.programmerkeyboard.model.FormFactorMode.RIGHT_DOCKED
            "FLOATING" -> com.programmerkeyboard.model.FormFactorMode.FLOATING
            else -> com.programmerkeyboard.model.FormFactorMode.FULL_WIDTH_DOCKED
        }

        keyboardState.shiftLockMode = if (isShiftLock) {
            com.programmerkeyboard.model.ShiftLockMode.SHIFT_LOCK
        } else {
            com.programmerkeyboard.model.ShiftLockMode.CAPS_LOCK
        }
        keyboardView.heightPercentage = heightPercent
        keyboardView.recalculateKeyBounds()
        keyboardView.requestLayout()
        keyboardView.invalidate()
    }

    private fun handleKeyAction(action: KeyAction) {
        val inputConnection = currentInputConnection ?: return

        when (action) {
            is KeyAction.SendText -> {
                val metaState = keyboardState.getMetaState()
                val isCtrlAltSuperActive = keyboardState.isCtrlActive || keyboardState.isAltActive || keyboardState.isSuperActive
                val text = action.text

                if (isCtrlAltSuperActive && text.length == 1) {
                    val char = text[0]
                    val keyCode = getKeyCodeForChar(char)
                    if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                        val eventTime = System.currentTimeMillis()
                        inputConnection.sendKeyEvent(
                            KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0, metaState)
                        )
                        inputConnection.sendKeyEvent(
                            KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0, metaState)
                        )
                    } else {
                        inputConnection.commitText(text, 1)
                    }
                } else {
                    val textToCommit = if (keyboardState.isShiftActive && text.length == 1 && text[0].isLowerCase()) {
                        text.uppercase()
                    } else {
                        text
                    }
                    inputConnection.commitText(textToCommit, 1)
                }

                if (keyboardState.consumeOneShotModifiers()) {
                    keyboardView.invalidate()
                }
            }
            is KeyAction.SendCode -> {
                if (action.code == KeyEvent.KEYCODE_ENTER) {
                    val imeOptions = currentInputEditorInfo?.imeOptions ?: 0
                    val actionId = imeOptions and android.view.inputmethod.EditorInfo.IME_MASK_ACTION
                    if (actionId != android.view.inputmethod.EditorInfo.IME_ACTION_NONE && actionId != android.view.inputmethod.EditorInfo.IME_ACTION_UNSPECIFIED) {
                        inputConnection.performEditorAction(actionId)
                        if (keyboardState.consumeOneShotModifiers()) {
                            keyboardView.invalidate()
                        }
                        return
                    }
                }
                val metaState = keyboardState.getMetaState()
                val eventTime = System.currentTimeMillis()
                inputConnection.sendKeyEvent(
                    KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, action.code, 0, metaState)
                )
                inputConnection.sendKeyEvent(
                    KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, action.code, 0, metaState)
                )

                if (keyboardState.consumeOneShotModifiers()) {
                    keyboardView.invalidate()
                }
            }
            is KeyAction.SwitchLayout -> {
                val layoutFile = if (action.target.endsWith(".json")) action.target else "${action.target}.json"
                keyboardView.layoutDefinition = LayoutParser.loadLayoutFromAsset(this, layoutFile)
            }
            is KeyAction.ToggleModifier -> {
                toggleModifierState(action.modifier)
            }
            is KeyAction.SelectAll -> {
                if (!inputConnection.performContextMenuAction(android.R.id.selectAll)) {
                    sendShortcutKey(inputConnection, KeyEvent.KEYCODE_A)
                }
            }
            is KeyAction.Copy -> {
                if (!inputConnection.performContextMenuAction(android.R.id.copy)) {
                    sendShortcutKey(inputConnection, KeyEvent.KEYCODE_C)
                }
            }
            is KeyAction.Cut -> {
                if (!inputConnection.performContextMenuAction(android.R.id.cut)) {
                    sendShortcutKey(inputConnection, KeyEvent.KEYCODE_X)
                }
            }
            is KeyAction.Paste -> {
                if (!inputConnection.performContextMenuAction(android.R.id.paste)) {
                    sendShortcutKey(inputConnection, KeyEvent.KEYCODE_V)
                }
            }
            is KeyAction.SwitchIme -> {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.showInputMethodPicker()
            }
            is KeyAction.ShowWidget -> {
                when (action.widget) {
                    "SETTINGS" -> {
                        val intent = Intent(this, SettingsActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                    }
                    "EMOJI_PICKER" -> {
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                        imm?.showInputMethodPicker()
                    }
                    "VOICE_INPUT" -> {
                        toggleVoiceRecognition()
                    }
                }
            }
            else -> {}
        }
    }

    private var speechRecognizer: android.speech.SpeechRecognizer? = null
    private fun toggleVoiceRecognition() {
        VoiceInputActivity.onSpeechResultListener = { text ->
            currentInputConnection?.commitText(text, 1)
        }
        val intent = Intent(this, VoiceInputActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Voice input activity not available", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        val prefs = getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(prefChangeListener)
        speechRecognizer?.destroy()
        speechRecognizer = null
        super.onDestroy()
    }

    private fun toggleModifierState(modifierName: String) {
        val key = modifierName.uppercase()
        when (key) {
            "SHIFT" -> {
                keyboardState.shiftState = when (keyboardState.shiftState) {
                    ModifierState.OFF -> ModifierState.LATCHED
                    ModifierState.LATCHED -> ModifierState.LOCKED
                    ModifierState.LOCKED -> ModifierState.OFF
                }
            }
            "CTRL" -> {
                keyboardState.ctrlState = when (keyboardState.ctrlState) {
                    ModifierState.OFF -> ModifierState.LATCHED
                    ModifierState.LATCHED -> ModifierState.LOCKED
                    ModifierState.LOCKED -> ModifierState.OFF
                }
            }
            "ALT" -> {
                keyboardState.altState = when (keyboardState.altState) {
                    ModifierState.OFF -> ModifierState.LATCHED
                    ModifierState.LATCHED -> ModifierState.LOCKED
                    ModifierState.LOCKED -> ModifierState.OFF
                }
            }
            "SUPER" -> {
                keyboardState.superState = when (keyboardState.superState) {
                    ModifierState.OFF -> ModifierState.LATCHED
                    ModifierState.LATCHED -> ModifierState.LOCKED
                    ModifierState.LOCKED -> ModifierState.OFF
                }
            }
        }
        keyboardView.invalidate()
    }

    private fun getKeyCodeForChar(c: Char): Int {
        val lower = c.lowercaseChar()
        if (lower in 'a'..'z') {
            return KeyEvent.KEYCODE_A + (lower - 'a')
        }
        if (c in '0'..'9') {
            return KeyEvent.KEYCODE_0 + (c - '0')
        }
        return when (c) {
            '`', '~' -> KeyEvent.KEYCODE_GRAVE
            '-', '_' -> KeyEvent.KEYCODE_MINUS
            '=', '+' -> KeyEvent.KEYCODE_EQUALS
            '[', '{' -> KeyEvent.KEYCODE_LEFT_BRACKET
            ']', '}' -> KeyEvent.KEYCODE_RIGHT_BRACKET
            '\\', '|' -> KeyEvent.KEYCODE_BACKSLASH
            ';', ':' -> KeyEvent.KEYCODE_SEMICOLON
            '\'', '"' -> KeyEvent.KEYCODE_APOSTROPHE
            ',', '<' -> KeyEvent.KEYCODE_COMMA
            '.', '>' -> KeyEvent.KEYCODE_PERIOD
            '/', '?' -> KeyEvent.KEYCODE_SLASH
            ' ' -> KeyEvent.KEYCODE_SPACE
            '\n' -> KeyEvent.KEYCODE_ENTER
            else -> KeyEvent.KEYCODE_UNKNOWN
        }
    }

    private fun sendShortcutKey(inputConnection: android.view.inputmethod.InputConnection, keyCode: Int) {
        val eventTime = System.currentTimeMillis()
        val metaState = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        inputConnection.sendKeyEvent(
            KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0, metaState)
        )
        inputConnection.sendKeyEvent(
            KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0, metaState)
        )
    }
}
