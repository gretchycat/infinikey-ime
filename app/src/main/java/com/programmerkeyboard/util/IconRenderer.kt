package com.programmerkeyboard.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.programmerkeyboard.R

class IconSpinnerAdapter(
    context: Context,
    private val items: List<Pair<String, String>>
) : ArrayAdapter<Pair<String, String>>(context, 0, items) {

    private fun createItemView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_spinner_icon, parent, false)
        val ivIcon = view.findViewById<ImageView>(R.id.ivSpinnerItemIcon)
        val tvLabel = view.findViewById<TextView>(R.id.tvSpinnerItemLabel)

        val item = items[position]
        tvLabel.text = item.first

        val iconName = item.second
        if (iconName == "custom") {
            ivIcon.visibility = View.VISIBLE
            val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 40f
                textAlign = Paint.Align.CENTER
            }
            val fm = paint.fontMetrics
            val baseline = 32f - (fm.ascent + fm.descent) / 2
            canvas.drawText("📁", 32f, baseline, paint)
            ivIcon.setImageBitmap(bitmap)
        } else if (iconName.isEmpty()) {
            ivIcon.setImageDrawable(null)
            ivIcon.visibility = View.GONE
        } else {
            ivIcon.visibility = View.VISIBLE
            val bitmap = IconRenderer.renderIconToBitmap(
                context,
                iconName,
                fgColor = Color.parseColor("#38BDF8"),
                width = 64,
                height = 64
            )
            if (bitmap != null) {
                ivIcon.setImageBitmap(bitmap)
            } else {
                ivIcon.setImageDrawable(null)
            }
        }
        return view
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createItemView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createItemView(position, convertView, parent)
    }
}

object IconRenderer {

    fun getUserIconsDir(context: Context): java.io.File {
        val layoutsDir = java.io.File(context.getExternalFilesDir(null), "layouts")
        val iconsDir = java.io.File(layoutsDir, "icons")
        if (!iconsDir.exists()) {
            iconsDir.mkdirs()
        }
        // Migrate internal files/icons if any exist
        val legacyInternalDir = java.io.File(context.filesDir, "icons")
        if (legacyInternalDir.exists() && legacyInternalDir.isDirectory) {
            legacyInternalDir.listFiles()?.forEach { file ->
                val destFile = java.io.File(iconsDir, file.name)
                if (!destFile.exists()) {
                    try { file.copyTo(destFile) } catch (_: Exception) {}
                }
            }
        }
        return iconsDir
    }

    fun saveUserIcon(context: Context, sourceUri: Uri): String? {
        return try {
            val dir = getUserIconsDir(context)
            val fileName = "user_icon_${System.currentTimeMillis()}.png"
            val targetFile = java.io.File(dir, fileName)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                java.io.FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getUserCustomIcons(context: Context): List<Pair<String, String>> {
        val dir = getUserIconsDir(context)
        val files = dir.listFiles() ?: return emptyList()
        return files.filter { it.isFile && (it.name.endsWith(".png") || it.name.endsWith(".jpg") || it.name.endsWith(".jpeg") || it.name.endsWith(".svg")) }
            .sortedByDescending { it.lastModified() }
            .map { file ->
                Pair("User Icon: ${file.name}", file.absolutePath)
            }
    }

    fun renderIconToBitmap(
        context: Context,
        iconName: String?,
        fgColor: Int = Color.parseColor("#38BDF8"),
        width: Int = 80,
        height: Int = 80
    ): Bitmap? {
        if (iconName.isNullOrEmpty()) return null

        val resolvedPath = if (iconName.startsWith("icons/")) {
            java.io.File(getUserIconsDir(context), iconName.removePrefix("icons/")).absolutePath
        } else {
            iconName
        }

        if (resolvedPath.startsWith("content://") || resolvedPath.startsWith("file://") || resolvedPath.startsWith("/")) {
            return try {
                val uri = Uri.parse(resolvedPath)
                if (resolvedPath.startsWith("content://")) {
                    context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) }
                } else {
                    val filePath = if (resolvedPath.startsWith("file://")) resolvedPath.substring(7) else resolvedPath
                    android.graphics.BitmapFactory.decodeFile(filePath)
                }
            } catch (_: Exception) {
                null
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fgColor
        }
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        val drawSuccess = drawVectorIcon(canvas, resolvedPath, rect, paint)
        return if (drawSuccess) bitmap else null
    }

    fun drawVectorIcon(canvas: Canvas, iconName: String, rect: RectF, paint: Paint): Boolean {
        val cleanName = iconName.lowercase().trim()
        val iconSize = minOf(rect.width(), rect.height()) * 0.55f

        return when (cleanName) {
            "mic", "microphone", "voice", "mic.svg", "assets/images/mic.svg" -> {
                drawMic(canvas, rect, paint, iconSize)
                true
            }
            "tts", "read_text", "speech", "tts.svg", "assets/images/tts.svg" -> {
                drawTts(canvas, rect, paint, iconSize)
                true
            }
            "paperclip", "clip", "paperclip.svg", "assets/images/paperclip.svg" -> {
                drawPaperclip(canvas, rect, paint, iconSize)
                true
            }
            "clipboard", "clipboard_history", "clipboard.svg", "assets/images/clipboard.svg" -> {
                drawClipboard(canvas, rect, paint, iconSize)
                true
            }
            "copy", "copy.svg", "assets/images/copy.svg" -> {
                drawCopy(canvas, rect, paint, iconSize)
                true
            }
            "cut", "cut.svg", "assets/images/cut.svg" -> {
                drawCut(canvas, rect, paint, iconSize)
                true
            }
            "paste", "paste.svg", "assets/images/paste.svg" -> {
                drawPaste(canvas, rect, paint, iconSize)
                true
            }
            "select_all", "select_all.svg", "assets/images/select_all.svg" -> {
                drawSelectAll(canvas, rect, paint, iconSize)
                true
            }
            "keyboard" -> {
                drawKeyboard(canvas, rect, paint, iconSize)
                true
            }
            else -> false
        }
    }

    private fun drawMic(canvas: Canvas, rect: RectF, paint: Paint, iconSize: Float) {
        val scale = iconSize / 24f
        val offsetX = rect.centerX() - (iconSize / 2f)
        val offsetY = rect.centerY() - (iconSize / 2f)

        val saveCount = canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paint.color
            style = Paint.Style.STROKE
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        canvas.drawRoundRect(RectF(9f, 3f, 15f, 14f), 3f, 3f, strokePaint)
        canvas.drawArc(RectF(5f, 4f, 19f, 18f), 0f, 180f, false, strokePaint)
        canvas.drawLine(12f, 18f, 12f, 21f, strokePaint)
        canvas.drawLine(9f, 21f, 15f, 21f, strokePaint)

        canvas.restoreToCount(saveCount)
    }

    private fun drawTts(canvas: Canvas, rect: RectF, paint: Paint, iconSize: Float) {
        val scale = iconSize / 100f
        val iconW = 110f * scale
        val offsetX = rect.centerX() - (iconW / 2f)
        val offsetY = rect.centerY() - (iconSize / 2f)

        val saveCount = canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paint.color
            style = Paint.Style.STROKE
            strokeWidth = 8f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val ttsPath = Path().apply {
            moveTo(22f, 85f)
            lineTo(22f, 62f)
            cubicTo(7f, 48f, 7f, 22f, 25f, 12f)
            cubicTo(45f, 2f, 64f, 16f, 64f, 35f)
            lineTo(69f, 50f)
            lineTo(62f, 50f)
            lineTo(62f, 56f)
            lineTo(53f, 58f)
            lineTo(62f, 64f)
            lineTo(62f, 70f)
            lineTo(47f, 70f)
            lineTo(47f, 85f)
            close()
        }
        canvas.drawPath(ttsPath, strokePaint)

        canvas.drawLine(70f, 56f, 84f, 48f, strokePaint)
        canvas.drawLine(71f, 60f, 86f, 60f, strokePaint)
        canvas.drawLine(70f, 64f, 84f, 72f, strokePaint)

        canvas.restoreToCount(saveCount)
    }

    private fun drawPaperclip(canvas: Canvas, rect: RectF, paint: Paint, iconSize: Float) {
        val scale = iconSize / 24f
        val offsetX = rect.centerX() - (iconSize / 2f)
        val offsetY = rect.centerY() - (iconSize / 2f)

        val saveCount = canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paint.color
            style = Paint.Style.STROKE
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val clipPath = Path().apply {
            moveTo(21.44f, 11.05f)
            lineTo(12.25f, 20.24f)
            cubicTo(9.9f, 22.59f, 6.1f, 22.59f, 3.76f, 20.24f)
            cubicTo(1.41f, 17.89f, 1.41f, 14.09f, 3.76f, 11.75f)
            lineTo(12.33f, 3.18f)
            cubicTo(13.89f, 1.62f, 16.42f, 1.62f, 17.99f, 3.18f)
            cubicTo(19.55f, 4.74f, 19.55f, 7.27f, 17.99f, 8.84f)
            lineTo(9.4f, 17.42f)
            cubicTo(8.62f, 18.2f, 7.35f, 18.2f, 6.57f, 17.42f)
            cubicTo(5.79f, 16.64f, 5.79f, 15.37f, 6.57f, 14.59f)
            lineTo(14.45f, 6.71f)
        }
        canvas.drawPath(clipPath, strokePaint)
        canvas.restoreToCount(saveCount)
    }

    private fun drawClipboard(canvas: Canvas, rect: RectF, paint: Paint, iconSize: Float) {
        val scale = iconSize / 24f
        val offsetX = rect.centerX() - (iconSize / 2f)
        val offsetY = rect.centerY() - (iconSize / 2f)

        val saveCount = canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paint.color
            style = Paint.Style.STROKE
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        canvas.drawRoundRect(RectF(8f, 2f, 16f, 6f), 1f, 1f, strokePaint)
        val boardPath = Path().apply {
            moveTo(16f, 4f)
            lineTo(18f, 4f)
            cubicTo(19.1f, 4f, 20f, 4.9f, 20f, 6f)
            lineTo(20f, 20f)
            cubicTo(20f, 21.1f, 19.1f, 22f, 18f, 22f)
            lineTo(6f, 22f)
            cubicTo(4.9f, 22f, 4f, 21.1f, 4f, 20f)
            lineTo(4f, 6f)
            cubicTo(4f, 4.9f, 4.9f, 4f, 6f, 4f)
            lineTo(8f, 4f)
        }
        canvas.drawPath(boardPath, strokePaint)
        canvas.drawLine(12f, 11f, 16f, 11f, strokePaint)
        canvas.drawLine(12f, 16f, 16f, 16f, strokePaint)
        canvas.drawLine(8f, 11f, 8.1f, 11f, strokePaint)
        canvas.drawLine(8f, 16f, 8.1f, 16f, strokePaint)

        canvas.restoreToCount(saveCount)
    }

    private fun drawCopy(canvas: Canvas, rect: RectF, paint: Paint, iconSize: Float) {
        val scale = iconSize / 24f
        val offsetX = rect.centerX() - (iconSize / 2f)
        val offsetY = rect.centerY() - (iconSize / 2f)

        val saveCount = canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paint.color
            style = Paint.Style.STROKE
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        canvas.drawRoundRect(RectF(8f, 8f, 22f, 22f), 2f, 2f, strokePaint)
        val backCard = Path().apply {
            moveTo(4f, 16f)
            cubicTo(2.9f, 16f, 2f, 15.1f, 2f, 14f)
            lineTo(2f, 4f)
            cubicTo(2f, 2.9f, 2.9f, 2f, 4f, 2f)
            lineTo(14f, 2f)
            cubicTo(15.1f, 2f, 16f, 2.9f, 16f, 4f)
        }
        canvas.drawPath(backCard, strokePaint)
        canvas.restoreToCount(saveCount)
    }

    private fun drawCut(canvas: Canvas, rect: RectF, paint: Paint, iconSize: Float) {
        val scale = iconSize / 24f
        val offsetX = rect.centerX() - (iconSize / 2f)
        val offsetY = rect.centerY() - (iconSize / 2f)

        val saveCount = canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paint.color
            style = Paint.Style.STROKE
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        canvas.drawCircle(6f, 6f, 3f, strokePaint)
        canvas.drawCircle(6f, 18f, 3f, strokePaint)
        canvas.drawLine(20f, 4f, 8.12f, 15.88f, strokePaint)
        canvas.drawLine(14.47f, 14.48f, 20f, 20f, strokePaint)
        canvas.drawLine(8.12f, 8.12f, 12f, 12f, strokePaint)

        canvas.restoreToCount(saveCount)
    }

    private fun drawPaste(canvas: Canvas, rect: RectF, paint: Paint, iconSize: Float) {
        val scale = iconSize / 24f
        val offsetX = rect.centerX() - (iconSize / 2f)
        val offsetY = rect.centerY() - (iconSize / 2f)

        val saveCount = canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paint.color
            style = Paint.Style.STROKE
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val boardPath = Path().apply {
            moveTo(16f, 4f)
            lineTo(18f, 4f)
            cubicTo(19.1f, 4f, 20f, 4.9f, 20f, 6f)
            lineTo(20f, 20f)
            cubicTo(20f, 21.1f, 19.1f, 22f, 18f, 22f)
            lineTo(6f, 22f)
            cubicTo(4.9f, 22f, 4f, 21.1f, 4f, 20f)
            lineTo(4f, 6f)
            cubicTo(4f, 4.9f, 4.9f, 4f, 6f, 4f)
            lineTo(8f, 4f)
        }
        canvas.drawPath(boardPath, strokePaint)
        canvas.drawRoundRect(RectF(8f, 2f, 16f, 6f), 1f, 1f, strokePaint)

        canvas.drawLine(12f, 11f, 12f, 17f, strokePaint)
        val arrowHead = Path().apply {
            moveTo(9f, 14f)
            lineTo(12f, 17f)
            lineTo(15f, 14f)
        }
        canvas.drawPath(arrowHead, strokePaint)

        canvas.restoreToCount(saveCount)
    }

    private fun drawSelectAll(canvas: Canvas, rect: RectF, paint: Paint, iconSize: Float) {
        val scale = iconSize / 24f
        val offsetX = rect.centerX() - (iconSize / 2f)
        val offsetY = rect.centerY() - (iconSize / 2f)

        val saveCount = canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paint.color
            style = Paint.Style.STROKE
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        canvas.drawLine(3f, 3f, 7f, 3f, strokePaint)
        canvas.drawLine(3f, 3f, 3f, 7f, strokePaint)

        canvas.drawLine(21f, 3f, 17f, 3f, strokePaint)
        canvas.drawLine(21f, 3f, 21f, 7f, strokePaint)

        canvas.drawLine(3f, 21f, 7f, 21f, strokePaint)
        canvas.drawLine(3f, 21f, 3f, 17f, strokePaint)

        canvas.drawLine(21f, 21f, 17f, 21f, strokePaint)
        canvas.drawLine(21f, 21f, 21f, 17f, strokePaint)

        canvas.drawRoundRect(RectF(8f, 8f, 16f, 16f), 1f, 1f, strokePaint)

        canvas.restoreToCount(saveCount)
    }

    private fun drawKeyboard(canvas: Canvas, rect: RectF, paint: Paint, iconSize: Float) {
        val p = Paint(paint).apply {
            textSize = iconSize * 1.3f
            textAlign = Paint.Align.CENTER
        }
        val fm = p.fontMetrics
        val bl = rect.centerY() - (fm.ascent + fm.descent) / 2
        canvas.drawText("⌨", rect.centerX(), bl, p)
    }
}
