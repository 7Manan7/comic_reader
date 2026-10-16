package com.example.comicreader.data

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class HistoryItem(
    val id: String,
    val title: String,
    val url: String,
    val timestamp: Long,
    val scrollProgress: Float = 0f,
    val detectedChapter: String? = null
) {
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    val formattedProgress: String
        get() {
            val pct = (scrollProgress.coerceIn(0f, 1f) * 100).toInt()
            return "$pct%"
        }
}

/**
 * Manages comic reader browsing history with persistent local storage.
 */
object HistoryManager {
    private const val PREFS_NAME = "comic_reader_history"
    private const val KEY_HISTORY = "history_entries"
    private const val MAX_HISTORY_ITEMS = 500

    fun getHistory(context: Context): List<HistoryItem> {
        val prefs = getPrefs(context)
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()

        return try {
            raw.lines().mapNotNull { line ->
                val parts = line.split(";;")
                if (parts.size >= 4) {
                    val progress = if (parts.size >= 5) parts[4].toFloatOrNull() ?: 0f else 0f
                    val chapter = if (parts.size >= 6) SafeDelimiterCodec.decode(parts[5]).ifBlank { null } else null
                    HistoryItem(
                        id = parts[0],
                        title = SafeDelimiterCodec.decode(parts[1]),
                        url = SafeDelimiterCodec.decode(parts[2]),
                        timestamp = parts[3].toLongOrNull() ?: 0L,
                        scrollProgress = progress,
                        detectedChapter = chapter
                    )
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addHistoryEntry(
        context: Context,
        title: String,
        url: String,
        scrollProgress: Float = 0f,
        detectedChapter: String? = null
    ): List<HistoryItem> {
        if (url.isBlank() || url.startsWith("about:") || url.startsWith("data:")) {
            return getHistory(context)
        }
        val current = getHistory(context).toMutableList()

        // Preserve previous progress/chapter if not provided in new call
        val previous = current.find { it.url.equals(url, ignoreCase = true) }
        val resolvedProgress = if (scrollProgress > 0f) scrollProgress else (previous?.scrollProgress ?: 0f)
        val resolvedChapter = detectedChapter ?: previous?.detectedChapter

        // Remove duplicate to bring to top
        current.removeAll { it.url.equals(url, ignoreCase = true) }

        val cleanTitle = title.ifBlank {
            runCatching { java.net.URI(url).host }.getOrNull() ?: url
        }
        val item = HistoryItem(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            url = url,
            timestamp = System.currentTimeMillis(),
            scrollProgress = resolvedProgress,
            detectedChapter = resolvedChapter
        )
        current.add(0, item)

        val trimmed = if (current.size > MAX_HISTORY_ITEMS) current.take(MAX_HISTORY_ITEMS) else current
        save(context, trimmed)
        return trimmed
    }

    fun updateProgress(
        context: Context,
        url: String,
        scrollProgress: Float,
        detectedChapter: String? = null
    ): List<HistoryItem> {
        if (url.isBlank() || url.startsWith("about:")) return getHistory(context)
        val current = getHistory(context).map { item ->
            if (item.url.equals(url, ignoreCase = true)) {
                item.copy(
                    scrollProgress = scrollProgress.coerceIn(0f, 1f),
                    detectedChapter = detectedChapter ?: item.detectedChapter
                )
            } else {
                item
            }
        }
        save(context, current)
        return current
    }

    fun deleteEntry(context: Context, id: String): List<HistoryItem> {
        val current = getHistory(context).filter { it.id != id }
        save(context, current)
        return current
    }

    fun clearAll(context: Context): List<HistoryItem> {
        getPrefs(context).edit().remove(KEY_HISTORY).apply()
        return emptyList()
    }

    private fun save(context: Context, items: List<HistoryItem>) {
        val raw = items.joinToString("\n") { 
            val encTitle = SafeDelimiterCodec.encode(it.title)
            val encUrl = SafeDelimiterCodec.encode(it.url)
            val encChapter = SafeDelimiterCodec.encode(it.detectedChapter.orEmpty())
            "${it.id};;$encTitle;;$encUrl;;${it.timestamp};;${it.scrollProgress};;$encChapter" 
        }
        getPrefs(context).edit().putString(KEY_HISTORY, raw).apply()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
