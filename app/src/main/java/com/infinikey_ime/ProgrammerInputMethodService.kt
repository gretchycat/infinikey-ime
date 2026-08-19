package com.infinikey_ime

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.infinikey_ime.engine.LayoutParser
import com.infinikey_ime.model.KeyAction
import com.infinikey_ime.model.KeyboardState
import com.infinikey_ime.model.ModifierState
import com.infinikey_ime.settings.SettingsActivity
import com.infinikey_ime.view.KeyboardView
import com.infinikey_ime.view.TrackpadView

/**
 * Main Android InputMethodService implementation for Infinikey IME.
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
    private var currentEditorInfo: android.view.inputmethod.EditorInfo? = null

    fun isTerminalTarget(): Boolean {
        val info = currentEditorInfo ?: return false
        val pkg = (info.packageName ?: "").lowercase()
        val inputType = info.inputType
        return pkg.contains("termux") || pkg.contains("terminal") || pkg.contains("connectbot") ||
               pkg.contains("juicessh") || pkg.contains("vnc") || pkg.contains("ssh") ||
               inputType == android.text.InputType.TYPE_NULL
    }

    private var lastNonMetaLayout: String = "main"
    private val layoutStack = java.util.ArrayDeque<String>()

    private fun isGeneratedLayoutId(layoutId: String): Boolean {
        val clean = layoutId.removePrefix("layouts/").removeSuffix(".json")
        return clean.equals("meta", ignoreCase = true) || clean.startsWith("emoji_auto", ignoreCase = true) || clean.startsWith("emoji", ignoreCase = true)
    }

    private fun pushCurrentLayoutToStack() {
        val currentId = keyboardView.layoutDefinition?.id?.removeSuffix(".json") ?: "main"
        if (!isGeneratedLayoutId(currentId)) {
            layoutStack.push(currentId)
        }
    }

    private val prefChangeListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "pref_custom_theme_json" ||
            key == "pref_theme_preset_idx" ||
            key == "pref_active_theme_key" ||
            key == "pref_theme_last_updated_time" ||
            key == "pref_keyboard_height_percent" ||
            key == "pref_keyboard_height_percent_portrait" ||
            key == "pref_keyboard_height_percent_landscape" ||
            key == "pref_keyboard_aspect_ratio" ||
            key == "pref_keyboard_aspect_ratio_portrait" ||
            key == "pref_keyboard_aspect_ratio_landscape" ||
            key == "pref_form_factor" ||
            key == "pref_form_factor_mode" ||
            key == "pref_keyboard_layout_target" ||
            key?.startsWith("pref_row_vis_") == true) {
            reloadKeyboardLayoutAndTheme()
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        reloadKeyboardLayoutAndTheme()
        try {
            showWindow(true)
        } catch (_: Exception) {}
    }

    override fun onInitializeInterface() {
        super.onInitializeInterface()
        reloadKeyboardLayoutAndTheme()
    }

    private fun reloadKeyboardLayoutAndTheme() {
        com.infinikey_ime.util.ThemeManager.ensureDefaultThemesCopied(this)
        if (::keyboardView.isInitialized) {
            val prefs = getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
            val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

            val defaultHeight = if (isLandscape) 45 else 30
            val heightKey = if (isLandscape) "pref_keyboard_height_percent_landscape" else "pref_keyboard_height_percent_portrait"
            val fallbackHeight = prefs.getInt("pref_keyboard_height_percent", defaultHeight)
            val newHeightPercent = prefs.getInt(heightKey, fallbackHeight).coerceIn(15, 65)

            keyboardView.heightPercentage = newHeightPercent

            val formFactorStr = prefs.getString("pref_form_factor", null)
                ?: prefs.getString("pref_form_factor_mode", "FULL_WIDTH_DOCKED")
                ?: "FULL_WIDTH_DOCKED"

            val targetFormFactor = when (formFactorStr) {
                "LEFT_DOCKED", "SIDE_DOCKED" -> com.infinikey_ime.model.FormFactorMode.LEFT_DOCKED
                "RIGHT_DOCKED" -> com.infinikey_ime.model.FormFactorMode.RIGHT_DOCKED
                "FLOATING" -> com.infinikey_ime.model.FormFactorMode.FLOATING
                "SPLIT" -> com.infinikey_ime.model.FormFactorMode.SPLIT
                else -> com.infinikey_ime.model.FormFactorMode.FULL_WIDTH_DOCKED
            }
            keyboardState.formFactorMode = targetFormFactor

            val currentLayoutId = keyboardView.layoutDefinition?.id?.removeSuffix(".json") ?: prefs.getString("pref_keyboard_layout_target", "main") ?: "main"
            val targetFile = if (currentLayoutId.endsWith(".json")) currentLayoutId else "$currentLayoutId.json"
            val freshLayout = com.infinikey_ime.engine.LayoutParser.loadLayoutFromAsset(this, targetFile)
            keyboardView.setLayout(freshLayout)
        }
    }

    private val clipboardHistoryList = mutableListOf<String>()

    private var lastSelfSetClipText: String? = null
    private var lastSelfSetClipTimeMs: Long = 0L

    private fun initClipboardHistoryListener() {
        try {
            synchronized(clipboardHistoryList) {
                loadClipboardHistoryFromPrefsLocked()
            }
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            clipboardManager?.addPrimaryClipChangedListener {
                try {
                    val clip = clipboardManager.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        val text = clip.getItemAt(0).text?.toString() ?: clip.getItemAt(0).coerceToText(this)?.toString()
                        if (!text.isNullOrEmpty()) {
                            val now = System.currentTimeMillis()
                            val cleanText = text.trim()
                            val cappedText = if (cleanText.length > 10000) cleanText.substring(0, 10000) else cleanText
                            if (cappedText == lastSelfSetClipText && (now - lastSelfSetClipTimeMs) < 3000L) {
                                return@addPrimaryClipChangedListener
                            }
                            addClipboardHistoryItem(text)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var lastClipChangeTimeMs: Long = 0L
    private var lastClipText: String = ""

    private fun addClipboardHistoryItem(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val cappedText = if (clean.length > 10000) clean.substring(0, 10000) else clean

        val now = System.currentTimeMillis()
        if (now - lastClipChangeTimeMs < 300L && cappedText == lastClipText) {
            return
        }
        lastClipChangeTimeMs = now
        lastClipText = cappedText

        synchronized(clipboardHistoryList) {
            loadClipboardHistoryFromPrefsLocked()

            clipboardHistoryList.remove(cappedText)
            clipboardHistoryList.add(0, cappedText)
            while (clipboardHistoryList.size > 30) {
                clipboardHistoryList.removeAt(clipboardHistoryList.size - 1)
            }
            saveClipboardHistoryToPrefsLocked()
        }
    }

    private fun saveClipboardHistoryToPrefsLocked() {
        try {
            val prefs = getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
            val jsonArray = org.json.JSONArray()
            clipboardHistoryList.forEach { jsonArray.put(it) }
            prefs.edit().putString("pref_clipboard_history_json", jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveClipboardHistoryToPrefs() {
        synchronized(clipboardHistoryList) {
            saveClipboardHistoryToPrefsLocked()
        }
    }

    private fun loadClipboardHistoryFromPrefsLocked() {
        clipboardHistoryList.clear()
        val prefs = getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("pref_clipboard_history_json", null) ?: return
        try {
            val jsonArray = org.json.JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.optString(i, null)
                if (!item.isNullOrEmpty()) {
                    clipboardHistoryList.add(item)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadClipboardHistoryFromPrefs() {
        synchronized(clipboardHistoryList) {
            loadClipboardHistoryFromPrefsLocked()
        }
    }

    private var activeClipboardOverlay: com.infinikey_ime.view.ClipboardHistoryOverlay? = null

    private fun dismissClipboardHistoryOverlay() {
        try {
            activeClipboardOverlay?.dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        activeClipboardOverlay = null
    }

    private fun showClipboardHistoryOverlay() {
        if (!::keyboardView.isInitialized || !keyboardView.isAttachedToWindow) {
            return
        }
        if (activeClipboardOverlay?.isShowing() == true) {
            dismissClipboardHistoryOverlay()
            return
        }
        dismissClipboardHistoryOverlay()

        val itemsCopy = synchronized(clipboardHistoryList) {
            loadClipboardHistoryFromPrefsLocked()
            clipboardHistoryList.toMutableList()
        }
        val prefs = getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        val initialEchoMode = prefs.getString("pref_clipboard_paste_method", "ECHO") == "ECHO"

        val overlay = com.infinikey_ime.view.ClipboardHistoryOverlay(
            context = this,
            historyItems = itemsCopy,
            initialEchoMode = initialEchoMode,
            onItemPicked = { selectedText, _ ->
                try {
                    val inputConnection = currentInputConnection
                    if (inputConnection == null || !inputConnection.commitText(selectedText, 1)) {
                        if (inputConnection?.performContextMenuAction(android.R.id.paste) != true) {
                            sendShortcutKey(inputConnection, android.view.KeyEvent.KEYCODE_V)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            onPasteMethodChanged = { isEchoMode ->
                val methodStr = if (isEchoMode) "ECHO" else "DIRECT"
                prefs.edit().putString("pref_clipboard_paste_method", methodStr).apply()
            },
            onDeleteItem = { _, deletedText ->
                synchronized(clipboardHistoryList) {
                    clipboardHistoryList.remove(deletedText)
                    saveClipboardHistoryToPrefsLocked()
                }
            },
            onClearHistory = {
                synchronized(clipboardHistoryList) {
                    clipboardHistoryList.clear()
                    lastClipText = ""
                    lastClipChangeTimeMs = 0L
                    saveClipboardHistoryToPrefsLocked()
                }
            },
            onDismissListener = {
                activeClipboardOverlay = null
            }
        )
        activeClipboardOverlay = overlay
        overlay.show(keyboardView)
    }

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(prefChangeListener)
        initClipboardHistoryListener()
        LayoutParser.syncAndUpgradeDefaultLayouts(this)
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
                handleKeyAction(KeyAction.SwitchLayout(targetLayout))
            }
            onRowToggleListener = { rowId, isVisible ->
                val profile = appProfiles.getOrPut(currentPackageName) { AppProfile() }
                val layoutId = keyboardView.layoutDefinition?.id ?: "main"
                profile.rowVisibility["$layoutId:$rowId"] = isVisible
                profile.rowVisibility[rowId] = isVisible
            }
            onScreenModeChangeListener = { mode ->
                if (mode == "FLOATING") {
                    if (!com.infinikey_ime.util.OverlayPermissionUtil.hasOverlayPermission(this@ProgrammerInputMethodService)) {
                        com.infinikey_ime.util.OverlayPermissionUtil.requestOverlayPermission(this@ProgrammerInputMethodService)
                    }
                }
                prefs.edit().putString("pref_form_factor", mode).apply()
                val newMode = when (mode) {
                    "SPLIT" -> com.infinikey_ime.model.FormFactorMode.SPLIT
                    "LEFT_DOCKED", "SIDE_DOCKED" -> com.infinikey_ime.model.FormFactorMode.LEFT_DOCKED
                    "RIGHT_DOCKED" -> com.infinikey_ime.model.FormFactorMode.RIGHT_DOCKED
                    "FLOATING" -> com.infinikey_ime.model.FormFactorMode.FLOATING
                    else -> com.infinikey_ime.model.FormFactorMode.FULL_WIDTH_DOCKED
                }
                keyboardView.keyboardState.formFactorMode = newMode
                keyboardView.recalculateKeyBounds()
                keyboardView.requestLayout()
                keyboardView.invalidate()
            }
            onFloatingBoundsChangedListener = {
                window?.window?.decorView?.post {
                    requestApplyInsets()
                }
            }
            preloadAudio()
        }

        rootLayout.addView(keyboardView)
        return rootLayout
    }

    override fun onComputeInsets(outInsets: android.inputmethodservice.InputMethodService.Insets) {
        super.onComputeInsets(outInsets)
        if (::keyboardView.isInitialized) {
            val formFactor = keyboardView.keyboardState.formFactorMode
            if (formFactor == com.infinikey_ime.model.FormFactorMode.FLOATING) {
                val decorView = window?.window?.decorView ?: return
                val h = decorView.height
                if (h > 0) {
                    outInsets.contentTopInsets = h
                    outInsets.visibleTopInsets = h

                    val floatCard = keyboardView.getFloatingCardBounds()
                    if (floatCard != null && !floatCard.isEmpty) {
                        outInsets.touchableInsets = android.inputmethodservice.InputMethodService.Insets.TOUCHABLE_INSETS_REGION
                        val rect = android.graphics.Rect(
                            floatCard.left.toInt().coerceAtLeast(0),
                            floatCard.top.toInt().coerceAtLeast(0),
                            floatCard.right.toInt().coerceAtMost(decorView.width),
                            floatCard.bottom.toInt().coerceAtMost(h)
                        )
                        outInsets.touchableRegion.set(rect)
                    }
                }
            }
        }
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

    private fun isMultiLineTextField(): Boolean {
        val info = currentInputEditorInfo ?: return false
        val inputType = info.inputType
        val mask = android.text.InputType.TYPE_MASK_CLASS
        val textClass = android.text.InputType.TYPE_CLASS_TEXT
        val multiLineFlag = android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        return (inputType and mask) == textClass && (inputType and multiLineFlag) != 0
    }

    override fun onStartInput(attribute: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        if (restarting) {
            try {
                showWindow(true)
            } catch (_: Exception) {}
        }
        // WORKAROUND [ANDROID-BUG-SELECTION]: Restore cursor position if keyboard service restarted after crash while modifying active selection. Remove when upstream Android framework selection bug is resolved.
        restoreCursorAfterCrash()
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        try {
            showWindow(true)
        } catch (_: Exception) {}
        // WORKAROUND [ANDROID-BUG-SELECTION]: Restore cursor position if keyboard service restarted after crash while modifying active selection. Remove when upstream Android framework selection bug is resolved.
        restoreCursorAfterCrash()
        currentEditorInfo = info
        if (::keyboardView.isInitialized) {
            keyboardView.preloadAudio()
        }

        val prefs = getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val defaultHeight = if (isLandscape) 45 else 30
        val heightKey = if (isLandscape) "pref_keyboard_height_percent_landscape" else "pref_keyboard_height_percent_portrait"
        val fallbackHeight = prefs.getInt("pref_keyboard_height_percent", defaultHeight)
        val heightPercent = prefs.getInt(heightKey, fallbackHeight).coerceIn(15, 65)
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

            if (lowerPkg.contains("termux") || lowerPkg.contains("terminal") || lowerPkg.contains("ide") || lowerPkg.contains("code")) {
                profile.layoutTarget = "main"
                profile.rowVisibility["main:1"] = true
                profile.rowVisibility["1"] = true
            } else {
                profile.layoutTarget = prefs.getString("pref_default_unseen_layout", "mobile") ?: "mobile"
            }
        }

        val lastPrimaryGlobal = prefs.getString("pref_last_actual_layout", "main") ?: "main"
        val appSavedTarget = profile.layoutTarget

        val activePrimaryTarget = if (!appSavedTarget.isNullOrEmpty() &&
            !appSavedTarget.equals("mobile_symbol", ignoreCase = true) &&
            !appSavedTarget.equals("mobile_number", ignoreCase = true) &&
            !appSavedTarget.equals("phone", ignoreCase = true) &&
            !appSavedTarget.equals("meta", ignoreCase = true) &&
            !appSavedTarget.startsWith("emoji_auto", ignoreCase = true)) {
            appSavedTarget
        } else {
            lastPrimaryGlobal
        }

        val targetLayout = when (inputClass) {
            android.text.InputType.TYPE_CLASS_PHONE -> "phone"
            android.text.InputType.TYPE_CLASS_NUMBER,
            android.text.InputType.TYPE_CLASS_DATETIME -> "mobile_number"
            else -> activePrimaryTarget
        }

        if (!isGeneratedLayoutId(targetLayout)) {
            layoutStack.clear()
            prefs.edit().putString("pref_last_actual_layout", targetLayout).apply()
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
            "SPLIT" -> com.infinikey_ime.model.FormFactorMode.SPLIT
            "LEFT_DOCKED", "SIDE_DOCKED" -> com.infinikey_ime.model.FormFactorMode.LEFT_DOCKED
            "RIGHT_DOCKED" -> com.infinikey_ime.model.FormFactorMode.RIGHT_DOCKED
            "FLOATING" -> com.infinikey_ime.model.FormFactorMode.FLOATING
            else -> com.infinikey_ime.model.FormFactorMode.FULL_WIDTH_DOCKED
        }

        keyboardState.shiftLockMode = if (isShiftLock) {
            com.infinikey_ime.model.ShiftLockMode.SHIFT_LOCK
        } else {
            com.infinikey_ime.model.ShiftLockMode.CAPS_LOCK
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

                    val currentLayoutId = keyboardView.layoutDefinition?.id ?: ""
                    if (currentLayoutId.startsWith("emoji")) {
                        val currentRecentStr = prefs.getString("pref_recent_emojis", "") ?: ""
                        val recentList = if (currentRecentStr.isEmpty()) mutableListOf() else currentRecentStr.split(",").toMutableList()
                        recentList.remove(text)
                        recentList.add(0, text)
                        val trimmedList = recentList.take(24)
                        prefs.edit().putString("pref_recent_emojis", trimmedList.joinToString(",")).apply()
                    }
                }

                if (keyboardState.consumeOneShotModifiers()) {
                    keyboardView.invalidate()
                }
            }
            is KeyAction.SendCode -> {
                val code = action.code
                val terminal = isTerminalApp(currentInputEditorInfo)

                if (!terminal) {
                    if (code == KeyEvent.KEYCODE_DPAD_LEFT) {
                        val textBefore = inputConnection.getTextBeforeCursor(1, 0)
                        if (textBefore.isNullOrEmpty()) {
                            return
                        }
                    } else if (code == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        val textAfter = inputConnection.getTextAfterCursor(1, 0)
                        if (textAfter.isNullOrEmpty()) {
                            return
                        }
                    } else if (code == KeyEvent.KEYCODE_DPAD_UP) {
                        if (!isMultiLineTextField()) {
                            return
                        }
                        val textBefore = inputConnection.getTextBeforeCursor(1000, 0)?.toString() ?: ""
                        if (!textBefore.contains("\n")) {
                            return
                        }
                    } else if (code == KeyEvent.KEYCODE_DPAD_DOWN) {
                        if (!isMultiLineTextField()) {
                            return
                        }
                        val textAfter = inputConnection.getTextAfterCursor(1000, 0)?.toString() ?: ""
                        if (!textAfter.contains("\n")) {
                            return
                        }
                    }
                }

                if (code == KeyEvent.KEYCODE_ENTER) {
                    val isCtrl = keyboardState.isCtrlActive
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
                    KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, code, 0, metaState)
                )
                inputConnection.sendKeyEvent(
                    KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, code, 0, metaState)
                )

                if (keyboardState.consumeOneShotModifiers()) {
                    keyboardView.invalidate()
                }
            }
            is KeyAction.SwitchLayout -> {
                val actionTarget = action.target
                val isPopAction = actionTarget == "[last]" || actionTarget == "last" || actionTarget == "previous" || actionTarget == "back"

                val target = if (isPopAction) {
                    if (layoutStack.isNotEmpty()) {
                        layoutStack.pop()
                    } else {
                        prefs.getString("pref_last_actual_layout", "main") ?: "main"
                    }
                } else {
                    actionTarget
                }

                if (target.equals("settings", ignoreCase = true)) {
                    val intent = Intent(this, com.infinikey_ime.settings.SettingsActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                    return
                }

                val isNextGenerated = isGeneratedLayoutId(target)

                if (isNextGenerated) {
                    pushCurrentLayoutToStack()
                } else {
                    layoutStack.clear()
                    prefs.edit()
                        .putString("pref_last_actual_layout", target)
                        .putString("pref_keyboard_layout_target", target)
                        .apply()
                    val profile = appProfiles.getOrPut(currentPackageName) { AppProfile() }
                    profile.layoutTarget = target
                }

                val layoutFile = if (target.endsWith(".json")) target else "${target}.json"
                val customLayoutJson = prefs.getString("pref_custom_layout_json_$target", null)
                    ?: if (target == "main") prefs.getString("pref_custom_layout_json", null) else null

                val lastLayoutForHeader = if (layoutStack.isNotEmpty()) layoutStack.peek() else (prefs.getString("pref_last_actual_layout", "main") ?: "main")

                val rawLayout = if (target == "meta") {
                    LayoutParser.createMetaLayout(this, lastLayoutForHeader)
                } else if (!customLayoutJson.isNullOrEmpty()) {
                    try { LayoutParser.parseJsonLayoutDescriptor(customLayoutJson) } catch (_: Exception) { LayoutParser.loadLayoutFromAsset(this, layoutFile, lastLayoutForHeader) }
                } else {
                    LayoutParser.loadLayoutFromAsset(this, layoutFile, lastLayoutForHeader)
                }
                keyboardView.layoutDefinition = LayoutParser.applyThemeOverrides(this, rawLayout)
            }
            is KeyAction.ToggleModifier -> {
                toggleModifierState(action.modifier)
            }
            is KeyAction.LockModifier -> {
                lockModifierState(action.modifier)
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
                var textToCommit: String? = null
                try {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    if (clipboard != null && clipboard.hasPrimaryClip()) {
                        val clipData = clipboard.primaryClip
                        if (clipData != null && clipData.itemCount > 0) {
                            val item = clipData.getItemAt(0)
                            textToCommit = item.text?.toString() ?: item.coerceToText(this)?.toString()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (!textToCommit.isNullOrEmpty()) {
                    inputConnection.commitText(textToCommit, 1)
                } else {
                    if (!inputConnection.performContextMenuAction(android.R.id.paste)) {
                        sendShortcutKey(inputConnection, KeyEvent.KEYCODE_V)
                    }
                }
                if (keyboardState.consumeOneShotModifiers()) {
                    keyboardView.invalidate()
                }
            }
            is KeyAction.PasteEcho -> {
                var textToCommit: String? = null
                try {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    if (clipboard != null && clipboard.hasPrimaryClip()) {
                        val clipData = clipboard.primaryClip
                        if (clipData != null && clipData.itemCount > 0) {
                            val item = clipData.getItemAt(0)
                            textToCommit = item.text?.toString() ?: item.coerceToText(this)?.toString()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (textToCommit.isNullOrEmpty()) {
                    textToCommit = synchronized(clipboardHistoryList) {
                        clipboardHistoryList.firstOrNull()
                    }
                }

                if (!textToCommit.isNullOrEmpty()) {
                    if (!inputConnection.commitText(textToCommit, 1)) {
                        if (!inputConnection.performContextMenuAction(android.R.id.paste)) {
                            sendShortcutKey(inputConnection, KeyEvent.KEYCODE_V)
                        }
                    }
                } else {
                    if (!inputConnection.performContextMenuAction(android.R.id.paste)) {
                        sendShortcutKey(inputConnection, KeyEvent.KEYCODE_V)
                    }
                }
                if (keyboardState.consumeOneShotModifiers()) {
                    keyboardView.invalidate()
                }
            }
            is KeyAction.SwitchIme -> {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.showInputMethodPicker()
            }
            is KeyAction.LaunchApp -> {
                val target = action.packageName.trim()
                if (target.isNotEmpty()) {
                    try {
                        val intent = if (target.startsWith("intent:") || target.startsWith("http://") || target.startsWith("https://")) {
                            Intent.parseUri(target, Intent.URI_INTENT_SCHEME).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        } else {
                            packageManager.getLaunchIntentForPackage(target)?.apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        }
                        if (intent != null) {
                            startActivity(intent)
                        } else {
                            android.widget.Toast.makeText(this, "App / Target '$target' is not available", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        android.widget.Toast.makeText(this, "Could not launch '$target': ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
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
                        pushCurrentLayoutToStack()
                        val lastLayoutForHeader = if (layoutStack.isNotEmpty()) layoutStack.peek() else (prefs.getString("pref_last_actual_layout", "main") ?: "main")
                        val autoEmojiLayout = LayoutParser.loadLayoutFromAsset(this, "emoji_auto_0", lastLayoutForHeader)
                        keyboardView.layoutDefinition = LayoutParser.applyThemeOverrides(this, autoEmojiLayout)
                    }
                    "VOICE_INPUT", "VOICE_INPUT_ONESHOT", "VOICE_INPUT_TERMINAL" -> {
                        toggleOneShotVoiceRecognition()
                    }
                    "VOICE_INPUT_STANDARD", "VOICE_INPUT_CONTINUOUS", "VOICE_INPUT_MOBILE" -> {
                        startStandardVoiceInput()
                    }
                    "READ_TEXT", "TEXT_TO_SPEECH", "READ_SELECTION" -> {
                        speakSelectedOrCurrentText()
                    }
                    "CLIPBOARD_HISTORY", "CLIPBOARD_MANAGER", "CLIPBOARD_LIST", "CLIPBOARD" -> {
                        showClipboardHistoryOverlay()
                    }
                }
            }
            is KeyAction.SetScreenMode -> {
                val mode = action.mode
                val currentMode = prefs.getString("pref_form_factor", "FULL_WIDTH_DOCKED") ?: "FULL_WIDTH_DOCKED"
                val targetModeStr = if (mode == "SPLIT") {
                    if (currentMode == "SPLIT") "FULL_WIDTH_DOCKED" else "SPLIT"
                } else {
                    mode
                }
                if (targetModeStr == "FLOATING") {
                    if (!com.infinikey_ime.util.OverlayPermissionUtil.hasOverlayPermission(this)) {
                        com.infinikey_ime.util.OverlayPermissionUtil.requestOverlayPermission(this)
                    }
                }
                prefs.edit().putString("pref_form_factor", targetModeStr).apply()
                val formFactorMode = when (targetModeStr) {
                    "SPLIT" -> com.infinikey_ime.model.FormFactorMode.SPLIT
                    "LEFT_DOCKED", "SIDE_DOCKED" -> com.infinikey_ime.model.FormFactorMode.LEFT_DOCKED
                    "RIGHT_DOCKED" -> com.infinikey_ime.model.FormFactorMode.RIGHT_DOCKED
                    "FLOATING" -> com.infinikey_ime.model.FormFactorMode.FLOATING
                    else -> com.infinikey_ime.model.FormFactorMode.FULL_WIDTH_DOCKED
                }
                keyboardView.keyboardState.formFactorMode = formFactorMode
                keyboardView.recalculateKeyBounds()
                keyboardView.requestLayout()
                keyboardView.invalidate()
            }
            else -> {}
        }
    }

    private fun toggleOneShotVoiceRecognition() {
        android.util.Log.d("STT_DEBUG", "toggleOneShotVoiceRecognition -> Custom Interface (Minimal Floating Pill Overlay)")
        keyboardView.showVoiceInputOverlay()
    }

    private fun startStandardVoiceInput() {
        android.util.Log.d("STT_DEBUG", "startStandardVoiceInput -> Normal System Interface (Hacker's Keyboard style)")
        keyboardView.activeListeningStatus = null

        // Try switching directly to System Voice Input Method (Google Voice Typing) like Hacker's Keyboard
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        if (imm != null) {
            try {
                val enabledImes = imm.enabledInputMethodList
                val voiceIme = enabledImes.firstOrNull { info ->
                    val id = info.id.lowercase()
                    id.contains("voice") || id.contains("googlequicksearchbox") || id.contains("speech") || id.contains("tts")
                }
                if (voiceIme != null) {
                    switchInputMethod(voiceIme.id)
                    return
                }
            } catch (_: Exception) {}
        }

        // Fallback: Proxy VoiceInputActivity with standard RecognizerIntent
        val isCapsLockActive = keyboardState.shiftState == ModifierState.LOCKED || keyboardState.isShiftActive
        VoiceInputActivity.onSpeechResultListener = { text ->
            val formattedText = if (isCapsLockActive) text.uppercase() else text
            currentInputConnection?.commitText(formattedText, 1)
        }
        val intent = Intent(this, VoiceInputActivity::class.java).apply {
            putExtra("IS_CAPS_LOCK_ACTIVE", isCapsLockActive)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Google Voice Typing or Speech Recognizer is not available", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        dismissClipboardHistoryOverlay()
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        dismissClipboardHistoryOverlay()
    }

    override fun onDestroy() {
        dismissClipboardHistoryOverlay()
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

    private fun lockModifierState(modifierName: String) {
        val key = modifierName.uppercase()
        when (key) {
            "SHIFT" -> {
                keyboardState.shiftState = if (keyboardState.shiftState == ModifierState.LOCKED) {
                    ModifierState.OFF
                } else {
                    ModifierState.LOCKED
                }
            }
            "CTRL" -> {
                keyboardState.ctrlState = if (keyboardState.ctrlState == ModifierState.LOCKED) {
                    ModifierState.OFF
                } else {
                    ModifierState.LOCKED
                }
            }
            "ALT" -> {
                keyboardState.altState = if (keyboardState.altState == ModifierState.LOCKED) {
                    ModifierState.OFF
                } else {
                    ModifierState.LOCKED
                }
            }
            "SUPER" -> {
                keyboardState.superState = if (keyboardState.superState == ModifierState.LOCKED) {
                    ModifierState.OFF
                } else {
                    ModifierState.LOCKED
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
        return true
    }

    override fun onShowInputRequested(flags: Int, configChange: Boolean): Boolean {
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
        if (newSelStart >= 0 && newSelEnd >= 0) {
            saveCursorSelectionState(newSelStart, newSelEnd, hasSelection)
        }
    }

    /**
     * WORKAROUND [ANDROID-BUG-SELECTION]:
     * Android framework/library bug where modifying text during an active selection can crash the IME.
     * This method persists the cursor/selection bounds to disk so cursor position can be restored post-crash.
     * 
     * REMOVAL NOTICE:
     * Remove this method, saveCursorSelectionState calls, restoreCursorAfterCrash(), and pref_has_active_selection logic
     * once the upstream Android framework text selection crash bug is resolved.
     */
    private fun saveCursorSelectionState(selStart: Int, selEnd: Int, hasSelection: Boolean) {
        try {
            val prefs = getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            if (hasSelection) {
                editor.putInt("pref_saved_selection_start", selStart)
                editor.putInt("pref_saved_selection_end", selEnd)
                editor.putBoolean("pref_has_active_selection", true)
                editor.commit()
            } else {
                editor.putInt("pref_saved_cursor_pos", selStart)
                editor.putBoolean("pref_has_active_selection", false)
                editor.apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * WORKAROUND [ANDROID-BUG-SELECTION]:
     * Restores cursor position to the start of the saved selection if the keyboard service comes back after a crash.
     * 
     * REMOVAL NOTICE:
     * Remove this method once the upstream Android framework text selection crash bug is resolved.
     */
    private fun restoreCursorAfterCrash() {
        try {
            val prefs = getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
            val hasActiveSelection = prefs.getBoolean("pref_has_active_selection", false)
            val savedStart = prefs.getInt("pref_saved_selection_start", -1)

            if (hasActiveSelection && savedStart >= 0) {
                val inputConnection = currentInputConnection
                if (inputConnection != null) {
                    val targetPos = savedStart.coerceAtLeast(0)
                    inputConnection.setSelection(targetPos, targetPos)
                }
                prefs.edit().putBoolean("pref_has_active_selection", false).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
            @Deprecated("Deprecated in Java")
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onError(utteranceId: String?) {
                keyboardView.activeListeningStatus = null
            }
            override fun onError(utteranceId: String?, errorCode: Int) {
                keyboardView.activeListeningStatus = null
            }
        })
        textToSpeech?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }
}
