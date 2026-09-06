package com.example.comicreader.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Unified persistent storage for all Kuro Reader user settings and preferences.
 * Guarantees that all user preferences (display refresh rate, night invert,
 * keep screen awake, volume scroll, zoom level, ad-block state, and session URL)
 * are preserved and restored identically when reopening or restarting the app.
 */
object AppPreferences {
    private const val PREFS_NAME = "kuro_reader_preferences"

    // Preference Keys
    private const val KEY_RESTORE_LAST_PAGE = "restore_last_visited_page"
    private const val KEY_LAST_VISITED_URL = "last_visited_url"
    private const val KEY_NIGHT_INVERT_MODE = "night_invert_mode"
    private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
    private const val KEY_VOLUME_SCROLL_ENABLED = "volume_scroll_enabled"
    private const val KEY_WEB_TEXT_ZOOM = "web_text_zoom"
    private const val KEY_ADBLOCK_ENABLED = "adblock_enabled"
    private const val KEY_TARGET_REFRESH_RATE = "target_refresh_rate"
    private const val KEY_IMMERSIVE_FULLSCREEN = "immersive_fullscreen"

    const val DEFAULT_HOME_URL = "https://comix.to/"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- Restore Last Page & Session ---
    fun isRestoreLastPageEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_RESTORE_LAST_PAGE, true)

    fun setRestoreLastPageEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_RESTORE_LAST_PAGE, enabled).apply()

    fun saveLastUrl(context: Context, url: String) {
        val trimmed = url.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            getPrefs(context).edit().putString(KEY_LAST_VISITED_URL, trimmed).apply()
        }
    }

    fun getLastUrl(context: Context): String =
        getPrefs(context).getString(KEY_LAST_VISITED_URL, null) ?: DEFAULT_HOME_URL

    fun getInitialUrl(context: Context): String {
        if (isRestoreLastPageEnabled(context)) {
            val lastUrl = getPrefs(context).getString(KEY_LAST_VISITED_URL, null)
            if (!lastUrl.isNullOrBlank() && (lastUrl.startsWith("http://") || lastUrl.startsWith("https://"))) {
                return lastUrl
            }
        }
        return DEFAULT_HOME_URL
    }

    // --- OLED Dark / Night Invert ---
    fun isNightInvertMode(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_NIGHT_INVERT_MODE, false)

    fun setNightInvertMode(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_NIGHT_INVERT_MODE, enabled).apply()

    // --- Keep Screen Awake ---
    fun isKeepScreenOn(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_KEEP_SCREEN_ON, true)

    fun setKeepScreenOn(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_KEEP_SCREEN_ON, enabled).apply()

    // --- Volume Key Scrolling ---
    fun isVolumeScrollEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_VOLUME_SCROLL_ENABLED, true)

    fun setVolumeScrollEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_VOLUME_SCROLL_ENABLED, enabled).apply()

    // --- Web Text / Page Zoom ---
    fun getWebTextZoom(context: Context): Int =
        getPrefs(context).getInt(KEY_WEB_TEXT_ZOOM, 100)

    fun setWebTextZoom(context: Context, zoom: Int) =
        getPrefs(context).edit().putInt(KEY_WEB_TEXT_ZOOM, zoom).apply()

    // --- Master AdBlock Switch ---
    fun isAdBlockEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_ADBLOCK_ENABLED, true)

    fun setAdBlockEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_ADBLOCK_ENABLED, enabled).apply()

    // --- Target Display Refresh Rate (Hz) ---
    fun getTargetRefreshRate(context: Context): Float =
        getPrefs(context).getFloat(KEY_TARGET_REFRESH_RATE, -1f)

    fun setTargetRefreshRate(context: Context, rateHz: Float) =
        getPrefs(context).edit().putFloat(KEY_TARGET_REFRESH_RATE, rateHz).apply()

    // --- Immersive Fullscreen Mode ---
    fun isImmersiveFullscreen(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_IMMERSIVE_FULLSCREEN, false)

    fun setImmersiveFullscreen(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_IMMERSIVE_FULLSCREEN, enabled).apply()
}
