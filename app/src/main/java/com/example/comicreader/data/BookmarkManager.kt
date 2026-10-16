package com.example.comicreader.data

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

data class Bookmark(
    val id: String,
    val name: String,
    val url: String,
    val icon: String = "📖"
)

/**
 * Manages custom and default bookmarks with persistent local storage.
 */
object BookmarkManager {
    private const val PREFS_NAME = "comic_reader_bookmarks"
    private const val KEY_BOOKMARKS = "saved_bookmarks"

    val DEFAULT_BOOKMARKS = listOf(
        Bookmark(id = "comix", name = "Comix", url = "https://comix.to/", icon = "📚"),
        Bookmark(id = "mangafreak", name = "MangaFreak", url = "https://ww3.mangafreak.me/", icon = "⚡"),
        Bookmark(id = "mangakatana", name = "MangaKatana", url = "https://mangakatana.com/", icon = "⚔️")
    )

    fun getBookmarks(context: Context): List<Bookmark> {
        val prefs = getPrefs(context)
        val raw = prefs.getString(KEY_BOOKMARKS, null) ?: return DEFAULT_BOOKMARKS
        if (raw.isBlank()) return DEFAULT_BOOKMARKS

        return try {
            raw.lines().mapNotNull { line ->
                val parts = line.split(";;")
                if (parts.size >= 3) {
                    Bookmark(
                        id = parts[0],
                        name = SafeDelimiterCodec.decode(parts[1]),
                        url = SafeDelimiterCodec.decode(parts[2]),
                        icon = if (parts.size >= 4) SafeDelimiterCodec.decode(parts[3]) else "📖"
                    )
                } else null
            }.ifEmpty { DEFAULT_BOOKMARKS }
        } catch (e: Exception) {
            DEFAULT_BOOKMARKS
        }
    }

    fun addBookmark(context: Context, name: String, url: String, icon: String = "📖"): List<Bookmark> {
        val trimmed = url.trim()
        if (trimmed.isBlank() || trimmed.startsWith("about:") || trimmed.startsWith("data:")) {
            return getBookmarks(context)
        }
        val current = getBookmarks(context).toMutableList()
        val normalizedUrl = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
        current.removeAll { it.url.trimEnd('/').equals(normalizedUrl.trimEnd('/'), ignoreCase = true) }
        val id = UUID.randomUUID().toString()
        val cleanName = name.ifBlank { normalizedUrl.substringAfter("://").substringBefore("/") }
        current.add(Bookmark(id, cleanName, normalizedUrl, icon))
        save(context, current)
        return current
    }

    fun removeBookmark(context: Context, id: String): List<Bookmark> {
        val current = getBookmarks(context).filter { it.id != id }
        save(context, current)
        return current
    }

    private fun save(context: Context, bookmarks: List<Bookmark>) {
        val raw = bookmarks.joinToString("\n") { 
            val encName = SafeDelimiterCodec.encode(it.name)
            val encUrl = SafeDelimiterCodec.encode(it.url)
            val encIcon = SafeDelimiterCodec.encode(it.icon)
            "${it.id};;$encName;;$encUrl;;$encIcon" 
        }
        getPrefs(context).edit().putString(KEY_BOOKMARKS, raw).apply()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
