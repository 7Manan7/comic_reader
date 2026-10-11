package com.example.comicreader.youtube

import android.net.Uri
import java.util.Locale

/**
 * YouTube mobile web performance and UX optimizer.
 *
 * Provides optimizations to deliver an app-like experience in Kuro Reader:
 * 1. Automatic ad skipping & fast-forwarding of video ads.
 * 2. Suppression of "Open in YouTube App", "Try Premium", and app-install nag banners.
 * 3. Native app double-tap to seek gesture (10s rewind on left, 10s fast-forward on right).
 * 4. Video playback state detection to automatically keep screen awake while watching.
 * 5. Streamlined player controls and full-width layout matching the official YouTube app.
 */
object YouTubeOptimizer {

    fun isYouTube(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val host = runCatching { Uri.parse(url).host }.getOrNull()?.lowercase(Locale.ROOT) ?: return false
        return host == "youtube.com" || host.endsWith(".youtube.com") ||
                host == "youtu.be" || host.endsWith(".youtu.be") ||
                host == "youtube-nocookie.com" || host.endsWith(".youtube-nocookie.com")
    }

    /**
     * Injected CSS to remove web clutter, app-install banners, and deliver an app-like interface.
     */
    const val YOUTUBE_APP_CSS = """
        (function() {
            var cssId = 'kuro-youtube-app-style';
            if (document.getElementById(cssId)) return;
            var style = document.createElement('style');
            style.id = cssId;
            style.type = 'text/css';
            style.innerHTML = `
                /* 1. Remove "Switch to App" and "Open App" sticky banners */
                ytm-app-promo-renderer,
                .ytm-app-promo-renderer,
                ytm-mealbar-promo-renderer,
                ytm-upsell-dialog-renderer,
                #upsell-dialog,
                dialog-renderer[dialog-type='app-promo'],
                .fullscreen-app-promo,
                .mobile-topbar-header .yt-spec-button-shape-next--call-to-action,
                .mobile-topbar-header [aria-label*='Open App'],
                .mobile-topbar-header [aria-label*='Get app'],
                ytm-pivot-bar-item-renderer:has([aria-label*='Get app']),
                .consent-bump-v2-lightbox,
                .ytm-pivot-bar-renderer-app-promo {
                    display: none !important;
                    visibility: hidden !important;
                    height: 0px !important;
                    opacity: 0 !important;
                    pointer-events: none !important;
                }

                /* 2. Hide promoted ads in feed and search results */
                ytm-compact-promoted-item-renderer,
                ytm-promoted-sparkles-web-renderer,
                .ytp-ad-overlay-container,
                .ytp-ad-message-container,
                .video-ads.ytp-ad-module {
                    display: none !important;
                }

                /* 3. Streamlined app-like player styling */
                #player-container-id,
                .player-container,
                ytm-player {
                    background-color: #000000 !important;
                }

                /* 4. Seek feedback badge ripple */
                .kuro-seek-indicator {
                    position: fixed;
                    z-index: 999999;
                    background: rgba(0, 0, 0, 0.7);
                    color: #ffffff;
                    font-size: 16px;
                    font-weight: bold;
                    padding: 10px 18px;
                    border-radius: 24px;
                    border: 1.5px solid rgba(255, 255, 255, 0.3);
                    pointer-events: none;
                    animation: kuroFade 0.6s ease-out forwards;
                    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.6);
                }

                @keyframes kuroFade {
                    0% { opacity: 0; transform: scale(0.7); }
                    30% { opacity: 1; transform: scale(1.05); }
                    70% { opacity: 1; transform: scale(1.0); }
                    100% { opacity: 0; transform: scale(0.9); }
                }
            `;
            (document.head || document.documentElement).appendChild(style);
        })();
    """

    /**
     * Injected JavaScript for automatic ad skipping, double-tap seek, and screen wake lock.
     */
    const val YOUTUBE_OPTIMIZER_JS = """
        (function() {
            if (window.__kuroYouTubeOptimizerLoaded) return;
            window.__kuroYouTubeOptimizerLoaded = true;

            // --- 1. Automatic Ad Skipper & Fast-Forward ---
            function autoSkipAds() {
                try {
                    var video = document.querySelector('video');
                    var adShowing = document.querySelector('.ad-showing, .ad-interrupting, .ytp-ad-player-overlay');
                    if (adShowing && video) {
                        // Attempt to click standard YouTube skip ad button
                        var skipBtn = document.querySelector(
                            '.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, .ytm-skip-ad-button, ' +
                            'button.ytp-ad-skip-button-modern, .ytp-ad-skip-button-slot button, [id^="skip-button"] button, ' +
                            '.ytp-ad-skip-button-container button'
                        );
                        if (skipBtn) {
                            skipBtn.click();
                        } else if (!isNaN(video.duration) && video.duration > 0 && video.currentTime < video.duration - 0.2) {
                            // Fast-forward unskippable ad video instantly
                            video.__kuroWasAdSpeeding = true;
                            video.playbackRate = 16.0;
                            video.muted = true;
                            video.currentTime = video.duration - 0.1;
                        }
                    } else if (video && video.__kuroWasAdSpeeding) {
                        // Restore normal playback speed and sound once ad concludes
                        video.__kuroWasAdSpeeding = false;
                        video.playbackRate = 1.0;
                        video.muted = false;
                    }
                } catch(e) {}
            }
            setInterval(autoSkipAds, 350);

            // --- 2. Auto Dismiss "Try Premium" / "Switch to App" Dialogs ---
            function dismissNags() {
                try {
                    var dismissBtns = document.querySelectorAll(
                        'ytm-mealbar-promo-renderer button[aria-label*="Dismiss"], ' +
                        'ytm-mealbar-promo-renderer button[aria-label*="No thanks"], ' +
                        'dialog-renderer button[aria-label*="Dismiss"], ' +
                        '#dismiss-button button'
                    );
                    dismissBtns.forEach(function(btn) { btn.click(); });
                } catch(e) {}
            }
            setInterval(dismissNags, 1500);

            // --- 3. Double-Tap to Seek (10s Back / 10s Forward) ---
            var lastTapTime = 0;
            var lastTapX = 0;
            var lastTapY = 0;

            function showSeekBadge(text, x, y) {
                try {
                    var badge = document.createElement('div');
                    badge.className = 'kuro-seek-indicator';
                    badge.innerText = text;
                    badge.style.left = (x - 45) + 'px';
                    badge.style.top = (y - 25) + 'px';
                    document.body.appendChild(badge);
                    setTimeout(function() {
                        if (badge.parentNode) badge.parentNode.removeChild(badge);
                    }, 650);
                } catch(e) {}
            }

            document.addEventListener('touchend', function(e) {
                var touch = e.changedTouches[0];
                if (!touch) return;
                var now = Date.now();
                var video = document.querySelector('video');
                if (!video) return;

                var player = document.querySelector('#player-container-id, #player, .player-container, ytm-player, #player-control-overlay');
                var rect = (player && player.offsetWidth > 0) ? player.getBoundingClientRect() : video.getBoundingClientRect();

                if (touch.clientY >= rect.top && touch.clientY <= rect.bottom && touch.clientX >= rect.left && touch.clientX <= rect.right) {
                    var timeDiff = now - lastTapTime;
                    var distDiff = Math.abs(touch.clientX - lastTapX) + Math.abs(touch.clientY - lastTapY);

                    if (timeDiff < 320 && distDiff < 60) {
                        var midX = rect.left + rect.width / 2;
                        if (touch.clientX < midX - 25) {
                            // Double tap on left side -> Rewind 10 seconds
                            video.currentTime = Math.max(0, video.currentTime - 10);
                            showSeekBadge('◀◀ 10s', rect.left + rect.width * 0.25, rect.top + rect.height * 0.5);
                            e.preventDefault();
                            lastTapTime = 0;
                            return;
                        } else if (touch.clientX > midX + 25) {
                            // Double tap on right side -> Fast-forward 10 seconds
                            var maxDuration = (!isNaN(video.duration) && video.duration > 0) ? video.duration : 99999;
                            video.currentTime = Math.min(maxDuration, video.currentTime + 10);
                            showSeekBadge('10s ▶▶', rect.left + rect.width * 0.75, rect.top + rect.height * 0.5);
                            e.preventDefault();
                            lastTapTime = 0;
                            return;
                        }
                    }
                    lastTapTime = now;
                    lastTapX = touch.clientX;
                    lastTapY = touch.clientY;
                }
            }, { passive: false });

            // --- 4. Video Playback State Hook (Keep Screen Awake) ---
            function hookVideoState() {
                var video = document.querySelector('video');
                if (video && !video.__kuroHooked) {
                    video.__kuroHooked = true;
                    if (!video.paused && !video.ended) {
                        if (window.KuroBridge && window.KuroBridge.onVideoPlaybackStateChanged) {
                            window.KuroBridge.onVideoPlaybackStateChanged(true);
                        }
                    }
                    video.addEventListener('playing', function() {
                        if (window.KuroBridge && window.KuroBridge.onVideoPlaybackStateChanged) {
                            window.KuroBridge.onVideoPlaybackStateChanged(true);
                        }
                    });
                    video.addEventListener('pause', function() {
                        if (window.KuroBridge && window.KuroBridge.onVideoPlaybackStateChanged) {
                            window.KuroBridge.onVideoPlaybackStateChanged(false);
                        }
                    });
                    video.addEventListener('ended', function() {
                        if (window.KuroBridge && window.KuroBridge.onVideoPlaybackStateChanged) {
                            window.KuroBridge.onVideoPlaybackStateChanged(false);
                        }
                    });
                }
            }
            setInterval(hookVideoState, 1000);
        })();
    """
}
