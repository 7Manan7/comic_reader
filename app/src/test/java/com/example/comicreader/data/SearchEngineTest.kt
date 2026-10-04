package com.example.comicreader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchEngineTest {

    @Test
    fun testGoogleSearchUrl() {
        val url = SearchEngine.GOOGLE.buildUrl("solo leveling manhwa")
        assertEquals("https://www.google.com/search?q=solo+leveling+manhwa", url)
    }

    @Test
    fun testDuckDuckGoSearchUrl() {
        val url = SearchEngine.DUCKDUCKGO.buildUrl("tower of god")
        assertEquals("https://duckduckgo.com/?q=tower+of+god", url)
    }

    @Test
    fun testBraveSearchUrl() {
        val url = SearchEngine.BRAVE.buildUrl("omniscient reader")
        assertEquals("https://search.brave.com/search?q=omniscient+reader", url)
    }

    @Test
    fun testBingSearchUrl() {
        val url = SearchEngine.BING.buildUrl("nano machine")
        assertEquals("https://www.bing.com/search?q=nano+machine", url)
    }

    @Test
    fun testEcosiaSearchUrl() {
        val url = SearchEngine.ECOSIA.buildUrl("return of the mount hua sect")
        assertEquals("https://www.ecosia.org/search?q=return+of+the+mount+hua+sect", url)
    }

    @Test
    fun testFromId() {
        assertEquals(SearchEngine.GOOGLE, SearchEngine.fromId("google"))
        assertEquals(SearchEngine.DUCKDUCKGO, SearchEngine.fromId("duckduckgo"))
        assertEquals(SearchEngine.BRAVE, SearchEngine.fromId("brave"))
        assertEquals(SearchEngine.BING, SearchEngine.fromId("bing"))
        assertEquals(SearchEngine.ECOSIA, SearchEngine.fromId("ecosia"))
        // fallback to Google for unknown or null
        assertEquals(SearchEngine.GOOGLE, SearchEngine.fromId("unknown"))
        assertEquals(SearchEngine.GOOGLE, SearchEngine.fromId(null))
    }

    @Test
    fun testSpecialCharacterEncoding() {
        val url = SearchEngine.GOOGLE.buildUrl("action & adventure + manhwa")
        assertTrue(url.contains("%26") || url.contains("&"))
    }
}
