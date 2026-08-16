package com.infinikey_ime.engine

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

/**
 * Registry manager for tracking alternate key selection usage counts per layout.
 * Priority files are saved as JSON in:
 * <User Accessible Layouts Directory>/alternate priorities/<layoutId>.json
 */
object AlternatePriorityManager {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val memoryCache: java.util.concurrent.ConcurrentHashMap<String, Map<String, Int>> = java.util.concurrent.ConcurrentHashMap()

    fun getAlternatePrioritiesDir(context: Context): File {
        val layoutsDir = File(context.getExternalFilesDir(null), "layouts")
        val prioritiesDir = File(layoutsDir, "alternate priorities")
        if (!prioritiesDir.exists()) {
            prioritiesDir.mkdirs()
        }
        return prioritiesDir
    }

    fun getPriorityFile(context: Context, layoutId: String): File {
        val cleanId = layoutId.removePrefix("layouts/").removeSuffix(".json")
        val dir = getAlternatePrioritiesDir(context)
        return File(dir, "$cleanId.json")
    }

    @Synchronized
    fun recordAlternateSelection(
        context: Context,
        layoutId: String,
        sourceKeyLabel: String?,
        selectedLabel: String
    ) {
        try {
            val file = getPriorityFile(context, layoutId)
            val jsonObject = if (file.exists()) {
                try {
                    val content = file.readText()
                    JsonParser.parseString(content).asJsonObject
                } catch (_: Exception) {
                    JsonObject()
                }
            } else {
                JsonObject()
            }

            val normalizedOption = if (selectedLabel.length == 1 && selectedLabel[0].isLetter()) {
                selectedLabel.lowercase()
            } else {
                selectedLabel
            }

            val cleanId = layoutId.removePrefix("layouts/").removeSuffix(".json")
            jsonObject.addProperty("layoutId", cleanId)
            jsonObject.addProperty("lastUpdated", System.currentTimeMillis())

            val totalSelections = (jsonObject.get("totalSelections")?.asLong ?: 0L) + 1L
            jsonObject.addProperty("totalSelections", totalSelections)

            val countsObj = if (jsonObject.has("counts") && jsonObject.get("counts").isJsonObject) {
                jsonObject.getAsJsonObject("counts")
            } else {
                JsonObject().also { jsonObject.add("counts", it) }
            }

            val currentCount = countsObj.get(normalizedOption)?.asInt ?: 0
            countsObj.addProperty(normalizedOption, currentCount + 1)

            if (!sourceKeyLabel.isNullOrEmpty()) {
                val keyCountsObj = if (jsonObject.has("keyCounts") && jsonObject.get("keyCounts").isJsonObject) {
                    jsonObject.getAsJsonObject("keyCounts")
                } else {
                    JsonObject().also { jsonObject.add("keyCounts", it) }
                }
                val keyScopeKey = "$sourceKeyLabel:$normalizedOption"
                val currentKeyCount = keyCountsObj.get(keyScopeKey)?.asInt ?: 0
                keyCountsObj.addProperty(keyScopeKey, currentKeyCount + 1)
            }

            file.writeText(gson.toJson(jsonObject))

            // Update in-memory cache
            val updatedMap = mutableMapOf<String, Int>()
            countsObj.entrySet().forEach { entry ->
                updatedMap[entry.key] = entry.value.asInt
            }
            memoryCache[cleanId] = updatedMap
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun getAlternateUsageCounts(context: Context, layoutId: String): Map<String, Int> {
        val cleanId = layoutId.removePrefix("layouts/").removeSuffix(".json")
        memoryCache[cleanId]?.let { return it }

        val file = getPriorityFile(context, layoutId)
        if (!file.exists()) {
            memoryCache[cleanId] = emptyMap()
            return emptyMap()
        }

        return try {
            val content = file.readText()
            val jsonObject = JsonParser.parseString(content).asJsonObject
            val countsObj = jsonObject.getAsJsonObject("counts") ?: return emptyMap()
            val result = mutableMapOf<String, Int>()
            countsObj.entrySet().forEach { entry ->
                result[entry.key] = entry.value.asInt
            }
            memoryCache[cleanId] = result
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
