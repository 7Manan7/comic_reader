package com.example.comicreader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeDelimiterCodecTest {

    @Test
    fun testEncodeDecodeBasic() {
        val original = "Standard Title - Chapter 1"
        val encoded = SafeDelimiterCodec.encode(original)
        assertEquals(original, encoded)
        val decoded = SafeDelimiterCodec.decode(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun testEncodeDecodeSemicolons() {
        val dangerousTitle = "Solo Leveling;; Chapter 142;; Asura Scans"
        val encoded = SafeDelimiterCodec.encode(dangerousTitle)
        assertFalse(encoded.contains(";;"))
        assertTrue(encoded.contains("&#59;&#59;"))

        val decoded = SafeDelimiterCodec.decode(encoded)
        assertEquals(dangerousTitle, decoded)
    }

    @Test
    fun testEncodeDecodeNewlines() {
        val multilineTitle = "Title Line 1\nTitle Line 2\r\nTitle Line 3"
        val encoded = SafeDelimiterCodec.encode(multilineTitle)
        assertFalse(encoded.contains("\n"))
        assertFalse(encoded.contains("\r"))

        val decoded = SafeDelimiterCodec.decode(encoded)
        assertEquals(multilineTitle, decoded)
    }

    @Test
    fun testEncodeDecodeCombinedDelimiters() {
        val complex = "Dangerous;; Title\nWith Newline and ;; semicolons\r\nEnd"
        val encoded = SafeDelimiterCodec.encode(complex)
        assertFalse(encoded.contains(";;"))
        assertFalse(encoded.contains("\n"))
        assertFalse(encoded.contains("\r"))

        val decoded = SafeDelimiterCodec.decode(encoded)
        assertEquals(complex, decoded)
    }

    @Test
    fun testBackwardCompatibilityWithLegacyStrings() {
        // Records stored before SafeDelimiterCodec was introduced should decode cleanly
        val legacyRecord = "Normal Title - Chapter 12"
        val decoded = SafeDelimiterCodec.decode(legacyRecord)
        assertEquals(legacyRecord, decoded)
    }

    @Test
    fun testEmptyAndBlankStrings() {
        assertEquals("", SafeDelimiterCodec.encode(""))
        assertEquals("", SafeDelimiterCodec.decode(""))
    }
}
