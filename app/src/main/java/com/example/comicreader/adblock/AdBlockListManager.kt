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
    UBLOCK_ASSETS(
        title = "uBlock Origin Built-in Filters (uAssets)",
        subtitle = "Maintained by uBlock Origin for ads, popups, telemetry & anti-circumvention"
    ),
    STANDARD_DEFAULTS(
        title = "Standard Default Third-Party Lists",
        subtitle = "Industry-standard blocking lists enabled in default uBlock Origin installations"
    ),
    OPTIONAL_COMMON(
        title = "Optional Common Lists (uBO & AdGuard)",
        subtitle = "Annoyances, cookie notices, social widgets, and mobile ads"
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
 * Manages downloading, caching, per-list toggling, and updates for 16 filter lists across:
 * 1. uBlock Origin Built-in Filters (uAssets)
 * 2. Standard Default Third-Party Lists
 * 3. Optional Common Lists (uBO & AdGuard)
 */
object AdBlockListManager {
    private const val TAG = "AdBlockListManager"
    private const val PREFS_NAME = "comic_adblock_prefs"
    private const val KEY_ENABLED_PREFIX = "filter_enabled_"
    private const val KEY_TIMESTAMP_PREFIX = "filter_ts_"

    val ALL_LISTS = listOf(
        // Group 1: uBlock Origin Built-in Filters (uAssets)
        FilterListDefinition(
            id = "ublock-filters",
            name = "uBlock filters (Base)",
            description = "Core uBlock Origin ad & popunder blocking rules",
            category = FilterListCategory.UBLOCK_ASSETS,
            primaryUrl = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt",
            filename = "ublock_filters.txt",
            isEnabledByDefault = true
        ),
        FilterListDefinition(
            id = "ublock-badware",
            name = "uBlock filters – Badware risks",
            description = "Malware, scareware, and aggressive comic redirect traps",
            category = FilterListCategory.UBLOCK_ASSETS,
            primaryUrl = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/badware.txt",
            filename = "ublock_badware.txt",
            isEnabledByDefault = true
        ),
        FilterListDefinition(
            id = "ublock-privacy",
            name = "uBlock filters – Privacy",
            description = "Anti-telemetry and ad tracking servers",
            category = FilterListCategory.UBLOCK_ASSETS,
            primaryUrl = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/privacy.txt",
            filename = "ublock_privacy.txt",
            isEnabledByDefault = true
        ),
        FilterListDefinition(
            id = "ublock-quick-fixes",
            name = "uBlock filters – Quick fixes",
            description = "Rapid response to newly detected ad circumvention",
            category = FilterListCategory.UBLOCK_ASSETS,
            primaryUrl = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/quick-fixes.txt",
            filename = "ublock_quick_fixes.txt",
            isEnabledByDefault = true
        ),
        FilterListDefinition(
            id = "ublock-unbreak",
            name = "uBlock filters – Unbreak",
            description = "Fixes legitimate comic viewer sites broken by general rules",
            category = FilterListCategory.UBLOCK_ASSETS,
            primaryUrl = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/unbreak.txt",
            filename = "ublock_unbreak.txt",
            isEnabledByDefault = true
        ),

        // Group 2: Standard Default Third-Party Lists
        FilterListDefinition(
            id = "easylist",
            name = "EasyList (Primary ad-blocking)",
            description = "Global ad banners, popups, and video overlays",
            category = FilterListCategory.STANDARD_DEFAULTS,
            primaryUrl = "https://easylist.to/easylist/easylist.txt",
            backupUrl = "https://raw.githubusercontent.com/easylist/easylist/master/easylist/easylist_general_block.txt",
            filename = "easylist.txt",
            assetFallback = "adblock/easylist_baseline.txt",
            isEnabledByDefault = true
        ),
        FilterListDefinition(
            id = "easyprivacy",
            name = "EasyPrivacy (Trackers & analytics)",
            description = "Tracker, beacon, and analytics blocking",
            category = FilterListCategory.STANDARD_DEFAULTS,
            primaryUrl = "https://easylist.to/easylist/easyprivacy.txt",
            backupUrl = "https://raw.githubusercontent.com/easylist/easylist/master/easyprivacy/easyprivacy_general_block.txt",
            filename = "easyprivacy.txt",
            assetFallback = "adblock/easyprivacy_baseline.txt",
            isEnabledByDefault = true
        ),
        FilterListDefinition(
            id = "peter-lowe",
            name = "Peter Lowe’s Ad and Tracking List",
            description = "Hosts-format ad & spyware hostnames",
            category = FilterListCategory.STANDARD_DEFAULTS,
            primaryUrl = "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts&showintro=0&mimetype=plaintext",
            backupUrl = "https://pgl.yoyo.org/as/serverlist",
            filename = "peter_lowe.txt",
            assetFallback = "adblock/peter_lowe_baseline.txt",
            isEnabledByDefault = true
        ),
        FilterListDefinition(
            id = "urlhaus",
            name = "URLhaus Malicious URLs",
            description = "Malware distribution and exploit URLs",
            category = FilterListCategory.STANDARD_DEFAULTS,
            primaryUrl = "https://curben.gitlab.io/malware-filter/urlhaus-filter-online.txt",
            backupUrl = "https://malware-filter.gitlab.io/urlhaus-filter/urlhaus-filter-online.txt",
            filename = "urlhaus.txt",
            assetFallback = "adblock/urlhaus_baseline.txt",
            isEnabledByDefault = true
        ),

        // Group 3: Optional Common Lists in uBlock Origin
        FilterListDefinition(
            id = "ublock-annoyances",
            name = "uBlock filters – Annoyances",
            description = "Overlays, popups, and floating newsletter banners",
            category = FilterListCategory.OPTIONAL_COMMON,
            primaryUrl = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/annoyances.txt",
            filename = "ublock_annoyances.txt",
            isEnabledByDefault = true
        ),
        FilterListDefinition(
            id = "fanboy-cookiemonster",
            name = "EasyList – Cookie Notices",
            description = "Fanboy's Cookie Monster list to eliminate cookie consent dialogs",
            category = FilterListCategory.OPTIONAL_COMMON,
            primaryUrl = "https://secure.fanboy.co.nz/fanboy-cookiemonster.txt",
            filename = "fanboy_cookiemonster.txt",
            isEnabledByDefault = true
        ),
        FilterListDefinition(
            id = "fanboy-annoyance",
            name = "Fanboy’s Annoyance List",
            description = "Popups, newsletters, in-page notifications, and spam",
            category = FilterListCategory.OPTIONAL_COMMON,
            primaryUrl = "https://secure.fanboy.co.nz/fanboy-annoyance.txt",
            filename = "fanboy_annoyance.txt",
            isEnabledByDefault = true
        ),
        FilterListDefinition(
            id = "fanboy-social",
            name = "Fanboy’s Social Blocking List",
            description = "Third-party social widgets, like buttons, and share trackers",
            category = FilterListCategory.OPTIONAL_COMMON,
            primaryUrl = "https://easylist.to/easylist/fanboy-social.txt",
            filename = "fanboy_social.txt",
            isEnabledByDefault = false
        ),
        FilterListDefinition(
            id = "adguard-base",
            name = "AdGuard Base Filter",
            description = "Enhanced ad blocking optimized for uBlock/Chromium",
            category = FilterListCategory.OPTIONAL_COMMON,
            primaryUrl = "https://filters.adtidy.org/extension/ublock/filters/2_without_easylist.txt",
            filename = "adguard_base.txt",
            isEnabledByDefault = true
        ),
        FilterListDefinition(
            id = "adguard-tracking",
            name = "AdGuard Tracking Protection",
            description = "Comprehensive tracking and telemetry filter list",
            category = FilterListCategory.OPTIONAL_COMMON,
            primaryUrl = "https://filters.adtidy.org/extension/ublock/filters/3.txt",
            filename = "adguard_tracking.txt",
            isEnabledByDefault = true
        ),
        FilterListDefinition(
            id = "adguard-mobile",
            name = "AdGuard Mobile Ads",
            description = "Mobile-specific popup redirects and ad networks",
            category = FilterListCategory.OPTIONAL_COMMON,
            primaryUrl = "https://filters.adtidy.org/extension/ublock/filters/11.txt",
            filename = "adguard_mobile.txt",
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
            urlhausRuleCount = listStatuses["urlhaus"]?.ruleCount ?: 0
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
            val inputStream: InputStream = if (isGzip) {
                GZIPInputStream(connection.inputStream)
            } else {
                connection.inputStream
            }

            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(32768)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
            }

            if (tempFile.length() > 0) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
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
