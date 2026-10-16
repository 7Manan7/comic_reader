package com.example.comicreader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryManagerTest {

    @Test
    fun testHistoryItemProperties() {
        val now = System.currentTimeMillis()
        val item = HistoryItem(
            id = "test-id",
            title = "Solo Leveling Chapter 1",
            url = "https://comix.to/comic/solo-leveling/1",
            timestamp = now
        )

        assertEquals("test-id", item.id)
        assertEquals("Solo Leveling Chapter 1", item.title)
        assertEquals("https://comix.to/comic/solo-leveling/1", item.url)
        assertEquals(now, item.timestamp)
        assertNotNull(item.formattedDate)
        assertTrue(item.formattedDate.isNotBlank())
    }

    @Test
    fun testHistoryItemFormattedDate() {
        val timestamp = 1700000000000L
        val item = HistoryItem(
            id = "1",
            title = "Test Manga",
            url = "https://mangakatana.com/manga/test",
            timestamp = timestamp
        )

        val formatted = item.formattedDate
        assertTrue(formatted.isNotEmpty())
    }

    @Test
    fun testHistoryItemScrollProgressAndChapter() {
        val item = HistoryItem(
            id = "2",
            title = "Nano Machine;; Chapter 100",
            url = "https://asuracomic.net/nano-machine-100",
            timestamp = System.currentTimeMillis(),
            scrollProgress = 0.72f,
            detectedChapter = "Ch. 100"
        )

        assertEquals(0.72f, item.scrollProgress, 0.001f)
        assertEquals("Ch. 100", item.detectedChapter)
        assertTrue(item.title.contains(";;"))
    }
}
