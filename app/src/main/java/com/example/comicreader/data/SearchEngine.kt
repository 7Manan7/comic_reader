package com.example.comicreader.data

import java.net.URLEncoder

/**
 * Supported search engines for the Kuro Reader Home tab and address bar search.
 */
enum class SearchEngine(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val iconEmoji: String,
    val hintText: String
) {
    GOOGLE(
        id = "google",
        displayName = "Google",
        baseUrl = "https://www.google.com/search?q=",
        iconEmoji = "🔍",
        hintText = "Search with Google..."
    ),
    DUCKDUCKGO(
        id = "duckduckgo",
        displayName = "DuckDuckGo",
        baseUrl = "https://duckduckgo.com/?q=",
        iconEmoji = "🦆",
        hintText = "Search privately with DuckDuckGo..."
    ),
    BRAVE(
        id = "brave",
        displayName = "Brave",
        baseUrl = "https://search.brave.com/search?q=",
        iconEmoji = "🦁",
        hintText = "Search with Brave..."
    ),
    BING(
        id = "bing",
        displayName = "Bing",
        baseUrl = "https://www.bing.com/search?q=",
        iconEmoji = "🌐",
        hintText = "Search with Bing..."
    ),
    ECOSIA(
        id = "ecosia",
        displayName = "Ecosia",
        baseUrl = "https://www.ecosia.org/search?q=",
        iconEmoji = "🌱",
        hintText = "Plant trees while searching with Ecosia..."
    );

    fun buildUrl(query: String): String {
        val trimmed = query.trim()
        val encoded = try {
            URLEncoder.encode(trimmed, "UTF-8")
        } catch (e: Exception) {
            trimmed.replace(" ", "+")
        }
        return baseUrl + encoded
    }

    companion object {
        fun fromId(id: String?): SearchEngine {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: GOOGLE
        }
    }
}
