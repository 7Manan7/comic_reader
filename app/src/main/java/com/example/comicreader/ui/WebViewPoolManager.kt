package com.example.comicreader.ui

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections
import java.util.LinkedHashMap
import kotlin.math.max

/**
 * State snapshot of a tab that has been evicted from active RAM into hibernation.
 */
data class HibernatedTabState(
    val tabId: String,
    val url: String,
    val scrollPercentage: Float = 0f,
    val webViewBundle: Bundle? = null,
    val detectedChapter: String? = null,
    val title: String = ""
)

/**
 * Bounded LRU WebView Hibernation Pool.
 *
 * Prevents native Chromium heap exhaustion and OOM crashes by capping active
 * live in-memory [ComicWebView] instances to [MAX_ACTIVE_WEBVIEWS] (default: 5).
 * Least-recently-used background tabs are safely hibernated with their DOM state
 * and scroll ratios preserved.
 */
object WebViewPoolManager {
    private const val TAG = "WebViewPoolManager"
    const val MAX_ACTIVE_WEBVIEWS = 5

    // Access-ordered LRU access timestamp tracker
    private val tabAccessMap = Collections.synchronizedMap(
        LinkedHashMap<String, Long>(MAX_ACTIVE_WEBVIEWS * 2, 0.75f, true)
    )

    // Hibernated tab states
    private val hibernatedStates = Collections.synchronizedMap(
        mutableMapOf<String, HibernatedTabState>()
    )

    private val _hibernatedTabIds = MutableStateFlow<Set<String>>(emptySet())
    val hibernatedTabIds: StateFlow<Set<String>> = _hibernatedTabIds.asStateFlow()

    private val _activeWebViewCount = MutableStateFlow(0)
    val activeWebViewCount: StateFlow<Int> = _activeWebViewCount.asStateFlow()

    /**
     * Records access for a tab so LRU ordering is up-to-date.
     */
    fun recordAccess(tabId: String) {
        tabAccessMap[tabId] = System.currentTimeMillis()
    }

    /**
     * Checks if a given tab is currently hibernated.
     */
    fun isHibernated(tabId: String): Boolean {
        return hibernatedStates.containsKey(tabId)
    }

    /**
     * Retrieves the saved hibernation state for a tab and unmarks it as hibernated.
     */
    fun consumeHibernatedState(tabId: String): HibernatedTabState? {
        val state = hibernatedStates.remove(tabId)
        _hibernatedTabIds.value = hibernatedStates.keys.toSet()
        return state
    }

    /**
     * Removes all traces of a tab when it is closed.
     */
    fun onTabClosed(tabId: String) {
        tabAccessMap.remove(tabId)
        hibernatedStates.remove(tabId)
        _hibernatedTabIds.value = hibernatedStates.keys.toSet()
    }

    /**
     * Clears all tracking (e.g. when all tabs are closed or app restarts).
     */
    fun clearAll() {
        tabAccessMap.clear()
        hibernatedStates.clear()
        _hibernatedTabIds.value = emptySet()
        _activeWebViewCount.value = 0
    }

    /**
     * Updates the tracked number of active live WebViews.
     */
    fun updateActiveCount(count: Int) {
        _activeWebViewCount.value = count
    }

    /**
     * Evaluates current active WebViews and evicts the least-recently-used background
     * tab if the count exceeds [MAX_ACTIVE_WEBVIEWS].
     *
     * Invariants:
     * 1. Never evicts the active tab.
     * 2. Never evicts a tab actively playing video/audio.
     */
    fun enforcePoolLimit(
        activeTabId: String,
        videoPlayingTabId: String?,
        liveWebViews: MutableMap<String, ComicWebView>,
        tabTitles: Map<String, String> = emptyMap()
    ): String? {
        updateActiveCount(liveWebViews.size)
        if (liveWebViews.size <= MAX_ACTIVE_WEBVIEWS) {
            return null
        }

        // Find eviction candidate: least recently accessed background tab
        val candidateTabId = synchronized(tabAccessMap) {
            tabAccessMap.entries
                .filter { it.key != activeTabId && it.key != videoPlayingTabId && liveWebViews.containsKey(it.key) }
                .minByOrNull { it.value }
                ?.key
        } ?: liveWebViews.keys.firstOrNull { it != activeTabId && it != videoPlayingTabId }

        if (candidateTabId == null) {
            Log.w(TAG, "Cannot find suitable candidate for WebView hibernation (all active or playing video)")
            return null
        }

        val webView = liveWebViews.remove(candidateTabId) ?: return null

        // Capture scroll ratio and WebView state bundle
        val scrollPercentage = runCatching {
            val contentH = max(1, webView.contentHeight)
            (webView.scrollY.toFloat() / contentH).coerceIn(0f, 1f)
        }.getOrDefault(0f)

        val bundle = Bundle()
        runCatching {
            webView.saveState(bundle)
        }

        val url = webView.url.orEmpty()
        val title = tabTitles[candidateTabId] ?: webView.title.orEmpty()

        hibernatedStates[candidateTabId] = HibernatedTabState(
            tabId = candidateTabId,
            url = url,
            scrollPercentage = scrollPercentage,
            webViewBundle = bundle,
            title = title
        )
        _hibernatedTabIds.value = hibernatedStates.keys.toSet()

        // Safely destroy native WebView
        runCatching {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.stopLoading()
            webView.onPause()
            webView.pauseTimers()
            webView.destroy()
        }

        updateActiveCount(liveWebViews.size)
        Log.i(TAG, "Hibernated background tab: $candidateTabId (URL: $url, Scroll: $scrollPercentage). Active WebViews: ${liveWebViews.size}")
        return candidateTabId
    }
}
