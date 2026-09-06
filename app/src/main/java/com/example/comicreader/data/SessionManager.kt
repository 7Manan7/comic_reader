package com.example.comicreader.data

import android.content.Context

/**
 * Manages user session state, delegating to [AppPreferences] for unified storage.
 */
object SessionManager {
    const val DEFAULT_HOME_URL = AppPreferences.DEFAULT_HOME_URL

    fun isRestoreLastPageEnabled(context: Context): Boolean =
        AppPreferences.isRestoreLastPageEnabled(context)

    fun setRestoreLastPageEnabled(context: Context, enabled: Boolean) =
        AppPreferences.setRestoreLastPageEnabled(context, enabled)

    fun saveLastUrl(context: Context, url: String) =
        AppPreferences.saveLastUrl(context, url)

    fun getLastUrl(context: Context): String =
        AppPreferences.getLastUrl(context)

    fun getInitialUrl(context: Context): String =
        AppPreferences.getInitialUrl(context)
}
