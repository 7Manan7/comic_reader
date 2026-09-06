package com.example.comicreader.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebView.HitTestResult
import android.webkit.WebViewClient
import com.example.comicreader.adblock.AdBlockEngine

/**
 * High-performance hardware-accelerated WebView optimized for comic/manga reading
 * and high refresh rate displays (60Hz, 120Hz, 144Hz, 165Hz).
 */
class ComicWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    private val TAG = "ComicWebView"

    var onProgressChanged: ((Int) -> Unit)? = null
    var onTitleReceived: ((String) -> Unit)? = null
    var onUrlChanged: ((String) -> Unit)? = null
    var onSingleTap: (() -> Unit)? = null
    var onBlockedAdCountChanged: ((Int) -> Unit)? = null
    var onScrollDirectionChanged: ((isScrollingDown: Boolean) -> Unit)? = null
    var onTouchFocus: (() -> Unit)? = null

    private var isInvertedMode: Boolean = false
    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L
    private var lastScrollDirectionChangeTime = 0L

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        setupHardwareAcceleration()
        setupSettings()
        setupClients()
        addJavascriptInterface(WebInputBridge(this), "KuroBridge")
    }

    private class WebInputBridge(private val webView: ComicWebView) {
        @JavascriptInterface
        fun onInputFocused() {
            webView.post {
                if (!webView.hasFocus()) {
                    webView.requestFocus()
                    webView.requestFocusFromTouch()
                }
                val imm = webView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(webView, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    companion object {
        private const val INPUT_FOCUS_JS = """
            (function() {
                if (window.__kuroInputListenerAttached) return;
                window.__kuroInputListenerAttached = true;
                document.addEventListener('focusin', function(e) {
                    if (e.target && (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA' || e.target.isContentEditable)) {
                        if (window.KuroBridge) {
                            window.KuroBridge.onInputFocused();
                        }
                    }
                }, true);
            })();
        """
    }

    /**
     * Enables hardware acceleration and smooth rasterization for high refresh rates.
     */
    private fun setupHardwareAcceleration() {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        isNestedScrollingEnabled = true
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupSettings() {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            mediaPlaybackRequiresUserGesture = false

            // Optimize text encoding and zoom
            defaultTextEncodingName = "utf-8"
            textZoom = 100

            // Speed up rendering
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
        }
    }

    private fun setupClients() {
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                val urlString = uri.toString()
                val currentHost = runCatching { Uri.parse(this@ComicWebView.url).host }.getOrNull()

                // Block dangerous ad redirect schemes (intent:, market:, etc.)
                if (AdBlockEngine.isDangerousRedirectScheme(urlString)) {
                    post { onBlockedAdCountChanged?.invoke(AdBlockEngine.getBlockedCount() + 1) }
                    return true
                }

                // Block ad navigation redirects
                if (AdBlockEngine.isAd(uri, currentHost)) {
                    post { onBlockedAdCountChanged?.invoke(AdBlockEngine.getBlockedCount()) }
                    return true
                }

                // Allow standard HTTP/HTTPS page navigation
                if (urlString.startsWith("http://") || urlString.startsWith("https://")) {
                    return false
                }

                return true
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url ?: return super.shouldInterceptRequest(view, request)
                val currentHost = runCatching { Uri.parse(this@ComicWebView.url).host }.getOrNull()
                if (AdBlockEngine.isAd(url, currentHost)) {
                    post { onBlockedAdCountChanged?.invoke(AdBlockEngine.getBlockedCount()) }
                    return AdBlockEngine.createEmptyResponse()
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                url?.let { onUrlChanged?.invoke(it) }
                AdBlockEngine.resetCounter()
                onBlockedAdCountChanged?.invoke(0)

                // Inject Anti-popup script as early as possible to kill window.open and click traps
                evaluateJavascript(AdBlockEngine.ANTI_POPUP_JS, null)
                // Inject input focus listener to trigger keyboard when site search bars are focused
                evaluateJavascript(INPUT_FOCUS_JS, null)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let { onUrlChanged?.invoke(it) }

                // Inject CSS cosmetic filtering to hide ad placeholders
                val host = url?.let { runCatching { Uri.parse(it).host }.getOrNull() }.orEmpty()
                evaluateJavascript(AdBlockEngine.getCosmeticCss(host), null)

                // Re-inject Anti-popup script for dynamically loaded ad nodes
                evaluateJavascript(AdBlockEngine.ANTI_POPUP_JS, null)

                // Re-inject input focus listener
                evaluateJavascript(INPUT_FOCUS_JS, null)

                // Re-apply invert color filter if active
                if (isInvertedMode) {
                    applyInvertFilter(true)
                }

                onBlockedAdCountChanged?.invoke(AdBlockEngine.getBlockedCount())
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                onProgressChanged?.invoke(newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                title?.let { onTitleReceived?.invoke(it) }
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                return true // Suppress web console noise
            }

            /**
             * Blocks popup spam and prevents rogue ads from opening new windows/popunders.
             * Only allows genuine user navigation.
             */
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                if (!isUserGesture) {
                    return false
                }

                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                val currentHost = runCatching { Uri.parse(this@ComicWebView.url).host }.getOrNull()

                val tempWebView = WebView(context)
                tempWebView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        targetView: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val targetUri = request?.url ?: return true
                        val targetUrl = targetUri.toString()

                        if (AdBlockEngine.isDangerousRedirectScheme(targetUrl) || AdBlockEngine.isAd(targetUri, currentHost)) {
                            post { onBlockedAdCountChanged?.invoke(AdBlockEngine.getBlockedCount()) }
                            return true
                        }

                        val targetHost = targetUri.host
                        if (targetHost != null && currentHost != null && (targetHost == currentHost || targetHost.endsWith(".$currentHost"))) {
                            this@ComicWebView.loadUrl(targetUrl)
                        } else if (!AdBlockEngine.isAd(targetUri, currentHost)) {
                            this@ComicWebView.loadUrl(targetUrl)
                        }
                        return true
                    }
                }
                transport.webView = tempWebView
                resultMsg.sendToTarget()
                return true
            }
        }
    }

    /**
     * Intercept touch events to support a clean tap-to-toggle HUD gesture.
     * HUD toggle is strictly constrained to the center reading zone so taps on site
     * headers, search buttons, navigation bars, and footers are never intercepted.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                downTime = System.currentTimeMillis()
                if (!hasFocus()) {
                    requestFocus()
                    requestFocusFromTouch()
                }
                onTouchFocus?.invoke()
            }
            MotionEvent.ACTION_UP -> {
                if (!hasFocus()) {
                    requestFocus()
                    requestFocusFromTouch()
                }
                val dx = Math.abs(event.x - downX)
                val dy = Math.abs(event.y - downY)
                val dt = System.currentTimeMillis() - downTime
                // If it was a quick tap with minimal movement (< 25px, < 300ms)
                if (dx < 25 && dy < 25 && dt < 300) {
                    val hitType = hitTestResult.type
                    // If user tapped directly on an edit text field, guarantee soft keyboard is shown
                    if (hitType == HitTestResult.EDIT_TEXT_TYPE) {
                        post {
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                            imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                        }
                    }

                    // Only allow tap-to-toggle HUD when tapping in the center reading zone.
                    // Taps in the top zone (site header, search button, tabs) or bottom zone
                    // (pagination, chapter buttons, footer) must NEVER toggle HUD.
                    val inCenterZoneY = event.y in (height * 0.30f)..(height * 0.70f)
                    val inCenterZoneX = event.x in (width * 0.20f)..(width * 0.80f)
                    if (inCenterZoneY && inCenterZoneX) {
                        val isNavigableLink = hitType == HitTestResult.SRC_ANCHOR_TYPE ||
                                              hitType == HitTestResult.SRC_IMAGE_ANCHOR_TYPE ||
                                              hitType == HitTestResult.EDIT_TEXT_TYPE ||
                                              hitType == HitTestResult.PHONE_TYPE ||
                                              hitType == HitTestResult.EMAIL_TYPE
                        if (!isNavigableLink) {
                            onSingleTap?.invoke()
                        }
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * Smoothly scrolls down by one viewport height (or volume key step).
     */
    fun scrollPageDown() {
        val scrollAmount = (height * 0.75).toInt()
        smoothScrollBy(0, scrollAmount)
    }

    /**
     * Smoothly scrolls up by one viewport height (or volume key step).
     */
    fun scrollPageUp() {
        val scrollAmount = -(height * 0.75).toInt()
        smoothScrollBy(0, scrollAmount)
    }

    private fun smoothScrollBy(dx: Int, dy: Int) {
        // Use smooth scroll animation in JS for maximum 120/144/165Hz fluidity
        val js = "window.scrollBy({ top: $dy, left: $dx, behavior: 'smooth' });"
        evaluateJavascript(js, null)
    }

    /**
     * Toggles night / color inversion filter for dark room manga reading.
     */
    fun setInvertMode(enabled: Boolean) {
        isInvertedMode = enabled
        applyInvertFilter(enabled)
    }

    private fun applyInvertFilter(enabled: Boolean) {
        val js = if (enabled) {
            """
                (function() {
                    var filter = document.getElementById('comic-reader-invert-filter');
                    if (!filter) {
                        filter = document.createElement('style');
                        filter.id = 'comic-reader-invert-filter';
                        filter.innerHTML = 'html { filter: invert(0.9) hue-rotate(180deg) !important; background-color: #111 !important; } img, video { filter: invert(1.1) hue-rotate(180deg) contrast(1.05) !important; }';
                        (document.head || document.documentElement).appendChild(filter);
                    }
                })();
            """
        } else {
            """
                (function() {
                    var filter = document.getElementById('comic-reader-invert-filter');
                    if (filter) filter.remove();
                })();
            """
        }
        evaluateJavascript(js, null)
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        val dy = t - oldt
        val now = System.currentTimeMillis()
        // Debounce: ignore direction changes within 300ms to prevent HUD flickering
        if (now - lastScrollDirectionChangeTime < 300) return
        if (t <= 15) {
            // Near top of page: restore browser UI
            lastScrollDirectionChangeTime = now
            onScrollDirectionChanged?.invoke(false)
        } else if (dy > 12) {
            // Scrolling down: enter fullscreen reader mode
            lastScrollDirectionChangeTime = now
            onScrollDirectionChanged?.invoke(true)
        } else if (dy < -12) {
            // Scrolling up: restore browser UI
            lastScrollDirectionChangeTime = now
            onScrollDirectionChanged?.invoke(false)
        }
    }
}
