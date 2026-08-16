package com.infinikey_ime.stt

import android.content.Context

/**
 * Bidirectional Profanity & Obscenity Processor for Speech-to-Text.
 * Handles both masking profanity (when filter is ON) and unmasking OS-censored asterisks (when filter is OFF).
 */
object ProfanityFilter {

    private val wordMap = listOf(
        Pair(Regex("(?i)\\bf[\\*#@!]+ing\\b"), Triple("fucking", "Fucking", "FUCKING")),
        Pair(Regex("(?i)\\bf[\\*#@!]+er\\b"), Triple("fucker", "Fucker", "FUCKER")),
        Pair(Regex("(?i)\\bf[\\*#@!]+ed\\b"), Triple("fucked", "Fucked", "FUCKED")),
        Pair(Regex("(?i)\\bf[\\*#@!]+k?\\b"), Triple("fuck", "Fuck", "FUCK")),
        Pair(Regex("(?i)\\bs[\\*#@!]+ting\\b"), Triple("shitting", "Shitting", "SHITTING")),
        Pair(Regex("(?i)\\bs[\\*#@!]+ty\\b"), Triple("shitty", "Shitty", "SHITTY")),
        Pair(Regex("(?i)\\bs[\\*#@!]+t?\\b"), Triple("shit", "Shit", "SHIT")),
        Pair(Regex("(?i)\\bb[\\*#@!]+h\\b"), Triple("bitch", "Bitch", "BITCH")),
        Pair(Regex("(?i)\\bb[\\*#@!]{3,6}\\b"), Triple("bitch", "Bitch", "BITCH")),
        Pair(Regex("(?i)\\ba[\\*#@!]+hole\\b"), Triple("asshole", "Asshole", "ASSHOLE")),
        Pair(Regex("(?i)\\ba[\\*#@!]{3,8}\\b"), Triple("asshole", "Asshole", "ASSHOLE")),
        Pair(Regex("(?i)\\bc[\\*#@!]+t\\b"), Triple("cunt", "Cunt", "CUNT")),
        Pair(Regex("(?i)\\bc[\\*#@!]{2,5}\\b"), Triple("cunt", "Cunt", "CUNT")),
        Pair(Regex("(?i)\\bd[\\*#@!]+k\\b"), Triple("dick", "Dick", "DICK")),
        Pair(Regex("(?i)\\bd[\\*#@!]{2,5}\\b"), Triple("dick", "Dick", "DICK")),
        Pair(Regex("(?i)\\bp[\\*#@!]+y\\b"), Triple("pussy", "Pussy", "PUSSY")),
        Pair(Regex("(?i)\\bp[\\*#@!]{3,6}\\b"), Triple("pussy", "Pussy", "PUSSY")),
        Pair(Regex("(?i)\\bm[\\*#@!]+f[\\*#@!]+\\b"), Triple("motherfucker", "Motherfucker", "MOTHERFUCKER")),
        Pair(Regex("(?i)\\bm[\\*#@!]{6,14}\\b"), Triple("motherfucker", "Motherfucker", "MOTHERFUCKER")),
        Pair(Regex("(?i)\\bb[\\*#@!]+s[\\*#@!]+\\b"), Triple("bullshit", "Bullshit", "BULLSHIT")),
        Pair(Regex("(?i)\\bb[\\*#@!]{5,10}\\b"), Triple("bullshit", "Bullshit", "BULLSHIT")),
        Pair(Regex("(?i)\\bb[\\*#@!]+ard\\b"), Triple("bastard", "Bastard", "BASTARD")),
        Pair(Regex("(?i)\\bd[\\*#@!]+n\\b"), Triple("damn", "Damn", "DAMN"))
    )

    private val rawProfanities = listOf(
        "motherfucker", "motherfucking", "bullshit", "asshole", "bastard",
        "fucking", "fucker", "fucked", "bitchy", "shitting", "shitty",
        "fuck", "shit", "bitch", "cunt", "dick", "pussy", "damn"
    )

    fun isSystemProfanityFilterEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("programmer_keyboard_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("pref_stt_filter_profanity", false)
    }

    /**
     * Main entry point to process STT output based on user's preference setting.
     */
    fun processText(context: Context, text: String): String {
        if (text.isEmpty()) return text
        val shouldMaskProfanity = isSystemProfanityFilterEnabled(context)

        android.util.Log.d("STT_DEBUG", "ProfanityFilter.processText IN: '$text' (filterProfanity=$shouldMaskProfanity)")

        val result = if (shouldMaskProfanity) {
            maskProfanity(text)
        } else {
            unmaskCensoredWords(text)
        }

        android.util.Log.d("STT_DEBUG", "ProfanityFilter.processText OUT: '$result'")
        return result
    }

    /**
     * Unmasks OS-censored asterisks (e.g. "f***" -> "fuck", "s***" -> "shit") when profanity filtering is turned OFF.
     */
    fun unmaskCensoredWords(input: String): String {
        var result = input
        for ((regex, replacements) in wordMap) {
            result = regex.replace(result) { matchResult ->
                val matchedText = matchResult.value
                when {
                    matchedText.all { it.isUpperCase() || !it.isLetter() } -> replacements.third
                    matchedText.firstOrNull()?.isUpperCase() == true -> replacements.second
                    else -> replacements.first
                }
            }
        }
        return result
    }

    /**
     * Masks raw profanities with asterisks (e.g. "fuck" -> "f***") when profanity filtering is turned ON.
     */
    fun maskProfanity(input: String): String {
        var result = input
        for (word in rawProfanities) {
            val regex = Regex("(?i)\\b$word\\b")
            result = regex.replace(result) { matchResult ->
                val matched = matchResult.value
                val firstChar = matched.first()
                firstChar + "*".repeat(matched.length - 1)
            }
        }
        return result
    }
}
