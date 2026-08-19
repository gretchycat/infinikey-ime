package com.infinikey_ime.util

import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.io.File

class AppPreferencesManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockContext: Context
    private lateinit var baseDir: File

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        baseDir = tempFolder.newFolder("files")
        `when`(mockContext.getExternalFilesDir(null)).thenReturn(baseDir)
        `when`(mockContext.filesDir).thenReturn(baseDir)
    }

    @Test
    fun testGetAppPreferencesDirCreatesFolder() {
        val dir = AppPreferencesManager.getAppPreferencesDir(mockContext)
        assertTrue(dir.exists())
        assertTrue(dir.isDirectory)
        assertEquals("app preferences", dir.name)
        assertEquals(baseDir.absolutePath, dir.parentFile?.absolutePath)
    }

    @Test
    fun testSaveAndGetLastLayoutForApp() {
        val pkg = "com.termux"
        assertFalse(AppPreferencesManager.hasAppPreferenceFile(mockContext, pkg))
        assertNull(AppPreferencesManager.getLastLayoutForApp(mockContext, pkg))

        AppPreferencesManager.saveLastLayoutForApp(mockContext, pkg, "function")

        assertTrue(AppPreferencesManager.hasAppPreferenceFile(mockContext, pkg))
        val loaded = AppPreferencesManager.getLastLayoutForApp(mockContext, pkg)
        assertEquals("function", loaded)

        val prefFile = File(AppPreferencesManager.getAppPreferencesDir(mockContext), "$pkg.json")
        assertTrue(prefFile.exists())
        val text = prefFile.readText()
        assertTrue(text.contains("\"layoutTarget\": \"function\""))
    }

    @Test
    fun testGetLastLayoutForAppFromRawTextFile() {
        val pkg = "com.example.editor"
        val dir = AppPreferencesManager.getAppPreferencesDir(mockContext)
        val file = File(dir, "$pkg.json")
        file.writeText("mobile")

        assertTrue(AppPreferencesManager.hasAppPreferenceFile(mockContext, pkg))
        assertEquals("mobile", AppPreferencesManager.getLastLayoutForApp(mockContext, pkg))
    }

    @Test
    fun testGetLastLayoutForAppFromTxtFile() {
        val pkg = "org.mozilla.firefox"
        val dir = AppPreferencesManager.getAppPreferencesDir(mockContext)
        val file = File(dir, "$pkg.txt")
        file.writeText("main.json")

        assertTrue(AppPreferencesManager.hasAppPreferenceFile(mockContext, pkg))
        assertEquals("main", AppPreferencesManager.getLastLayoutForApp(mockContext, pkg))
    }

    @Test
    fun testGetLastLayoutForAppMissingFileReturnsNull() {
        val pkg = "com.nonexistent.app"
        assertFalse(AppPreferencesManager.hasAppPreferenceFile(mockContext, pkg))
        assertNull(AppPreferencesManager.getLastLayoutForApp(mockContext, pkg))
    }

    @Test
    fun testFileDeletionReturnsNull() {
        val pkg = "com.whatsapp"
        AppPreferencesManager.saveLastLayoutForApp(mockContext, pkg, "main")
        assertEquals("main", AppPreferencesManager.getLastLayoutForApp(mockContext, pkg))

        val prefFile = File(AppPreferencesManager.getAppPreferencesDir(mockContext), "$pkg.json")
        prefFile.delete()

        assertFalse(AppPreferencesManager.hasAppPreferenceFile(mockContext, pkg))
        assertNull(AppPreferencesManager.getLastLayoutForApp(mockContext, pkg))
    }
}
