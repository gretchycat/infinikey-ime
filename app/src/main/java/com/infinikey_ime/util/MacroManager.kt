package com.infinikey_ime.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.infinikey_ime.model.KeyAction

/**
 * Manager for macro key recording, persistence, and execution.
 */
object MacroManager {

    private const val PREFS_NAME = "programmer_keyboard_prefs"
    private const val MACRO_PREF_PREFIX = "pref_macro_sequence_"
    private val gson = Gson()

    var recordingMacroId: String? = null
        private set

    val recordingBuffer = mutableListOf<KeyAction>()

    fun isRecordingAny(): Boolean = recordingMacroId != null

    fun isRecording(macroId: String): Boolean = recordingMacroId == macroId

    fun startRecording(context: Context, macroId: String): Pair<String, Int>? {
        var savedPrevious: Pair<String, Int>? = null
        if (isRecordingAny()) {
            savedPrevious = stopRecording(context)
        }
        recordingMacroId = macroId
        recordingBuffer.clear()
        return savedPrevious
    }

    fun stopRecording(context: Context): Pair<String, Int>? {
        val macroId = recordingMacroId ?: return null
        val steps = recordingBuffer.toList()
        saveMacro(context, macroId, steps)
        recordingMacroId = null
        recordingBuffer.clear()
        return Pair(macroId, steps.size)
    }

    fun cancelRecording() {
        recordingMacroId = null
        recordingBuffer.clear()
    }

    fun recordAction(action: KeyAction) {
        if (!isRecordingAny()) return
        // Do not record nested Macro actions into the macro buffer
        if (action is KeyAction.Macro) return
        recordingBuffer.add(action)
    }

    fun saveMacro(context: Context, macroId: String, steps: List<KeyAction>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JsonArray()
        for (step in steps) {
            val obj = serializeActionToJson(step)
            if (obj != null) {
                array.add(obj)
            }
        }
        prefs.edit().putString("$MACRO_PREF_PREFIX$macroId", gson.toJson(array)).apply()
    }

    fun getMacro(context: Context, macroId: String): List<KeyAction>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("$MACRO_PREF_PREFIX$macroId", null) ?: return null
        return try {
            val array = gson.fromJson(jsonStr, JsonArray::class.java) ?: return null
            val result = mutableListOf<KeyAction>()
            for (elem in array) {
                if (elem.isJsonObject) {
                    val action = parseJsonToAction(elem.asJsonObject)
                    if (action !is KeyAction.None) {
                        result.add(action)
                    }
                }
            }
            if (result.isEmpty()) null else result
        } catch (_: Exception) {
            null
        }
    }

    fun hasMacro(context: Context, macroId: String): Boolean {
        val steps = getMacro(context, macroId)
        return !steps.isNullOrEmpty()
    }

    private fun serializeActionToJson(action: KeyAction): JsonObject? {
        val obj = JsonObject()
        when (action) {
            is KeyAction.SendText -> {
                obj.addProperty("type", "SEND_TEXT")
                obj.addProperty("text", action.text)
            }
            is KeyAction.SendCode -> {
                obj.addProperty("type", "SEND_CODE")
                obj.addProperty("code", action.code)
            }
            is KeyAction.ToggleModifier -> {
                obj.addProperty("type", "TOGGLE_MODIFIER")
                obj.addProperty("modifier", action.modifier)
            }
            is KeyAction.LockModifier -> {
                obj.addProperty("type", "LOCK_MODIFIER")
                obj.addProperty("modifier", action.modifier)
            }
            is KeyAction.SwitchLayout -> {
                obj.addProperty("type", "SWITCH_LAYOUT")
                obj.addProperty("target", action.target)
            }
            is KeyAction.SelectAll -> obj.addProperty("type", "SELECT_ALL")
            is KeyAction.Copy -> obj.addProperty("type", "COPY")
            is KeyAction.Cut -> obj.addProperty("type", "CUT")
            is KeyAction.Paste -> obj.addProperty("type", "PASTE")
            is KeyAction.PasteEcho -> obj.addProperty("type", "PASTE_ECHO")
            else -> return null
        }
        return obj
    }

    private fun parseJsonToAction(obj: JsonObject): KeyAction {
        val type = obj.get("type")?.asString ?: return KeyAction.None
        return when (type) {
            "SEND_TEXT" -> KeyAction.SendText(obj.get("text")?.asString ?: "")
            "SEND_CODE" -> KeyAction.SendCode(obj.get("code")?.asInt ?: 0)
            "TOGGLE_MODIFIER" -> KeyAction.ToggleModifier(obj.get("modifier")?.asString ?: "SHIFT")
            "LOCK_MODIFIER" -> KeyAction.LockModifier(obj.get("modifier")?.asString ?: "SHIFT")
            "SWITCH_LAYOUT" -> KeyAction.SwitchLayout(obj.get("target")?.asString ?: "main")
            "SELECT_ALL" -> KeyAction.SelectAll
            "COPY" -> KeyAction.Copy
            "CUT" -> KeyAction.Cut
            "PASTE" -> KeyAction.Paste
            "PASTE_ECHO" -> KeyAction.PasteEcho
            else -> KeyAction.None
        }
    }
}
