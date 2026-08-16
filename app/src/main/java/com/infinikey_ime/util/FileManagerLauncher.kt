package com.infinikey_ime.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import java.io.File

/**
 * Strict File Manager Launcher for Infinikey IME.
 * EXCLUSIVELY targets the official Android System Files application (com.google.android.documentsui / com.android.documentsui).
 * Never displays an OS intent chooser or allows third-party apps, text editors, or incompatible viewers.
 */
object FileManagerLauncher {

    private const val TAG = "FileManagerLauncher"

    private val SYSTEM_FILES_PACKAGES = listOf(
        "com.google.android.documentsui",
        "com.android.documentsui"
    )

    private val SYSTEM_FILE_COMPONENTS = listOf(
        ComponentName("com.google.android.documentsui", "com.android.documentsui.files.FilesActivity"),
        ComponentName("com.android.documentsui", "com.android.documentsui.files.FilesActivity"),
        ComponentName("com.google.android.documentsui", "com.android.documentsui.LauncherActivity"),
        ComponentName("com.android.documentsui", "com.android.documentsui.LauncherActivity"),
        ComponentName("com.google.android.documentsui", "com.android.documentsui.ViewDownloadsActivity")
    )

    /**
     * Opens a specific app subfolder EXCLUSIVELY in the official System Files Application.
     */
    fun openDirectory(context: Context, subFolder: String, fallbackFile: File? = null): Boolean {
        val packageName = context.packageName
        val cleanSubFolder = subFolder.trim('/').removePrefix("Android/data/$packageName/files/").trim('/')
        val docId = if (cleanSubFolder.isEmpty()) {
            "primary:Android/data/$packageName/files"
        } else {
            "primary:Android/data/$packageName/files/$cleanSubFolder"
        }

        val folderUri = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", docId)

        // 1. Attempt 1: Target System Files Component directly
        for (comp in SYSTEM_FILE_COMPONENTS) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    this.component = comp
                    setDataAndType(folderUri, "vnd.android.document/directory")
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, folderUri)
                    putExtra("android.provider.extra.INITIAL_URI", folderUri)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    Log.d(TAG, "Launched directory using System Files component: ${comp.className}")
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Component launch failed: ${e.message}")
            }
        }

        // 2. Attempt 2: Target System Files Package explicitly (com.google.android.documentsui / com.android.documentsui)
        for (sysPackage in SYSTEM_FILES_PACKAGES) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setPackage(sysPackage)
                    setDataAndType(folderUri, "vnd.android.document/directory")
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, folderUri)
                    putExtra("android.provider.extra.INITIAL_URI", folderUri)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    Log.d(TAG, "Launched directory using System Files package: $sysPackage")
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Package launch failed: ${e.message}")
            }
        }

        // 3. Attempt 3: Target System Files via ACTION_OPEN_DOCUMENT_TREE restricted to System Files Package
        for (sysPackage in SYSTEM_FILES_PACKAGES) {
            try {
                val safIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    setPackage(sysPackage)
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, folderUri)
                    putExtra("android.provider.extra.INITIAL_URI", folderUri)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                if (safIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(safIntent)
                    Log.d(TAG, "Launched directory using System Files SAF tree: $sysPackage")
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "SAF tree launch failed: ${e.message}")
            }
        }

        // NO THIRD PARTY APPS, NO CHOOSER. Display exact directory path in a Toast.
        val displayPath = fallbackFile?.absolutePath ?: docId
        Toast.makeText(context, "Folder Location: $displayPath", Toast.LENGTH_LONG).show()
        return false
    }
}
