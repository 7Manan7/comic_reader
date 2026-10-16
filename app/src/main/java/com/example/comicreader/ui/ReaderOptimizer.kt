package com.example.comicreader.ui

import java.util.Locale

/**
 * Advanced Reading Color Profiles for eye comfort and ambient lighting.
 */
enum class ReadingColorProfile(
    val id: String,
    val displayName: String,
    val iconEmoji: String,
    val description: String
) {
    ORIGINAL(
        id = "original",
        displayName = "True Color",
        iconEmoji = "☀️",
        description = "Standard full-color web rendering"
    ),
    OLED_INVERT(
        id = "oled_invert",
        displayName = "OLED Dark",
        iconEmoji = "🌙",
        description = "Inverts light backgrounds while preserving artwork"
    ),
    WARM_AMBER(
        id = "warm_amber",
        displayName = "Warm Amber",
        iconEmoji = "📖",
        description = "Sepia blue-light filter for evening reading"
    ),
    E_INK_MONO(
        id = "e_ink_mono",
        displayName = "E-Ink Mono",
        iconEmoji = "📄",
        description = "High-contrast monochrome for classic manga"
    );

    companion object {
        fun fromId(id: String?): ReadingColorProfile {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ORIGINAL
        }
    }
}

/**
 * Specialized Manga, Manhwa & Webtoon Reader Engine.
 *
 * Provides:
 * 1. Seamless Webtoon Image Stitcher (eliminates 1-3px gaps between manhwa slices).
 * 2. Lazy-Load Booster (accelerates image decoding ahead of 165Hz scrolling).
 * 3. Hands-Free 165Hz Auto-Scroll Engine (variable pixel velocity normalized via rAF).
 * 4. Smart Chapter Detector (DOM scanning for Next/Prev chapter links and chapter labels).
 * 5. Real-Time Scroll Progress Tracking.
 * 6. Advanced Eye-Comfort Color Profiles (True Color, OLED Invert, Warm Amber, E-Ink).
 */
object ReaderOptimizer {

    /**
     * Seamless image stitching CSS targeting common webtoon / manhwa reader containers.
     */
    const val SEAMLESS_WEBTOON_CSS = """
        (function() {
            var cssId = 'kuro-seamless-webtoon-css';
            var existing = document.getElementById(cssId);
            if (existing) return;
            var style = document.createElement('style');
            style.id = cssId;
            style.type = 'text/css';
            style.innerHTML = `
                .reading-content img,
                #readerarea img,
                .chapter-content img,
                .container-chapter-reader img,
                .page-break img,
                .wp-manga-chapter-img,
                .reader-area img,
                .manga-image,
                img[class*='page'],
                img[class*='chapter'],
                img[id*='image-'] {
                    display: block !important;
                    margin-top: 0px !important;
                    margin-bottom: 0px !important;
                    margin-left: auto !important;
                    margin-right: auto !important;
                    padding: 0px !important;
                    border: none !important;
                    vertical-align: top !important;
                    max-width: 100% !important;
                    height: auto !important;
                }
                .page-break, .reading-content > div {
                    margin: 0px !important;
                    padding: 0px !important;
                }
            `;
            (document.head || document.documentElement).appendChild(style);
        })();
    """

    /**
     * Lazy-Load booster script converting lazy images into eager pre-decoded images.
     */
    const val LAZY_LOAD_BOOSTER_JS = """
        (function() {
            if (window.__kuroLazyBoosterAttached) return;
            window.__kuroLazyBoosterAttached = true;

            function boostImages() {
                try {
                    var imgs = document.querySelectorAll('img[data-src], img[data-lazy-src], img[data-original], img[loading="lazy"]');
                    for (var i = 0; i < imgs.length; i++) {
                        var img = imgs[i];
                        var realSrc = img.getAttribute('data-src') || img.getAttribute('data-lazy-src') || img.getAttribute('data-original');
                        if (realSrc && realSrc !== img.src && (!img.src || img.src.indexOf('data:image') !== -1 || img.src.indexOf('placeholder') !== -1)) {
                            img.src = realSrc;
                        }
                        img.loading = 'eager';
                        img.decoding = 'async';
                    }
                } catch(e) {}
            }

            boostImages();
            if (window.MutationObserver) {
                var observer = new MutationObserver(function() {
                    boostImages();
                });
                observer.observe(document.body || document.documentElement, { childList: true, subtree: true });
            }
            setInterval(boostImages, 1500);
        })();
    """

    /**
     * Generates JavaScript code for the Hands-Free 165Hz Auto-Scroll Engine.
     */
    fun getAutoScrollStartJs(speedMultiplier: Float): String {
        val speed = speedMultiplier.coerceIn(0.2f, 10.0f)
        return """
            (function() {
                window.__kuroScrollSpeed = $speed;
                window.__kuroAutoScrollPaused = false;
                if (window.__kuroAutoScrollActive) return;
                window.__kuroAutoScrollActive = true;

                var lastTime = performance.now();
                function step(now) {
                    if (!window.__kuroAutoScrollActive) return;
                    var dt = Math.min(now - lastTime, 50);
                    lastTime = now;
                    if (!window.__kuroAutoScrollPaused) {
                        // Base velocity: ~2.2px per 16.67ms frame at 1.0x normalized
                        var pixels = (window.__kuroScrollSpeed * 2.2) * (dt / 16.67);
                        window.scrollBy(0, pixels);

                        var maxScroll = Math.max(1, document.documentElement.scrollHeight - window.innerHeight);
                        if (window.scrollY >= maxScroll - 5) {
                            window.__kuroAutoScrollActive = false;
                            if (window.KuroBridge && window.KuroBridge.onAutoScrollEnded) {
                                window.KuroBridge.onAutoScrollEnded();
                            }
                            return;
                        }
                    }
                    window.requestAnimationFrame(step);
                }
                window.requestAnimationFrame(step);
            })();
        """.trimIndent()
    }

    const val AUTO_SCROLL_STOP_JS = """
        (function() {
            window.__kuroAutoScrollActive = false;
            window.__kuroAutoScrollPaused = false;
        })();
    """

    const val AUTO_SCROLL_PAUSE_JS = """
        (function() {
            if (window.__kuroAutoScrollActive) {
                window.__kuroAutoScrollPaused = true;
            }
        })();
    """

    const val AUTO_SCROLL_RESUME_JS = """
        (function() {
            if (window.__kuroAutoScrollActive) {
                window.__kuroAutoScrollPaused = false;
            }
        })();
    """

    fun getAutoScrollSpeedUpdateJs(speedMultiplier: Float): String {
        val speed = speedMultiplier.coerceIn(0.2f, 10.0f)
        return """
            (function() {
                window.__kuroScrollSpeed = $speed;
            })();
        """.trimIndent()
    }

    /**
     * Injected script that listens to scroll events and DOM changes to detect:
     * 1. Real-time scroll progress percentage.
     * 2. Next and Previous chapter navigation links.
     */
    const val READER_MONITOR_JS = """
        (function() {
            if (window.__kuroReaderMonitorAttached) return;
            window.__kuroReaderMonitorAttached = true;

            // --- 1. Real-Time Scroll Progress Reporting ---
            var lastReportedProgress = -1;
            function reportScrollProgress() {
                try {
                    var docHeight = document.documentElement.scrollHeight - window.innerHeight;
                    if (docHeight <= 0) return;
                    var currentY = window.scrollY || window.pageYOffset || 0;
                    var ratio = Math.max(0, Math.min(1, currentY / docHeight));
                    // Report on noticeable changes (every 1%)
                    if (Math.abs(ratio - lastReportedProgress) >= 0.01) {
                        lastReportedProgress = ratio;
                        if (window.KuroBridge && window.KuroBridge.onScrollProgressChanged) {
                            window.KuroBridge.onScrollProgressChanged(ratio);
                        }
                    }
                } catch(e) {}
            }
            window.addEventListener('scroll', reportScrollProgress, { passive: true });
            setTimeout(reportScrollProgress, 500);

            // --- 2. Smart Chapter Detector & Next/Prev Extractor ---
            function detectChapterNav() {
                try {
                    var links = document.querySelectorAll('a[href], button[data-href], div[data-href]');
                    var nextUrl = '';
                    var prevUrl = '';

                    for (var i = 0; i < links.length; i++) {
                        var el = links[i];
                        var href = el.getAttribute('href') || el.getAttribute('data-href') || '';
                        if (!href || href === '#' || href.indexOf('javascript:') === 0) continue;
                        var absUrl = '';
                        try {
                            absUrl = new URL(href, window.location.href).href;
                        } catch(err) {
                            continue;
                        }

                        var text = (el.innerText || el.textContent || '').trim().toLowerCase();
                        var rel = (el.getAttribute('rel') || '').toLowerCase();
                        var className = (el.className && typeof el.className === 'string') ? el.className.toLowerCase() : '';
                        var ariaLabel = (el.getAttribute('aria-label') || '').toLowerCase();
                        var combined = text + ' ' + rel + ' ' + className + ' ' + ariaLabel;

                        if (!nextUrl && (rel === 'next' || combined.indexOf('next chapter') !== -1 || combined.indexOf('ch-next') !== -1 || combined.indexOf('next-ch') !== -1 || (combined.indexOf('next') !== -1 && (combined.indexOf('chapter') !== -1 || combined.indexOf('ep') !== -1)))) {
                            nextUrl = absUrl;
                        } else if (!prevUrl && (rel === 'prev' || combined.indexOf('prev chapter') !== -1 || combined.indexOf('previous chapter') !== -1 || combined.indexOf('ch-prev') !== -1 || combined.indexOf('prev-ch') !== -1 || (combined.indexOf('prev') !== -1 && (combined.indexOf('chapter') !== -1 || combined.indexOf('ep') !== -1)))) {
                            prevUrl = absUrl;
                        }
                    }

                    if (window.KuroBridge && window.KuroBridge.onChapterNavigationDetected) {
                        window.KuroBridge.onChapterNavigationDetected('', prevUrl, nextUrl);
                    }
                } catch(e) {}
            }

            detectChapterNav();
            setTimeout(detectChapterNav, 1200);
            setTimeout(detectChapterNav, 3000);
        })();
    """

    /**
     * Generates CSS to apply the requested [ReadingColorProfile].
     */
    fun getColorProfileCss(profile: ReadingColorProfile): String {
        return when (profile) {
            ReadingColorProfile.ORIGINAL -> """
                (function() {
                    var filter = document.getElementById('comic-reader-color-filter');
                    if (filter) filter.remove();
                })();
            """.trimIndent()

            ReadingColorProfile.OLED_INVERT -> """
                (function() {
                    var filter = document.getElementById('comic-reader-color-filter');
                    if (!filter) {
                        filter = document.createElement('style');
                        filter.id = 'comic-reader-color-filter';
                        (document.head || document.documentElement).appendChild(filter);
                    }
                    filter.innerHTML = 'html { filter: invert(0.9) hue-rotate(180deg) !important; background-color: #111 !important; } img, video, canvas, picture { filter: invert(1.1) hue-rotate(180deg) contrast(1.05) !important; }';
                })();
            """.trimIndent()

            ReadingColorProfile.WARM_AMBER -> """
                (function() {
                    var filter = document.getElementById('comic-reader-color-filter');
                    if (!filter) {
                        filter = document.createElement('style');
                        filter.id = 'comic-reader-color-filter';
                        (document.head || document.documentElement).appendChild(filter);
                    }
                    filter.innerHTML = 'html { filter: sepia(0.35) saturate(0.85) brightness(0.92) !important; background-color: #f7f1e3 !important; }';
                })();
            """.trimIndent()

            ReadingColorProfile.E_INK_MONO -> """
                (function() {
                    var filter = document.getElementById('comic-reader-color-filter');
                    if (!filter) {
                        filter = document.createElement('style');
                        filter.id = 'comic-reader-color-filter';
                        (document.head || document.documentElement).appendChild(filter);
                    }
                    filter.innerHTML = 'html { filter: grayscale(1.0) contrast(1.12) !important; }';
                })();
            """.trimIndent()
        }
    }

    private val chapterRegexPatterns = listOf(
        Regex("""(?i)\b(?:chapter|ch\.|ch|chap)\s*([0-9]+(?:\.[0-9]+)?)"""),
        Regex("""(?i)\b(?:episode|ep\.|ep)\s*([0-9]+(?:\.[0-9]+)?)"""),
        Regex("""(?i)[/-](?:chapter|ch|ep)-([0-9]+(?:\.[0-9]+)?)[/-]"""),
        Regex("""(?i)[/-](?:chapter|ch|ep)-([0-9]+(?:\.[0-9]+)?)$""")
    )

    /**
     * Pure Kotlin helper for extracting formatted chapter number labels (e.g. "Ch. 142")
     * from page titles or URLs. Used by UI and unit tests.
     */
    fun extractChapterLabel(title: String, url: String = ""): String? {
        val combined = "$title $url"
        for (pattern in chapterRegexPatterns) {
            val match = pattern.find(combined)
            if (match != null && match.groupValues.size >= 2) {
                val num = match.groupValues[1]
                return "Ch. $num"
            }
        }
        return null
    }
}
