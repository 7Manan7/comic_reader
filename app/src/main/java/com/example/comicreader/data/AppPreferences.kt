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

    const val DEFAULT_HOME_URL = "about:home"
    private const val KEY_SEARCH_ENGINE = "preferred_search_engine"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- Search Engine Selection ---
    fun getSearchEngine(context: Context): SearchEngine {
        val id = getPrefs(context).getString(KEY_SEARCH_ENGINE, SearchEngine.GOOGLE.id)
        return SearchEngine.fromId(id)
    }

    fun setSearchEngine(context: Context, engine: SearchEngine) {
        getPrefs(context).edit().putString(KEY_SEARCH_ENGINE, engine.id).apply()
    }

    // --- Restore Last Page & Session ---
    fun isRestoreLastPageEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_RESTORE_LAST_PAGE, true)

    fun setRestoreLastPageEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_RESTORE_LAST_PAGE, enabled).apply()

    fun saveLastUrl(context: Context, url: String) {
        val trimmed = url.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed == DEFAULT_HOME_URL) {
            getPrefs(context).edit().putString(KEY_LAST_VISITED_URL, trimmed).apply()
        }
    }

    fun getLastUrl(context: Context): String =
        getPrefs(context).getString(KEY_LAST_VISITED_URL, null) ?: DEFAULT_HOME_URL

    fun getInitialUrl(context: Context): String {
        if (isRestoreLastPageEnabled(context)) {
            val lastUrl = getPrefs(context).getString(KEY_LAST_VISITED_URL, null)
            if (!lastUrl.isNullOrBlank() && (lastUrl.startsWith("http://") || lastUrl.startsWith("https://") || lastUrl == DEFAULT_HOME_URL)) {
                return lastUrl
            }
        }
        return DEFAULT_HOME_URL
    }

    private const val KEY_READING_COLOR_PROFILE = "reading_color_profile"
    private const val KEY_WEBTOON_SEAMLESS_MODE = "webtoon_seamless_mode"
    private const val KEY_AUTO_SCROLL_SPEED = "auto_scroll_speed"

    // --- Reading Color Profiles & Night Invert ---
    fun getColorProfile(context: Context): com.example.comicreader.ui.ReadingColorProfile {
        val id = getPrefs(context).getString(KEY_READING_COLOR_PROFILE, null)
        if (id != null) {
            return com.example.comicreader.ui.ReadingColorProfile.fromId(id)
        }
        // Fallback to legacy boolean if present
        return if (getPrefs(context).getBoolean(KEY_NIGHT_INVERT_MODE, false)) {
            com.example.comicreader.ui.ReadingColorProfile.OLED_INVERT
        } else {
            com.example.comicreader.ui.ReadingColorProfile.ORIGINAL
        }
    }

    fun setColorProfile(context: Context, profile: com.example.comicreader.ui.ReadingColorProfile) {
        getPrefs(context).edit()
            .putString(KEY_READING_COLOR_PROFILE, profile.id)
            .putBoolean(KEY_NIGHT_INVERT_MODE, profile == com.example.comicreader.ui.ReadingColorProfile.OLED_INVERT)
            .apply()
    }

    fun getReadingColorProfile(context: Context): com.example.comicreader.ui.ReadingColorProfile =
        getColorProfile(context)

    fun setReadingColorProfile(context: Context, profile: com.example.comicreader.ui.ReadingColorProfile) =
        setColorProfile(context, profile)

    fun isNightInvertMode(context: Context): Boolean =
        getColorProfile(context) == com.example.comicreader.ui.ReadingColorProfile.OLED_INVERT

    fun setNightInvertMode(context: Context, enabled: Boolean) {
        val profile = if (enabled) com.example.comicreader.ui.ReadingColorProfile.OLED_INVERT else com.example.comicreader.ui.ReadingColorProfile.ORIGINAL
        setColorProfile(context, profile)
    }

    // --- Webtoon Seamless Stitching & Lazy Booster ---
    fun isWebtoonSeamlessModeEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_WEBTOON_SEAMLESS_MODE, true)

    fun setWebtoonSeamlessModeEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_WEBTOON_SEAMLESS_MODE, enabled).apply()

    fun isWebtoonSeamlessMode(context: Context): Boolean =
        isWebtoonSeamlessModeEnabled(context)

    fun setWebtoonSeamlessMode(context: Context, enabled: Boolean) =
        setWebtoonSeamlessModeEnabled(context, enabled)

    // --- Hands-Free Auto Scroll Speed ---
    fun getAutoScrollSpeed(context: Context): Float =
        getPrefs(context).getFloat(KEY_AUTO_SCROLL_SPEED, 1.0f)

    fun setAutoScrollSpeed(context: Context, speed: Float) =
        getPrefs(context).edit().putFloat(KEY_AUTO_SCROLL_SPEED, speed).apply()

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
