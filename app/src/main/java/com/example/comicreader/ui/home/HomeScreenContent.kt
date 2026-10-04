package com.example.comicreader.ui.home

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.comicreader.R
import com.example.comicreader.data.AppPreferences
import com.example.comicreader.data.Bookmark
import com.example.comicreader.data.BookmarkManager
import com.example.comicreader.data.HistoryItem
import com.example.comicreader.data.HistoryManager
import com.example.comicreader.data.ManhwaDirectory
import com.example.comicreader.data.ManhwaSite
import com.example.comicreader.data.SearchEngine

/**
 * Dedicated Native Home Tab Composable for Kuro Reader.
 *
 * Provides:
 * 1. Search Engine selection (Google, DuckDuckGo, Brave, Bing, Ecosia) with instant search.
 * 2. Curated Manhwa & Manga sites directory with 1-tap reading and New Tab opening.
 * 3. Integrated bookmarking allowing users to star any site or add custom manhwa URLs.
 * 4. Recent history resume section for quick chapter continuation.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreenContent(
    onNavigate: (url: String, title: String) -> Unit,
    onOpenInNewTab: (url: String, title: String) -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Search Engine State
    var selectedEngine by remember { mutableStateOf(AppPreferences.getSearchEngine(context)) }
    var searchQuery by remember { mutableStateOf("") }

    // Bookmarks and History
    var userBookmarks by remember { mutableStateOf(BookmarkManager.getBookmarks(context)) }
    var recentHistory by remember { mutableStateOf(HistoryManager.getHistory(context).take(4)) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var newBookmarkTitle by remember { mutableStateOf("") }
    var newBookmarkUrl by remember { mutableStateOf("") }

    // Popular manhwa search suggestions
    val trendingSearches = remember {
        listOf(
            "Solo Leveling",
            "Tower of God",
            "Omniscient Reader",
            "Return of the Mount Hua Sect",
            "Nano Machine",
            "Pick Me Up Infinite Gacha"
        )
    }

    // Filter categories for manhwa sites
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Top Manhwa", "Cultivation", "Webtoons", "Scanlations")

    val filteredSites = remember(selectedCategory) {
        when (selectedCategory) {
            "Top Manhwa" -> ManhwaDirectory.SITES.filter { it.tag.contains("Manhwa", ignoreCase = true) }
            "Cultivation" -> ManhwaDirectory.SITES.filter { it.description.contains("Cultivation", ignoreCase = true) || it.tag.contains("Fantasy", ignoreCase = true) }
            "Webtoons" -> ManhwaDirectory.SITES.filter { it.tag.contains("Webtoon", ignoreCase = true) || it.name.contains("Webtoon", ignoreCase = true) }
            "Scanlations" -> ManhwaDirectory.SITES.filter { it.tag.contains("Scan", ignoreCase = true) || it.description.contains("Scan", ignoreCase = true) }
            else -> ManhwaDirectory.SITES
        }
    }

    fun executeSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            focusManager.clearFocus()
            val destination = selectedEngine.buildUrl(trimmed)
            onNavigate(destination, "$trimmed - ${selectedEngine.displayName}")
        }
    }

    fun toggleBookmark(site: ManhwaSite) {
        val existing = userBookmarks.find { it.url.trimEnd('/') == site.url.trimEnd('/') }
        userBookmarks = if (existing != null) {
            BookmarkManager.removeBookmark(context, existing.id)
        } else {
            BookmarkManager.addBookmark(context, site.name, site.url, site.iconEmoji)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1117))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. HERO HEADER WITH APP LOGO ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF2563EB), Color(0xFF7C3AED))
                        )
                    )
                    .border(1.5.dp, Color(0xFF60A5FA).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.book_5295974),
                    contentDescription = "Kuro Reader Logo",
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "KURO READER",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFF1E3A8A),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "MANHWA",
                            color = Color(0xFF93C5FD),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "High-speed reader with AdBlock & 165Hz display",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 2. SEARCH ENGINES & SEARCH BAR ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF242938))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Search engine selection header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SEARCH ENGINE",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Text(
                        text = "Active: ${selectedEngine.displayName}",
                        color = Color(0xFF60A5FA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Horizontal list of search engine selector chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchEngine.entries.forEach { engine ->
                        val isSelected = engine == selectedEngine
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedEngine = engine
                                    AppPreferences.setSearchEngine(context, engine)
                                },
                            color = if (isSelected) Color(0xFF1D3557) else Color(0xFF1F2432),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF2B3244)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = engine.iconEmoji, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = engine.displayName,
                                    color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Input Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = selectedEngine.hintText,
                            color = Color(0xFF64748B),
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    },
                    leadingIcon = {
                        Text(
                            text = selectedEngine.iconEmoji,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Button(
                                onClick = { executeSearch(searchQuery) },
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .height(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2563EB)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Go", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { executeSearch(searchQuery) }),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFF2A3144),
                        focusedContainerColor = Color(0xFF10131A),
                        unfocusedContainerColor = Color(0xFF10131A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Trending search suggestions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Popular:",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    trendingSearches.forEach { title ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    searchQuery = title
                                    executeSearch(title)
                                },
                            color = Color(0xFF1F2432),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = title,
                                color = Color(0xFF93C5FD),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 3. CURATED BOOKMARKED MANHWA SITES ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📚 Bookmarked Manhwa Sites",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap any source to start reading instantly",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }

            Surface(
                color = Color(0xFF1F2432),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "${ManhwaDirectory.SITES.size} sources",
                    color = Color(0xFF60A5FA),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Category filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                val isSelected = category == selectedCategory
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedCategory = category },
                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFF181B26),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF3B82F6) else Color(0xFF282F42))
                ) {
                    Text(
                        text = category,
                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Manhwa sites grid
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            filteredSites.forEach { site ->
                val isBookmarked = userBookmarks.any { it.url.trimEnd('/') == site.url.trimEnd('/') }
                ManhwaSiteCard(
                    site = site,
                    isBookmarked = isBookmarked,
                    onOpen = { onNavigate(site.url, site.name) },
                    onOpenInNewTab = { onOpenInNewTab(site.url, site.name) },
                    onToggleBookmark = { toggleBookmark(site) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 4. MY SAVED BOOKMARKS SECTION ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF242938))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "⭐ My Bookmarks",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${userBookmarks.size} custom & pinned shortcuts",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = { showAddBookmarkDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add",
                            color = Color(0xFF60A5FA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (userBookmarks.isEmpty()) {
                    Text(
                        text = "No custom bookmarks yet. Tap the star on any site above to bookmark it!",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        userBookmarks.forEach { bookmark ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onNavigate(bookmark.url, bookmark.name) },
                                color = Color(0xFF1B202D),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF262C3E))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = bookmark.icon, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = bookmark.name,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = bookmark.url,
                                            color = Color(0xFF64748B),
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            userBookmarks = BookmarkManager.removeBookmark(context, bookmark.id)
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove",
                                            tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 5. RECENT HISTORY SECTION ---
        if (recentHistory.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF242938))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🕒 Continue Reading",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Jump right back into recently opened chapters",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }

                        TextButton(onClick = onOpenHistory) {
                            Text(
                                text = "All History",
                                color = Color(0xFF60A5FA),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        recentHistory.forEach { item ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onNavigate(item.url, item.title) },
                                color = Color(0xFF1B202D),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title.ifBlank { item.url },
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.url,
                                            color = Color(0xFF64748B),
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Open",
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Add Bookmark Dialog
    if (showAddBookmarkDialog) {
        Dialog(onDismissRequest = { showAddBookmarkDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                color = Color(0xFF1E2230),
                border = BorderStroke(1.dp, Color(0xFF333A4D))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Add Custom Bookmark",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newBookmarkTitle,
                        onValueChange = { newBookmarkTitle = it },
                        label = { Text("Site Name (e.g. Asura Scans)", color = Color.Gray, fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF475569)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newBookmarkUrl,
                        onValueChange = { newBookmarkUrl = it },
                        label = { Text("URL (e.g. asuracomic.net)", color = Color.Gray, fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF475569)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddBookmarkDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newBookmarkUrl.isNotBlank()) {
                                    userBookmarks = BookmarkManager.addBookmark(
                                        context,
                                        newBookmarkTitle,
                                        newBookmarkUrl,
                                        "📖"
                                    )
                                    newBookmarkTitle = ""
                                    newBookmarkUrl = ""
                                    showAddBookmarkDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("Save Bookmark")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual Card representing a curated Manhwa & Manga Site.
 */
@Composable
private fun ManhwaSiteCard(
    site: ManhwaSite,
    isBookmarked: Boolean,
    onOpen: () -> Unit,
    onOpenInNewTab: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(site.accentColorHex)

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF232838))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top Row: Emoji Icon + Title + Star Bookmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.18f))
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = site.iconEmoji, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = site.name,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = site.tag,
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) Color(0xFFFBBF24) else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = site.description,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row: Open In New Tab button + Read button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenInNewTab),
                    color = Color(0xFF1E2433),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+ New Tab",
                            color = Color(0xFF93C5FD),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpen),
                    color = accentColor.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Read ➔",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
