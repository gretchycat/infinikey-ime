package com.infinikey_ime.util

import android.content.Context
import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

/**
 * Theme Manager for Infinikey IME.
 * Manages copying default themes to the user-accessible directory (Android/data/com.infinikey_ime/files/themes),
 * with version checking and edit protection so user edits are NEVER overwritten by automatic copies.
 */
object ThemeManager {

    private const val TAG = "ThemeManager"
    const val CURRENT_THEME_VERSION = 1

    val PRESET_KEY_NAMES = listOf(
        "system_auto",
        "system_light",
        "system_dark",
        "slate",
        "cyberpunk",
        "oled",
        "matrix",
        "retro",
        "muted_slate"
    )

    /**
     * Gets the user-accessible themes directory.
     * Path: Android/data/com.infinikey_ime/files/themes/
     */
    fun getUserThemesDir(context: Context): File {
        val externalDir = context.getExternalFilesDir(null)
        val dir = if (externalDir != null) {
            File(externalDir, "themes")
        } else {
            File(context.filesDir, "themes")
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Ensures all default theme files exist in the user-accessible themes directory.
     * Respects user edits (`userEdited = true`) and version numbers (`version`), never overwriting user edits.
     */
    fun ensureDefaultThemesCopied(context: Context) {
        try {
            val themesDir = getUserThemesDir(context)
            val assetThemes = try {
                context.assets.list("themes")?.toList() ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            for (assetFile in assetThemes) {
                if (assetFile.endsWith(".json")) {
                    val themeName = assetFile.removeSuffix(".json")
                    val targetFile = File(themesDir, assetFile)
                    val assetJson = loadDefaultThemeFromAssets(context, themeName)
                    processThemeCopyOrUpgrade(assetJson, targetFile)
                }
            }

            val rootThemes = listOf("system_light", "system_dark", "system_auto", "slate", "cyberpunk", "oled", "matrix", "retro", "muted_slate")
            for (themeKey in rootThemes) {
                val targetFile = File(themesDir, "$themeKey.json")
                val assetJson = loadDefaultThemeFromAssets(context, themeKey)
                if (assetJson.isNotEmpty()) {
                    processThemeCopyOrUpgrade(assetJson, targetFile)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in ensureDefaultThemesCopied: ${e.message}")
        }
    }

    private fun processThemeCopyOrUpgrade(assetJsonStr: String, targetFile: File) {
        if (assetJsonStr.isEmpty()) return

        val assetRoot = try {
            JsonParser.parseString(assetJsonStr).asJsonObject
        } catch (_: Exception) {
            return
        }

        val assetVersion = assetRoot.get("version")?.asInt ?: CURRENT_THEME_VERSION
        if (!assetRoot.has("version")) {
            assetRoot.addProperty("version", assetVersion)
        }
        if (!assetRoot.has("userEdited")) {
            assetRoot.addProperty("userEdited", false)
        }

        if (!targetFile.exists() || targetFile.length() == 0L) {
            try {
                targetFile.writeText(formatPrettyJson(assetRoot.toString()))
                Log.d(TAG, "Copied default theme version $assetVersion to '${targetFile.name}'.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed writing default theme '${targetFile.name}': ${e.message}")
            }
            return
        }

        // File exists in user storage - check if edited by user
        try {
            val userContent = targetFile.readText()
            val userRoot = JsonParser.parseString(userContent).asJsonObject

            val isUserEdited = userRoot.get("userEdited")?.asBoolean ?: false
            val userVersion = userRoot.get("version")?.asInt ?: 0

            if (isUserEdited) {
                // USER HAS EDITED THIS THEME - DO NOT OVERWRITE!
                Log.d(TAG, "Theme '${targetFile.name}' was edited by user. Preserving user edits.")
                return
            }

            if (assetVersion > userVersion) {
                // Unedited theme asset was updated to a newer version by app update - safely upgrade
                targetFile.writeText(formatPrettyJson(assetRoot.toString()))
                Log.d(TAG, "Upgraded default theme '${targetFile.name}' from v$userVersion to v$assetVersion.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking user theme file '${targetFile.name}': ${e.message}")
        }
    }

    /**
     * Loads theme JSON string. Checks user-accessible directory first, then falls back to assets.
     */
    fun loadThemeJson(context: Context, themeName: String): String {
        ensureDefaultThemesCopied(context)
        val themesDir = getUserThemesDir(context)
        val targetFile = File(themesDir, "$themeName.json")

        if (targetFile.exists() && targetFile.length() > 0L) {
            try {
                val content = targetFile.readText()
                if (content.trim().isNotEmpty()) {
                    return content
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading user theme file '$themeName.json': ${e.message}")
            }
        }

        return loadDefaultThemeFromAssets(context, themeName)
    }

    /**
     * Saves updated theme JSON directly to the user-accessible file, marking userEdited = true.
     */
    fun saveThemeJson(context: Context, themeName: String, jsonContent: String, markEdited: Boolean = true): Boolean {
        return try {
            val themesDir = getUserThemesDir(context)
            val targetFile = File(themesDir, "$themeName.json")

            val rootObj = try {
                JsonParser.parseString(jsonContent).asJsonObject
            } catch (_: Exception) {
                JsonObject()
            }

            val currentVersion = rootObj.get("version")?.asInt ?: CURRENT_THEME_VERSION
            rootObj.addProperty("version", currentVersion)
            rootObj.addProperty("userEdited", markEdited)

            val formatted = formatPrettyJson(rootObj.toString())
            targetFile.writeText(formatted)
            Log.d(TAG, "Successfully saved theme '$themeName.json' (version=$currentVersion, userEdited=$markEdited) to user storage.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving theme '$themeName.json': ${e.message}")
            false
        }
    }

    /**
     * Resets a theme in user storage back to factory default asset state.
     */
    fun resetThemeToDefault(context: Context, themeName: String): Boolean {
        val assetJson = loadDefaultThemeFromAssets(context, themeName)
        if (assetJson.isEmpty()) return false
        val themesDir = getUserThemesDir(context)
        val targetFile = File(themesDir, "$themeName.json")
        return try {
            val root = JsonParser.parseString(assetJson).asJsonObject
            root.addProperty("version", CURRENT_THEME_VERSION)
            root.addProperty("userEdited", false)
            targetFile.writeText(formatPrettyJson(root.toString()))
            Log.d(TAG, "Reset theme '$themeName' to factory default.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting theme '$themeName': ${e.message}")
            false
        }
    }

    /**
     * Deletes a custom theme from user storage.
     */
    fun deleteCustomTheme(context: Context, themeName: String): Boolean {
        val themesDir = getUserThemesDir(context)
        val targetFile = File(themesDir, "$themeName.json")
        return if (targetFile.exists()) {
            targetFile.delete()
        } else false
    }

    private fun loadDefaultThemeFromAssets(context: Context, themeName: String): String {
        return try {
            context.assets.open("themes/$themeName.json").bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            try {
                val rootStr = context.assets.open("themes.json").bufferedReader().use { it.readText() }
                val root = JsonParser.parseString(rootStr).asJsonObject
                root.getAsJsonObject(themeName)?.toString() ?: ""
            } catch (_: Exception) {
                ""
            }
        }
    }

    private fun formatPrettyJson(jsonStr: String): String {
        return try {
            val elem = JsonParser.parseString(jsonStr)
            com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(elem)
        } catch (_: Exception) {
            jsonStr
        }
    }
}
