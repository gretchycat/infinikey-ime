package com.infinikey_ime.util

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.os.Build
import android.widget.TextView

object FontFallbackManager {
    private var isInitialized = false
    private var symbolTypeface: Typeface? = null
    private var combinedFallbackTypeface: Typeface? = null

    fun init(context: Context) {
        if (isInitialized) return
        try {
            val assets = context.assets
            
            // Load custom merged asset font containing all LCD & symbol glyphs
            symbolTypeface = Typeface.createFromAsset(assets, "fonts/InfinikeySymbols-Regular.ttf")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val font = Font.Builder(assets, "fonts/InfinikeySymbols-Regular.ttf").build()
                    val family = FontFamily.Builder(font).build()
                    
                    combinedFallbackTypeface = Typeface.CustomFallbackBuilder(family)
                        .setSystemFallback("sans-serif")
                        .build()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (combinedFallbackTypeface == null) {
                combinedFallbackTypeface = symbolTypeface
            }
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Returns the primary application-wide Typeface containing font fallbacks.
     */
    fun getAppTypeface(defaultTypeface: Typeface = Typeface.DEFAULT_BOLD): Typeface {
        return combinedFallbackTypeface ?: symbolTypeface ?: defaultTypeface
    }

    /**
     * Resolves the optimal Typeface for rendering [text].
     * If the default typeface lacks a glyph for [text], returns the custom font with glyph support.
     */
    fun getTypefaceForText(text: String, defaultTypeface: Typeface = Typeface.DEFAULT_BOLD): Typeface {
        if (text.isEmpty()) return getAppTypeface(defaultTypeface)
        
        if (combinedFallbackTypeface != null) {
            return combinedFallbackTypeface!!
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && symbolTypeface != null) {
            try {
                val checkPaint = Paint().apply { typeface = defaultTypeface }
                if (!checkPaint.hasGlyph(text)) {
                    checkPaint.typeface = symbolTypeface
                    if (checkPaint.hasGlyph(text)) return symbolTypeface!!
                }
            } catch (_: Exception) {}
        }

        return symbolTypeface ?: defaultTypeface
    }

    /**
     * Helper to apply the appropriate Typeface to a [Paint] object for a given [text].
     */
    fun applyToPaint(paint: Paint, text: String? = null): Paint {
        val targetTypeface = if (text.isNullOrEmpty()) {
            getAppTypeface(paint.typeface ?: Typeface.DEFAULT_BOLD)
        } else {
            getTypefaceForText(text, paint.typeface ?: Typeface.DEFAULT_BOLD)
        }
        paint.typeface = targetTypeface
        return paint
    }

    /**
     * Helper to apply the fallback Typeface to an Android [TextView].
     */
    fun applyToTextView(textView: TextView, text: String? = null) {
        val targetText = text ?: textView.text.toString()
        val currentTypeface = textView.typeface ?: Typeface.DEFAULT_BOLD
        textView.typeface = getTypefaceForText(targetText, currentTypeface)
    }

    /**
     * Recursively applies the fallback Typeface to all [TextView]s, [EditText]s, and [Button]s within a [android.view.View] hierarchy.
     */
    fun applyToView(view: android.view.View?) {
        if (view == null) return
        if (view is TextView) {
            val currentText = view.text?.toString() ?: ""
            view.typeface = getTypefaceForText(currentText, view.typeface ?: Typeface.DEFAULT)
        } else if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                applyToView(view.getChildAt(i))
            }
        }
    }
}
