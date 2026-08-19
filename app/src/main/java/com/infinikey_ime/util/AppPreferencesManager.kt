package com.infinikey_ime.util

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

/**
 * Manager for saving and loading app-specific layout preferences.
 * Preferences are stored in a directory called "app preferences" in the user accessible private directory:
 * <User Accessible Private Directory>/app preferences/<packageName>.json
 */
object AppPreferencesManager {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Gets the user accessible "app preferences" directory.
     * Path: Android/data/com.infinikey_ime/files/app preferences/
     */
    fun getAppPreferencesDir(context: Context): File {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(baseDir, "app preferences")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Checks if a preference file exists for the given package name in app preferences directory.
     */
    fun hasAppPreferenceFile(context: Context, packageName: String): Boolean {
        if (packageName.isBlank()) return false
        val dir = getAppPreferencesDir(context)
        val candidates = listOf(
            File(dir, "$packageName.json"),
            File(dir, "$packageName.txt"),
            File(dir, packageName)
        )
        return candidates.any { it.exists() && it.isFile && it.length() > 0 }
    }

    /**
     * Reads the saved last layout for a given package name if the preference file exists.
     * Returns null if no preference file exists or if it could not be read.
     */
    fun getLastLayoutForApp(context: Context, packageName: String): String? {
        if (packageName.isBlank()) return null
        val dir = getAppPreferencesDir(context)
        val candidates = listOf(
            File(dir, "$packageName.json"),
            File(dir, "$packageName.txt"),
            File(dir, packageName)
        )
        val file = candidates.firstOrNull { it.exists() && it.isFile && it.length() > 0 } ?: return null

        return try {
            val content = file.readText().trim()
            if (content.isEmpty()) return null

            val layoutStr = if (content.startsWith("{")) {
                val json = JsonParser.parseString(content).asJsonObject
                json.get("layoutTarget")?.asString
                    ?: json.get("layout")?.asString
                    ?: json.get("target")?.asString
            } else if (content.startsWith("\"") && content.endsWith("\"")) {
                JsonParser.parseString(content).asString
            } else {
                content
            }

            layoutStr?.trim()?.removeSuffix(".json")?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves the last layout used for a given package name into app preferences directory.
     */
    fun saveLastLayoutForApp(context: Context, packageName: String, layoutId: String) {
        if (packageName.isBlank() || layoutId.isBlank()) return
        val cleanLayoutId = layoutId.removeSuffix(".json")
        try {
            val dir = getAppPreferencesDir(context)
            val file = File(dir, "$packageName.json")
            val jsonObject = JsonObject().apply {
                addProperty("layoutTarget", cleanLayoutId)
                addProperty("lastUpdated", System.currentTimeMillis())
            }
            file.writeText(gson.toJson(jsonObject))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
