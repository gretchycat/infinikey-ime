package com.infinikey_ime.util

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

/**
 * Manager for saving and loading app-specific layout & row visibility preferences.
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
     * Reads the saved row visibility map for a given package name if present in app preferences.
     */
    fun getRowVisibilityForApp(context: Context, packageName: String): Map<String, Boolean> {
        if (packageName.isBlank()) return emptyMap()
        val dir = getAppPreferencesDir(context)
        val candidates = listOf(
            File(dir, "$packageName.json"),
            File(dir, "$packageName.txt"),
            File(dir, packageName)
        )
        val file = candidates.firstOrNull { it.exists() && it.isFile && it.length() > 0 } ?: return emptyMap()

        return try {
            val content = file.readText().trim()
            if (content.isEmpty() || !content.startsWith("{")) return emptyMap()

            val json = JsonParser.parseString(content).asJsonObject
            val rowVisObj = json.getAsJsonObject("rowVisibility") ?: return emptyMap()
            val result = mutableMapOf<String, Boolean>()
            for ((key, value) in rowVisObj.entrySet()) {
                if (value.isJsonPrimitive && value.asJsonPrimitive.isBoolean) {
                    result[key] = value.asBoolean
                }
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    /**
     * Saves app preferences (layout target and/or row visibility) for a given package name.
     */
    fun saveAppPreferences(
        context: Context,
        packageName: String,
        layoutId: String? = null,
        rowVisibility: Map<String, Boolean>? = null
    ) {
        if (packageName.isBlank()) return
        try {
            val dir = getAppPreferencesDir(context)
            val file = File(dir, "$packageName.json")
            val existingObj = if (file.exists() && file.length() > 0) {
                try {
                    val content = file.readText().trim()
                    if (content.startsWith("{")) JsonParser.parseString(content).asJsonObject else JsonObject()
                } catch (_: Exception) { JsonObject() }
            } else {
                JsonObject()
            }

            if (!layoutId.isNullOrBlank()) {
                val cleanLayoutId = layoutId.removeSuffix(".json")
                existingObj.addProperty("layoutTarget", cleanLayoutId)
            }

            if (rowVisibility != null) {
                val rowVisJson = JsonObject()
                rowVisibility.forEach { (k, v) ->
                    rowVisJson.addProperty(k, v)
                }
                existingObj.add("rowVisibility", rowVisJson)
            }

            existingObj.addProperty("lastUpdated", System.currentTimeMillis())
            file.writeText(gson.toJson(existingObj))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Saves the last layout used for a given package name into app preferences directory.
     */
    fun saveLastLayoutForApp(context: Context, packageName: String, layoutId: String) {
        saveAppPreferences(context, packageName, layoutId = layoutId)
    }

    /**
     * Saves the row visibility map for a given package name into app preferences directory.
     */
    fun saveRowVisibilityForApp(context: Context, packageName: String, rowVisibility: Map<String, Boolean>) {
        saveAppPreferences(context, packageName, rowVisibility = rowVisibility)
    }
}
