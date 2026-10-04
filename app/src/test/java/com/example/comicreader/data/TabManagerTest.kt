package com.example.comicreader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TabManagerTest {

    @Test
    fun testTabDataClass() {
        val tab = Tab(
            id = "tab-123",
            title = "Solo Leveling - Comix",
            url = "https://comix.to/comic/solo-leveling",
            lastAccessed = 1700000000000L
        )

        assertEquals("tab-123", tab.id)
        assertEquals("Solo Leveling - Comix", tab.title)
        assertEquals("https://comix.to/comic/solo-leveling", tab.url)
        assertEquals(1700000000000L, tab.lastAccessed)
    }

    @Test
    fun testTabDefaultValues() {
        val tab = Tab()
        assertNotNull(tab.id)
        assertTrue(tab.id.isNotBlank())
        assertEquals("New Tab", tab.title)
        assertEquals(AppPreferences.DEFAULT_HOME_URL, tab.url)
        assertTrue(tab.lastAccessed > 0)
    }

    @Test
    fun testTabCopy() {
        val tab = Tab(id = "1", title = "Initial", url = "https://comix.to/")
        val updated = tab.copy(title = "Updated Title", url = "https://mangakatana.com/")

        assertEquals("1", updated.id)
        assertEquals("Updated Title", updated.title)
        assertEquals("https://mangakatana.com/", updated.url)
    }

    @Test
    fun testTabListManagement() {
        val tab1 = Tab(id = "1", title = "Home", url = "https://comix.to/")
        val tab2 = Tab(id = "2", title = "MangaKatana", url = "https://mangakatana.com/")
        val tab3 = Tab(id = "3", title = "MangaFreak", url = "https://ww3.mangafreak.me/")

        val list = mutableListOf(tab1, tab2, tab3)
        assertEquals(3, list.size)

        // Close middle tab
        list.removeAt(1)
        assertEquals(2, list.size)
        assertEquals("1", list[0].id)
        assertEquals("3", list[1].id)

        // Add new tab
        val tab4 = Tab(id = "4", title = "New Tab", url = AppPreferences.DEFAULT_HOME_URL)
        list.add(tab4)
        assertEquals(3, list.size)
        assertEquals("4", list[2].id)
    }

    @Test
    fun testTabIncognitoProperty() {
        val standardTab = Tab(id = "std-1", isIncognito = false)
        val privateTab = Tab(id = "pvt-1", title = "Private Tab", isIncognito = true)

        assertEquals(false, standardTab.isIncognito)
        assertEquals(true, privateTab.isIncognito)

        val updatedPrivate = standardTab.copy(isIncognito = true)
        assertEquals(true, updatedPrivate.isIncognito)
    }

    @Test
    fun testIncognitoTabFiltering() {
        val tabs = listOf(
            Tab(id = "1", title = "Home", isIncognito = false),
            Tab(id = "2", title = "Private 1", isIncognito = true),
            Tab(id = "3", title = "Manga", isIncognito = false),
            Tab(id = "4", title = "Private 2", isIncognito = true)
        )

        val regularTabs = tabs.filter { !it.isIncognito }
        val incognitoTabs = tabs.filter { it.isIncognito }

        assertEquals(2, regularTabs.size)
        assertEquals("1", regularTabs[0].id)
        assertEquals("3", regularTabs[1].id)

        assertEquals(2, incognitoTabs.size)
        assertEquals("2", incognitoTabs[0].id)
        assertEquals("4", incognitoTabs[1].id)
    }
}
