package com.example.comicreader.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search

import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.comicreader.data.Bookmark
import com.example.comicreader.data.BookmarkManager
import com.example.comicreader.data.HistoryItem
import com.example.comicreader.data.HistoryManager
import com.example.comicreader.data.SessionManager
import com.example.comicreader.data.AppPreferences
import com.example.comicreader.data.Tab
import com.example.comicreader.data.TabManager
import com.example.comicreader.data.SearchEngine
import com.example.comicreader.ui.home.HomeScreenContent
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
    onToggleVolumeScroll: (Boolean) -> Unit,
    onRegisterWebView: (ComicWebView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val coroutineScope = rememberCoroutineScope()
    val adBlockListsStatus by AdBlockListManager.status.collectAsState()

    // Tabs state with persistent multi-tab support
    var tabs by remember { mutableStateOf(TabManager.getTabs(context)) }
    var activeTabId by remember { mutableStateOf(TabManager.getActiveTabId(context)) }
    var showTabsSheet by remember { mutableStateOf(false) }

    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull() ?: Tab().also {
        tabs = listOf(it)
        activeTabId = it.id
    }
    val isHomeTab = activeTab.url == AppPreferences.DEFAULT_HOME_URL || activeTab.url == "about:home" || activeTab.url.isBlank()

    // Session & Startup state
    var isRestoreLastPageEnabled by remember {
        mutableStateOf(SessionManager.isRestoreLastPageEnabled(context))
    }

    // Web navigation state
    var currentUrl by remember { mutableStateOf(activeTab.url) }
    var inputUrl by remember { mutableStateOf(activeTab.url) }
    var pageTitle by remember { mutableStateOf(activeTab.title) }
    var pageProgress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var blockedAdCount by remember { mutableIntStateOf(0) }

    var webViewRef by remember { mutableStateOf<ComicWebView?>(null) }

    // Multi-tab WebViews cache (keeps background tabs in memory for instant switching without losing reading progress)
    val tabWebViews = remember { mutableMapOf<String, ComicWebView>() }

    fun selectTab(tabId: String) {
        if (tabId != activeTabId) {
            tabWebViews[activeTabId]?.onPause()
            activeTabId = tabId
            TabManager.setActiveTabId(context, tabId)
            val target = tabs.find { it.id == tabId }
            if (target != null) {
                currentUrl = target.url
                inputUrl = if (target.url == AppPreferences.DEFAULT_HOME_URL || target.url == "about:home") "" else target.url
                pageTitle = target.title
                val targetWebView = tabWebViews[tabId]
                if (targetWebView != null) {
                    canGoBack = targetWebView.canGoBack()
                    canGoForward = targetWebView.canGoForward()
                    webViewRef = targetWebView
                    onRegisterWebView(targetWebView)
                    targetWebView.onResume()
                } else {
                    canGoBack = false
                    canGoForward = false
                    webViewRef = null
                }
            }
        }
    }

    fun addNewTab(
        url: String = AppPreferences.DEFAULT_HOME_URL,
        title: String = "Home",
        isIncognito: Boolean = false
    ) {
        val cleanTitle = if (title == "Home" && isIncognito) "Private Tab" else title
        val (updatedTabs, newTab) = TabManager.createTab(context, url, cleanTitle, isIncognito)
        tabs = updatedTabs
        selectTab(newTab.id)
    }

    fun closeTab(tabId: String) {
        tabWebViews[tabId]?.let { wv ->
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.stopLoading()
            wv.clearHistory()
            wv.clearCache(true)
            wv.destroy()
            tabWebViews.remove(tabId)
        }
        val (updatedTabs, newActiveId) = TabManager.closeTab(context, tabId)
        tabs = updatedTabs
        if (activeTabId == tabId) {
            activeTabId = newActiveId
            val target = updatedTabs.find { it.id == newActiveId }
            if (target != null) {
                currentUrl = target.url
                inputUrl = if (target.url == AppPreferences.DEFAULT_HOME_URL || target.url == "about:home") "" else target.url
                pageTitle = target.title
                val targetWebView = tabWebViews[newActiveId]
                if (targetWebView != null) {
                    canGoBack = targetWebView.canGoBack()
                    canGoForward = targetWebView.canGoForward()
                    webViewRef = targetWebView
                    onRegisterWebView(targetWebView)
                    targetWebView.onResume()
                } else {
                    canGoBack = false
                    canGoForward = false
                    webViewRef = null
                }
            }
        }
    }

    fun closeAllTabs() {
        tabWebViews.values.forEach { wv ->
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.stopLoading()
            wv.clearHistory()
            wv.clearCache(true)
            wv.destroy()
        }
        tabWebViews.clear()
        val (updatedTabs, newActiveId) = TabManager.closeAllTabs(context)
        tabs = updatedTabs
        activeTabId = newActiveId
        val target = updatedTabs.first()
        currentUrl = target.url
        inputUrl = ""
        pageTitle = target.title
        canGoBack = false
        canGoForward = false
        webViewRef = null
    }

    fun closeAllIncognitoTabs() {
        tabs.filter { it.isIncognito }.forEach { tab ->
            tabWebViews[tab.id]?.let { wv ->
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.stopLoading()
                wv.clearHistory()
                wv.clearCache(true)
                wv.destroy()
                tabWebViews.remove(tab.id)
            }
        }
        val (updatedTabs, newActiveId) = TabManager.closeAllIncognitoTabs(context)
        tabs = updatedTabs
        selectTab(newActiveId)
    }

    DisposableEffect(Unit) {
        onDispose {
            tabWebViews.values.forEach { wv ->
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.stopLoading()
                wv.destroy()
            }
            tabWebViews.clear()
        }
    }

    // UI visibility state (Default to standard browser view)
    var isHudVisible by remember { mutableStateOf(true) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showAdBlockDialog by remember { mutableStateOf(false) }
    var showBraveMenu by remember { mutableStateOf(false) }

    // History state
    var historyList by remember { mutableStateOf(HistoryManager.getHistory(context)) }
    var historySearchQuery by remember { mutableStateOf("") }

    // Settings state (Persisted across restarts via AppPreferences)
    var isImmersiveFullscreen by remember { mutableStateOf(AppPreferences.isImmersiveFullscreen(context)) }
    var isNightInvertMode by remember { mutableStateOf(AppPreferences.isNightInvertMode(context)) }
    var isKeepScreenOn by remember { mutableStateOf(AppPreferences.isKeepScreenOn(context)) }
    var isVolumeScrollEnabled by remember { mutableStateOf(AppPreferences.isVolumeScrollEnabled(context)) }
    var webTextZoom by remember { mutableIntStateOf(AppPreferences.getWebTextZoom(context)) }
    var isUrlInputFocused by remember { mutableStateOf(false) }

    // Refresh rate state
    val activeRefreshRate by RefreshRateManager.currentRefreshRate.collectAsState()
    val availableRates by RefreshRateManager.availableModes.collectAsState()
    val selectedRateLabel by RefreshRateManager.selectedModeLabel.collectAsState()

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
    BackHandler(enabled = showTabsSheet || canGoBack || !isHomeTab) {
        if (showTabsSheet) {
            showTabsSheet = false
        } else if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else if (!isHomeTab) {
            val homeUrl = AppPreferences.DEFAULT_HOME_URL
            currentUrl = homeUrl
            inputUrl = ""
            pageTitle = if (activeTab.isIncognito) "Private Tab" else "Home"
            tabs = TabManager.updateTab(context, activeTabId, title = pageTitle, url = homeUrl)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top Navigation Bar (Auto-hiding HUD)
        AnimatedVisibility(
            visible = isHudVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (activeTab.isIncognito) Color(0xE6140D1E) else Color(0xE61E1E24))
                    .then(if (!isImmersiveFullscreen) Modifier.statusBarsPadding() else Modifier)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Full-Width Firefox-style URL / Search bar
                    val displayUrl = if (inputUrl == AppPreferences.DEFAULT_HOME_URL || inputUrl == "about:home") "" else inputUrl
                    OutlinedTextField(
                        value = displayUrl,
                        onValueChange = { inputUrl = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .onFocusChanged { isUrlInputFocused = it.isFocused },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = if (activeTab.isIncognito) "Search or enter private URL..." else "Search or enter comic URL...",
                                color = if (activeTab.isIncognito) Color(0xFFA855F7).copy(alpha = 0.7f) else Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            if (activeTab.isIncognito) {
                                Text(
                                    text = "🕶️",
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            } else if (isHomeTab) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(
                                    text = "🛡️",
                                    fontSize = 14.sp,
                                    modifier = Modifier
                                        .clickable { showAdBlockDialog = true }
                                        .padding(start = 6.dp)
                                )
                            }
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (displayUrl.isNotEmpty()) {
                                    IconButton(
                                        onClick = { inputUrl = "" },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                if (!isHomeTab) {
                                    IconButton(
                                        onClick = { webViewRef?.reload() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Reload",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            focusManager.clearFocus()
                            val destination = normalizeUrl(inputUrl, context)
                            currentUrl = destination
                            val title = if (destination.startsWith("http")) inputUrl else if (activeTab.isIncognito) "Private Tab" else "Home"
                            tabs = TabManager.updateTab(context, activeTabId, title = title, url = destination)
                            tabWebViews[activeTabId]?.loadUrl(destination)
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (activeTab.isIncognito) Color(0xFFA855F7) else Color(0xFF64B5F6),
                            unfocusedBorderColor = if (activeTab.isIncognito) Color(0xFF581C87) else Color(0xFF44444F),
                            focusedContainerColor = if (activeTab.isIncognito) Color(0xFF221634) else Color(0xFF2A2A32),
                            unfocusedContainerColor = if (activeTab.isIncognito) Color(0xFF221634) else Color(0xFF2A2A32)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Tabs Switcher Button
                    TabBadgeButton(
                        tabCount = tabs.size,
                        isIncognito = activeTab.isIncognito,
                        onClick = { showTabsSheet = true }
                    )

                    // Brave Menu (⋮)
                    IconButton(
                        onClick = { showBraveMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu & Settings",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Web Content View & Loading Progress Indicator
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isHomeTab) {
                HomeScreenContent(
                    onNavigate = { destination, title ->
                        currentUrl = destination
                        inputUrl = destination
                        pageTitle = title
                        tabs = TabManager.updateTab(context, activeTabId, title = title, url = destination)
                        tabWebViews[activeTabId]?.loadUrl(destination)
                    },
                    onOpenInNewTab = { destination, title ->
                        addNewTab(url = destination, title = title, isIncognito = activeTab.isIncognito)
                    },
                    onOpenHistory = {
                        historyList = HistoryManager.getHistory(context)
                        showHistorySheet = true
                    },
                    isIncognito = activeTab.isIncognito,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Main Comic WebView keyed to active tab
                key(activeTabId) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            val existing = tabWebViews[activeTabId]
                            val targetTabId = activeTabId
                            val wv = if (existing != null) {
                                (existing.parent as? ViewGroup)?.removeView(existing)
                                existing.onResume()
                                existing.resumeTimers()
                                if (existing.url != activeTab.url && activeTab.url.startsWith("http")) {
                                    existing.loadUrl(activeTab.url)
                                }
                                existing
                            } else {
                                ComicWebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    configureIncognito(activeTab.isIncognito)

                                    onProgressChanged = { progress ->
                                        if (activeTabId == targetTabId) {
                                            pageProgress = progress
                                            canGoBack = canGoBack()
                                            canGoForward = canGoForward()
                                        }
                                    }

                                    onTitleReceived = { title ->
                                        if (activeTabId == targetTabId) {
                                            pageTitle = title
                                            if (currentUrl.startsWith("http") && title.isNotBlank() && !activeTab.isIncognito) {
                                                historyList = HistoryManager.addHistoryEntry(context, title, currentUrl)
                                            }
                                        }
                                        tabs = TabManager.updateTab(context, targetTabId, title = title)
                                    }

                                    onUrlChanged = { newUrl ->
                                        if (activeTabId == targetTabId) {
                                            currentUrl = newUrl
                                            inputUrl = newUrl
                                            canGoBack = canGoBack()
                                            canGoForward = canGoForward()
                                            if (newUrl.startsWith("http") && !activeTab.isIncognito) {
                                                val title = pageTitle.ifBlank { newUrl }
                                                historyList = HistoryManager.addHistoryEntry(context, title, newUrl)
                                                SessionManager.saveLastUrl(context, newUrl)
                                            }
                                        }
                                        tabs = TabManager.updateTab(context, targetTabId, url = newUrl)
                                    }

                                    onNewTabRequested = { targetUrl ->
                                        addNewTab(targetUrl, "New Tab", isIncognito = activeTab.isIncognito)
                                    }

                                    onSwipeBackToHome = {
                                        val homeUrl = AppPreferences.DEFAULT_HOME_URL
                                        currentUrl = homeUrl
                                        inputUrl = ""
                                        pageTitle = if (activeTab.isIncognito) "Private Tab" else "Home"
                                        tabs = TabManager.updateTab(context, targetTabId, title = pageTitle, url = homeUrl)
                                    }

                                    onScrollDirectionChanged = { isScrollingDown ->
                                        if (isScrollingDown) {
                                            if (isHudVisible) {
                                                isHudVisible = false
                                            }
                                            if (!isImmersiveFullscreen) {
                                                onToggleFullscreen(true)
                                            }
                                        } else {
                                            if (!isHudVisible) {
                                                isHudVisible = true
                                            }
                                            if (!isImmersiveFullscreen) {
                                                onToggleFullscreen(false)
                                            }
                                        }
                                    }

                                    onSingleTap = {
                                        val showBrowser = !isHudVisible
                                        isHudVisible = showBrowser
                                        if (!isImmersiveFullscreen) {
                                            onToggleFullscreen(!showBrowser)
                                        }
                                    }

                                    onBlockedAdCountChanged = { count ->
                                        if (activeTabId == targetTabId) {
                                            blockedAdCount = count
                                        }
                                    }

                                    onTouchFocus = {
                                        if (isUrlInputFocused) {
                                            focusManager.clearFocus()
                                            isUrlInputFocused = false
                                        }
                                    }

                                    loadUrl(activeTab.url)
                                    tabWebViews[targetTabId] = this
                                }
                            }
                            webViewRef = wv
                            onRegisterWebView(wv)
                            wv
                        },
                        update = { webView ->
                            webViewRef = webView
                            webView.setInvertMode(isNightInvertMode)
                            webView.settings.textZoom = webTextZoom
                            if (webView.url != activeTab.url && activeTab.url.startsWith("http")) {
                                webView.loadUrl(activeTab.url)
                            }
                        }
                    )
                }

                // Loading Progress Bar
                if (pageProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { pageProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        color = Color(0xFF64B5F6),
                        trackColor = Color.Transparent
                    )
                }
            }
        }

        // Bottom Reading Bar (Auto-hiding HUD)
        AnimatedVisibility(
            visible = isHudVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (activeTab.isIncognito) Color(0xE6140D1E) else Color(0xE61E1E24))
                    .then(if (!isImmersiveFullscreen) Modifier.navigationBarsPadding() else Modifier)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                // Clean Bottom Navigation Bar (No arrows - swiping controlled)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home Button
                    IconButton(onClick = {
                        val homeUrl = AppPreferences.DEFAULT_HOME_URL
                        currentUrl = homeUrl
                        inputUrl = ""
                        pageTitle = if (activeTab.isIncognito) "Private Tab" else "Home"
                        tabs = TabManager.updateTab(context, activeTabId, title = pageTitle, url = homeUrl)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = if (isHomeTab) (if (activeTab.isIncognito) Color(0xFFA855F7) else Color(0xFF64B5F6)) else Color.White
                        )
                    }

                    // + New Tab Button
                    IconButton(onClick = { addNewTab(isIncognito = activeTab.isIncognito) }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Tab",
                            tint = Color.White
                        )
                    }

                    // Incognito / Private Tab Quick Launcher
                    IconButton(onClick = {
                        val existingIncognito = tabs.firstOrNull { it.isIncognito }
                        if (existingIncognito != null && !activeTab.isIncognito) {
                            selectTab(existingIncognito.id)
                        } else {
                            addNewTab(isIncognito = true)
                        }
                    }) {
                        Surface(
                            shape = CircleShape,
                            color = if (activeTab.isIncognito) Color(0xFF581C87) else Color.Transparent
                        ) {
                            Text(
                                text = "🕶️",
                                fontSize = 16.sp,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }

                    // Tabs Switcher Button
                    TabBadgeButton(
                        tabCount = tabs.size,
                        isIncognito = activeTab.isIncognito,
                        onClick = { showTabsSheet = true }
                    )

                    // Brave Menu (⋮)
                    IconButton(onClick = { showBraveMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu & Settings",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }



    // Brave Browser-Style Unified Menu (All settings visible, safe boundary borders)
    if (showBraveMenu || showSettingsSheet || showAdBlockDialog) {
        Dialog(
            onDismissRequest = {
                showBraveMenu = false
                showSettingsSheet = false
                showAdBlockDialog = false
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            showBraveMenu = false
                            showSettingsSheet = false
                            showAdBlockDialog = false
                        }
                    )
                    // Invisible border on up, down, left and right:
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.86f)
                        // Visible high-contrast border for the menu:
                        .border(
                            width = 2.dp,
                            color = Color(0xFF535A7B),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* consume click */ }
                        ),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF141620),
                    shadowElevation = 24.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Fixed Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1D202D))
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🦁 Kuro Menu",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (AdBlockEngine.isEnabled) Color(0xFF1B5E20) else Color(0xFFB71C1C))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "🛡️ $blockedAdCount blocked",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    showBraveMenu = false
                                    showSettingsSheet = false
                                    showAdBlockDialog = false
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Menu",
                                    tint = Color.LightGray
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFF2B2E42), thickness = 1.dp)

                        // Scrollable Content Column (Every setting is visible here)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // 1. Quick Action Bar (Brave style)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2230)),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFF2E3246))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Reload
                                    IconButton(
                                        onClick = {
                                            webViewRef?.reload()
                                            showBraveMenu = false
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Reload",
                                            tint = Color.White
                                        )
                                    }

                                    // Star / Bookmark current page
                                    val isCurrentBookmarked = bookmarks.any { it.url.trimEnd('/') == currentUrl.trimEnd('/') }
                                    IconButton(
                                        onClick = {
                                            val currentHost = runCatching { Uri.parse(currentUrl).host }.getOrNull() ?: currentUrl
                                            val title = pageTitle.ifBlank { currentHost }
                                            if (isCurrentBookmarked) {
                                                val existing = bookmarks.find { it.url.trimEnd('/') == currentUrl.trimEnd('/') }
                                                if (existing != null) {
                                                    bookmarks = BookmarkManager.removeBookmark(context, existing.id)
                                                }
                                            } else {
                                                bookmarks = BookmarkManager.addBookmark(context, title, currentUrl, "⭐")
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrentBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "Bookmark Page",
                                            tint = if (isCurrentBookmarked) Color(0xFFFFD54F) else Color.White
                                        )
                                    }

                                    // Dark / OLED Invert toggle
                                    IconButton(
                                        onClick = {
                                            val newMode = !isNightInvertMode
                                            isNightInvertMode = newMode
                                            AppPreferences.setNightInvertMode(context, newMode)
                                        }
                                    ) {
                                        Text(
                                            text = if (isNightInvertMode) "🌙" else "☀️",
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Private Browsing Quick Action
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        showBraveMenu = false
                                        addNewTab(isIncognito = true)
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF261938)),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "🕶️", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "New Private Tab",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Browse without saving history, cache, or cookies",
                                            color = Color(0xFFC084FC),
                                            fontSize = 11.sp
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Open",
                                        tint = Color(0xFFA855F7),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // 2. Refresh Rate Selector (up to 165Hz)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E212E)),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFF2B2E42))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "⚡ Screen Refresh Rate",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64B5F6),
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Current: ${activeRefreshRate.roundToInt()} Hz • Selected: $selectedRateLabel",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val targets = listOf(
                                            "Auto" to -1f,
                                            "165Hz" to 165f,
                                            "144Hz" to 144f,
                                            "120Hz" to 120f,
                                            "60Hz" to 60f
                                        )
                                        targets.forEach { (label, rate) ->
                                            val isSelected = (rate <= 0f && selectedRateLabel.startsWith("Auto")) ||
                                                    (rate > 0f && selectedRateLabel.startsWith("${rate.roundToInt()}"))

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isSelected) Color(0xFF1E88E5) else Color(0xFF282B3B))
                                                    .clickable {
                                                        activity?.let {
                                                            RefreshRateManager.setRefreshRate(it, rate)
                                                        }
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (isSelected) Color.White else Color.LightGray,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. Kuro Shields & AdBlock Engine (16 Lists)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E212E)),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFF2B2E42))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(
                                                text = "🛡️ Kuro Shields",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "${adBlockListsStatus.activeListCount}/16 lists active • ${adBlockListsStatus.totalRuleCount} rules",
                                                fontSize = 11.sp,
                                                color = Color(0xFF81C784)
                                            )
                                        }
                                        Switch(
                                            checked = AdBlockEngine.isEnabled,
                                            onCheckedChange = {
                                                AdBlockEngine.isEnabled = it
                                                AppPreferences.setAdBlockEnabled(context, it)
                                                webViewRef?.reload()
                                            }
                                        )
                                    }

                                    // Whitelist toggle for current host
                                    val currentHost = runCatching { Uri.parse(currentUrl).host }.getOrNull().orEmpty()
                                    if (currentHost.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        val isWhitelisted = AdBlockEngine.isWhitelisted(currentHost)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Whitelist $currentHost",
                                                color = Color.LightGray,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                                            )
                                            Switch(
                                                checked = isWhitelisted,
                                                onCheckedChange = { enable ->
                                                    if (enable) AdBlockEngine.addToWhitelist(currentHost, context)
                                                    else AdBlockEngine.removeFromWhitelist(currentHost, context)
                                                    webViewRef?.reload()
                                                }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Refresh Filter Lists Button
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
                                                modifier = Modifier.size(14.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Updating Lists...", fontSize = 12.sp, color = Color.White)
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Update Lists",
                                                modifier = Modifier.size(14.dp),
                                                tint = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("🔄 Update Filter Lists Now", fontSize = 12.sp, color = Color.White)
                                        }
                                    }

                                    // Category list toggles expander
                                    var showFilterListDetails by remember { mutableStateOf(false) }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    TextButton(
                                        onClick = { showFilterListDetails = !showFilterListDetails },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (showFilterListDetails) "Hide Filter Lists ▲" else "View / Toggle Filter Lists (${adBlockListsStatus.activeListCount}/${AdBlockListManager.ALL_LISTS.size}) ▼",
                                            color = Color(0xFF64B5F6),
                                            fontSize = 12.sp
                                        )
                                    }

                                    if (showFilterListDetails) {
                                        FilterListCategory.entries.forEach { category ->
                                            val categoryLists = AdBlockListManager.ALL_LISTS.filter { it.category == category }
                                            val activeCategoryCount = categoryLists.count { listDef ->
                                                adBlockListsStatus.listItems[listDef.id]?.isEnabled ?: listDef.isEnabledByDefault
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = category.title,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF90CAF9),
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = "$activeCategoryCount/${categoryLists.size}",
                                                    color = Color(0xFF81C784),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            categoryLists.forEach { listDef ->
                                                val itemStatus = adBlockListsStatus.listItems[listDef.id]
                                                val isChecked = itemStatus?.isEnabled ?: listDef.isEnabledByDefault
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 2.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = listDef.name,
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                                    )
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
                            }

                            // 4. Reading Controls & Toggles
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E212E)),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFF2B2E42))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Immersive Fullscreen
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text("🔲 Immersive Fullscreen", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            Text("Hides status bar and cutouts completely", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Switch(
                                            checked = isImmersiveFullscreen,
                                            onCheckedChange = {
                                                isImmersiveFullscreen = it
                                                AppPreferences.setImmersiveFullscreen(context, it)
                                                onToggleFullscreen(it)
                                            }
                                        )
                                    }

                                    // Night OLED Invert
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text("🌙 OLED Dark / Invert", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            Text("Inverts white web pages for dark rooms", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Switch(
                                            checked = isNightInvertMode,
                                            onCheckedChange = {
                                                isNightInvertMode = it
                                                AppPreferences.setNightInvertMode(context, it)
                                            }
                                        )
                                    }

                                    // Volume Key Scrolling
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text("🔊 Volume Button Scrolling", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            Text("Turn pages with hardware volume rocker", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Switch(
                                            checked = isVolumeScrollEnabled,
                                            onCheckedChange = {
                                                isVolumeScrollEnabled = it
                                                onToggleVolumeScroll(it)
                                                AppPreferences.setVolumeScrollEnabled(context, it)
                                            }
                                        )
                                    }

                                    // Keep Screen Awake
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text("💡 Keep Screen Awake", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            Text("Prevents display sleep while reading chapters", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Switch(
                                            checked = isKeepScreenOn,
                                            onCheckedChange = {
                                                isKeepScreenOn = it
                                                onToggleKeepScreenOn(it)
                                                AppPreferences.setKeepScreenOn(context, it)
                                            }
                                        )
                                    }

                                    // Restore Last Visited Page on Startup
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text("📖 Restore Last Visited Page", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            Text("Reopen last comic/chapter when app restarts", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Switch(
                                            checked = isRestoreLastPageEnabled,
                                            onCheckedChange = { enabled ->
                                                isRestoreLastPageEnabled = enabled
                                                SessionManager.setRestoreLastPageEnabled(context, enabled)
                                            }
                                        )
                                    }

                                    // Page Zoom
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text("🔍 Page Zoom", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            Text("$webTextZoom%", color = Color(0xFF64B5F6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFF282B3B))
                                                    .clickable {
                                                        if (webTextZoom > 75) {
                                                            webTextZoom -= 25
                                                            AppPreferences.setWebTextZoom(context, webTextZoom)
                                                        }
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text("-25%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (webTextZoom == 100) Color(0xFF1E88E5) else Color(0xFF282B3B))
                                                    .clickable {
                                                        webTextZoom = 100
                                                        AppPreferences.setWebTextZoom(context, 100)
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text("100%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFF282B3B))
                                                    .clickable {
                                                        if (webTextZoom < 250) {
                                                            webTextZoom += 25
                                                            AppPreferences.setWebTextZoom(context, webTextZoom)
                                                        }
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text("+25%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }

                            // 5. Quick Navigation Links (Tabs, New Tab, Bookmarks, History)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        showBraveMenu = false
                                        showTabsSheet = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64B5F6)),
                                    border = BorderStroke(1.dp, Color(0xFF3F445A)),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text("📑 Tabs (${tabs.size})", fontSize = 11.sp, maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = {
                                        showBraveMenu = false
                                        addNewTab()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64B5F6)),
                                    border = BorderStroke(1.dp, Color(0xFF3F445A)),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "New Tab", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("New", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        showBraveMenu = false
                                        showBookmarksSheet = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64B5F6)),
                                    border = BorderStroke(1.dp, Color(0xFF3F445A)),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Bookmark, contentDescription = "Bookmarks", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Sites", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        showBraveMenu = false
                                        historyList = HistoryManager.getHistory(context)
                                        showHistorySheet = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64B5F6)),
                                    border = BorderStroke(1.dp, Color(0xFF3F445A)),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.History, contentDescription = "History", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("History", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
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
                    .navigationBarsPadding()
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
                val isAlreadyBookmarked = bookmarks.any { it.url.trimEnd('/') == currentUrl.trimEnd('/') }
                Button(
                    onClick = {
                        if (!isAlreadyBookmarked) {
                            val currentHost = runCatching { Uri.parse(currentUrl).host }.getOrNull() ?: currentUrl
                            val title = pageTitle.ifBlank { currentHost }
                            bookmarks = BookmarkManager.addBookmark(context, title, currentUrl, "⭐")
                        }
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
                                    currentUrl = bookmark.url
                                    tabs = TabManager.updateTab(context, activeTabId, title = bookmark.name, url = bookmark.url)
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
                    .navigationBarsPadding()
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
                                            currentUrl = item.url
                                            tabs = TabManager.updateTab(context, activeTabId, title = item.title, url = item.url)
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

    // Tabs Switcher Dialog / Overview
    if (showTabsSheet) {
        var tabFilter by remember {
            mutableStateOf(if (activeTab.isIncognito) "private" else "standard")
        }
        val regularTabs = tabs.filter { !it.isIncognito }
        val incognitoTabs = tabs.filter { it.isIncognito }
        val displayedTabs = if (tabFilter == "private") incognitoTabs else regularTabs

        Dialog(
            onDismissRequest = { showTabsSheet = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showTabsSheet = false }
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.90f)
                        .border(
                            width = 2.dp,
                            color = if (tabFilter == "private") Color(0xFF9333EA).copy(alpha = 0.6f) else Color(0xFF3B4261),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* consume click */ }
                        ),
                    shape = RoundedCornerShape(24.dp),
                    color = if (tabFilter == "private") Color(0xFF130F1E) else Color(0xFF141620),
                    shadowElevation = 24.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Top Header: Segmented switch (Standard vs Private) + Close
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Segmented Switcher Pill
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1F2333))
                                    .padding(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Standard Tabs Button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (tabFilter == "standard") Color(0xFF2563EB) else Color.Transparent
                                        )
                                        .clickable { tabFilter = "standard" }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "📑 Tabs (${regularTabs.size})",
                                        fontSize = 12.sp,
                                        fontWeight = if (tabFilter == "standard") FontWeight.Bold else FontWeight.Medium,
                                        color = if (tabFilter == "standard") Color.White else Color(0xFF94A3B8)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Private Tabs Button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (tabFilter == "private") Color(0xFF7E22CE) else Color.Transparent
                                        )
                                        .clickable { tabFilter = "private" }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "🕶️ Private (${incognitoTabs.size})",
                                        fontSize = 12.sp,
                                        fontWeight = if (tabFilter == "private") FontWeight.Bold else FontWeight.Medium,
                                        color = if (tabFilter == "private") Color.White else Color(0xFF94A3B8)
                                    )
                                }
                            }

                            // Close Dialog Button
                            IconButton(
                                onClick = { showTabsSheet = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Secondary Action Bar (Close All / Add Tab header shortcuts)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (tabFilter == "private") "Private Tabs" else "Open Tabs",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (tabFilter == "private") Color(0xFFD8B4FE) else Color(0xFFCBD5E1)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (tabFilter == "private" && incognitoTabs.isNotEmpty()) {
                                    TextButton(
                                        onClick = {
                                            closeAllIncognitoTabs()
                                            tabFilter = "standard"
                                        },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Close All Private",
                                            color = Color(0xFFF87171),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                } else if (tabFilter == "standard" && regularTabs.size > 1) {
                                    TextButton(
                                        onClick = {
                                            closeAllTabs()
                                            showTabsSheet = false
                                        },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Close All",
                                            color = Color(0xFFEF5350),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Tab Content Area: either empty state (if private empty) or grid
                        if (tabFilter == "private" && incognitoTabs.isEmpty()) {
                            // Sleek Firefox-style Private Browsing Empty State
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF3B0764).copy(alpha = 0.7f))
                                            .border(1.5.dp, Color(0xFFA855F7), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "🕶️", fontSize = 34.sp)
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "Private Browsing",
                                        color = Color(0xFFF3E8FF),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Comic Reader won't save visited pages, search queries, or cookies when using private tabs.\n\nSwipe left and right anywhere to navigate history effortlessly.",
                                        color = Color(0xFFC084FC).copy(alpha = 0.85f),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 17.sp
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Button(
                                        onClick = {
                                            addNewTab(isIncognito = true)
                                            showTabsSheet = false
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "New Private Tab",
                                            modifier = Modifier.size(16.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open Private Tab", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                    }
                                }
                            }
                        } else {
                            // Grid of Tabs
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 145.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(displayedTabs, key = { it.id }) { tab ->
                                    val isActive = tab.id == activeTabId
                                    val domain = runCatching { Uri.parse(tab.url).host }.getOrNull().orEmpty()
                                    val isComicSite = domain.contains("comix") || domain.contains("manga")

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clickable {
                                                selectTab(tab.id)
                                                showTabsSheet = false
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (tab.isIncognito) {
                                                if (isActive) Color(0xFF2E1065) else Color(0xFF1B0E2B)
                                            } else {
                                                if (isActive) Color(0xFF1E2438) else Color(0xFF1C1F2C)
                                            }
                                        ),
                                        border = BorderStroke(
                                            width = if (isActive) 2.dp else 1.dp,
                                            color = if (tab.isIncognito) {
                                                if (isActive) Color(0xFFA855F7) else Color(0xFF4C1D95)
                                            } else {
                                                if (isActive) Color(0xFF64B5F6) else Color(0xFF2E3346)
                                            }
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Card Top: Icon, Domain, Close Button
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = if (tab.isIncognito) "🕶️" else if (isComicSite) "📚" else "🌐",
                                                        fontSize = 13.sp
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = domain.ifBlank { if (tab.isIncognito) "Private" else "Home" },
                                                        color = if (tab.isIncognito) Color(0xFFD8B4FE) else Color(0xFF90A4AE),
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                // Close Tab button
                                                IconButton(
                                                    onClick = {
                                                        closeTab(tab.id)
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Close tab",
                                                        tint = Color.Gray,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }

                                            // Card Middle: Title & Active Badge
                                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                                Text(
                                                    text = tab.title.ifBlank { if (tab.isIncognito) "Private Tab" else "New Tab" },
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (isActive) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(
                                                                if (tab.isIncognito) Color(0xFF7E22CE) else Color(0xFF1E88E5)
                                                            )
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = if (tab.isIncognito) "🕶️ ACTIVE" else "ACTIVE",
                                                            color = Color.White,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.ExtraBold
                                                        )
                                                    }
                                                }
                                            }

                                            // Card Bottom: URL preview
                                            Text(
                                                text = if (tab.url == AppPreferences.DEFAULT_HOME_URL || tab.url == "about:home") "about:home" else tab.url,
                                                color = if (tab.isIncognito) Color(0xFFA855F7).copy(alpha = 0.7f) else Color.DarkGray,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bottom Action Button: + New Tab (or New Private Tab)
                        Button(
                            onClick = {
                                if (tabFilter == "private") {
                                    addNewTab(isIncognito = true)
                                } else {
                                    addNewTab(isIncognito = false)
                                }
                                showTabsSheet = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (tabFilter == "private") Color(0xFF9333EA) else Color(0xFF2979FF)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = if (tabFilter == "private") "New Private Tab" else "New Tab",
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (tabFilter == "private") "New Private Tab" else "New Tab",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab counter badge button displaying the number of active tabs inside a rounded box.
 */
@Composable
private fun TabBadgeButton(
    tabCount: Int,
    isIncognito: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(
                    width = 1.8.dp,
                    color = if (isIncognito) Color(0xFFA855F7) else Color.White,
                    shape = RoundedCornerShape(6.dp)
                )
                .background(
                    if (isIncognito) Color(0xFF581C87).copy(alpha = 0.4f) else Color.Transparent,
                    shape = RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isIncognito) "🕶️" else (if (tabCount > 99) "99+" else tabCount.toString()),
                color = if (isIncognito) Color(0xFFE9D5FF) else Color.White,
                fontSize = if (isIncognito) 10.sp else (if (tabCount > 9) 10.sp else 12.sp),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp
            )
        }
    }
}

/**
 * Normalizes input string to either a valid URL or a search query using the preferred search engine.
 */
private fun normalizeUrl(input: String, context: Context): String {
    val trimmed = input.trim()
    if (trimmed.isEmpty() || trimmed == "about:home") {
        return AppPreferences.DEFAULT_HOME_URL
    }
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("about:")) {
        return trimmed
    }
    if (trimmed.contains(".") && !trimmed.contains(" ")) {
        return "https://$trimmed"
    }
    // Search query using preferred search engine
    val engine = AppPreferences.getSearchEngine(context)
    return engine.buildUrl(trimmed)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
