package com.infinikey_ime.model

import com.infinikey_ime.engine.LayoutParser
import org.junit.Assert.*
import org.junit.Test

class AccessorySpaceLayoutTest {

    @Test
    fun testLayoutMetadataAccessoryField() {
        val metadata = LayoutMetadata(accessoryLayout = "mobile_number")
        assertEquals("mobile_number", metadata.accessoryLayout)
        assertEquals("mobile_number", metadata.effectiveAccessoryLayout)
    }

    @Test
    fun testJsonParsingAccessoryLayout() {
        val json = """
            {
                "id": "test_layout",
                "name": "Test Layout",
                "version": "1.0",
                "metadata": {
                    "accessoryLayout": "function"
                },
                "rows": []
            }
        """.trimIndent()

        val parsed = LayoutParser.parseJsonLayoutDescriptor(json)
        assertEquals("function", parsed.metadata.accessoryLayout)
        assertEquals("function", parsed.metadata.effectiveAccessoryLayout)
    }

    @Test
    fun testIdealWidthFittingCondition() {
        // Numpad layout simulation: 4 columns, 4 rows, available height = 200f
        // dsRowHeight = (200 - 5 * 4) / 4 = 45f
        // idealWidth = 4.0 * 45f + 5 * 4f = 200f
        val idealWidthNumpad = 200f

        val accessoryAvailableNarrow = 150f
        val accessoryAvailableWide = 300f
        val accessoryExactIdeal = 200f

        assertFalse("Narrow accessory space (< ideal layout width) must not fit layout", accessoryAvailableNarrow >= idealWidthNumpad)
        assertTrue("Wide accessory space (>= ideal layout width) must fit layout", accessoryAvailableWide >= idealWidthNumpad)
        assertTrue("Exact ideal accessory space must fit layout", accessoryExactIdeal >= idealWidthNumpad)
    }

    @Test
    fun testBlankAccessoryLayoutValidation() {
        val metadataNone = LayoutMetadata(accessoryLayout = "none")
        val metadataBlank = LayoutMetadata(accessoryLayout = "")
        val metadataNull = LayoutMetadata(accessoryLayout = null)

        assertEquals("none", metadataNone.accessoryLayout)
        assertNull(metadataBlank.effectiveAccessoryLayout)
        assertNull(metadataNull.effectiveAccessoryLayout)
    }

    @Test
    fun testPerLayoutAccessoryDesignation() {
        val mainJson = """
            {
                "id": "main",
                "name": "Main Layout",
                "version": "1.0",
                "metadata": { "accessoryLayout": "mobile_number" },
                "rows": []
            }
        """.trimIndent()

        val symbolJson = """
            {
                "id": "mobile_symbol",
                "name": "Symbol Layout",
                "version": "1.0",
                "metadata": { "accessoryLayout": "function" },
                "rows": []
            }
        """.trimIndent()

        val phoneJson = """
            {
                "id": "phone",
                "name": "Phone Layout",
                "version": "1.0",
                "metadata": { "accessoryLayout": "none" },
                "rows": []
            }
        """.trimIndent()

        val mainDef = LayoutParser.parseJsonLayoutDescriptor(mainJson)
        val symbolDef = LayoutParser.parseJsonLayoutDescriptor(symbolJson)
        val phoneDef = LayoutParser.parseJsonLayoutDescriptor(phoneJson)

        assertEquals("mobile_number", mainDef.metadata.effectiveAccessoryLayout)
        assertEquals("function", symbolDef.metadata.effectiveAccessoryLayout)
        assertEquals("none", phoneDef.metadata.effectiveAccessoryLayout)
    }
}
