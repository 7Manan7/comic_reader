package com.example.comicreader.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderOptimizerTest {

    @Test
    fun testExtractChapterLabelFromTitle() {
        assertEquals("Ch. 145", ReaderOptimizer.extractChapterLabel("Solo Leveling Chapter 145 - Asura Scans"))
        assertEquals("Ch. 85", ReaderOptimizer.extractChapterLabel("Tower of God Ch. 85 - Webtoons"))
        assertEquals("Ch. 12", ReaderOptimizer.extractChapterLabel("Wind Breaker Episode 12"))
        assertEquals("Ch. 304.5", ReaderOptimizer.extractChapterLabel("Martial Peak chap 304.5"))
    }

    @Test
    fun testExtractChapterLabelFromUrl() {
        assertEquals("Ch. 142", ReaderOptimizer.extractChapterLabel("Solo Leveling", "https://asuracomic.net/series/solo-leveling-chapter-142/"))
        assertEquals("Ch. 99", ReaderOptimizer.extractChapterLabel("Manga", "https://comix.to/manga/ch-99/"))
        assertEquals("Ch. 50", ReaderOptimizer.extractChapterLabel("Webtoon", "https://site.com/series/ep-50/"))
    }

    @Test
    fun testExtractChapterLabelNonChapter() {
        assertNull(ReaderOptimizer.extractChapterLabel("Solo Leveling - Read All Chapters Free", "https://asuracomic.net/series/solo-leveling/"))
        assertNull(ReaderOptimizer.extractChapterLabel("Home - Kuro Reader", "about:home"))
        assertNull(ReaderOptimizer.extractChapterLabel("Bookmarks", ""))
    }

    @Test
    fun testSeamlessWebtoonCssContent() {
        val css = ReaderOptimizer.SEAMLESS_WEBTOON_CSS
        assertTrue(css.contains(".reading-content img"))
        assertTrue(css.contains("#readerarea img"))
        assertTrue(css.contains("display: block !important"))
        assertTrue(css.contains("margin-top: 0px !important"))
        assertTrue(css.contains("margin-bottom: 0px !important"))
    }

    @Test
    fun testLazyLoadBoosterJsContent() {
        val js = ReaderOptimizer.LAZY_LOAD_BOOSTER_JS
        assertTrue(js.contains("data-src"))
        assertTrue(js.contains("data-lazy-src"))
        assertTrue(js.contains("loading=\"lazy\""))
        assertTrue(js.contains("loading = 'eager'"))
    }

    @Test
    fun testAutoScrollStartJs() {
        val js1 = ReaderOptimizer.getAutoScrollStartJs(1.0f)
        assertTrue(js1.contains("window.__kuroScrollSpeed = 1.0;"))
        assertTrue(js1.contains("window.requestAnimationFrame"))

        val js2 = ReaderOptimizer.getAutoScrollStartJs(3.5f)
        assertTrue(js2.contains("window.__kuroScrollSpeed = 3.5;"))
    }

    @Test
    fun testAutoScrollSpeedLimits() {
        val jsMin = ReaderOptimizer.getAutoScrollStartJs(0.01f)
        assertTrue(jsMin.contains("window.__kuroScrollSpeed = 0.2;"))

        val jsMax = ReaderOptimizer.getAutoScrollStartJs(99.0f)
        assertTrue(jsMax.contains("window.__kuroScrollSpeed = 10.0;"))
    }

    @Test
    fun testReadingColorProfileCss() {
        val originalCss = ReaderOptimizer.getColorProfileCss(ReadingColorProfile.ORIGINAL)
        assertTrue(originalCss.contains("filter.remove()"))

        val oledCss = ReaderOptimizer.getColorProfileCss(ReadingColorProfile.OLED_INVERT)
        assertTrue(oledCss.contains("invert(0.9)"))
        assertTrue(oledCss.contains("hue-rotate(180deg)"))

        val warmAmberCss = ReaderOptimizer.getColorProfileCss(ReadingColorProfile.WARM_AMBER)
        assertTrue(warmAmberCss.contains("sepia(0.35)"))
        assertTrue(warmAmberCss.contains("#f7f1e3"))

        val eInkCss = ReaderOptimizer.getColorProfileCss(ReadingColorProfile.E_INK_MONO)
        assertTrue(eInkCss.contains("grayscale(1.0)"))
    }
}
