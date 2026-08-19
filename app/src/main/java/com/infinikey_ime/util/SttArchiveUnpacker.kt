package com.infinikey_ime.util

import android.util.Log
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Utility for automatically uncompressing STT model archives (.zip, .tar.bz2, .tar.gz)
 * placed into the Infinikey IME stt_models directory.
 */
object SttArchiveUnpacker {

    private const val TAG = "SttArchiveUnpacker"

    fun unpackAllArchives(modelsDir: File): Int {
        if (!modelsDir.exists()) return 0
        var extractedCount = 0

        val archives = modelsDir.listFiles()?.filter {
            it.isFile && (it.name.endsWith(".zip", ignoreCase = true) ||
                    it.name.endsWith(".tar.bz2", ignoreCase = true) ||
                    it.name.endsWith(".tar.gz", ignoreCase = true) ||
                    it.name.endsWith(".tgz", ignoreCase = true) ||
                    it.name.endsWith(".tar", ignoreCase = true))
        } ?: emptyList()

        for (archive in archives) {
            try {
                if (archive.name.endsWith(".zip", ignoreCase = true)) {
                    extractedCount += unpackZip(archive, modelsDir)
                } else {
                    extractedCount += unpackTar(archive, modelsDir)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unpack archive ${archive.name}: ${e.message}")
            }
        }
        return extractedCount
    }

    private fun unpackZip(zipFile: File, targetDir: File): Int {
        var count = 0
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                    outFile.setReadable(true, false)
                    outFile.setExecutable(true, false)
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.parentFile?.setReadable(true, false)
                    outFile.parentFile?.setExecutable(true, false)
                    FileOutputStream(outFile).use { fos ->
                        zis.copyTo(fos)
                    }
                    outFile.setReadable(true, false)
                    count++
                }
                entry = zis.nextEntry
            }
        }
        return count
    }

    private fun unpackTar(tarFile: File, targetDir: File): Int {
        return try {
            val pb = ProcessBuilder("tar", "-xf", tarFile.absolutePath, "-C", targetDir.absolutePath)
            val process = pb.start()
            val exitCode = process.waitFor()
            try {
                targetDir.walkTopDown().forEach { file ->
                    file.setReadable(true, false)
                    if (file.isDirectory) file.setExecutable(true, false)
                }
            } catch (_: Exception) {}
            if (exitCode == 0) 1 else 0
        } catch (e: Exception) {
            Log.w(TAG, "Tar command failed for ${tarFile.name}: ${e.message}")
            0
        }
    }
}
