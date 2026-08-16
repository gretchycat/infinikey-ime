package com.infinikey_ime.eyetracking

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast

/**
 * Interactive Popup Window displaying full numerical calibration results with Copy-to-Clipboard button.
 */
class CalibrationReportActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reportText = intent.getStringExtra("REPORT_TEXT") ?: "No report data available."
        val isSuccess = intent.getBooleanExtra("IS_SUCCESS", false)
        val title = if (isSuccess) "🎉 Calibration Report" else "❌ Calibration Failure Report"

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(reportText)
            .setCancelable(false)
            .setPositiveButton("📋 Copy to Clipboard") { dialog, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Gaze Calibration Report", reportText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Report copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                finish()
            }
            .setNegativeButton("Close") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .show()
    }
}
