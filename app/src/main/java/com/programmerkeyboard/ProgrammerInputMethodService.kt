package com.programmerkeyboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
    private var mainContainer: ViewGroup? = null
    private val keyboardState = KeyboardState()
    private data class AppProfile(
        var layoutTarget: String = "main",
        val rowVisibility: MutableMap<String, Boolean> = mutableMapOf()
    )

    private val appProfiles = mutableMapOf<String, AppProfile>()
    private var currentPackageName: String = "default"

    private var lastNonMetaLayout: String = "main"

    private val prefChangeListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "pref_custom_theme_json" ||
            key == "pref_theme_preset_idx" ||
            key == "pref_keyboard_height_percent" ||
            key == "pref_keyboard_aspect_ratio" ||
            key == "pref_form_factor" ||
            key == "pref_form_factor_mode" ||
            key == "pref_keyboard_layout_target" ||
            key?.startsWith("pref_row_vis_") == true) {
            reloadKeyboardLayoutAndTheme()
        }
    }

    private fun reloadKeyboardLayoutAndTheme() {
        if (::keyboardView.isInitialized) {
            val prefs = getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)

            val newHeightPercent = prefs.getInt("pref_keyboard_height_percent", 30).coerceIn(20, 35)
            keyboardView.heightPercentage = newHeightPercent

            val formFactorStr = prefs.getString("pref_form_factor", null)
                ?: prefs.getString("pref_form_factor_mode", "FULL_WIDTH_DOCKED")
                ?: "FULL_WIDTH_DOCKED"

            val targetFormFactor = when (formFactorStr) {
                "LEFT_DOCKED", "SIDE_DOCKED" -> com.programmerkeyboard.model.FormFactorMode.LEFT_DOCKED
                "RIGHT_DOCKED" -> com.programmerkeyboard.model.FormFactorMode.RIGHT_DOCKED
                "FLOATING" -> com.programmerkeyboard.model.FormFactorMode.FLOATING
                "SPLIT" -> com.programmerkeyboard.model.FormFactorMode.SPLIT
                else -> com.programmerkeyboard.model.FormFactorMode.FULL_WIDTH_DOCKED
            }
            keyboardState.formFactorMode = targetFormFactor

            val currentLayout = keyboardView.layoutDefinition
            if (currentLayout != null) {
                val updatedLayout = com.programmerkeyboard.engine.LayoutParser.applyThemeOverrides(this, currentLayout)
                keyboardView.layoutDefinition = updatedLayout
            }

            keyboardView.requestLayout()
            keyboardView.recalculateKeyBounds()
            keyboardView.invalidate()
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

        val rootLayout = android.widget.FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
        window.window?.let { win ->
            win.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            win.decorView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            win.findViewById<View>(android.R.id.content)?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            win.setDimAmount(0f)
            win.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
        mainContainer = rootLayout

        keyboardView = KeyboardView(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL or android.view.Gravity.BOTTOM
            }
            heightPercentage = heightPercent
            layoutDefinition = LayoutParser.loadLayoutFromAsset(this@ProgrammerInputMethodService, "mobile.json")
            keyboardState = this@ProgrammerInputMethodService.keyboardState
            onKeyActionListener = { action -> handleKeyAction(action) }
            onLayoutChangeListener = { targetLayout ->
                val currentId = keyboardView.layoutDefinition?.id?.removeSuffix(".json") ?: "main"
                if (!currentId.equals("meta", ignoreCase = true) && !currentId.startsWith("emoji_auto", ignoreCase = true)) {
                    lastNonMetaLayout = currentId
                }

                val isActualLayout = !targetLayout.equals("meta", ignoreCase = true) && !targetLayout.startsWith("emoji_auto", ignoreCase = true)

                if (isActualLayout) {
                    val profile = appProfiles.getOrPut(currentPackageName) { AppProfile() }
                    profile.layoutTarget = targetLayout
                    val prefs = getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString("pref_keyboard_layout_target", targetLayout)
                        .putString("pref_last_primary_layout_target", targetLayout)
                        .putString("pref_last_actual_layout", targetLayout)
                        .apply()
                }

                val layoutFile = if (targetLayout.endsWith(".json")) targetLayout else "$targetLayout.json"
                layoutDefinition = LayoutParser.loadLayoutFromAsset(this@ProgrammerInputMethodService, layoutFile, lastNonMetaLayout)
            }
            onRowToggleListener = { rowId, isVisible ->
                val profile = appProfiles.getOrPut(currentPackageName) { AppProfile() }
                val layoutId = keyboardView.layoutDefinition?.id ?: "main"
                profile.rowVisibility["$layoutId:$rowId"] = isVisible
                profile.rowVisibility[rowId] = isVisible
            }
            onScreenModeChangeListener = { mode ->
                // Mode change handler
            }
        }

        rootLayout.addView(keyboardView)
        return rootLayout
    }

    private fun isTerminalApp(editorInfo: android.view.inputmethod.EditorInfo?): Boolean {
        if (editorInfo == null) return false
        val pkg = editorInfo.packageName?.lowercase() ?: ""
        if (pkg.contains("termux") || pkg.contains("terminal") || pkg.contains("connectbot") || pkg.contains("juicessh")) {
            return true
        }
        val inputType = editorInfo.inputType
        if (inputType == android.text.InputType.TYPE_NULL) return true
        val flagNoEnter = editorInfo.imeOptions and android.view.inputmethod.EditorInfo.IME_FLAG_NO_ENTER_ACTION
        if (flagNoEnter != 0) return true
        return false
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)

        val prefs = getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        val heightPercent = prefs.getInt("pref_keyboard_height_percent", 30)
        val isShiftLock = prefs.getBoolean("pref_is_shift_lock", false)
        val formFactorStr = prefs.getString("pref_form_factor", "FULL_WIDTH_DOCKED") ?: "FULL_WIDTH_DOCKED"

        val pkgName = info?.packageName ?: "default"
        currentPackageName = pkgName

        val isNewProfile = !appProfiles.containsKey(pkgName)
        val profile = appProfiles.getOrPut(pkgName) { AppProfile() }

        val inputType = info?.inputType ?: 0
        val inputClass = inputType and android.text.InputType.TYPE_MASK_CLASS

        if (isNewProfile) {
            val lowerPkg = pkgName.lowercase()
            val isUriField = (inputType and android.text.InputType.TYPE_TEXT_VARIATION_URI) == android.text.InputType.TYPE_TEXT_VARIATION_URI

            if (lowerPkg.contains("termux") || lowerPkg.contains("terminal") || lowerPkg.contains("ide") || lowerPkg.contains("code")) {
                profile.layoutTarget = "main"
                profile.rowVisibility["main:1"] = true
                profile.rowVisibility["1"] = true
            } else if (lowerPkg.contains("chrome") || lowerPkg.contains("browser") || lowerPkg.contains("search") || isUriField) {
                profile.layoutTarget = "mobile"
                profile.rowVisibility["main:1"] = false
                profile.rowVisibility["1"] = false
            } else {
                profile.layoutTarget = "mobile"
            }
        }

        val lastPrimaryGlobal = prefs.getString("pref_last_primary_layout_target", "mobile") ?: "mobile"
        val appSavedTarget = profile.layoutTarget

        val activePrimaryTarget = if (!appSavedTarget.isNullOrEmpty() &&
            !appSavedTarget.equals("mobile_symbol", ignoreCase = true) &&
            !appSavedTarget.equals("mobile_number", ignoreCase = true) &&
            !appSavedTarget.equals("phone", ignoreCase = true) &&
            !appSavedTarget.equals("meta", ignoreCase = true)) {
            appSavedTarget
        } else {
            if (lastPrimaryGlobal.equals("mobile_symbol", ignoreCase = true) || lastPrimaryGlobal.equals("mobile_number", ignoreCase = true)) {
                "mobile"
            } else {
                lastPrimaryGlobal
            }
        }

        val targetLayout = when (inputClass) {
            android.text.InputType.TYPE_CLASS_PHONE -> "phone"
            android.text.InputType.TYPE_CLASS_NUMBER,
            android.text.InputType.TYPE_CLASS_DATETIME -> "mobile_number"
            else -> activePrimaryTarget
        }

        val layoutFile = if (targetLayout.endsWith(".json")) targetLayout else "$targetLayout.json"
        val customLayoutJson = prefs.getString("pref_custom_layout_json_$targetLayout", null)
            ?: if (targetLayout == "main") prefs.getString("pref_custom_layout_json", null) else null

        val rawLayout = if (!customLayoutJson.isNullOrEmpty()) {
            try { LayoutParser.parseJsonLayoutDescriptor(customLayoutJson) } catch (_: Exception) { LayoutParser.loadLayoutFromAsset(this, layoutFile) }
        } else {
            LayoutParser.loadLayoutFromAsset(this, layoutFile)
        }
        val layout = LayoutParser.applyThemeOverrides(this, rawLayout)

        keyboardView.layoutDefinition = layout
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
        val prefs = getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)

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
                    val isCtrl = keyboardState.isCtrlActive
                    val terminal = isTerminalApp(currentInputEditorInfo)

                    if (isCtrl && !terminal) {
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
                val currentId = keyboardView.layoutDefinition?.id?.removeSuffix(".json") ?: "main"
                if (!currentId.equals("meta", ignoreCase = true) && !currentId.startsWith("emoji_auto", ignoreCase = true)) {
                    lastNonMetaLayout = currentId
                }

                val savedLastActual = prefs.getString("pref_last_actual_layout", null)
                    ?: prefs.getString("pref_last_primary_layout_target", "main")
                    ?: "main"

                val target = if (action.target == "previous" || action.target == "back") {
                    if (lastNonMetaLayout.isNotBlank() && !lastNonMetaLayout.equals("meta", ignoreCase = true) && !lastNonMetaLayout.startsWith("emoji_auto", ignoreCase = true)) {
                        lastNonMetaLayout
                    } else {
                        savedLastActual
                    }
                } else {
                    action.target
                }

                if (target.equals("settings", ignoreCase = true)) {
                    val intent = Intent(this, com.programmerkeyboard.settings.SettingsActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                    return
                }

                if (!target.startsWith("emoji_auto", ignoreCase = true) && !target.equals("meta", ignoreCase = true)) {
                    lastNonMetaLayout = target
                    prefs.edit()
                        .putString("pref_last_actual_layout", target)
                        .putString("pref_last_primary_layout_target", target)
                        .apply()
                }

                val layoutFile = if (target.endsWith(".json")) target else "${target}.json"
                val customLayoutJson = prefs.getString("pref_custom_layout_json_$target", null)
                    ?: if (target == "main") prefs.getString("pref_custom_layout_json", null) else null
                val rawLayout = if (target == "meta") {
                    LayoutParser.createMetaLayout(this, lastNonMetaLayout.ifBlank { "main" })
                } else if (!customLayoutJson.isNullOrEmpty()) {
                    try { LayoutParser.parseJsonLayoutDescriptor(customLayoutJson) } catch (_: Exception) { LayoutParser.loadLayoutFromAsset(this, layoutFile, lastNonMetaLayout) }
                } else {
                    LayoutParser.loadLayoutFromAsset(this, layoutFile, lastNonMetaLayout)
                }
                keyboardView.layoutDefinition = LayoutParser.applyThemeOverrides(this, rawLayout)
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
                    "EMOJI_PICKER", "EMOJI", "EMOJI_KEYBOARD" -> {
                        val currentTarget = keyboardView.layoutDefinition?.id?.removeSuffix(".json") ?: "main"
                        if (!currentTarget.equals("meta", ignoreCase = true) && !currentTarget.startsWith("emoji_auto", ignoreCase = true)) {
                            lastNonMetaLayout = currentTarget
                            prefs.edit()
                                .putString("pref_last_actual_layout", currentTarget)
                                .putString("pref_last_primary_layout_target", currentTarget)
                                .apply()
                        }

                        val autoEmojiLayout = LayoutParser.loadLayoutFromAsset(this, "emoji_auto_0", lastNonMetaLayout)
                        keyboardView.layoutDefinition = LayoutParser.applyThemeOverrides(this, autoEmojiLayout)
                    }
                    "VOICE_INPUT", "VOICE_INPUT_ONESHOT", "VOICE_INPUT_TERMINAL" -> {
                        toggleOneShotVoiceRecognition()
                    }
                    "VOICE_INPUT_CONTINUOUS", "VOICE_INPUT_MOBILE" -> {
                        toggleContinuousVoiceRecognition()
                    }
                    "READ_TEXT", "TEXT_TO_SPEECH", "READ_SELECTION" -> {
                        speakSelectedOrCurrentText()
                    }
                }
            }
            else -> {}
        }
    }

    private var oneShotRecognizer: android.speech.SpeechRecognizer? = null

    private fun toggleOneShotVoiceRecognition() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            keyboardView.activeListeningStatus = "🎙 Requesting mic permission..."
            PermissionRequestActivity.onPermissionResultListener = { granted ->
                if (granted) {
                    toggleOneShotVoiceRecognition()
                } else {
                    keyboardView.activeListeningStatus = null
                    android.widget.Toast.makeText(this, "Microphone permission is required for speech-to-text!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            val intent = Intent(this, PermissionRequestActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            return
        }

        if (android.speech.SpeechRecognizer.isRecognitionAvailable(this)) {
            try {
                oneShotRecognizer?.destroy()
                keyboardView.activeListeningStatus = "🎙 Speak now..."

                oneShotRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(this).apply {
                    setRecognitionListener(object : android.speech.RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            keyboardView.activeListeningStatus = "🎙 Speak now..."
                        }

                        override fun onBeginningOfSpeech() {
                            keyboardView.activeListeningStatus = "🗣 Recording..."
                        }

                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            keyboardView.activeListeningStatus = "Processing speech..."
                        }

                        override fun onError(error: Int) {
                            if (error == android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                                PermissionRequestActivity.onPermissionResultListener = { granted ->
                                    if (granted) {
                                        toggleOneShotVoiceRecognition()
                                    }
                                }
                                val intent = Intent(this@ProgrammerInputMethodService, PermissionRequestActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                startActivity(intent)
                                keyboardView.activeListeningStatus = null
                                return
                            }
                            val errorMsg = when (error) {
                                android.speech.SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                                android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timed out"
                                android.speech.SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                                else -> "Speech error ($error)"
                            }
                            android.widget.Toast.makeText(this@ProgrammerInputMethodService, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
                            keyboardView.activeListeningStatus = null
                            oneShotRecognizer?.destroy()
                            oneShotRecognizer = null
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                val textWithSpace = matches[0] + " "
                                currentInputConnection?.commitText(textWithSpace, 1)
                            }
                            keyboardView.activeListeningStatus = null
                            oneShotRecognizer?.destroy()
                            oneShotRecognizer = null
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                keyboardView.activeListeningStatus = "🗣 ${matches[0]}"
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                oneShotRecognizer?.startListening(intent)
                return
            } catch (_: Exception) {
                keyboardView.activeListeningStatus = null
            }
        }

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

    private fun toggleContinuousVoiceRecognition() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        val token = window?.window?.attributes?.token

        var launchedVoiceIme = false
        if (imm != null && token != null) {
            val enabledImes = imm.enabledInputMethodList
            val googleVoiceIme = enabledImes.firstOrNull { imi ->
                val id = imi.id.lowercase()
                id.contains("googlequicksearchbox") || id.contains("voiceinputmethodservice") || id.contains("google.android.tts")
            }
            if (googleVoiceIme != null) {
                try {
                    imm.setInputMethod(token, googleVoiceIme.id)
                    launchedVoiceIme = true
                } catch (_: Exception) {}
            }
        }

        if (!launchedVoiceIme) {
            VoiceInputActivity.onSpeechResultListener = { text ->
                currentInputConnection?.commitText(text, 1)
            }
            VoiceInputContinuousActivity.onContinuousSpeechResultListener = { text ->
                currentInputConnection?.commitText(text, 1)
            }
            val intent = Intent(this, VoiceInputContinuousActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                startActivity(intent)
            } catch (_: Exception) {
                val fallbackIntent = Intent(this, VoiceInputActivity::class.java).apply {
                    putExtra("IS_CONTINUOUS", true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    startActivity(fallbackIntent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(this, "Google Voice Typing or Speech Recognizer is required", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() {
        val prefs = getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(prefChangeListener)
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
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

    private var textToSpeech: android.speech.tts.TextToSpeech? = null

    override fun onEvaluateFullscreenMode(): Boolean {
        return false
    }

    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        return true
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        val hasSelection = newSelStart != newSelEnd
        if (::keyboardView.isInitialized) {
            keyboardView.isTextSelected = hasSelection
        }
    }

    private fun speakSelectedOrCurrentText() {
        val inputConnection = currentInputConnection
        val selectedText = inputConnection?.getSelectedText(0)?.toString()
        val textToRead = if (!selectedText.isNullOrBlank()) {
            selectedText
        } else {
            inputConnection?.getTextBeforeCursor(200, 0)?.toString()
        }

        if (textToRead.isNullOrBlank()) {
            android.widget.Toast.makeText(this, "No text to read", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        if (textToSpeech == null) {
            textToSpeech = android.speech.tts.TextToSpeech(applicationContext) { status ->
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    textToSpeech?.language = java.util.Locale.getDefault()
                    performSpeak(textToRead)
                } else {
                    android.widget.Toast.makeText(this@ProgrammerInputMethodService, "Text-to-Speech engine failed to initialize", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            performSpeak(textToRead)
        }
    }

    private fun performSpeak(text: String) {
        if (textToSpeech?.isSpeaking == true) {
            textToSpeech?.stop()
            keyboardView.activeListeningStatus = null
            return
        }
        keyboardView.activeListeningStatus = "🔊 Reading text..."
        val params = android.os.Bundle()
        val utteranceId = "read_text_${System.currentTimeMillis()}"
        textToSpeech?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                keyboardView.activeListeningStatus = "🔊 Reading text..."
            }
            override fun onDone(utteranceId: String?) {
                keyboardView.activeListeningStatus = null
            }
            override fun onError(utteranceId: String?) {
                keyboardView.activeListeningStatus = null
            }
        })
        textToSpeech?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }
}
