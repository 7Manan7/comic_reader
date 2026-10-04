package com.example.comicreader.adblock

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPInputStream

enum class FilterListCategory(val title: String, val subtitle: String) {
    DEFAULT(
        title = "Default",
        subtitle = "Core standard blocking rules for ads, popups, and trackers"
    ),
    PRIVACY(
        title = "Privacy",
        subtitle = "URL parameter stripping and intrusion protection"
    ),
    MALWARE(
        title = "Malware protection, security",
        subtitle = "Malware domains, badware risks, and rogue redirect traps"
    ),
    ANNOYANCES(
        title = "Annoyances",
        subtitle = "Cookie notices, popups, overlays, notifications, social and AI widgets"
    )
}

data class FilterListDefinition(
    val id: String,
    val name: String,
    val description: String,
    val category: FilterListCategory,
    val primaryUrl: String,
    val backupUrl: String? = null,
    val filename: String,
    val assetFallback: String? = null,
    val isEnabledByDefault: Boolean = true
)

data class FilterListItemStatus(
    val definition: FilterListDefinition,
    val isEnabled: Boolean,
    val ruleCount: Int = 0,
    val lastUpdated: Long = 0L,
    val isUpdating: Boolean = false
)

data class AdBlockListsStatus(
    val isUpdating: Boolean = false,
    val totalRuleCount: Int = 0,
    val activeListCount: Int = 0,
    val statusMessage: String? = null,
    val lastUpdated: Long = 0L,
    val listItems: Map<String, FilterListItemStatus> = emptyMap(),
    // Backward compatibility helpers
    val easyListRuleCount: Int = 0,
    val easyPrivacyRuleCount: Int = 0,
    val peterLoweRuleCount: Int = 0,
    val urlhausRuleCount: Int = 0
) {
    val formattedLastUpdated: String
        get() {
            if (lastUpdated <= 0L) return "Baseline bundled"
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(lastUpdated))
        }
}

/**
 * Manages downloading, caching, per-list toggling, and updates for 15 filter lists across:
 * 1. Default (4 lists)
 * 2. Privacy (2 lists)
 * 3. Malware protection, security (2 lists)
 * 4. Annoyances (7 lists)
 */
object AdBlockListManager {
    private const val TAG = "AdBlockListManager"
    private const val PREFS_NAME = "comic_adblock_prefs"
    private const val KEY_ENABLED_PREFIX = "filter_enabled_"
    private const val KEY_TIMESTAMP_PREFIX = "filter_ts_"

    val ALL_LISTS = listOf(
        // Group 1: Default (4/4)
        FilterListDefinition(
            id = "easylist",
            name = "EasyList",
            description = "Primary ad-blocking filter list for banner, popup, and video ads",
            category = FilterListCategory.DEFAULT,
            primaryUrl = "https://ublockorigin.github.io/uAssets/thirdparties/easylist.txt",
            backupUrl = "https://easylist.to/easylist/easylist.txt",
            filename = "easylist.txt",
            assetFallback = "adblock/easylist_baseline.txt",
            isEnabledByDefault = true
        ),
        FilterListDefinition(
            id = "easyprivacy",
            name = "EasyPrivacy",
            description = "Blocks tracking scripts, web beacons, and analytics collectors",
            category = FilterListCategory.DEFAULT,
            primaryUrl = "https://ublockorigin.github.io/uAssets/thirdparties/easyprivacy.txt",
            backupUrl = "https://easylist.to/easylist/easyprivacy.txt",
            filename = "easyprivacy.txt",
            assetFallback = "adblock/easyprivacy_baseline.txt",
            isEnabledByDefault = true
        ),
        FilterListDefinition(
            id = "peter-lowe",
            name = "Peter Lowe – Ads, trackers, and more",
            description = "Authoritative list of ad and tracking server hostnames",
            category = FilterListCategory.DEFAULT,
            primaryUrl = "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts&showintro=0&mimetype=plaintext",
            backupUrl = "https://raw.githubusercontent.com/gorhill/uBlock/master/assets/thirdparties/pgl.yoyo.org/as/serverlist",
            filename = "peter_lowe.txt",
            assetFallback = "adblock/peter_lowe_baseline.txt",
            isEnabledByDefault = true
        ),
        FilterListDefinition(
            id = "ublock-filters",
            name = "uBlock filters – Ads, trackers, and more",
            description = "uBlock Origin specific rules for ads, circumventions, and trackers",
            category = FilterListCategory.DEFAULT,
            primaryUrl = "https://ublockorigin.github.io/uAssets/filters/filters.txt",
            backupUrl = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt",
            filename = "ublock_filters.txt",
            isEnabledByDefault = true
        ),

        // Group 2: Privacy (1/2)
        FilterListDefinition(
            id = "url-tracking-protection",
            name = "AdGuard/uBO – URL Tracking Protection",
            description = "Strips tracking parameters and telemetry queries from links and URLs",
            category = FilterListCategory.PRIVACY,
            primaryUrl = "https://ublockorigin.github.io/uAssets/filters/privacy-removeparam.txt",
            backupUrl = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/privacy-removeparam.txt",
            filename = "url_tracking_protection.txt",
            isEnabledByDefault = true
        ),
        FilterListDefinition(
            id = "block-lan",
            name = "Block Outsider Intrusion into LAN",
            description = "Prevents external websites from probing or attacking localhost and LAN addresses",
            category = FilterListCategory.PRIVACY,
            primaryUrl = "https://ublockorigin.github.io/uAssets/filters/lan-block.txt",
            backupUrl = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/lan-block.txt",
            filename = "block_lan.txt",
            isEnabledByDefault = false
        ),

        // Group 3: Malware protection, security (2/2)
        FilterListDefinition(
            id = "ublock-badware",
            name = "uBlock filters – Badware risks",
            description = "Aggressive comic redirect traps, fake download triggers, and rogue scripts",
            category = FilterListCategory.MALWARE,
            primaryUrl = "https://ublockorigin.github.io/uAssets/filters/badware.txt",
            backupUrl = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/badware.txt",
            filename = "ublock_badware.txt",
            isEnabledByDefault = true
        ),
        FilterListDefinition(
            id = "malicious-url-blocklist",
            name = "Malicious URL Blocklist",
            description = "Actively updated list of domains distributing malware and exploit kits",
            category = FilterListCategory.MALWARE,
            primaryUrl = "https://malware-filter.gitlab.io/urlhaus-filter/urlhaus-filter-ag-online.txt",
            backupUrl = "https://curben.gitlab.io/malware-filter/urlhaus-filter-online.txt",
            filename = "malicious_urls.txt",
            assetFallback = "adblock/urlhaus_baseline.txt",
            isEnabledByDefault = true
        ),

        // Group 4: Annoyances (6/7)
        FilterListDefinition(
            id = "annoyances-ai",
            name = "EasyList – AI Widgets",
            description = "Blocks AI summaries, floating assist widgets, and AI generation bars",
            category = FilterListCategory.ANNOYANCES,
            primaryUrl = "https://ublockorigin.github.io/uAssets/thirdparties/easylist-ai.txt",
            backupUrl = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/thirdparties/easylist/easylist-ai.txt",
            filename = "easylist_ai.txt",
            isEnabledByDefault = false
        ),
        FilterListDefinition(
            id = "annoyances-cookies",
            name = "EasyList/uBO – Cookie Notices",
            description = "Removes GDPR/CCPA cookie consent dialogs and modal consent walls",
            category = FilterListCategory.ANNOYANCES,
            primaryUrl = "https://ublockorigin.github.io/uAssets/thirdparties/easylist-cookies.txt",
            backupUrl = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/thirdparties/easylist/easylist-cookies.txt",
            filename = "easylist_cookies.txt",
            isEnabledByDefault = true
        ),
        FilterListDefinition(
            id = "annoyances-notifications",
            name = "EasyList – Notifications",
            description = "Blocks website push notification prompts and subscription modals",
            category = FilterListCategory.ANNOYANCES,
            primaryUrl = "https://ublockorigin.github.io/uAssets/thirdparties/easylist-notifications.txt",
            backupUrl = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/thirdparties/easylist/easylist-notifications.txt",
            filename = "easylist_notifications.txt",
            isEnabledByDefault = true
        ),
        FilterListDefinition(
            id = "annoyances-others",
            name = "EasyList – Other Annoyances",
            description = "Eliminates misc page irritations, surveys, self-promos, and visual nags",
            category = FilterListCategory.ANNOYANCES,
            primaryUrl = "https://ublockorigin.github.io/uAssets/thirdparties/easylist-annoyances.txt",
            backupUrl = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/thirdparties/easylist/easylist-annoyances.txt",
            filename = "easylist_annoyances.txt",
            isEnabledByDefault = true
        ),
        FilterListDefinition(
            id = "annoyances-overlays",
            name = "EasyList/uBO – Overlay Notices",
            description = "Blocks newsletter overlays, anti-adblock modals, and sign-up paywalls",
            category = FilterListCategory.ANNOYANCES,
            primaryUrl = "https://ublockorigin.github.io/uAssets/thirdparties/easylist-newsletters.txt",
            backupUrl = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/thirdparties/easylist/easylist-newsletters.txt",
            filename = "easylist_newsletters.txt",
            isEnabledByDefault = true
        ),
        FilterListDefinition(
            id = "annoyances-social",
            name = "EasyList – Social Widgets",
            description = "Hides social sharing buttons, follower counts, and comment widgets",
            category = FilterListCategory.ANNOYANCES,
            primaryUrl = "https://ublockorigin.github.io/uAssets/thirdparties/easylist-social.txt",
            backupUrl = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/thirdparties/easylist/easylist-social.txt",
            filename = "easylist_social.txt",
            isEnabledByDefault = true
        ),
        FilterListDefinition(
            id = "annoyances-widgets",
            name = "EasyList – Chat Widgets",
            description = "Suppresses floating customer service chat bubbles and support popups",
            category = FilterListCategory.ANNOYANCES,
            primaryUrl = "https://ublockorigin.github.io/uAssets/thirdparties/easylist-chat.txt",
            backupUrl = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/thirdparties/easylist/easylist-chat.txt",
            filename = "easylist_chat.txt",
            isEnabledByDefault = true
        )
    )

    private val _status = MutableStateFlow(AdBlockListsStatus())
    val status: StateFlow<AdBlockListsStatus> = _status.asStateFlow()

    private var isInitialized = false

    /**
     * Initializes filter lists on startup.
     */
    suspend fun init(context: Context) = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext
        reloadRules(context, "Loaded baseline filter lists")
        isInitialized = true
    }

    /**
     * Toggles an individual list on or off.
     */
    suspend fun toggleList(context: Context, listId: String, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        val prefs = getPrefs(context)
        prefs.edit().putBoolean(KEY_ENABLED_PREFIX + listId, isEnabled).apply()
        reloadRules(context, if (isEnabled) "Enabled $listId" else "Disabled $listId")
    }

    /**
     * Downloads and updates all enabled filter lists concurrently.
     */
    suspend fun updateLists(context: Context): Boolean = withContext(Dispatchers.IO) {
        _status.value = _status.value.copy(
            isUpdating = true,
            statusMessage = "Updating filter lists concurrently..."
        )

        val prefs = getPrefs(context)
        val dir = getAdBlockDir(context)
        val enabledLists = ALL_LISTS.filter { isListEnabled(prefs, it) }

        var successCount = 0
        coroutineScope {
            val jobs = enabledLists.map { listDef ->
                async {
                    val targetFile = File(dir, listDef.filename)
                    val success = downloadList(listDef.primaryUrl, listDef.backupUrl, targetFile)
                    if (success) {
                        prefs.edit().putLong(KEY_TIMESTAMP_PREFIX + listDef.id, System.currentTimeMillis()).apply()
                    }
                    success
                }
            }
            successCount = jobs.map { it.await() }.count { it }
        }

        reloadRules(context, "Updated $successCount/${enabledLists.size} lists successfully")
        true
    }

    /**
     * Re-parses all enabled lists from disk cache/assets and updates AdBlockEngine.
     */
    private fun reloadRules(context: Context, message: String) {
        val prefs = getPrefs(context)
        val dir = getAdBlockDir(context)

        val listStatuses = mutableMapOf<String, FilterListItemStatus>()
        var combinedRules = ParsedFilterRules()
        var maxTimestamp = 0L

        for (listDef in ALL_LISTS) {
            val enabled = isListEnabled(prefs, listDef)
            val ts = prefs.getLong(KEY_TIMESTAMP_PREFIX + listDef.id, 0L)
            if (ts > maxTimestamp) maxTimestamp = ts

            val parsed = if (enabled) {
                loadOrFallback(context, File(dir, listDef.filename), listDef.assetFallback)
            } else {
                ParsedFilterRules()
            }

            if (enabled) {
                combinedRules = combinedRules + parsed
            }

            listStatuses[listDef.id] = FilterListItemStatus(
                definition = listDef,
                isEnabled = enabled,
                ruleCount = parsed.totalRuleCount,
                lastUpdated = ts
            )
        }

        AdBlockEngine.loadRules(combinedRules)

        _status.value = AdBlockListsStatus(
            isUpdating = false,
            totalRuleCount = combinedRules.totalRuleCount,
            activeListCount = listStatuses.values.count { it.isEnabled },
            statusMessage = message,
            lastUpdated = maxTimestamp,
            listItems = listStatuses,
            easyListRuleCount = listStatuses["easylist"]?.ruleCount ?: 0,
            easyPrivacyRuleCount = listStatuses["easyprivacy"]?.ruleCount ?: 0,
            peterLoweRuleCount = listStatuses["peter-lowe"]?.ruleCount ?: 0,
            urlhausRuleCount = (listStatuses["malicious-url-blocklist"] ?: listStatuses["urlhaus"])?.ruleCount ?: 0
        )

        Log.i(TAG, "AdBlock reloaded with ${combinedRules.totalRuleCount} rules across ${_status.value.activeListCount} active lists.")
    }

    private fun isListEnabled(prefs: SharedPreferences, def: FilterListDefinition): Boolean {
        return prefs.getBoolean(KEY_ENABLED_PREFIX + def.id, def.isEnabledByDefault)
    }

    private fun loadOrFallback(context: Context, cachedFile: File, assetPath: String?): ParsedFilterRules {
        if (cachedFile.exists() && cachedFile.length() > 0) {
            try {
                return FileInputStream(cachedFile).use { EasyListParser.parse(it) }
            } catch (e: Exception) {
                Log.w(TAG, "Failed reading cached ${cachedFile.name}", e)
            }
        }
        if (assetPath != null) {
            try {
                return context.assets.open(assetPath).use { EasyListParser.parse(it) }
            } catch (e: Exception) {
                Log.w(TAG, "Asset not found or unreadable: $assetPath")
            }
        }
        return ParsedFilterRules()
    }

    private fun downloadList(primaryUrl: String, backupUrl: String?, targetFile: File): Boolean {
        return tryDownload(primaryUrl, targetFile) || (backupUrl != null && tryDownload(backupUrl, targetFile))
    }

    private fun tryDownload(urlString: String, targetFile: File): Boolean {
        var connection: HttpURLConnection? = null
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
        return try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 25000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) ComicReader/1.0")
                setRequestProperty("Accept-Encoding", "gzip")
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                Log.w(TAG, "HTTP $responseCode from $urlString")
                return false
            }

            val isGzip = "gzip".equals(connection.contentEncoding, ignoreCase = true)
            val rawStream = connection.inputStream
            val inputStream: InputStream = try {
                if (isGzip) GZIPInputStream(rawStream) else rawStream
            } catch (e: Exception) {
                rawStream.close()
                throw e
            }

            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (tempFile.length() > 0) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
                Log.i(TAG, "Downloaded ${targetFile.name} (${targetFile.length()} bytes)")
                true
            } else {
                tempFile.delete()
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed downloading from $urlString: ${e.message}")
            if (tempFile.exists()) tempFile.delete()
            false
        } finally {
            connection?.disconnect()
        }
    }

    private fun getAdBlockDir(context: Context): File {
        val dir = File(context.filesDir, "adblock")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
