package com.example.comicreader.adblock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdBlockEngineTest {

    @Before
    fun setUp() {
        AdBlockEngine.isEnabled = true
        AdBlockEngine.resetCounter()
    }

    @Test
    fun testBlocksKnownAdDomains() {
        assertTrue(AdBlockEngine.isAd("https://googleads.g.doubleclick.net/pagead/ads?client=ca-pub-123"))
        assertTrue(AdBlockEngine.isAd("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js"))
        assertTrue(AdBlockEngine.isAd("https://popads.net/serve/script.js"))
        assertTrue(AdBlockEngine.isAd("https://syndication.exdynsrv.com/splash.php?id=123"))
        assertTrue(AdBlockEngine.isAd("https://adsterra.com/banner.js"))
        assertTrue(AdBlockEngine.isAd("https://sub.propellerads.com/zone?id=456"))
    }

    @Test
    fun testAllowsGenuineComicContent() {
        assertFalse(AdBlockEngine.isAd("https://mangadex.org/title/abc-123/chapter-1"))
        assertFalse(AdBlockEngine.isAd("https://uploads.mangadex.org/data/hash/1-x.jpg"))
        assertFalse(AdBlockEngine.isAd("https://webtoons.com/en/fantasy/tower-of-god/viewer?episode_no=1"))
        assertFalse(AdBlockEngine.isAd("https://comick.io/comic/one-piece"))
    }

    @Test
    fun testBlocksAdPathPatterns() {
        assertTrue(AdBlockEngine.isAd("https://example.com/ads/ad_banner_top.png"))
        assertTrue(AdBlockEngine.isAd("https://example.com/vignette/popunder.js"))
        assertTrue(AdBlockEngine.isAd("https://example.com/api/adserver/request"))
    }

    @Test
    fun testBlockedCounterIncrements() {
        AdBlockEngine.resetCounter()
        assertEquals(0, AdBlockEngine.getBlockedCount())

        AdBlockEngine.isAd("https://doubleclick.net/test")
        AdBlockEngine.isAd("https://popads.net/banner")
        AdBlockEngine.isAd("https://mangadex.org/chapter") // Not an ad

        assertEquals(2, AdBlockEngine.getBlockedCount())
    }

    @Test
    fun testWhitelistDomain() {
        val testUrl = "https://adservice.google.com/test"
        assertTrue(AdBlockEngine.isAd(testUrl))

        AdBlockEngine.addToWhitelist("google.com")
        assertFalse(AdBlockEngine.isAd(testUrl))

        AdBlockEngine.removeFromWhitelist("google.com")
        assertTrue(AdBlockEngine.isAd(testUrl))
    }

    @Test
    fun testDisableAdBlockEngine() {
        AdBlockEngine.isEnabled = false
        assertFalse(AdBlockEngine.isAd("https://doubleclick.net/ad"))

        AdBlockEngine.isEnabled = true
        assertTrue(AdBlockEngine.isAd("https://doubleclick.net/ad"))
    }

    @Test
    fun testEasyListParser() {
        val rawList = """
            ! Title: Test EasyList
            [Adblock Plus 2.0]
            ! Some comment
            ||badads.net^
            ||tracker.io^${'$'}third-party
            /ad_popup/
            ##.ad-placeholder
            manga.com##.sponsor-box
            @@||safe.badads.net^
            @@/safe_script.js
        """.trimIndent()

        val parsed = EasyListParser.parse(rawList.lineSequence())

        assertTrue(parsed.blockedDomains.contains("badads.net"))
        assertTrue(parsed.blockedDomains.contains("tracker.io"))
        assertTrue(parsed.pathPatterns.contains("/ad_popup/"))
        assertTrue(parsed.cosmeticSelectors.contains(".ad-placeholder"))
        assertEquals(listOf(".sponsor-box"), parsed.domainCosmeticSelectors["manga.com"])
        assertTrue(parsed.exceptionDomains.contains("safe.badads.net"))
        assertTrue(parsed.exceptionPatterns.contains("/safe_script.js"))
    }

    @Test
    fun testEasyListExceptionPrecedence() {
        val rawList = """
            ||malicious-ad-domain.com^
            @@||safe.malicious-ad-domain.com^
        """.trimIndent()

        val parsed = EasyListParser.parse(rawList.lineSequence())
        AdBlockEngine.loadRules(parsed)

        // Block parent domain
        assertTrue(AdBlockEngine.isAd("https://malicious-ad-domain.com/ad.js"))
        assertTrue(AdBlockEngine.isAd("https://sub.malicious-ad-domain.com/ad.js"))

        // Exception rule should allow safe subdomain
        assertFalse(AdBlockEngine.isAd("https://safe.malicious-ad-domain.com/api"))
    }

    @Test
    fun testMergeParsedRules() {
        val rules1 = ParsedFilterRules(
            blockedDomains = setOf("domain1.com"),
            pathPatterns = listOf("/ad1/")
        )
        val rules2 = ParsedFilterRules(
            blockedDomains = setOf("domain2.com"),
            pathPatterns = listOf("/ad2/")
        )

        val merged = rules1 + rules2
        assertEquals(2, merged.blockedDomains.size)
        assertTrue(merged.blockedDomains.contains("domain1.com"))
        assertTrue(merged.blockedDomains.contains("domain2.com"))
        assertEquals(2, merged.pathPatterns.size)
    }

    @Test
    fun testPeterLoweHostsFormatParser() {
        val hostsContent = """
            # Peter Lowe's ad and tracking server list
            127.0.0.1 localhost
            127.0.0.1 005.free-counter.co.uk
            127.0.0.1 101com.com
            0.0.0.0 tracking-server.net
            # Another comment
        """.trimIndent()

        val parsed = EasyListParser.parse(hostsContent.lineSequence())
        assertFalse(parsed.blockedDomains.contains("localhost"))
        assertTrue(parsed.blockedDomains.contains("005.free-counter.co.uk"))
        assertTrue(parsed.blockedDomains.contains("101com.com"))
        assertTrue(parsed.blockedDomains.contains("tracking-server.net"))
    }

    @Test
    fun testUrlhausMalwareParser() {
        val urlhausContent = """
            [Adblock Plus 2.0]
            ! Title: URLhaus Malicious URLs
            ||malware-drop.com^
            ||103.149.28.138^
            /payload.exe
            /miner.bin
        """.trimIndent()

        val parsed = EasyListParser.parse(urlhausContent.lineSequence())
        assertTrue(parsed.blockedDomains.contains("malware-drop.com"))
        assertTrue(parsed.blockedDomains.contains("103.149.28.138"))
        assertTrue(parsed.pathPatterns.contains("/payload.exe"))
        assertTrue(parsed.pathPatterns.contains("/miner.bin"))
    }

    @Test
    fun test16FilterListsCoverage() {
        val lists = AdBlockListManager.ALL_LISTS
        assertEquals(16, lists.size)

        // Verify uBlock Origin Built-in Filters (5 lists)
        val ublockLists = lists.filter { it.category == FilterListCategory.UBLOCK_ASSETS }
        assertEquals(5, ublockLists.size)
        val ublockIds = ublockLists.map { it.id }
        assertTrue(ublockIds.contains("ublock-filters"))
        assertTrue(ublockIds.contains("ublock-badware"))
        assertTrue(ublockIds.contains("ublock-privacy"))
        assertTrue(ublockIds.contains("ublock-quick-fixes"))
        assertTrue(ublockIds.contains("ublock-unbreak"))

        // Verify Standard Default Third-Party Lists (4 lists)
        val standardLists = lists.filter { it.category == FilterListCategory.STANDARD_DEFAULTS }
        assertEquals(4, standardLists.size)
        val standardIds = standardLists.map { it.id }
        assertTrue(standardIds.contains("easylist"))
        assertTrue(standardIds.contains("easyprivacy"))
        assertTrue(standardIds.contains("peter-lowe"))
        assertTrue(standardIds.contains("urlhaus"))

        // Verify Optional Common Lists (7 lists)
        val optionalLists = lists.filter { it.category == FilterListCategory.OPTIONAL_COMMON }
        assertEquals(7, optionalLists.size)
        val optionalIds = optionalLists.map { it.id }
        assertTrue(optionalIds.contains("ublock-annoyances"))
        assertTrue(optionalIds.contains("fanboy-cookiemonster"))
        assertTrue(optionalIds.contains("fanboy-annoyance"))
        assertTrue(optionalIds.contains("fanboy-social"))
        assertTrue(optionalIds.contains("adguard-base"))
        assertTrue(optionalIds.contains("adguard-tracking"))
        assertTrue(optionalIds.contains("adguard-mobile"))
    }

    @Test
    fun testDangerousSchemesBlocked() {
        assertTrue(AdBlockEngine.isDangerousRedirectScheme("intent://scan/#Intent;scheme=zxing;package=com.google.zxing.client.android;end"))
        assertTrue(AdBlockEngine.isDangerousRedirectScheme("market://details?id=com.malware.app"))
        assertTrue(AdBlockEngine.isDangerousRedirectScheme("vnd.youtube:video_id"))
        assertTrue(AdBlockEngine.isAd("intent://open.app/malicious"))
        assertTrue(AdBlockEngine.isAd("market://details?id=test"))
    }

    @Test
    fun testFirstPartyComicProtection() {
        // Comic reader sites should not be blocked when navigating or reading chapters
        assertFalse(AdBlockEngine.isAd("https://comix.to/comic/123/chapter-1", currentHost = "comix.to"))
        assertFalse(AdBlockEngine.isAd("https://comix.to/assets/page-1.jpg", currentHost = "comix.to"))
        assertFalse(AdBlockEngine.isAd("https://ww3.mangafreak.me/manga/solo-leveling", currentHost = "ww3.mangafreak.me"))
        assertFalse(AdBlockEngine.isAd("https://mangakatana.com/manga/one-piece.1/c1", currentHost = "mangakatana.com"))

        // Known aggressive ad networks are always blocked
        assertTrue(AdBlockEngine.isAd("https://alwingulla.com/script.js", currentHost = "comix.to"))
        assertTrue(AdBlockEngine.isAd("https://adsterra.com/banner.js", currentHost = "mangafreak.me"))
        assertTrue(AdBlockEngine.isAd("https://monetag.com/popunder", currentHost = "mangakatana.com"))
    }
}
