package com.programmerkeyboard.engine

import android.content.Context
import android.graphics.Color
import android.view.KeyEvent
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.programmerkeyboard.model.DimensionValue
import com.programmerkeyboard.model.KeyAction
import com.programmerkeyboard.model.KeyDefinition
import com.programmerkeyboard.model.KeyRow
import com.programmerkeyboard.model.KeyStyle
import com.programmerkeyboard.model.LayoutDefinition
import com.programmerkeyboard.model.LayoutMetadata
import com.programmerkeyboard.model.LayoutTheme

/**
 * LayoutParser parses JSON Layout Descriptors into internal LayoutDefinition data structures.
 */
object LayoutParser {

    private val gson = Gson()

    fun loadLayoutFromAsset(context: Context, fileName: String = "main.json", previousLayoutId: String = "main"): LayoutDefinition {
        val cleanName = fileName.removePrefix("layouts/").removeSuffix(".json")
        if (cleanName.equals("meta", ignoreCase = true)) {
            return createMetaLayout(context, previousLayoutId)
        }
        if (cleanName.startsWith("emoji_auto") || cleanName.equals("emoji", ignoreCase = true) || cleanName.equals("emoji_picker", ignoreCase = true)) {
            val catIdx = cleanName.removePrefix("emoji_auto_").removePrefix("emoji_auto").toIntOrNull() ?: 0
            val rawEmojiLayout = EmojiLayoutGenerator.generateSupportedEmojiLayout(context, catIdx)
            return applyThemeOverrides(context, rawEmojiLayout)
        }
        val layout = try {
            val assetPath = if (fileName.startsWith("layouts/")) fileName else "layouts/$fileName"
            val jsonString = context.assets.open(assetPath)
                .bufferedReader().use { it.readText() }
            val parsed = parseJsonLayoutDescriptor(jsonString)
            applyThemeOverrides(context, parsed)
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackSimpleLayout(context)
        }
        return ensureNavigationKey(layout)
    }

    fun loadThemePresetFromAssets(context: Context, themeName: String): Pair<LayoutTheme?, Map<String, KeyStyle>> {
        return try {
            val jsonStr = try {
                context.assets.open("themes/$themeName.json").bufferedReader().use { it.readText() }
            } catch (_: Exception) {
                val rootStr = context.assets.open("themes.json").bufferedReader().use { it.readText() }
                val root = com.google.gson.JsonParser.parseString(rootStr).asJsonObject
                root.getAsJsonObject(themeName)?.toString() ?: ""
            }
            if (jsonStr.isEmpty()) return Pair(null, emptyMap())

            val themeEntry = com.google.gson.JsonParser.parseString(jsonStr).asJsonObject
            val themeObj = themeEntry.getAsJsonObject("theme")
            val bgStr = themeObj?.get("backgroundColor")?.asString
            val offDotStr = themeObj?.get("modifierOffDotColor")?.asString
            val latchedDotStr = themeObj?.get("modifierLatchedDotColor")?.asString
            val lockedDotStr = themeObj?.get("modifierLockedDotColor")?.asString

            val theme = LayoutTheme(
                backgroundColor = parseColorHex(bgStr),
                fontFamily = null,
                modifierOffDotColor = parseColorHex(offDotStr),
                modifierLatchedDotColor = parseColorHex(latchedDotStr),
                modifierLockedDotColor = parseColorHex(lockedDotStr)
            )

            val stylesMap = mutableMapOf<String, KeyStyle>()
            val stylesObj = themeEntry.getAsJsonObject("styles")
            stylesObj?.entrySet()?.forEach { (k, v) ->
                if (v.isJsonObject) {
                    stylesMap[k] = parseKeyStyle(v.asJsonObject)
                }
            }
            Pair(theme, stylesMap)
        } catch (_: Exception) {
            Pair(null, emptyMap())
        }
    }

    fun applyThemeOverrides(context: Context, layout: LayoutDefinition): LayoutDefinition {
        val prefs = context.getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        val presetIdx = prefs.getInt("pref_theme_preset_idx", 0).coerceIn(0, 7)
        val presetNames = listOf("system_auto", "slate", "cyberpunk", "oled", "matrix", "retro", "muted_slate")

        val (presetTheme, presetStyles) = if (presetIdx in 0..6) {
            val targetPreset = if (presetIdx == 0) {
                val isNight = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                if (isNight) "slate" else "system_light"
            } else {
                presetNames[presetIdx]
            }
            loadThemePresetFromAssets(context, targetPreset)
        } else Pair(null, emptyMap())

        var currentTheme = layout.theme
        if (presetTheme != null) {
            currentTheme = currentTheme.copy(
                backgroundColor = presetTheme.backgroundColor ?: currentTheme.backgroundColor,
                modifierOffDotColor = presetTheme.modifierOffDotColor ?: currentTheme.modifierOffDotColor,
                modifierLatchedDotColor = presetTheme.modifierLatchedDotColor ?: currentTheme.modifierLatchedDotColor,
                modifierLockedDotColor = presetTheme.modifierLockedDotColor ?: currentTheme.modifierLockedDotColor
            )
        }

        val mergedStyles = layout.styles.toMutableMap()
        presetStyles.forEach { (k, v) -> mergedStyles[k] = v }

        val customThemeJson = prefs.getString("pref_custom_theme_json", null)
        if (presetIdx == 7 && !customThemeJson.isNullOrEmpty()) {
            try {
                val themeRoot = com.google.gson.JsonParser.parseString(customThemeJson).asJsonObject
                val customThemeObj = themeRoot.getAsJsonObject("theme")
                val customBgStr = customThemeObj?.get("backgroundColor")?.asString
                val customOffDotStr = customThemeObj?.get("modifierOffDotColor")?.asString
                val customLatchedDotStr = customThemeObj?.get("modifierLatchedDotColor")?.asString
                val customLockedDotStr = customThemeObj?.get("modifierLockedDotColor")?.asString

                currentTheme = currentTheme.copy(
                    backgroundColor = parseColorHex(customBgStr) ?: currentTheme.backgroundColor,
                    modifierOffDotColor = parseColorHex(customOffDotStr) ?: currentTheme.modifierOffDotColor,
                    modifierLatchedDotColor = parseColorHex(customLatchedDotStr) ?: currentTheme.modifierLatchedDotColor,
                    modifierLockedDotColor = parseColorHex(customLockedDotStr) ?: currentTheme.modifierLockedDotColor
                )

                val customStylesObj = themeRoot.getAsJsonObject("styles")
                customStylesObj?.entrySet()?.forEach { (key, elem) ->
                    if (elem.isJsonObject) {
                        mergedStyles[key] = parseKeyStyle(elem.asJsonObject)
                    }
                }
            } catch (_: Exception) {}
        }

        // Re-apply merged category styles onto every KeyDefinition across all rows
        val updatedRows = layout.rows.map { row ->
            val updatedKeys = row.keys.map { key ->
                val inferredCategory = inferKeyStyleName(key.primaryLabel, key.onPressAction)
                val categoryStyle = (key.styleName?.let { mergedStyles[it] })
                    ?: mergedStyles[inferredCategory]
                    ?: (if (key.styleName in listOf("arrowKey", "pagingKey", "homeEndKey")) mergedStyles["navigationKey"] else null)
                    ?: (if (key.styleName == "clipboardKey") mergedStyles["editingKey"] else null)

                if (categoryStyle != null) {
                    key.copy(
                        bgColor = categoryStyle.bgColor?.let { parseColorHex(it) } ?: key.bgColor,
                        fgColor = categoryStyle.fgColor?.let { parseColorHex(it) } ?: key.fgColor,
                        pressedBgColor = categoryStyle.pressedBgColor?.let { parseColorHex(it) } ?: key.pressedBgColor,
                        activeBgColor = categoryStyle.activeBgColor?.let { parseColorHex(it) } ?: key.activeBgColor
                    )
                } else key
            }
            row.copy(keys = updatedKeys)
        }

        return layout.copy(theme = currentTheme, styles = mergedStyles, rows = updatedRows)
    }

    fun ensureNavigationKey(layout: LayoutDefinition): LayoutDefinition {
        return layout
    }

    fun createMetaLayout(context: Context, previousLayoutId: String = "main"): LayoutDefinition {
        val prefs = context.getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        val assetManager = context.assets

        val layoutFiles = try {
            (assetManager.list("layouts") ?: emptyArray()).filter { it.endsWith(".json") }
        } catch (e: Exception) {
            listOf("main.json", "mobile.json", "function.json", "mobile_number.json", "mobile_symbol.json", "phone.json")
        }

        val layoutMap = mutableMapOf<String, LayoutDefinition>()

        for (file in layoutFiles) {
            val id = file.removeSuffix(".json")
            try {
                val customJson = prefs.getString("pref_custom_layout_json_$id", null)
                val jsonString = if (!customJson.isNullOrEmpty()) customJson else {
                    assetManager.open("layouts/$file").bufferedReader().use { it.readText() }
                }
                val parsed = parseJsonLayoutDescriptor(jsonString)
                layoutMap[id] = parsed
            } catch (_: Exception) {}
        }

        val allPrefKeys = prefs.all.keys
        for (key in allPrefKeys) {
            if (key.startsWith("pref_custom_layout_json_")) {
                val id = key.removePrefix("pref_custom_layout_json_")
                if (!layoutMap.containsKey(id)) {
                    val customJson = prefs.getString(key, null)
                    if (!customJson.isNullOrEmpty()) {
                        try {
                            val parsed = parseJsonLayoutDescriptor(customJson)
                            layoutMap[id] = parsed
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        val prevLayoutDef = layoutMap[previousLayoutId]
        val previousDisplayName = prevLayoutDef?.name?.takeIf { it.isNotBlank() }
            ?: when (previousLayoutId) {
                "main" -> "Full Programmer Keyboard"
                "mobile" -> "Standard Mobile Layout"
                "function" -> "Function & Nav"
                "mobile_number" -> "Numeric Keypad"
                "mobile_symbol" -> "Symbols & Math"
                "phone" -> "Phone Dialer"
                "emoji_auto" -> "Emoji Picker"
                else -> previousLayoutId.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }

        val rowList = mutableListOf<KeyRow>()

        // Dynamic Header Row 1: Return to Previous Layout
        rowList.add(
            KeyRow(
                id = 1,
                keys = listOf(
                    KeyDefinition(
                        primaryLabel = "← Return ($previousDisplayName)",
                        styleName = "actionKey",
                        widthWeight = DimensionValue.Ratio(1.0f),
                        isFlexible = true,
                        showPreview = false,
                        onPressAction = KeyAction.SwitchLayout("[last]")
                    )
                )
            )
        )

        val layoutItems = layoutMap.map { (id, def) ->
            val icon = when (id) {
                "main" -> "⌨ "
                "mobile" -> "📱 "
                "function" -> "⚡ "
                "mobile_number" -> "🔢 "
                "mobile_symbol" -> "🔣 "
                "phone" -> "📞 "
                else -> "📄 "
            }
            val labelName = def.name.takeIf { it.isNotBlank() } ?: id.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            Pair(id, "$icon$labelName")
        }

        // Group layout buttons into rows of 2 keys
        var rowId = 2
        layoutItems.chunked(2).forEach { pairChunk ->
            val keys = pairChunk.map { (layoutId, labelText) ->
                KeyDefinition(
                    primaryLabel = labelText,
                    styleName = "functionKey",
                    widthWeight = DimensionValue.Ratio(1.0f),
                    isFlexible = true,
                    showPreview = false,
                    onPressAction = KeyAction.SwitchLayout(layoutId)
                )
            }
            rowList.add(
                KeyRow(
                    id = rowId++,
                    splitIndex = 1,
                    keys = keys
                )
            )
        }

        val stylesMap = mapOf(
            "functionKey" to KeyStyle(
                bgColor = "#1E293B",
                fgColor = "#F8FAFC",
                pressedBgColor = "#334155",
                borderColor = "#334155",
                borderWidth = DimensionValue.Absolute(1),
                cornerRadius = DimensionValue.Absolute(10)
            ),
            "actionKey" to KeyStyle(
                bgColor = "#0F172A",
                fgColor = "#38BDF8",
                pressedBgColor = "#1E293B",
                borderColor = "#38BDF8",
                borderWidth = DimensionValue.Absolute(1),
                cornerRadius = DimensionValue.Absolute(10)
            )
        )

        return LayoutDefinition(
            id = "meta",
            name = "Meta Layout Picker",
            version = "1.0",
            author = "Programmer Keyboard Team",
            description = "Dynamic Meta Layout listing all available keyboard layouts.",
            metadata = LayoutMetadata(
                horizontalSpacing = DimensionValue.Absolute(6),
                verticalSpacing = DimensionValue.Absolute(6),
                defaultScreenMode = "FULL_WIDTH_DOCKED",
                defaultHeightPercentage = 30,
                showKeyPreview = false,
                maxFontSize = DimensionValue.Absolute(15)
            ),
            theme = LayoutTheme(backgroundColor = parseColorHex("#0F172A")),
            styles = stylesMap,
            rows = rowList
        )
    }

    fun parseJsonLayoutDescriptor(jsonString: String): LayoutDefinition {
        val root = gson.fromJson(jsonString, JsonObject::class.java)

        val id = root.get("id")?.asString ?: "custom_layout"
        val name = root.get("name")?.asString ?: "Custom Layout"
        val version = root.get("version")?.asString ?: "1.0"
        val author = root.get("author")?.asString ?: "Unknown"
        val description = root.get("description")?.asString ?: ""

        // Metadata
        val metadataObj = root.getAsJsonObject("metadata")
        val hSpacing = parseDimensionValue(metadataObj?.get("horizontalSpacing")) ?: DimensionValue.Absolute(4)
        val vSpacing = parseDimensionValue(metadataObj?.get("verticalSpacing")) ?: DimensionValue.Absolute(4)
        val screenMode = metadataObj?.get("defaultScreenMode")?.asString ?: "FULL_WIDTH_DOCKED"
        val heightPct = metadataObj?.get("defaultHeightPercentage")?.asInt ?: 30
        val longPressMs = metadataObj?.get("longPressTimeoutMs")?.asLong ?: 350L
        val autoRepeatMs = metadataObj?.get("autoRepeatIntervalMs")?.asLong ?: 50L
        val splitClusterRatio = metadataObj?.get("splitClusterRatio")?.asFloat
        val showKeyPreview = metadataObj?.get("showKeyPreview")?.asBoolean
            ?: metadataObj?.get("showPreview")?.asBoolean
            ?: metadataObj?.get("enableKeyPreview")?.asBoolean
            ?: true
        val maxFontSize = parseDimensionValue(metadataObj?.get("maxFontSize"))
        val scrollDir = metadataObj?.get("scrollDirection")?.asString
            ?: metadataObj?.get("scroll")?.asString
        val maxRows = metadataObj?.get("maxVisibleRows")?.asInt
            ?: metadataObj?.get("scrollRows")?.asInt
        val maxCols = metadataObj?.get("maxVisibleColumns")?.asInt
            ?: metadataObj?.get("scrollColumns")?.asInt

        val metadata = LayoutMetadata(
            horizontalSpacing = hSpacing,
            verticalSpacing = vSpacing,
            defaultScreenMode = screenMode,
            defaultHeightPercentage = heightPct,
            longPressTimeoutMs = longPressMs,
            autoRepeatIntervalMs = autoRepeatMs,
            splitClusterRatio = splitClusterRatio,
            showKeyPreview = showKeyPreview,
            maxFontSize = maxFontSize,
            scrollDirection = scrollDir,
            maxVisibleRows = maxRows,
            maxVisibleColumns = maxCols
        )

        // Theme
        val themeObj = root.getAsJsonObject("theme")
        val bgColorStr = themeObj?.get("backgroundColor")?.asString
        val fontFamilyStr = themeObj?.get("fontFamily")?.asString
        val offDotColorStr = themeObj?.get("modifierOffDotColor")?.asString
        val latchedDotColorStr = themeObj?.get("modifierLatchedDotColor")?.asString
        val lockedDotColorStr = themeObj?.get("modifierLockedDotColor")?.asString

        val theme = LayoutTheme(
            backgroundColor = parseColorHex(bgColorStr),
            fontFamily = fontFamilyStr,
            modifierOffDotColor = parseColorHex(offDotColorStr),
            modifierLatchedDotColor = parseColorHex(latchedDotColorStr),
            modifierLockedDotColor = parseColorHex(lockedDotColorStr)
        )

        // Styles
        val stylesObj = root.getAsJsonObject("styles")
        val stylesMap = mutableMapOf<String, KeyStyle>()
        stylesObj?.entrySet()?.forEach { (styleName, styleElem) ->
            if (styleElem.isJsonObject) {
                stylesMap[styleName] = parseKeyStyle(styleElem.asJsonObject)
            }
        }

        // Surface Gestures
        val gesturesObj = root.getAsJsonObject("gestures")
        val gesturesMap = mutableMapOf<String, KeyAction>()
        gesturesObj?.entrySet()?.forEach { (gestureName, actionElem) ->
            if (actionElem.isJsonObject) {
                gesturesMap[gestureName] = parseAction(actionElem.asJsonObject)
            }
        }

        // Rows
        val rowsArray = root.getAsJsonArray("rows") ?: JsonArray()
        val rows = parseRows(rowsArray, stylesMap, id)

        return LayoutDefinition(
            id = id,
            name = name,
            version = version,
            author = author,
            description = description,
            metadata = metadata,
            theme = theme,
            styles = stylesMap,
            gestures = gesturesMap,
            rows = rows
        )
    }

    private fun parseKeyStyle(obj: JsonObject): KeyStyle {
        return KeyStyle(
            bgColor = obj.get("bgColor")?.asString,
            pressedBgColor = obj.get("pressedBgColor")?.asString,
            activeBgColor = obj.get("activeBgColor")?.asString,
            fgColor = obj.get("fgColor")?.asString,
            secondaryFgColor = obj.get("secondaryFgColor")?.asString,
            activeFgColor = obj.get("activeFgColor")?.asString,
            borderColor = obj.get("borderColor")?.asString,
            borderWidth = parseDimensionValue(obj.get("borderWidth")),
            cornerRadius = parseDimensionValue(obj.get("cornerRadius")),
            fontSize = parseDimensionValue(obj.get("fontSize")),
            secondaryFontSize = parseDimensionValue(obj.get("secondaryFontSize")),
            backgroundImage = obj.get("backgroundImage")?.asString,
            showPreview = obj.get("showPreview")?.asBoolean ?: obj.get("showKeyPreview")?.asBoolean
        )
    }

    private fun parseRows(rowsArray: JsonArray, stylesMap: Map<String, KeyStyle>, layoutId: String): List<KeyRow> {
        val rowsList = mutableListOf<KeyRow>()

        rowsArray.forEachIndexed { rowIndex, rowElem ->
            if (!rowElem.isJsonObject) return@forEachIndexed
            val rowObj = rowElem.asJsonObject
            val rowId: Any = rowObj.get("id")?.let {
                if (it.asJsonPrimitive.isNumber) it.asInt else it.asString
            } ?: (rowIndex + 1)
            val hidden = rowObj.get("hidden")?.asBoolean ?: false

            val splitIndex = rowObj.get("splitIndex")?.asInt
            val splitKey = rowObj.get("splitKey")?.asBoolean ?: false
            val splitRatio = rowObj.get("splitRatio")?.asFloat
            val leftOffset = parseDimensionValue(rowObj.get("leftOffset"))
            val rightOffset = parseDimensionValue(rowObj.get("rightOffset"))

            val keysArray = rowObj.getAsJsonArray("keys") ?: JsonArray()
            val keysList = mutableListOf<KeyDefinition>()

            keysArray.forEach { keyElem ->
                if (keyElem.isJsonObject) {
                    val kObj = keyElem.asJsonObject
                    val label = kObj.get("label")?.asString ?: ""
                    val secondaryLabel = kObj.get("secondaryLabel")?.asString
                    val styleName = kObj.get("style")?.asString

                    val onPressObj = kObj.getAsJsonObject("onPress")
                    val onLongPressObj = kObj.getAsJsonObject("onLongPress")
                    val onSwipeUpObj = kObj.getAsJsonObject("onSwipeUp")
                    val onSwipeDownObj = kObj.getAsJsonObject("onSwipeDown")
                    val onSwipeLeftObj = kObj.getAsJsonObject("onSwipeLeft")
                    val onSwipeRightObj = kObj.getAsJsonObject("onSwipeRight")

                    val onPressAction = if (onPressObj != null) parseAction(onPressObj) else mapLabelToDefaultAction(label)
                    val onSwipeUpAction = if (onSwipeUpObj != null) parseAction(onSwipeUpObj) else KeyAction.None
                    val onSwipeDownAction = if (onSwipeDownObj != null) parseAction(onSwipeDownObj) else KeyAction.None
                    val onSwipeLeftAction = if (onSwipeLeftObj != null) parseAction(onSwipeLeftObj) else KeyAction.None
                    val onSwipeRightAction = if (onSwipeRightObj != null) parseAction(onSwipeRightObj) else KeyAction.None

                    val inferredStyleName = inferKeyStyleName(label, onPressAction)
                    val styleObj = styleName?.let { stylesMap[it] } ?: stylesMap[inferredStyleName]

                    val isSplitKey = kObj.get("isSplitKey")?.asBoolean ?: (label.equals("space", ignoreCase = true) || label == "␣")
                    val splitLeftWeight = parseDimensionValue(kObj.get("splitLeftWeight"))
                    val splitRightWeight = parseDimensionValue(kObj.get("splitRightWeight"))
                    val isFlexible = kObj.get("flexible")?.asBoolean
                        ?: kObj.get("grow")?.asBoolean
                        ?: kObj.get("canGrow")?.asBoolean
                        ?: false
                    val maxWeight = kObj.get("maxWeight")?.asFloat ?: kObj.get("maxWidth")?.asFloat
                    val isSpacer = kObj.get("spacer")?.asBoolean
                        ?: (styleName == "spacer" || (label.isEmpty() && kObj.getAsJsonObject("onPress") == null))
                    val showPreview = kObj.get("showPreview")?.asBoolean
                        ?: kObj.get("showKeyPreview")?.asBoolean
                        ?: styleObj?.showPreview
                    val startOffset = parseDimensionValue(kObj.get("startOffset") ?: kObj.get("offset"))

                    // Dimension values (local override -> style class default)
                    val weight = parseDimensionValue(kObj.get("weight") ?: kObj.get("width"))
                        ?: DimensionValue.Ratio(1.0f)
                    val height = parseDimensionValue(kObj.get("height"))

                    // Visual colors (local override -> style class default)
                    val fgColorStr = kObj.get("fgColor")?.asString ?: styleObj?.fgColor
                    val secondaryFgColorStr = kObj.get("secondaryFgColor")?.asString ?: styleObj?.secondaryFgColor
                    val bgColorStr = kObj.get("bgColor")?.asString ?: styleObj?.bgColor
                    val pressedBgColorStr = kObj.get("pressedBgColor")?.asString ?: styleObj?.pressedBgColor
                    val activeBgColorStr = kObj.get("activeBgColor")?.asString ?: styleObj?.activeBgColor
                    val borderColorStr = kObj.get("borderColor")?.asString ?: styleObj?.borderColor

                    val borderWidth = parseDimensionValue(kObj.get("borderWidth")) ?: styleObj?.borderWidth
                    val cornerRadius = parseDimensionValue(kObj.get("cornerRadius")) ?: styleObj?.cornerRadius
                    val fontSize = parseDimensionValue(kObj.get("fontSize")) ?: styleObj?.fontSize
                    val maxFontSize = parseDimensionValue(kObj.get("maxFontSize"))
                    val secondaryFontSize = parseDimensionValue(kObj.get("secondaryFontSize")) ?: styleObj?.secondaryFontSize

                    val iconStr = kObj.get("icon")?.asString
                    val bgImgStr = kObj.get("backgroundImage")?.asString ?: styleObj?.backgroundImage
                    
                    val alternatesList = mutableListOf<String>()
                    kObj.getAsJsonArray("alternates")?.forEach { elem -> alternatesList.add(elem.asString) }
                    if (alternatesList.isEmpty()) {
                        kObj.getAsJsonArray("alternateKeys")?.forEach { elem -> alternatesList.add(elem.asString) }
                    }

                    val longPressOptionsList = mutableListOf<String>()
                    kObj.getAsJsonArray("longPressOptions")?.forEach { elem -> longPressOptionsList.add(elem.asString) }

                    val topLeftLabel = kObj.get("topLeftLabel")?.asString
                    val topRightLabel = kObj.get("topRightLabel")?.asString ?: alternatesList.firstOrNull() ?: secondaryLabel

                    // Popups come EXCLUSIVELY from the layout definition
                    val layoutPopupOptions = mutableListOf<String>()
                    alternatesList.forEach { if (!layoutPopupOptions.contains(it)) layoutPopupOptions.add(it) }
                    longPressOptionsList.forEach { if (!layoutPopupOptions.contains(it)) layoutPopupOptions.add(it) }

                    val parsedLongPress = if (onLongPressObj != null) parseAction(onLongPressObj) else null
                    val onLongPressAction = when {
                        parsedLongPress is KeyAction.ShowPopup -> {
                            val mergedList = mutableListOf<String>()
                            layoutPopupOptions.forEach { opt -> if (!mergedList.contains(opt)) mergedList.add(opt) }
                            parsedLongPress.options.forEach { opt -> if (!mergedList.contains(opt)) mergedList.add(opt) }
                            if (mergedList.isNotEmpty()) KeyAction.ShowPopup(mergedList) else KeyAction.None
                        }
                        parsedLongPress != null -> parsedLongPress
                        layoutPopupOptions.isNotEmpty() -> KeyAction.ShowPopup(layoutPopupOptions)
                        else -> KeyAction.None
                    }

                    val keyDef = KeyDefinition(
                        primaryLabel = label,
                        secondaryLabel = secondaryLabel,
                        styleName = styleName,
                        widthWeight = weight,
                        heightRatio = height,
                        isSplitKey = isSplitKey,
                        splitLeftWeight = splitLeftWeight,
                        splitRightWeight = splitRightWeight,
                        isFlexible = isFlexible,
                        maxWeight = maxWeight,
                        isSpacer = isSpacer,
                        showPreview = showPreview,
                        alternates = alternatesList,
                        topLeftLabel = topLeftLabel,
                        topRightLabel = topRightLabel,
                        startOffset = startOffset,
                        fgColor = parseColorHex(fgColorStr),
                        secondaryFgColor = parseColorHex(secondaryFgColorStr),
                        bgColor = parseColorHex(bgColorStr),
                        pressedBgColor = parseColorHex(pressedBgColorStr),
                        activeBgColor = parseColorHex(activeBgColorStr),
                        borderColor = parseColorHex(borderColorStr),
                        borderWidth = borderWidth,
                        cornerRadius = cornerRadius,
                        fontSize = fontSize,
                        maxFontSize = maxFontSize,
                        secondaryFontSize = secondaryFontSize,
                        iconName = iconStr,
                        backgroundImage = bgImgStr,
                        onPressAction = onPressAction,
                        onLongPressAction = onLongPressAction,
                        onSwipeUpAction = onSwipeUpAction,
                        onSwipeDownAction = onSwipeDownAction,
                        onSwipeLeftAction = onSwipeLeftAction,
                        onSwipeRightAction = onSwipeRightAction
                    )
                    keysList.add(keyDef)
                } else if (keyElem.isJsonPrimitive) {
                    val label = keyElem.asString
                    keysList.add(createKeyDefinitionFromLabel(label))
                }
            }

            rowsList.add(KeyRow(
                id = rowId,
                hidden = hidden,
                splitIndex = splitIndex,
                splitKey = splitKey,
                splitRatio = splitRatio,
                leftOffset = leftOffset,
                rightOffset = rightOffset,
                keys = keysList
            ))
        }

        return rowsList
    }

    private fun parseAction(obj: JsonObject): KeyAction {
        val typeStr = obj.get("type")?.asString ?: return KeyAction.None
        return when (typeStr) {
            "SEND_TEXT" -> KeyAction.SendText(obj.get("text")?.asString ?: "")
            "SEND_CODE" -> KeyAction.SendCode(obj.get("code")?.asInt ?: KeyEvent.KEYCODE_UNKNOWN)
            "SWITCH_LAYOUT" -> KeyAction.SwitchLayout(obj.get("target")?.asString ?: "main")
            "SET_SCREEN_MODE" -> KeyAction.SetScreenMode(obj.get("mode")?.asString ?: "FULL_WIDTH_DOCKED")
            "ADJUST_HEIGHT" -> KeyAction.AdjustHeight(
                delta = obj.get("delta")?.asInt,
                percentage = obj.get("percentage")?.asInt
            )
            "SHOW_POPUP" -> {
                val opts = obj.getAsJsonArray("options")?.map { it.asString } ?: emptyList()
                val actionsArray = obj.getAsJsonArray("actions")
                val actionsList = if (actionsArray != null) {
                    actionsArray.map { if (it.isJsonObject) parseAction(it.asJsonObject) else resolveActionFromLabel(it.asString) }
                } else {
                    opts.map { resolveActionFromLabel(it) }
                }
                KeyAction.ShowPopup(opts, actionsList)
            }
            "SHOW_WIDGET" -> KeyAction.ShowWidget(obj.get("widget")?.asString ?: "JOYSTICK")
            "AUTO_REPEAT" -> KeyAction.AutoRepeat(
                code = obj.get("code")?.asInt ?: KeyEvent.KEYCODE_DEL,
                intervalMs = obj.get("intervalMs")?.asLong ?: 50L
            )
            "TOGGLE_ROW" -> {
                val rowIdObj = obj.get("rowId")
                val rowId: Any = if (rowIdObj != null && rowIdObj.asJsonPrimitive.isNumber) {
                    rowIdObj.asInt
                } else {
                    rowIdObj?.asString ?: "all_hidden"
                }
                KeyAction.ToggleRow(rowId)
            }
            "TOGGLE_MODIFIER" -> KeyAction.ToggleModifier(obj.get("modifier")?.asString ?: "SHIFT")
            "SELECT_ALL" -> KeyAction.SelectAll
            "COPY" -> KeyAction.Copy
            "CUT" -> KeyAction.Cut
            "PASTE" -> KeyAction.Paste
            "PASTE_ECHO", "ECHO_CLIPBOARD", "PASTE_TEXT" -> KeyAction.PasteEcho
            "SWITCH_IME" -> KeyAction.SwitchIme
            else -> KeyAction.None
        }
    }

    private fun resolveActionFromLabel(label: String): KeyAction {
        return when (label.trim().uppercase()) {
            "SELECT ALL", "SELECT_ALL", "全选" -> KeyAction.SelectAll
            "COPY", "复制" -> KeyAction.Copy
            "CUT", "剪切" -> KeyAction.Cut
            "PASTE", "粘贴" -> KeyAction.Paste
            "PASTE ECHO", "ECHO", "ECHO_CLIPBOARD", "PASTE_ECHO", "PASTE_TEXT", "📋" -> KeyAction.PasteEcho
            "SWITCH IME", "SWITCH_IME", "KEYBOARD", "⌨" -> KeyAction.SwitchIme
            else -> KeyAction.SendText(label)
        }
    }

    private fun parseDimensionValue(element: JsonElement?): DimensionValue? {
        if (element == null || !element.isJsonPrimitive) return null
        val primitive = element.asJsonPrimitive
        return if (primitive.isNumber) {
            val str = primitive.asString
            if (str.contains(".") || str.contains("f") || str.contains("F")) {
                DimensionValue.Ratio(primitive.asFloat)
            } else {
                DimensionValue.Absolute(primitive.asInt)
            }
        } else null
    }

    private fun parseColorHex(colorStr: String?): Int? {
        if (colorStr.isNullOrEmpty()) return null
        var hex = colorStr.trim()
        if (hex.startsWith("#") && hex.length == 4) {
            val r = hex[1]
            val g = hex[2]
            val b = hex[3]
            hex = "#$r$r$g$g$b$b"
        }
        return try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            null
        }
    }

    private fun mapLabelToDefaultAction(label: String): KeyAction {
        return when (label) {
            "Tab" -> KeyAction.SendCode(KeyEvent.KEYCODE_TAB)
            "Backspace" -> KeyAction.SendCode(KeyEvent.KEYCODE_DEL)
            "Enter" -> KeyAction.SendCode(KeyEvent.KEYCODE_ENTER)
            "Esc" -> KeyAction.SendCode(KeyEvent.KEYCODE_ESCAPE)
            "Ctrl" -> KeyAction.ToggleModifier("CTRL")
            "Shift" -> KeyAction.ToggleModifier("SHIFT")
            "Alt" -> KeyAction.ToggleModifier("ALT")
            "Super" -> KeyAction.ToggleModifier("SUPER")
            "Fn" -> KeyAction.SwitchLayout("function")
            "Mic" -> KeyAction.ShowWidget("VOICE_INPUT")
            "Settings", "⚙" -> KeyAction.ShowWidget("SETTINGS")
            "Emoji" -> KeyAction.ShowWidget("EMOJI_PICKER")
            "SelAll" -> KeyAction.SelectAll
            "Copy" -> KeyAction.Copy
            "Cut" -> KeyAction.Cut
            "Paste" -> KeyAction.Paste
            "PasteEcho" -> KeyAction.PasteEcho
            "🌐", "ImePicker", "SwitchIme" -> KeyAction.SwitchIme
            "UpArrow" -> KeyAction.SendCode(KeyEvent.KEYCODE_DPAD_UP)
            "DownArrow" -> KeyAction.SendCode(KeyEvent.KEYCODE_DPAD_DOWN)
            "LeftArrow" -> KeyAction.SendCode(KeyEvent.KEYCODE_DPAD_LEFT)
            "RightArrow" -> KeyAction.SendCode(KeyEvent.KEYCODE_DPAD_RIGHT)
            "Space" -> KeyAction.SendText(" ")
            else -> KeyAction.SendText(label)
        }
    }

    private fun getShiftedVersion(label: String, secondaryLabel: String?, onSwipeUpAction: KeyAction): String? {
        if (label.length == 1 && label[0].isLetter()) return null
        if (!secondaryLabel.isNullOrEmpty()) {
            if (secondaryLabel.length == 1 && secondaryLabel[0].isLetter()) return null
            return secondaryLabel
        }
        if (onSwipeUpAction is KeyAction.SendText && onSwipeUpAction.text.isNotEmpty()) {
            val t = onSwipeUpAction.text
            if (t.length == 1 && t[0].isLetter()) return null
            return t
        }
        if (label.length == 1) {
            val ch = label[0]
            return when (ch) {
                '1' -> "!"
                '2' -> "@"
                '3' -> "#"
                '4' -> "$"
                '5' -> "%"
                '6' -> "^"
                '7' -> "&"
                '8' -> "*"
                '9' -> "("
                '0' -> ")"
                '`' -> "~"
                '-' -> "_"
                '=' -> "+"
                '[' -> "{"
                ']' -> "}"
                '\\' -> "|"
                ';' -> ":"
                '\'' -> "\""
                ',' -> "<"
                '.' -> ">"
                '/' -> "?"
                else -> null
            }
        }
        return null
    }

    fun createKeyDefinitionFromLabel(label: String): KeyDefinition {
        val action = mapLabelToDefaultAction(label)
        val weight = when (label) {
            "Space", "␣" -> DimensionValue.Ratio(3.0f)
            "Backspace", "Enter", "Shift", "Tab", "Ctrl" -> DimensionValue.Ratio(1.4f)
            else -> DimensionValue.Ratio(1.0f)
        }

        return KeyDefinition(
            primaryLabel = label,
            widthWeight = weight,
            onPressAction = action,
            onLongPressAction = KeyAction.None
        )
    }

    private fun inferKeyStyleName(label: String, onPressAction: KeyAction): String {
        val trimmed = label.trim()

        val isNav = trimmed in listOf("PgUp", "PgDn", "PageUp", "PageDown", "Home", "End", "←", "↑", "↓", "→", "Left", "Right", "Up", "Down")
                || (onPressAction is KeyAction.SendCode && onPressAction.code in listOf(
            android.view.KeyEvent.KEYCODE_PAGE_UP,
            android.view.KeyEvent.KEYCODE_PAGE_DOWN,
            android.view.KeyEvent.KEYCODE_MOVE_HOME,
            android.view.KeyEvent.KEYCODE_MOVE_END,
            android.view.KeyEvent.KEYCODE_DPAD_LEFT,
            android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
            android.view.KeyEvent.KEYCODE_DPAD_UP,
            android.view.KeyEvent.KEYCODE_DPAD_DOWN
        ))
        if (isNav) return "navigationKey"

        val isEdit = trimmed in listOf("Ins", "Insert", "Del", "Delete", "Cut", "Copy", "Paste", "PasteEcho", "SelAll")
                || onPressAction is KeyAction.Cut || onPressAction is KeyAction.Copy || onPressAction is KeyAction.Paste || onPressAction is KeyAction.PasteEcho || onPressAction is KeyAction.SelectAll
                || (onPressAction is KeyAction.SendCode && onPressAction.code == android.view.KeyEvent.KEYCODE_INSERT)
        if (isEdit) return "editingKey"

        val isMod = trimmed in listOf("Shift", "Ctrl", "Alt", "Meta", "Super", "Caps", "Fn", "⇧", "⇪")
                || onPressAction is KeyAction.ToggleModifier
        if (isMod) return "modifierKey"

        val isFn = (trimmed.length in 2..3 && trimmed.startsWith("F", ignoreCase = true) && trimmed.substring(1).toIntOrNull() != null)
                || trimmed in listOf("Esc", "Tab")
        if (isFn) return "functionKey"

        val isAct = trimmed in listOf("Enter", "Return", "Space", "␣", "Backspace", "⌫")
                || (onPressAction is KeyAction.SendCode && (onPressAction.code == android.view.KeyEvent.KEYCODE_ENTER || onPressAction.code == android.view.KeyEvent.KEYCODE_DEL || onPressAction.code == android.view.KeyEvent.KEYCODE_SPACE))
        if (isAct) return "actionKey"

        if (trimmed.length == 1 && (trimmed[0].isDigit() || trimmed == "=" || trimmed == "+")) return "numberKey"

        return "alphaKey"
    }

    private fun fallbackSimpleLayout(context: Context): LayoutDefinition {
        return try {
            val jsonString = context.assets.open("layouts/main.json").bufferedReader().use { it.readText() }
            val parsed = parseJsonLayoutDescriptor(jsonString)
            applyThemeOverrides(context, parsed)
        } catch (_: Exception) {
            LayoutDefinition(
                id = "main",
                name = "Full Programmer Keyboard",
                version = "1.0",
                rows = emptyList()
            )
        }
    }
}
