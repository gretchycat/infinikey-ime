package com.infinikey_ime.util

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache

object LauncherPreferencesManager {

    private const val PREF_NAME = "infinikey_launcher_prefs"
    private val iconCache = LruCache<String, Bitmap>(60)
    private val labelCache = mutableMapOf<String, String>()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getAppForSlot(context: Context, slotId: String): String? {
        if (slotId.isBlank()) return null
        return getPrefs(context).getString("slot_$slotId", null)
    }

    fun saveAppForSlot(context: Context, slotId: String, packageName: String) {
        if (slotId.isBlank()) return
        getPrefs(context).edit().putString("slot_$slotId", packageName.trim()).apply()
    }

    fun clearAppForSlot(context: Context, slotId: String) {
        if (slotId.isBlank()) return
        getPrefs(context).edit().remove("slot_$slotId").apply()
    }

    fun getAppIconBitmap(context: Context, packageName: String): Bitmap? {
        if (packageName.isBlank()) return null
        val cached = iconCache.get(packageName)
        if (cached != null && !cached.isRecycled) return cached

        return try {
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(packageName)
            val bitmap = drawableToBitmap(drawable)
            if (bitmap != null) {
                iconCache.put(packageName, bitmap)
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    fun getAppLabel(context: Context, packageName: String): String {
        if (packageName.isBlank()) return ""
        labelCache[packageName]?.let { return it }

        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val label = pm.getApplicationLabel(appInfo).toString()
            labelCache[packageName] = label
            label
        } catch (e: Exception) {
            packageName
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
            return drawable.bitmap
        }

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
