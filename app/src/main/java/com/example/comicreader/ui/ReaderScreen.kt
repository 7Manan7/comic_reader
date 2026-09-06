package com.example.comicreader.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import com.example.comicreader.data.Bookmark
import com.example.comicreader.data.BookmarkManager
import com.example.comicreader.data.HistoryItem
import com.example.comicreader.data.HistoryManager
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.comicreader.adblock.AdBlockListManager
import com.example.comicreader.adblock.FilterListCategory
import com.example.comicreader.adblock.FilterListDefinition
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.comicreader.adblock.AdBlockEngine
import com.example.comicreader.display.RefreshRateManager
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    onToggleFullscreen: (Boolean) -> Unit,
    onToggleKeepScreenOn: (Boolean) -> Unit,
    onRegisterWebView: (ComicWebView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val coroutineScope = rememberCoroutineScope()
    val adBlockListsStatus by AdBlockListManager.status.collectAsState()

    // Web navigation state (Default to comix.to)
    var currentUrl by remember { mutableStateOf("https://comix.to/") }
    var inputUrl by remember { mutableStateOf("https://comix.to/") }
    var pageTitle by remember { mutableStateOf("Kuro Reader") }
    var pageProgress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var blockedAdCount by remember { mutableIntStateOf(0) }

    // UI visibility state (Default to standard browser view)
    var isHudVisible by remember { mutableStateOf(true) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showAdBlockDialog by remember { mutableStateOf(false) }

    // History state
    var historyList by remember { mutableStateOf(HistoryManager.getHistory(context)) }
    var historySearchQuery by remember { mutableStateOf("") }

    // Settings state (Default to false: standard browser mode, switch to fullscreen only on scroll down)
    var isImmersiveFullscreen by remember { mutableStateOf(false) }
    var isTransparentStatusBar by remember { mutableStateOf(false) }
    var isNightInvertMode by remember { mutableStateOf(false) }
    var isKeepScreenOn by remember { mutableStateOf(true) }
    var isVolumeScrollEnabled by remember { mutableStateOf(true) }
    var webTextZoom by remember { mutableIntStateOf(100) }

    // Refresh rate state
    val activeRefreshRate by RefreshRateManager.currentRefreshRate.collectAsState()
    val availableRates by RefreshRateManager.availableModes.collectAsState()
    val selectedRateLabel by RefreshRateManager.selectedModeLabel.collectAsState()

    var webViewRef by remember { mutableStateOf<ComicWebView?>(null) }
    val focusManager = LocalFocusManager.current

    // Bookmarks state with persistent storage (Comix, MangaFreak, MangaKatana + custom additions)
    var bookmarks by remember { mutableStateOf(BookmarkManager.getBookmarks(context)) }
    var isAddingCustomBookmark by remember { mutableStateOf(false) }
    var customBookmarkName by remember { mutableStateOf("") }
    var customBookmarkUrl by remember { mutableStateOf("") }

    // Initialize activity fullscreen & screen awake settings
    LaunchedEffect(isImmersiveFullscreen) {
        onToggleFullscreen(isImmersiveFullscreen)
    }

    LaunchedEffect(isKeepScreenOn) {
        onToggleKeepScreenOn(isKeepScreenOn)
    }

    // Hardware back press handler
    BackHandler(enabled = canGoBack) {
        webViewRef?.let {
            if (it.canGoBack()) {
                it.goBack()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Main Comic WebView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                ComicWebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    onProgressChanged = { progress ->
                        pageProgress = progress
                        canGoBack = canGoBack()
                        canGoForward = canGoForward()
                    }

                    onTitleReceived = { title ->
                        pageTitle = title
                        if (currentUrl.startsWith("http")) {
                            historyList = HistoryManager.addHistoryEntry(context, title, currentUrl)
                        }
                    }

                    onUrlChanged = { newUrl ->
                        currentUrl = newUrl
                        inputUrl = newUrl
                        canGoBack = canGoBack()
                        canGoForward = canGoForward()
                        if (newUrl.startsWith("http")) {
                            val title = pageTitle.ifBlank { newUrl }
                            historyList = HistoryManager.addHistoryEntry(context, title, newUrl)
                        }
                    }

                    onScrollDirectionChanged = { isScrollingDown ->
                        if (isScrollingDown) {
                            // User scrolled down: switch to fullscreen reading mode
                            if (isHudVisible || !isImmersiveFullscreen) {
                                isHudVisible = false
                                isImmersiveFullscreen = true
                                onToggleFullscreen(true)
                            }
                        } else {
                            // User scrolled up or reached top: restore basic browser UI
                            if (!isHudVisible || isImmersiveFullscreen) {
                                isHudVisible = true
                                isImmersiveFullscreen = false
                                onToggleFullscreen(false)
                            }
                        }
                    }

                    onSingleTap = {
                        // Toggle UI HUD and fullscreen mode on tap
                        val showBrowser = !isHudVisible
                        isHudVisible = showBrowser
                        isImmersiveFullscreen = !showBrowser
                        onToggleFullscreen(!showBrowser)
                    }

                    onBlockedAdCountChanged = { count ->
                        blockedAdCount = count
                    }

                    loadUrl(currentUrl)
                    webViewRef = this
                    onRegisterWebView(this)
                }
            },
            update = { webView ->
                webViewRef = webView
                webView.setInvertMode(isNightInvertMode)
                webView.settings.textZoom = webTextZoom
            }
        )

        // Loading Progress Bar
        if (pageProgress in 1..99) {
            LinearProgressIndicator(
                progress = { pageProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .then(
                        if (isHudVisible) Modifier.statusBarsPadding().padding(top = 56.dp)
                        else Modifier
                    ),
                color = Color(0xFF64B5F6),
                trackColor = Color.Transparent
            )
        }

        // Top Navigation Bar (Auto-hiding HUD)
        AnimatedVisibility(
            visible = isHudVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xE61E1E24))
                    .then(if (!isImmersiveFullscreen) Modifier.statusBarsPadding() else Modifier)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    IconButton(
                        onClick = { webViewRef?.let { if (it.canGoBack()) it.goBack() } },
                        enabled = canGoBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (canGoBack) Color.White else Color.Gray
                        )
                    }

                    // Forward button
                    IconButton(
                        onClick = { webViewRef?.let { if (it.canGoForward()) it.goForward() } },
                        enabled = canGoForward
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward",
                            tint = if (canGoForward) Color.White else Color.Gray
                        )
                    }

                    // URL / Search text box
                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        singleLine = true,
                        placeholder = {
                            Text("Search or enter comic URL...", color = Color.Gray, maxLines = 1)
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            focusManager.clearFocus()
                            val destination = normalizeUrl(inputUrl)
                            webViewRef?.loadUrl(destination)
                        }),
                        trailingIcon = {
                            if (inputUrl.isNotEmpty()) {
                                IconButton(onClick = { inputUrl = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color.Gray
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF64B5F6),
                            unfocusedBorderColor = Color(0xFF44444F),
                            focusedContainerColor = Color(0xFF2A2A32),
                            unfocusedContainerColor = Color(0xFF2A2A32)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // AdBlock Shield Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (AdBlockEngine.isEnabled) Color(0xFF1B5E20) else Color(0xFFB71C1C))
                            .clickable { showAdBlockDialog = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🛡️ $blockedAdCount",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Refresh Button
                    IconButton(onClick = { webViewRef?.reload() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Bottom Reading Bar (Auto-hiding HUD)
        AnimatedVisibility(
            visible = isHudVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xE61E1E24))
                    .then(if (!isImmersiveFullscreen) Modifier.navigationBarsPadding() else Modifier)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                // Quick Comic Bookmarks Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bookmarks.forEach { bookmark ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF2E2E38))
                                .clickable {
                                    inputUrl = bookmark.url
                                    webViewRef?.loadUrl(bookmark.url)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${bookmark.icon} ${bookmark.name}",
                                color = Color(0xFFE0E0E0),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // "+ Sites" button to open Bookmarks Sheet
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E88E5))
                            .clickable { showBookmarksSheet = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Manage Sites",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sites",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // "History" button to open History Sheet
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF37474F))
                            .clickable {
                                historyList = HistoryManager.getHistory(context)
                                showHistorySheet = true
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "History",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home Button
                    IconButton(onClick = {
                        inputUrl = "https://comix.to/"
                        webViewRef?.loadUrl("https://comix.to/")
                    }) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White)
                    }

                    // Refresh Rate Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1E88E5))
                            .clickable { showSettingsSheet = true }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "⚡ ${activeRefreshRate.roundToInt()}Hz",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Fullscreen / Status Bar Mode Toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isImmersiveFullscreen) Color(0xFF43A047) else Color(0xFF757575))
                            .clickable {
                                isImmersiveFullscreen = !isImmersiveFullscreen
                                onToggleFullscreen(isImmersiveFullscreen)
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (isImmersiveFullscreen) "🔲 Fullscreen" else "🔲 Status Bar",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }

                    // Night Invert Filter Toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isNightInvertMode) Color(0xFF673AB7) else Color(0xFF37474F))
                            .clickable { isNightInvertMode = !isNightInvertMode }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (isNightInvertMode) "🌙 Dark" else "☀️ Normal",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }

                    // Browsing History Button
                    IconButton(onClick = {
                        historyList = HistoryManager.getHistory(context)
                        showHistorySheet = true
                    }) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = Color.White)
                    }

                    // Reader Settings Button
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }
            }
        }
    }

    // AdBlock Info Dialog / Bottom Sheet
    if (showAdBlockDialog) {
        ModalBottomSheet(
            onDismissRequest = { showAdBlockDialog = false },
            containerColor = Color(0xFF222228),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛡️ Comic AdBlocker",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Switch(
                        checked = AdBlockEngine.isEnabled,
                        onCheckedChange = {
                            AdBlockEngine.isEnabled = it
                            webViewRef?.reload()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D36)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Blocked on this page: $blockedAdCount ads & trackers",
                            color = Color(0xFF81C784),
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Active rules: ${adBlockListsStatus.totalRuleCount} across ${adBlockListsStatus.activeListCount} filter lists",
                            fontSize = 12.sp,
                            color = Color(0xFF90CAF9)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Last updated: ${adBlockListsStatus.formattedLastUpdated}",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                        adBlockListsStatus.statusMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = msg,
                                fontSize = 11.sp,
                                color = Color(0xFFFFD54F)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val currentHost = runCatching { Uri.parse(currentUrl).host }.getOrNull().orEmpty()
                if (currentHost.isNotEmpty()) {
                    val isWhitelisted = AdBlockEngine.isWhitelisted(currentHost)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Whitelist $currentHost", color = Color.White)
                        Switch(
                            checked = isWhitelisted,
                            onCheckedChange = { enable ->
                                if (enable) AdBlockEngine.addToWhitelist(currentHost)
                                else AdBlockEngine.removeFromWhitelist(currentHost)
                                webViewRef?.reload()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Refresh & Update Filter Lists Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            AdBlockListManager.updateLists(context)
                        }
                    },
                    enabled = !adBlockListsStatus.isUpdating,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (adBlockListsStatus.isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Updating Filter Lists...", fontSize = 13.sp, color = Color.White)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Update Lists",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🔄 Refresh & Update Filter Lists Now", fontSize = 13.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        showAdBlockDialog = false
                        showSettingsSheet = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64B5F6))
                ) {
                    Text("⚙️ Manage 16 Filter Lists (${adBlockListsStatus.activeListCount} active)")
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Reader Settings Bottom Sheet
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = Color(0xFF222228),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "⚙️ Kuro Reader & Display Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // High Refresh Rate Section
                Text(
                    text = "Screen Refresh Rate Optimization",
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64B5F6),
                    fontSize = 14.sp
                )
                Text(
                    text = "Current Display: ${activeRefreshRate.roundToInt()} Hz | Mode: $selectedRateLabel",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Refresh Rate Buttons (Auto-Max, 165Hz, 144Hz, 120Hz, 60Hz)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val targets = listOf(
                        "Max (Auto)" to -1f,
                        "165 Hz" to 165f,
                        "144 Hz" to 144f,
                        "120 Hz" to 120f,
                        "60 Hz" to 60f
                    )

                    targets.forEach { (label, rate) ->
                        val isSelected = (rate <= 0f && selectedRateLabel.startsWith("Auto")) ||
                                (rate > 0f && selectedRateLabel.startsWith("${rate.roundToInt()}"))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF1E88E5) else Color(0xFF33333E))
                                .clickable {
                                    activity?.let {
                                        RefreshRateManager.setRefreshRate(it, rate)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color.LightGray,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status Bar / Fullscreen Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Immersive Fullscreen", color = Color.White)
                        Text(
                            "Hides status bar and notch completely for distraction-free comics",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isImmersiveFullscreen,
                        onCheckedChange = {
                            isImmersiveFullscreen = it
                            onToggleFullscreen(it)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Volume Keys Scroll
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Volume Key Scrolling", color = Color.White)
                        Text(
                            "Scroll up/down with Volume buttons for one-handed reading",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isVolumeScrollEnabled,
                        onCheckedChange = { isVolumeScrollEnabled = it }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Keep Screen On
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Keep Screen On", color = Color.White)
                        Text(
                            "Prevents screen timeout while reading manga chapters",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isKeepScreenOn,
                        onCheckedChange = {
                            isKeepScreenOn = it
                            onToggleKeepScreenOn(it)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Zoom Level Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Page Zoom: $webTextZoom%", color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { if (webTextZoom > 75) webTextZoom -= 25 }) {
                            Text("-25%", color = Color(0xFF64B5F6))
                        }
                        TextButton(onClick = { webTextZoom = 100 }) {
                            Text("100%", color = Color(0xFF64B5F6))
                        }
                        TextButton(onClick = { if (webTextZoom < 250) webTextZoom += 25 }) {
                            Text("+25%", color = Color(0xFF64B5F6))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // AdBlock Filter Lists Section (16 Lists across 3 Categories)
                Text(
                    text = "🛡️ AdBlock & Security Filter Lists (16 Lists)",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64B5F6),
                    fontSize = 15.sp
                )
                Text(
                    text = "Active Rules: ${adBlockListsStatus.totalRuleCount} | Enabled Lists: ${adBlockListsStatus.activeListCount}/16",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Group lists by Category
                FilterListCategory.entries.forEach { category ->
                    val categoryLists = AdBlockListManager.ALL_LISTS.filter { it.category == category }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E36)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = category.title,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Text(
                                text = category.subtitle,
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            categoryLists.forEach { listDef ->
                                val itemStatus = adBlockListsStatus.listItems[listDef.id]
                                val isChecked = itemStatus?.isEnabled ?: listDef.isEnabledByDefault
                                val ruleCount = itemStatus?.ruleCount ?: 0

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = listDef.name,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = if (isChecked && ruleCount > 0) "$ruleCount rules • ${listDef.description}" else listDef.description,
                                            color = if (isChecked && ruleCount > 0) Color(0xFF81C784) else Color.Gray,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Switch(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            coroutineScope.launch {
                                                AdBlockListManager.toggleList(context, listDef.id, checked)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    "Last updated: ${adBlockListsStatus.formattedLastUpdated}",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
                adBlockListsStatus.statusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        msg,
                        fontSize = 11.sp,
                        color = Color(0xFFFFD54F)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            AdBlockListManager.updateLists(context)
                        }
                    },
                    enabled = !adBlockListsStatus.isUpdating,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (adBlockListsStatus.isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Updating Filter Lists...", fontSize = 13.sp, color = Color.White)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Update Lists",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Update Lists Now", fontSize = 13.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Bookmarks / Sites Management Bottom Sheet
    if (showBookmarksSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBookmarksSheet = false
                isAddingCustomBookmark = false
            },
            containerColor = Color(0xFF222228),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📚 Manga & Comic Sites",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    IconButton(onClick = { showBookmarksSheet = false }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Action: Bookmark Current Page
                Button(
                    onClick = {
                        val currentHost = runCatching { Uri.parse(currentUrl).host }.getOrNull() ?: currentUrl
                        val title = pageTitle.ifBlank { currentHost }
                        bookmarks = BookmarkManager.addBookmark(context, title, currentUrl, "⭐")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdd,
                        contentDescription = "Bookmark Current Page",
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bookmark Current Page", fontSize = 14.sp, color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section: Add Custom Site
                if (!isAddingCustomBookmark) {
                    OutlinedButton(
                        onClick = { isAddingCustomBookmark = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64B5F6))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Custom Site",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Custom Manga Site", fontSize = 14.sp)
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D36)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Add New Site",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = customBookmarkName,
                                onValueChange = { customBookmarkName = it },
                                label = { Text("Site Name (e.g. MangaDex)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF64B5F6),
                                    unfocusedBorderColor = Color(0xFF55555F),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = Color(0xFF64B5F6),
                                    unfocusedLabelColor = Color.LightGray
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = customBookmarkUrl,
                                onValueChange = { customBookmarkUrl = it },
                                label = { Text("Site URL (e.g. mangadex.org)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF64B5F6),
                                    unfocusedBorderColor = Color(0xFF55555F),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = Color(0xFF64B5F6),
                                    unfocusedLabelColor = Color.LightGray
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        isAddingCustomBookmark = false
                                        customBookmarkName = ""
                                        customBookmarkUrl = ""
                                    }
                                ) {
                                    Text("Cancel", color = Color.Gray)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        if (customBookmarkUrl.isNotBlank()) {
                                            bookmarks = BookmarkManager.addBookmark(
                                                context,
                                                customBookmarkName,
                                                customBookmarkUrl,
                                                "🌐"
                                            )
                                            isAddingCustomBookmark = false
                                            customBookmarkName = ""
                                            customBookmarkUrl = ""
                                        }
                                    },
                                    enabled = customBookmarkUrl.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                                ) {
                                    Text("Save Site")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Saved Sites (${bookmarks.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF90CAF9)
                )

                Spacer(modifier = Modifier.height(8.dp))

                bookmarks.forEach { bookmark ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E38)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    inputUrl = bookmark.url
                                    webViewRef?.loadUrl(bookmark.url)
                                    showBookmarksSheet = false
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = bookmark.icon,
                                    fontSize = 20.sp,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Column {
                                    Text(
                                        text = bookmark.name,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = bookmark.url,
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    bookmarks = BookmarkManager.removeBookmark(context, bookmark.id)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Bookmark",
                                    tint = Color(0xFFEF5350),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // Browsing History Bottom Sheet
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showHistorySheet = false
                historySearchQuery = ""
            },
            containerColor = Color(0xFF222228),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Browsing History (${historyList.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (historyList.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    historyList = HistoryManager.clearAll(context)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear All",
                                    tint = Color(0xFFEF5350),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear", color = Color(0xFFEF5350), fontSize = 13.sp)
                            }
                        }
                        IconButton(onClick = {
                            showHistorySheet = false
                            historySearchQuery = ""
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search field
                if (historyList.isNotEmpty()) {
                    OutlinedTextField(
                        value = historySearchQuery,
                        onValueChange = { historySearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search history...", color = Color.Gray, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (historySearchQuery.isNotEmpty()) {
                                IconButton(onClick = { historySearchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF64B5F6),
                            unfocusedBorderColor = Color(0xFF44444F),
                            focusedContainerColor = Color(0xFF2A2A32),
                            unfocusedContainerColor = Color(0xFF2A2A32),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Filtered entries
                val filteredHistory = remember(historyList, historySearchQuery) {
                    if (historySearchQuery.isBlank()) historyList
                    else historyList.filter {
                        it.title.contains(historySearchQuery, ignoreCase = true) ||
                        it.url.contains(historySearchQuery, ignoreCase = true)
                    }
                }

                if (filteredHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (historyList.isEmpty()) "No browsing history yet" else "No matching history found",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        filteredHistory.forEach { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E38)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            inputUrl = item.url
                                            webViewRef?.loadUrl(item.url)
                                            showHistorySheet = false
                                            historySearchQuery = ""
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = item.title,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.url,
                                            fontSize = 11.sp,
                                            color = Color(0xFF90CAF9),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.formattedDate,
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            historyList = HistoryManager.deleteEntry(context, item.id)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete entry",
                                            tint = Color(0xFFEF5350),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }
            }
        }
    }
}

/**
 * Normalizes input string to either a valid URL or a search query.
 */
private fun normalizeUrl(input: String): String {
    val trimmed = input.trim()
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        return trimmed
    }
    if (trimmed.contains(".") && !trimmed.contains(" ")) {
        return "https://$trimmed"
    }
    // Search query using DuckDuckGo
    return "https://duckduckgo.com/?q=" + Uri.encode(trimmed)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
