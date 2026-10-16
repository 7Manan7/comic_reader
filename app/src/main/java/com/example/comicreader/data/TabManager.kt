package com.example.comicreader.data

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Manages multi-tab state and persistent storage for Kuro Reader.
 */
object TabManager {
    private const val PREFS_NAME = "comic_reader_tabs"
    private const val KEY_TABS = "open_tabs"
    private const val KEY_ACTIVE_TAB_ID = "active_tab_id"
    private const val MAX_TABS = 50
    @Volatile
    private var isFirstLaunch = true
    private var cachedTabs: MutableList<Tab>? = null
    private var cachedActiveTabId: String? = null

    fun resetFirstLaunchForTesting() {
        isFirstLaunch = true
        cachedTabs = null
        cachedActiveTabId = null
    }

    fun getTabs(context: Context): List<Tab> {
        val memory = cachedTabs
        if (memory != null) {
            return memory.toList()
        }
        val prefs = getPrefs(context)
        val raw = prefs.getString(KEY_TABS, null)
        if (raw.isNullOrBlank()) {
            val defaultUrl = SessionManager.getInitialUrl(context)
            val defaultTab = Tab(
                id = UUID.randomUUID().toString(),
                title = "Home",
                url = defaultUrl,
                lastAccessed = System.currentTimeMillis()
            )
            saveTabs(context, listOf(defaultTab), defaultTab.id)
            return listOf(defaultTab)
        }

        val loadedTabs = try {
            val parsedTabs = raw.lines().mapNotNull { line ->
                val parts = line.split(";;")
                if (parts.size >= 3) {
                    val id = parts[0]
                    val title = SafeDelimiterCodec.decode(parts[1]).ifBlank { "New Tab" }
                    val url = SafeDelimiterCodec.decode(parts[2]).ifBlank { AppPreferences.DEFAULT_HOME_URL }
                    val lastAccessed = if (parts.size >= 4) parts[3].toLongOrNull() ?: System.currentTimeMillis() else System.currentTimeMillis()
                    val isIncognito = if (parts.size >= 5) parts[4].toBoolean() else false
                    val scrollProgress = if (parts.size >= 6) parts[5].toFloatOrNull() ?: 0f else 0f
                    val detectedChapter = if (parts.size >= 7) SafeDelimiterCodec.decode(parts[6]).ifBlank { null } else null
                    Tab(
                        id = id,
                        title = title,
                        url = url,
                        lastAccessed = lastAccessed,
                        isIncognito = isIncognito,
                        scrollProgress = scrollProgress,
                        detectedChapter = detectedChapter
                    )
                } else null
            }

            // On cold start / fresh launch: purge any incognito tabs from previous session
            val sanitized = if (isFirstLaunch) {
                isFirstLaunch = false
                val filtered = parsedTabs.filter { !it.isIncognito }
                if (filtered.size != parsedTabs.size) {
                    val defaultUrl = SessionManager.getInitialUrl(context)
                    val toSave = filtered.ifEmpty {
                        listOf(Tab(title = "Home", url = defaultUrl, lastAccessed = System.currentTimeMillis()))
                    }
                    val currentActive = prefs.getString(KEY_ACTIVE_TAB_ID, null)
                    val nextActive = if (toSave.any { it.id == currentActive }) currentActive!! else toSave.first().id
                    saveTabs(context, toSave, nextActive)
                    toSave
                } else {
                    parsedTabs
                }
            } else {
                parsedTabs
            }

            sanitized.ifEmpty {
                val defaultUrl = SessionManager.getInitialUrl(context)
                val defaultTab = Tab(
                    id = UUID.randomUUID().toString(),
                    title = "Home",
                    url = defaultUrl,
                    lastAccessed = System.currentTimeMillis()
                )
                saveTabs(context, listOf(defaultTab), defaultTab.id)
                listOf(defaultTab)
            }
        } catch (e: Exception) {
            val defaultUrl = SessionManager.getInitialUrl(context)
            val defaultTab = Tab(
                id = UUID.randomUUID().toString(),
                title = "Home",
                url = defaultUrl,
                lastAccessed = System.currentTimeMillis()
            )
            listOf(defaultTab)
        }

        cachedTabs = loadedTabs.toMutableList()
        return loadedTabs
    }

    fun getActiveTabId(context: Context): String {
        val inMemoryId = cachedActiveTabId
        val tabs = getTabs(context)
        if (inMemoryId != null && tabs.any { it.id == inMemoryId }) {
            return inMemoryId
        }
        val prefs = getPrefs(context)
        val activeId = prefs.getString(KEY_ACTIVE_TAB_ID, null)
        val resolved = if (activeId != null && tabs.any { it.id == activeId }) {
            activeId
        } else {
            tabs.firstOrNull()?.id ?: UUID.randomUUID().toString()
        }
        cachedActiveTabId = resolved
        return resolved
    }

    fun setActiveTabId(context: Context, tabId: String) {
        cachedActiveTabId = tabId
        getPrefs(context).edit().putString(KEY_ACTIVE_TAB_ID, tabId).apply()
    }

    fun createTab(
        context: Context,
        url: String = AppPreferences.DEFAULT_HOME_URL,
        title: String = "New Tab",
        isIncognito: Boolean = false
    ): Pair<List<Tab>, Tab> {
        val currentTabs = getTabs(context).toMutableList()
        val newTab = Tab(
            id = UUID.randomUUID().toString(),
            title = if (title == "New Tab" && isIncognito) "Private Tab" else title,
            url = if (url.isBlank()) AppPreferences.DEFAULT_HOME_URL else url,
            lastAccessed = System.currentTimeMillis(),
            isIncognito = isIncognito
        )
        currentTabs.add(newTab)
        val trimmed = if (currentTabs.size > MAX_TABS) currentTabs.takeLast(MAX_TABS) else currentTabs
        saveTabs(context, trimmed, newTab.id)
        return Pair(trimmed, newTab)
    }

    fun closeTab(context: Context, tabId: String): Pair<List<Tab>, String> {
        val currentTabs = getTabs(context).toMutableList()
        val index = currentTabs.indexOfFirst { it.id == tabId }
        val currentActiveId = getActiveTabId(context)

        if (index != -1) {
            currentTabs.removeAt(index)
        }

        if (currentTabs.isEmpty()) {
            val defaultTab = Tab(
                id = UUID.randomUUID().toString(),
                title = "Home",
                url = AppPreferences.DEFAULT_HOME_URL,
                lastAccessed = System.currentTimeMillis()
            )
            saveTabs(context, listOf(defaultTab), defaultTab.id)
            return Pair(listOf(defaultTab), defaultTab.id)
        }

        val newActiveId = if (currentActiveId == tabId) {
            val nextIndex = if (index < currentTabs.size) index else currentTabs.size - 1
            currentTabs[nextIndex].id
        } else {
            currentActiveId
        }

        saveTabs(context, currentTabs, newActiveId)
        return Pair(currentTabs, newActiveId)
    }

    fun closeAllTabs(context: Context): Pair<List<Tab>, String> {
        val defaultTab = Tab(
            id = UUID.randomUUID().toString(),
            title = "Home",
            url = AppPreferences.DEFAULT_HOME_URL,
            lastAccessed = System.currentTimeMillis()
        )
        saveTabs(context, listOf(defaultTab), defaultTab.id)
        return Pair(listOf(defaultTab), defaultTab.id)
    }

    fun closeAllIncognitoTabs(context: Context): Pair<List<Tab>, String> {
        val currentTabs = getTabs(context).toMutableList()
        currentTabs.removeAll { it.isIncognito }
        if (currentTabs.isEmpty()) {
            val defaultTab = Tab(
                id = UUID.randomUUID().toString(),
                title = "Home",
                url = AppPreferences.DEFAULT_HOME_URL,
                lastAccessed = System.currentTimeMillis(),
                isIncognito = false
            )
            currentTabs.add(defaultTab)
        }
        val currentActiveId = getActiveTabId(context)
        val newActiveId = if (currentTabs.any { it.id == currentActiveId }) {
            currentActiveId
        } else {
            currentTabs.first().id
        }
        saveTabs(context, currentTabs, newActiveId)
        return Pair(currentTabs, newActiveId)
    }

    fun updateTab(
        context: Context,
        tabId: String,
        title: String? = null,
        url: String? = null,
        isIncognito: Boolean? = null,
        scrollProgress: Float? = null,
        detectedChapter: String? = null
    ): List<Tab> {
        val currentTabs = getTabs(context).map { tab ->
            if (tab.id == tabId) {
                tab.copy(
                    title = title?.ifBlank { tab.title } ?: tab.title,
                    url = url?.ifBlank { tab.url } ?: tab.url,
                    lastAccessed = System.currentTimeMillis(),
                    isIncognito = isIncognito ?: tab.isIncognito,
                    scrollProgress = scrollProgress ?: tab.scrollProgress,
                    detectedChapter = detectedChapter ?: tab.detectedChapter
                )
            } else {
                tab
            }
        }
        val activeId = getActiveTabId(context)
        saveTabs(context, currentTabs, activeId)
        return currentTabs
    }

    fun updateTabScrollProgress(
        context: Context,
        tabId: String,
        scrollProgress: Float,
        detectedChapter: String? = null
    ): List<Tab> {
        return updateTab(
            context = context,
            tabId = tabId,
            scrollProgress = scrollProgress.coerceIn(0f, 1f),
            detectedChapter = detectedChapter
        )
    }

    fun saveTabs(context: Context, tabs: List<Tab>, activeTabId: String) {
        cachedTabs = tabs.toMutableList()
        cachedActiveTabId = activeTabId
        val raw = tabs.joinToString("\n") {
            val encTitle = SafeDelimiterCodec.encode(it.title)
            val encUrl = SafeDelimiterCodec.encode(it.url)
            val encChapter = SafeDelimiterCodec.encode(it.detectedChapter.orEmpty())
            "${it.id};;$encTitle;;$encUrl;;${it.lastAccessed};;${it.isIncognito};;${it.scrollProgress};;$encChapter"
        }
        getPrefs(context).edit()
            .putString(KEY_TABS, raw)
            .putString(KEY_ACTIVE_TAB_ID, activeTabId)
            .apply()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
