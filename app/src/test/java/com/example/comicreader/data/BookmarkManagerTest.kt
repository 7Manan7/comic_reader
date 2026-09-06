package com.example.comicreader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkManagerTest {

    @Test
    fun testDefaultBookmarksMatchRequirements() {
        val defaults = BookmarkManager.DEFAULT_BOOKMARKS

        assertEquals(3, defaults.size)

        val urls = defaults.map { it.url }
        assertTrue(urls.contains("https://comix.to/"))
        assertTrue(urls.contains("https://ww3.mangafreak.me/"))
        assertTrue(urls.contains("https://mangakatana.com/"))

        // Verify old defaults are completely removed
        assertFalse(urls.any { it.contains("mangadex.org") })
        assertFalse(urls.any { it.contains("webtoons.com") })
        assertFalse(urls.any { it.contains("mangareader.to") })
        assertFalse(urls.any { it.contains("asuracomic.net") })
        assertFalse(urls.any { it.contains("comick.io") })
    }

    @Test
    fun testBookmarkDataClass() {
        val bookmark = Bookmark(id = "1", name = "Test", url = "https://example.com", icon = "📖")
        assertEquals("1", bookmark.id)
        assertEquals("Test", bookmark.name)
        assertEquals("https://example.com", bookmark.url)
        assertEquals("📖", bookmark.icon)
    }
}
