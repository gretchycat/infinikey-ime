package com.infinikey_ime.util

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.util.Log
import com.infinikey_ime.R
import java.io.File
import java.io.FileNotFoundException

/**
 * DocumentsProvider implementation for Infinikey IME.
 * Registers Infinikey IME's internal app files directory (stt_models, layouts, themes)
 * as a visible storage root in the official Android System Files application navigation drawer.
 */
class InfinikeyDocumentsProvider : DocumentsProvider() {

    companion object {
        private const val TAG = "InfinikeyDocProvider"
        private const val ROOT_ID = "infinikey_root"

        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_ICON,
            DocumentsContract.Root.COLUMN_SUMMARY
        )

        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_SIZE
        )
    }

    private fun getBaseDir(): File {
        val context = context ?: throw IllegalStateException("Context is null")
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)
        val baseDir = getBaseDir()

        result.newRow().apply {
            add(DocumentsContract.Root.COLUMN_ROOT_ID, ROOT_ID)
            add(
                DocumentsContract.Root.COLUMN_FLAGS,
                DocumentsContract.Root.FLAG_SUPPORTS_CREATE or
                        DocumentsContract.Root.FLAG_SUPPORTS_RECENTS or
                        DocumentsContract.Root.FLAG_SUPPORTS_SEARCH
            )
            add(DocumentsContract.Root.COLUMN_TITLE, "Infinikey IME Storage")
            add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, getDocIdForFile(baseDir))
            add(DocumentsContract.Root.COLUMN_ICON, R.mipmap.ic_launcher)
            add(DocumentsContract.Root.COLUMN_SUMMARY, "STT Models, Layouts & Themes")
        }
        return result
    }

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val file = getFileForDocId(documentId)
        includeFile(result, documentId, file)
        return result
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val parent = getFileForDocId(parentDocumentId)
        parent.listFiles()?.forEach { file ->
            includeFile(result, null, file)
        }
        return result
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        val file = getFileForDocId(documentId)
        val accessMode = ParcelFileDescriptor.parseMode(mode)
        return ParcelFileDescriptor.open(file, accessMode)
    }

    override fun createDocument(
        parentDocumentId: String,
        mimeType: String,
        displayName: String
    ): String {
        val parent = getFileForDocId(parentDocumentId)
        val file = File(parent, displayName)
        if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
            file.mkdirs()
        } else {
            file.createNewFile()
        }
        return getDocIdForFile(file)
    }

    override fun deleteDocument(documentId: String) {
        val file = getFileForDocId(documentId)
        if (!file.delete()) {
            throw FileNotFoundException("Failed to delete file for documentId: $documentId")
        }
    }

    private fun getDocIdForFile(file: File): String {
        val baseDir = getBaseDir()
        var path = file.absolutePath
        val basePath = baseDir.absolutePath
        if (path == basePath) return ROOT_ID
        if (path.startsWith(basePath)) {
            path = path.substring(basePath.length).trim('/')
        }
        return path
    }

    private fun getFileForDocId(docId: String): File {
        val baseDir = getBaseDir()
        if (docId == ROOT_ID || docId.isEmpty()) return baseDir
        return File(baseDir, docId)
    }

    private fun includeFile(result: MatrixCursor, docId: String?, file: File) {
        val id = docId ?: getDocIdForFile(file)
        var flags = 0
        if (file.isDirectory && file.canWrite()) {
            flags = flags or DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE
        }
        if (file.canWrite()) {
            flags = flags or DocumentsContract.Document.FLAG_SUPPORTS_WRITE
            flags = flags or DocumentsContract.Document.FLAG_SUPPORTS_DELETE
        }

        val mimeType = if (file.isDirectory) {
            DocumentsContract.Document.MIME_TYPE_DIR
        } else {
            getTypeForName(file.name)
        }

        result.newRow().apply {
            add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, id)
            add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, file.name)
            add(DocumentsContract.Document.COLUMN_SIZE, file.length())
            add(DocumentsContract.Document.COLUMN_MIME_TYPE, mimeType)
            add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, file.lastModified())
            add(DocumentsContract.Document.COLUMN_FLAGS, flags)
        }
    }

    private fun getTypeForName(name: String): String {
        return when {
            name.endsWith(".json", ignoreCase = true) -> "application/json"
            name.endsWith(".onnx", ignoreCase = true) -> "application/octet-stream"
            name.endsWith(".gguf", ignoreCase = true) -> "application/octet-stream"
            name.endsWith(".bin", ignoreCase = true) -> "application/octet-stream"
            name.endsWith(".zip", ignoreCase = true) -> "application/zip"
            name.endsWith(".tar.bz2", ignoreCase = true) -> "application/x-bzip2"
            name.endsWith(".tar.gz", ignoreCase = true) -> "application/gzip"
            else -> "application/octet-stream"
        }
    }
}
