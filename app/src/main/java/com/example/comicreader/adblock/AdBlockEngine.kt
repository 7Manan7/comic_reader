package com.example.comicreader.adblock

import android.net.Uri
import android.util.Log
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * High-performance AdBlock Engine tailored for comic/manga websites and enhanced
 * with dynamic EasyList & EasyPrivacy rule integration.
 *
 * Employs sub-microsecond suffix domain matching, request interception,
 * exception precedence, and cosmetic CSS filtering.
 */
object AdBlockEngine {
    private const val TAG = "AdBlockEngine"

    // Atomic counter for blocked ads in current session/page
    private val blockedAdsCounter = AtomicInteger(0)

    var isEnabled: Boolean = true

    // Built-in core ad, tracking, and aggressive manga popup networks
    private val coreBlockedDomains: Set<String> = hashSetOf(
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "google-analytics.com",
        "analytics.google.com",
        "adnxs.com",
        "criteo.com",
        "criteo.net",
        "outbrain.com",
        "taboola.com",
        "amazon-adsystem.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "smartadserver.com",
        "casalemedia.com",
        "yieldmo.com",
        "sovrn.com",
        "infolinks.com",
        "bidswitch.net",
        "adroll.com",
        "moatads.com",
        "scorecardresearch.com",
        "adtechus.com",
        "mediavine.com",
        "adthrive.com",
        "ezoic.com",
        "snigelweb.com",
        "anyclip.com",
        "popads.net",
        "popcash.net",
        "exoclick.com",
        "propellerads.com",
        "propellerclick.com",
        "adsterra.com",
        "monetag.com",
        "admaven.com",
        "hilltopads.com",
        "clickadu.com",
        "trafficjunky.com",
        "mgid.com",
        "adkeeper.co.uk",
        "juicyads.com",
        "cpmstar.com",
        "yadro.ru",
        "aniview.com",
        "spotxchange.com",
        "springserve.com",
        "creativecdn.com",
        "clickcease.com",
        "trafficstars.com",
        "plugrush.com",
        "adxcore.com",
        "realsrv.com",
        "syndication.exdynsrv.com",
        "syndication.realsrv.com",
        "a.magsrv.com",
        "ptradvs.com",
        "tsyndicate.com",
        "wpadmngr.com",
        "yllix.com",
        "ero-advertising.com",
        "adtrue.com",
        "clickaine.com",
        "adtng.com",
        "runative-syndicate.com",
        "revcontent.com",
        "bidvertiser.com",
        "adbuffs.com",
        "chitika.com",
        "popmyads.com",
        "popunder.net",
        "adcash.com",
        "adreactor.com",
        "adexc.net",
        "richaudience.com",
        "teads.tv",
        "undertone.com",
        "gumgum.com",
        "exponential.com",
        "sonobi.com",
        "triplelift.com",
        "kargo.com",
        "quantserve.com",
        "sharethrough.com",
        "lijit.com",
        "adcolony.com",
        "chartboost.com",
        "applovin.com",
        "unityads.unity3d.com",
        "ironsrc.com",
        "vungle.com",
        "alwingulla.com",
        "highcpmrevenuenetwork.com",
        "admaven.com",
        "bidgear.com",
        "vidoomy.com",
        "clarium.net",
        "pubfuture.com",
        "yieldlove.com",
        "dianomi.com",
        "a-ads.com"
    )

    private val coreBlockedPathPatterns: List<String> = listOf(
        "/pagead/",
        "/ad_banner",
        "/ads/",
        "/banners/",
        "/adserver/",
        "/adsystem/",
        "/popunder",
        "/pop_ad",
        "/interstitial",
        "/ad-delivery/",
        "/ad_unit",
        "/native_ad",
        "google_ads",
        "gpt/pubads",
        "/prebid"
    )

    // Active working sets (Core + Loaded Filter Lists)
    @Volatile
    private var activeBlockedDomains: Set<String> = coreBlockedDomains

    @Volatile
    private var activeExceptionDomains: Set<String> = emptySet()

    @Volatile
    private var activePathPatterns: List<String> = coreBlockedPathPatterns

    @Volatile
    private var activeExceptionPatterns: List<String> = emptyList()

    @Volatile
    private var activeCosmeticSelectors: List<String> = emptyList()

    @Volatile
    private var activeDomainCosmeticSelectors: Map<String, List<String>> = emptyMap()

    // User-whitelisted domains (thread-safe + persisted)
    private val whitelistedDomains: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * Loads rules parsed from all active filter lists into memory.
     * Merges with core baseline rules and replaces active sets atomically.
     */
    fun loadRules(rules: ParsedFilterRules) {
        val mergedBlocked = HashSet<String>(coreBlockedDomains.size + rules.blockedDomains.size).apply {
            addAll(coreBlockedDomains)
            addAll(rules.blockedDomains)
        }

        val mergedPaths = ArrayList<String>(coreBlockedPathPatterns.size + rules.pathPatterns.size).apply {
            addAll(coreBlockedPathPatterns)
            addAll(rules.pathPatterns)
        }

        activeBlockedDomains = mergedBlocked
        activeExceptionDomains = rules.exceptionDomains
        activePathPatterns = mergedPaths
        activeExceptionPatterns = rules.exceptionPatterns
        activeCosmeticSelectors = rules.cosmeticSelectors
        activeDomainCosmeticSelectors = rules.domainCosmeticSelectors

        runCatching {
            Log.i(TAG, "AdBlockEngine loaded: ${activeBlockedDomains.size} blocked domains, " +
                    "${activeExceptionDomains.size} exception domains, ${activePathPatterns.size} patterns, " +
                    "${activeCosmeticSelectors.size} cosmetic selectors.")
        }
    }

    /**
     * Total number of unique rules loaded in the engine.
     */
    fun getLoadedRuleCount(): Int {
        return activeBlockedDomains.size + activeExceptionDomains.size + activePathPatterns.size + activeCosmeticSelectors.size
    }

    /**
     * Checks whether the given URI should be blocked.
     */
    fun isAd(uri: Uri, currentHost: String? = null): Boolean {
        return isAd(uri.toString(), currentHost)
    }

    /**
     * Checks whether the given URL string should be blocked.
     * Takes optional currentHost to protect authentic comic chapters and image CDNs from false positives.
     */
    fun isAd(urlString: String, currentHost: String? = null): Boolean {
        if (!isEnabled) return false

        val lowerUrl = urlString.trim().lowercase(Locale.ROOT)

        // Block rogue app-store or intent redirect schemes triggered by ad networks
        if (isDangerousRedirectScheme(lowerUrl)) {
            blockedAdsCounter.incrementAndGet()
            return true
        }

        // Allow data: and blob: and about:
        if (lowerUrl.startsWith("data:") || lowerUrl.startsWith("blob:") || lowerUrl.startsWith("about:")) {
            return false
        }

        val host = extractHost(lowerUrl) ?: return false

        // Check if user explicitly whitelisted this domain
        if (whitelistedDomains.any { host.contains(it) }) {
            return false
        }

        // Check exception domains (Adblock @@ rules have precedence)
        if (isException(host, lowerUrl)) {
            return false
        }

        // Domain matching via fast suffix lookups
        if (matchesDomainSuffix(host, activeBlockedDomains)) {
            // Safeguard: do not block the active comic website domain itself
            if (currentHost != null && (host == currentHost || host.endsWith(".$currentHost"))) {
                return false
            }
            blockedAdsCounter.incrementAndGet()
            return true
        }

        // Path pattern matching (strictly for non-first-party or known ad endpoints)
        for (pattern in activePathPatterns) {
            if (lowerUrl.contains(pattern)) {
                if (currentHost != null && (host == currentHost || host.endsWith(".$currentHost"))) {
                    // Even if on same domain, only block if explicitly in core ad paths
                    if (!coreBlockedPathPatterns.any { lowerUrl.contains(it) }) {
                        continue
                    }
                }
                blockedAdsCounter.incrementAndGet()
                return true
            }
        }

        return false
    }

    /**
     * Checks if scheme is a rogue redirect (e.g. intent, market, deep link) used by ad networks.
     */
    fun isDangerousRedirectScheme(url: String): Boolean {
        return url.startsWith("intent:") ||
                url.startsWith("market:") ||
                url.startsWith("vnd.youtube:") ||
                url.startsWith("itms-appss:") ||
                url.startsWith("alipayqr:")
    }

    private fun isException(host: String, fullUrl: String): Boolean {
        if (activeExceptionDomains.isNotEmpty() && matchesDomainSuffix(host, activeExceptionDomains)) {
            return true
        }
        for (pattern in activeExceptionPatterns) {
            if (fullUrl.contains(pattern)) {
                return true
            }
        }
        return false
    }

    /**
     * Fast sub-microsecond domain suffix matcher.
     * For "sub.ads.doubleclick.net", checks:
     * 1. sub.ads.doubleclick.net
     * 2. ads.doubleclick.net
     * 3. doubleclick.net
     */
    private fun matchesDomainSuffix(host: String, domainsSet: Set<String>): Boolean {
        if (domainsSet.contains(host)) return true
        var dotIndex = host.indexOf('.')
        while (dotIndex != -1 && dotIndex < host.length - 1) {
            val parent = host.substring(dotIndex + 1)
            if (domainsSet.contains(parent)) return true
            dotIndex = host.indexOf('.', dotIndex + 1)
        }
        return false
    }

    private fun extractHost(url: String): String? {
        val withoutProtocol = when {
            url.startsWith("http://") -> url.substring(7)
            url.startsWith("https://") -> url.substring(8)
            url.startsWith("//") -> url.substring(2)
            else -> url
        }
        val slashIndex = withoutProtocol.indexOf('/')
        val hostAndPort = if (slashIndex != -1) withoutProtocol.substring(0, slashIndex) else withoutProtocol
        val colonIndex = hostAndPort.indexOf(':')
        return if (colonIndex != -1) hostAndPort.substring(0, colonIndex) else hostAndPort
    }

    /**
     * Returns an empty response to cleanly drop blocked requests without browser errors.
     */
    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            ByteArrayInputStream(ByteArray(0))
        )
    }

    /**
     * Resets the blocked ads counter (e.g. on new page navigation).
     */
    fun resetCounter() {
        blockedAdsCounter.set(0)
    }

    /**
     * Returns the total count of ads blocked for the current session/page.
     */
    fun getBlockedCount(): Int {
        return blockedAdsCounter.get()
    }

    fun isWhitelisted(domain: String): Boolean {
        return whitelistedDomains.contains(domain.lowercase(Locale.ROOT))
    }

    /**
     * Loads persisted whitelist from SharedPreferences.
     */
    fun loadWhitelist(context: android.content.Context) {
        val prefs = context.getSharedPreferences("adblock_whitelist", android.content.Context.MODE_PRIVATE)
        val raw = prefs.getStringSet("domains", emptySet()) ?: emptySet()
        whitelistedDomains.clear()
        whitelistedDomains.addAll(raw)
    }

    /**
     * Persists the current whitelist to SharedPreferences.
     */
    private fun saveWhitelist(context: android.content.Context) {
        val prefs = context.getSharedPreferences("adblock_whitelist", android.content.Context.MODE_PRIVATE)
        prefs.edit().putStringSet("domains", whitelistedDomains.toSet()).apply()
    }

    /**
     * Adds a domain to the whitelist and persists.
     */
    fun addToWhitelist(domain: String, context: android.content.Context? = null) {
        whitelistedDomains.add(domain.lowercase(Locale.ROOT))
        context?.let { saveWhitelist(it) }
    }

    /**
     * Removes a domain from the whitelist and persists.
     */
    fun removeFromWhitelist(domain: String, context: android.content.Context? = null) {
        whitelistedDomains.remove(domain.lowercase(Locale.ROOT))
        context?.let { saveWhitelist(it) }
    }

    /**
     * Builds dynamic cosmetic CSS combining standard selectors and EasyList selectors.
     */
    fun getCosmeticCss(host: String = ""): String {
        val extraSelectors = StringBuilder()
        // Add parsed EasyList cosmetic rules (capped to prevent huge script injection)
        val sampleSelectors = activeCosmeticSelectors.take(150)
        for (sel in sampleSelectors) {
            extraSelectors.append(sel).append(",\n")
        }
        if (host.isNotEmpty()) {
            activeDomainCosmeticSelectors[host.lowercase(Locale.ROOT)]?.forEach { sel ->
                extraSelectors.append(sel).append(",\n")
            }
        }

        return """
        (function() {
            var css = `
                $extraSelectors
                .adsbygoogle,
                [id^='ad_'],
                [id*='-ad-'],
                [id*='_ad_'],
                [id*='gpt-ad'],
                [class*='-ad-'],
                [class*='ad-box'],
                [class*='ad_container'],
                [class*='ad-container'],
                [class*='ad-banner'],
                [class*='adthrive'],
                [class*='mediavine'],
                [class*='ezoic'],
                [class*='ad-placement'],
                [class*='ad-slot'],
                [class*='ad-wrapper'],
                [class*='ad-zone'],
                [class*='sticky-bottom-ad'],
                [class*='ad-floating'],
                .advertisement,
                .ad-banner,
                .sponsor,
                .popup-overlay,
                #cookie-banner,
                .floating-ad,
                iframe[src*='ad'],
                iframe[src*='banner'],
                div[id*='MarketGid'],
                div[id*='disclaimer'] {
                    display: none !important;
                    visibility: hidden !important;
                    height: 0px !important;
                    min-height: 0px !important;
                    max-height: 0px !important;
                    margin: 0px !important;
                    padding: 0px !important;
                    opacity: 0 !important;
                    pointer-events: none !important;
                }
            `;
            var style = document.getElementById('comic-reader-adblock-css');
            if (!style) {
                style = document.createElement('style');
                style.id = 'comic-reader-adblock-css';
                style.type = 'text/css';
                style.innerHTML = css;
                (document.head || document.documentElement).appendChild(style);
            }
        })();
        """
    }

    const val COSMETIC_CSS = """
        (function() {
            var css = `
                .adsbygoogle,
                [id^='ad_'],
                [id*='-ad-'],
                [id*='_ad_'],
                [id*='gpt-ad'],
                [class*='-ad-'],
                [class*='ad-box'],
                [class*='ad_container'],
                [class*='ad-container'],
                [class*='ad-banner'],
                [class*='adthrive'],
                [class*='mediavine'],
                [class*='ezoic'],
                [class*='ad-placement'],
                [class*='ad-slot'],
                [class*='ad-wrapper'],
                [class*='ad-zone'],
                [class*='sticky-bottom-ad'],
                [class*='ad-floating'],
                .advertisement,
                .ad-banner,
                .sponsor,
                .popup-overlay,
                #cookie-banner,
                .floating-ad,
                iframe[src*='ad'],
                iframe[src*='banner'],
                div[id*='MarketGid'],
                div[id*='disclaimer'] {
                    display: none !important;
                    visibility: hidden !important;
                    height: 0px !important;
                    min-height: 0px !important;
                    max-height: 0px !important;
                    margin: 0px !important;
                    padding: 0px !important;
                    opacity: 0 !important;
                    pointer-events: none !important;
                }
            `;
            var style = document.getElementById('comic-reader-adblock-css');
            if (!style) {
                style = document.createElement('style');
                style.id = 'comic-reader-adblock-css';
                style.type = 'text/css';
                style.innerHTML = css;
                (document.head || document.documentElement).appendChild(style);
            }
        })();
    """

    const val ANTI_POPUP_JS = """
        (function() {
            // Neutralize window.open: return dummy window object to satisfy ad scripts without opening popups
            var dummyWin = {
                closed: true,
                focus: function() {},
                blur: function() {},
                close: function() {},
                postMessage: function() {},
                location: { href: 'about:blank', replace: function() {} },
                document: { open: function(){}, write: function(){}, close: function(){} }
            };
            
            try {
                window.open = function(url, target, features) {
                    console.log('[ComicReader AdBlock] Blocked window.open:', url);
                    return dummyWin;
                };
            } catch(e) {}

            // Suppress beforeunload dialog traps
            try {
                window.onbeforeunload = null;
                window.addEventListener('beforeunload', function(e) {
                    e.stopImmediatePropagation();
                }, true);
            } catch(e) {}

            // Suppress excessive alert/confirm spam
            try {
                var alertCount = 0;
                var origAlert = window.alert;
                window.alert = function(msg) {
                    alertCount++;
                    if (alertCount <= 2) origAlert(msg);
                };
                window.confirm = function() { return true; };
            } catch(e) {}

            // Remove transparent / fullscreen clickjack overlay divs that trigger popunders
            function sweepClickTraps() {
                try {
                    var els = document.querySelectorAll('div, a, span, iframe');
                    for (var i = 0; i < els.length; i++) {
                        var el = els[i];
                        var style = window.getComputedStyle(el);
                        if (style && (style.position === 'fixed' || style.position === 'absolute')) {
                            var z = parseInt(style.zIndex, 10);
                            if (z >= 999) {
                                var rect = el.getBoundingClientRect();
                                if (rect.width >= window.innerWidth * 0.7 && rect.height >= window.innerHeight * 0.7) {
                                    el.remove();
                                }
                            }
                        }
                    }
                } catch(e) {}
            }

            // Stubs for anti-adblock detection evasion
            try {
                window.google_ad_client = true;
                window.adsbygoogle = { push: function(){} };
                window.popns = {};
            } catch(e) {}

            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', sweepClickTraps);
            } else {
                sweepClickTraps();
            }
            setInterval(sweepClickTraps, 2000);
        })();
    """
}
