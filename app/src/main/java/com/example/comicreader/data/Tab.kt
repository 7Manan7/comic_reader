package com.example.comicreader.data

import java.util.UUID

/**
 * Represents a single browser tab in Kuro Reader.
 */
data class Tab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Tab",
    val url: String = AppPreferences.DEFAULT_HOME_URL,
    val lastAccessed: Long = System.currentTimeMillis()
)
