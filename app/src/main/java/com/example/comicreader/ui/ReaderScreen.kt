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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import java.util.Locale
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
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
    var isVideoPlaying by remember { mutableStateOf(false) }

    // Reading Engine live HUD state
    var readingScrollProgress by remember { mutableFloatStateOf(activeTab.scrollProgress) }
    var detectedChapterLabel by remember { mutableStateOf(activeTab.detectedChapter.orEmpty()) }
    var prevChapterUrl by remember { mutableStateOf("") }
    var nextChapterUrl by remember { mutableStateOf("") }
    var isAutoScrolling by remember { mutableStateOf(false) }
    var showSpeedSlider by remember { mutableStateOf(false) }

    // Multi-tab WebViews cache (managed via WebViewPoolManager LRU hibernation)
    val tabWebViews = remember { mutableMapOf<String, ComicWebView>() }

    fun navigateToUrl(destination: String, title: String = "") {
        val normalized = normalizeUrl(destination, context)
        currentUrl = normalized
        inputUrl = if (normalized == AppPreferences.DEFAULT_HOME_URL || normalized == "about:home") "" else normalized
        val resolvedTitle = if (title.isNotBlank()) title else if (normalized.startsWith("http")) normalized else if (activeTab.isIncognito) "Private Tab" else "Home"
        pageTitle = resolvedTitle
        tabs = TabManager.updateTab(context, activeTabId, title = resolvedTitle, url = normalized)
        val wv = tabWebViews[activeTabId]
        if (wv != null && normalized.startsWith("http")) {
            wv.loadUrl(normalized)
        }
    }

    fun selectTab(tabId: String) {
        if (tabId != activeTabId) {
            tabWebViews[activeTabId]?.onPause()
            isVideoPlaying = false
            activeTabId = tabId
            TabManager.setActiveTabId(context, tabId)
            WebViewPoolManager.recordAccess(tabId)
            val target = tabs.find { it.id == tabId }
            if (target != null) {
                readingScrollProgress = target.scrollProgress
                detectedChapterLabel = target.detectedChapter.orEmpty()
                prevChapterUrl = ""
                nextChapterUrl = ""
                isAutoScrolling = false
                val targetWebView = tabWebViews[tabId]
                if (targetWebView != null) {
                    val liveUrl = targetWebView.url
                    if (!liveUrl.isNullOrBlank() && liveUrl.startsWith("http")) {
                        currentUrl = liveUrl
                        inputUrl = liveUrl
                        pageTitle = targetWebView.title ?: target.title
                    } else {
                        currentUrl = target.url
                        inputUrl = if (target.url == AppPreferences.DEFAULT_HOME_URL || target.url == "about:home") "" else target.url
                        pageTitle = target.title
                    }
                    canGoBack = targetWebView.canGoBack()
                    canGoForward = targetWebView.canGoForward()
                    webViewRef = targetWebView
                    onRegisterWebView(targetWebView)
                    targetWebView.onResume()
                } else {
                    currentUrl = target.url
                    inputUrl = if (target.url == AppPreferences.DEFAULT_HOME_URL || target.url == "about:home") "" else target.url
                    pageTitle = target.title
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
        WebViewPoolManager.onTabClosed(tabId)
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
        WebViewPoolManager.clearAll()
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
            WebViewPoolManager.onTabClosed(tab.id)
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
    var activeColorProfile by remember { mutableStateOf(AppPreferences.getReadingColorProfile(context)) }
    var isWebtoonSeamlessMode by remember { mutableStateOf(AppPreferences.isWebtoonSeamlessMode(context)) }
    var autoScrollSpeed by remember { mutableFloatStateOf(AppPreferences.getAutoScrollSpeed(context)) }
    var isKeepScreenOn by remember { mutableStateOf(AppPreferences.isKeepScreenOn(context)) }
    var isVolumeScrollEnabled by remember { mutableStateOf(AppPreferences.isVolumeScrollEnabled(context)) }
    var webTextZoom by remember { mutableIntStateOf(AppPreferences.getWebTextZoom(context)) }
    var isUrlInputFocused by remember { mutableStateOf(false) }

    // Hibernation state from WebViewPoolManager
    val hibernatedTabIds by WebViewPoolManager.hibernatedTabIds.collectAsState()
    val activeWebViewCount by WebViewPoolManager.activeWebViewCount.collectAsState()

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

    // Fullscreen video state (YouTube / HTML5 media)
    var customVideoView by remember { mutableStateOf<android.view.View?>(null) }
    var customVideoCallback by remember { mutableStateOf<android.webkit.WebChromeClient.CustomViewCallback?>(null) }

    // Initialize activity fullscreen & screen awake settings
    LaunchedEffect(isImmersiveFullscreen) {
        onToggleFullscreen(isImmersiveFullscreen)
    }

    LaunchedEffect(isKeepScreenOn, isVideoPlaying) {
        onToggleKeepScreenOn(isKeepScreenOn || isVideoPlaying)
    }

    // Hardware back press handler
    BackHandler(enabled = customVideoView != null || showTabsSheet || canGoBack || !isHomeTab) {
        if (customVideoView != null) {
            customVideoCallback?.onCustomViewHidden()
            customVideoView = null
            customVideoCallback = null
        } else if (showTabsSheet) {
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
                    // Full-Width URL / Search bar
                    val displayUrl = if (inputUrl == AppPreferences.DEFAULT_HOME_URL || inputUrl == "about:home") "" else inputUrl
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = if (activeTab.isIncognito) Color(0xFF221634) else Color(0xFF2A2A32),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isUrlInputFocused) {
                                if (activeTab.isIncognito) Color(0xFFA855F7) else Color(0xFF64B5F6)
                            } else {
                                if (activeTab.isIncognito) Color(0xFF581C87) else Color(0xFF44444F)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (activeTab.isIncognito) {
                                Text(
                                    text = "🕶️",
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            } else if (isHomeTab) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.Gray,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(end = 4.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            } else {
                                Text(
                                    text = "🛡️",
                                    fontSize = 14.sp,
                                    modifier = Modifier
                                        .clickable { showAdBlockDialog = true }
                                        .padding(end = 8.dp)
                                )
                            }

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (displayUrl.isEmpty()) {
                                    Text(
                                        text = if (activeTab.isIncognito) "Search or enter private URL..." else "Search or enter comic URL...",
                                        color = if (activeTab.isIncognito) Color(0xFFA855F7).copy(alpha = 0.7f) else Color.Gray,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = displayUrl,
                                    onValueChange = { inputUrl = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { isUrlInputFocused = it.isFocused },
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    singleLine = true,
                                    cursorBrush = SolidColor(if (activeTab.isIncognito) Color(0xFFA855F7) else Color(0xFF64B5F6)),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                    keyboardActions = KeyboardActions(onGo = {
                                        focusManager.clearFocus()
                                        navigateToUrl(inputUrl)
                                    })
                                )
                            }

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
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Dedicated Refresh Button
                    IconButton(
                        onClick = {
                            if (isHomeTab) {
                                tabWebViews[activeTabId]?.reload()
                            } else {
                                webViewRef?.reload()
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
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
                        navigateToUrl(destination, title)
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

                            fun ComicWebView.bindCallbacks() {
                                onShowCustomView = { view, callback ->
                                    customVideoView = view
                                    customVideoCallback = callback
                                }
                                onHideCustomView = {
                                    customVideoView = null
                                    customVideoCallback = null
                                }
                                onVideoPlaybackChanged = { isPlaying ->
                                    if (activeTabId == targetTabId) {
                                        isVideoPlaying = isPlaying
                                    }
                                }
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
                                        val urlForHistory = url ?: currentUrl
                                        if (urlForHistory.startsWith("http") && title.isNotBlank() && !activeTab.isIncognito) {
                                            historyList = HistoryManager.addHistoryEntry(context, title, urlForHistory)
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
                                onScrollProgress = { progress ->
                                    if (activeTabId == targetTabId) {
                                        readingScrollProgress = progress
                                        tabs = TabManager.updateTabScrollProgress(context, targetTabId, progress, detectedChapterLabel)
                                        if (!activeTab.isIncognito) {
                                            HistoryManager.updateProgress(context, currentUrl, progress, detectedChapterLabel)
                                        }
                                    }
                                }
                                onChapterNavigation = { label, prevUrl, nextUrl ->
                                    if (activeTabId == targetTabId) {
                                        if (label.isNotBlank()) {
                                            detectedChapterLabel = label
                                            tabs = TabManager.updateTabScrollProgress(context, targetTabId, readingScrollProgress, label)
                                        }
                                        prevChapterUrl = prevUrl
                                        nextChapterUrl = nextUrl
                                    }
                                }
                                onAutoScrollStatusChanged = { active ->
                                    if (activeTabId == targetTabId) {
                                        isAutoScrolling = active
                                    }
                                }
                                onTouchFocus = {
                                    if (isUrlInputFocused) {
                                        focusManager.clearFocus()
                                        isUrlInputFocused = false
                                    }
                                }
                            }

                            val wv = if (existing != null) {
                                (existing.parent as? ViewGroup)?.removeView(existing)
                                existing.onResume()
                                existing.resumeTimers()
                                existing.bindCallbacks()
                                existing.setColorProfile(activeColorProfile)
                                existing.isWebtoonSeamlessMode = isWebtoonSeamlessMode
                                existing.settings.textZoom = webTextZoom
                                canGoBack = existing.canGoBack()
                                canGoForward = existing.canGoForward()
                                blockedAdCount = AdBlockEngine.getBlockedCount()
                                if (existing.url.isNullOrBlank() && activeTab.url.startsWith("http")) {
                                    existing.loadUrl(activeTab.url)
                                }
                                existing
                            } else {
                                val hibernated = WebViewPoolManager.consumeHibernatedState(targetTabId)
                                val seamless = isWebtoonSeamlessMode
                                ComicWebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    configureIncognito(activeTab.isIncognito)
                                    bindCallbacks()
                                    this.isWebtoonSeamlessMode = seamless
                                    setColorProfile(activeColorProfile)
                                    settings.textZoom = webTextZoom
                                    if (hibernated?.webViewBundle != null) {
                                        restoreState(hibernated.webViewBundle)
                                        setPendingScrollRatio(hibernated.scrollPercentage)
                                    } else if (activeTab.url.startsWith("http")) {
                                        setPendingScrollRatio(activeTab.scrollProgress)
                                        loadUrl(activeTab.url)
                                    }
                                    tabWebViews[targetTabId] = this
                                    WebViewPoolManager.recordAccess(targetTabId)
                                    WebViewPoolManager.enforcePoolLimit(
                                        activeTabId = targetTabId,
                                        videoPlayingTabId = if (isVideoPlaying) activeTabId else null,
                                        liveWebViews = tabWebViews,
                                        tabTitles = tabs.associate { it.id to it.title }
                                    )
                                }
                            }
                            webViewRef = wv
                            onRegisterWebView(wv)
                            wv
                        },
                        update = { webView ->
                            webViewRef = webView
                            webView.setColorProfile(activeColorProfile)
                            webView.isWebtoonSeamlessMode = isWebtoonSeamlessMode
                            webView.settings.textZoom = webTextZoom
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

                // Top Edge Reading Progress Bar (Ultra-thin 2.5dp neon gradient when HUD collapsed)
                if (!isHudVisible && readingScrollProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .align(Alignment.TopCenter)
                            .background(Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(readingScrollProgress.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF00E5FF), Color(0xFF3B82F6), Color(0xFFA855F7))
                                    )
                                )
                        )
                    }
                }

                // Floating Quick-Action Reader Pill (visible when HUD is shown during web reading)
                if (isHudVisible) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(22.dp)),
                        color = Color(0xF2161A26),
                        border = BorderStroke(1.dp, Color(0xFF2C354E)),
                        shadowElevation = 12.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Prev Chapter Button
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (prevChapterUrl.isNotBlank()) Color(0xFF232B40) else Color(0xFF1B202D),
                                    border = BorderStroke(0.5.dp, if (prevChapterUrl.isNotBlank()) Color(0xFF3B82F6) else Color.Transparent),
                                    modifier = Modifier.clickable(enabled = prevChapterUrl.isNotBlank()) {
                                        navigateToUrl(prevChapterUrl)
                                    }
                                ) {
                                    Text(
                                        text = "⏮ Prev",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (prevChapterUrl.isNotBlank()) Color(0xFF93C5FD) else Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }

                                // Auto-Scroll Toggle (Cruise Mode)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isAutoScrolling) Color(0xFF166534) else Color(0xFF232B40),
                                    border = BorderStroke(0.5.dp, if (isAutoScrolling) Color(0xFF4ADE80) else Color(0xFF38BDF8)),
                                    modifier = Modifier.clickable {
                                        if (isAutoScrolling) {
                                            webViewRef?.stopAutoScroll()
                                        } else {
                                            webViewRef?.startAutoScroll(autoScrollSpeed)
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isAutoScrolling) "⏸ Cruise" else "▶ Cruise",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAutoScrolling) Color(0xFFDCFCE7) else Color(0xFFE0F2FE)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", autoScrollSpeed)}x",
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8),
                                            modifier = Modifier.clickable { showSpeedSlider = !showSpeedSlider }
                                        )
                                    }
                                }

                                // Chapter Badge / Scroll Percentage (Tap to scroll top or bottom)
                                val scrollPct = (readingScrollProgress * 100).toInt().coerceIn(0, 100)
                                val chText = if (detectedChapterLabel.isNotBlank()) "$detectedChapterLabel • $scrollPct%" else "$scrollPct%"
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF2E1A47),
                                    border = BorderStroke(0.5.dp, Color(0xFFA855F7)),
                                    modifier = Modifier.clickable {
                                        val wv = webViewRef
                                        if (wv != null) {
                                            if (readingScrollProgress > 0.5f) {
                                                wv.scrollTo(0, 0)
                                            } else {
                                                wv.scrollToBottom()
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        text = chText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE9D5FF),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }

                                // Next Chapter Button (highlighted accent when > 92% read)
                                val isNearEnd = readingScrollProgress >= 0.92f
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (nextChapterUrl.isNotBlank()) {
                                        if (isNearEnd) Color(0xFF1E3A8A) else Color(0xFF232B40)
                                    } else Color(0xFF1B202D),
                                    border = BorderStroke(
                                        width = if (isNearEnd && nextChapterUrl.isNotBlank()) 1.5.dp else 0.5.dp,
                                        color = if (nextChapterUrl.isNotBlank()) {
                                            if (isNearEnd) Color(0xFF60A5FA) else Color(0xFF3B82F6)
                                        } else Color.Transparent
                                    ),
                                    modifier = Modifier.clickable(enabled = nextChapterUrl.isNotBlank()) {
                                        navigateToUrl(nextChapterUrl)
                                    }
                                ) {
                                    Text(
                                        text = "Next ⏭",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (nextChapterUrl.isNotBlank()) {
                                            if (isNearEnd) Color(0xFFBFDBFE) else Color(0xFF93C5FD)
                                        } else Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            // Inline Cruise Speed Slider
                            if (showSpeedSlider) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Text("Speed", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    Slider(
                                        value = autoScrollSpeed,
                                        onValueChange = { newSpeed ->
                                            autoScrollSpeed = newSpeed
                                            AppPreferences.setAutoScrollSpeed(context, newSpeed)
                                            webViewRef?.setAutoScrollSpeed(newSpeed)
                                        },
                                        valueRange = 0.5f..5.0f,
                                        steps = 8,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFF38BDF8),
                                            activeTrackColor = Color(0xFF0284C7),
                                            inactiveTrackColor = Color(0xFF334155)
                                        ),
                                        modifier = Modifier.width(130.dp)
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", autoScrollSpeed)}x",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Reading Bar (Auto-hiding HUD)
        AnimatedVisibility(
            visible = isHudVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val haptic = LocalHapticFeedback.current
            var bottomBarDragOffset by remember { mutableFloatStateOf(0f) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (activeTab.isIncognito) Color(0xE6140D1E) else Color(0xE61E1E24))
                    .then(if (!isImmersiveFullscreen) Modifier.navigationBarsPadding() else Modifier)
                    .pointerInput(tabs, activeTabId) {
                        detectHorizontalDragGestures(
                            onDragStart = { bottomBarDragOffset = 0f },
                            onDragEnd = {
                                val currentIndex = tabs.indexOfFirst { it.id == activeTabId }
                                if (currentIndex != -1 && tabs.size > 1) {
                                    if (bottomBarDragOffset > 80f) {
                                        // Swiped right -> Previous tab
                                        val prevIndex = if (currentIndex > 0) currentIndex - 1 else tabs.size - 1
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectTab(tabs[prevIndex].id)
                                    } else if (bottomBarDragOffset < -80f) {
                                        // Swiped left -> Next tab
                                        val nextIndex = if (currentIndex < tabs.size - 1) currentIndex + 1 else 0
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectTab(tabs[nextIndex].id)
                                    }
                                }
                                bottomBarDragOffset = 0f
                            },
                            onDragCancel = { bottomBarDragOffset = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                bottomBarDragOffset += dragAmount
                            }
                        )
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                // Bottom Navigation Bar (Back, Forward, Home, Tabs, Menu)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button
                    IconButton(
                        onClick = {
                            if (webViewRef?.canGoBack() == true) {
                                webViewRef?.goBack()
                            } else if (!isHomeTab) {
                                val homeUrl = AppPreferences.DEFAULT_HOME_URL
                                currentUrl = homeUrl
                                inputUrl = ""
                                pageTitle = if (activeTab.isIncognito) "Private Tab" else "Home"
                                tabs = TabManager.updateTab(context, activeTabId, title = pageTitle, url = homeUrl)
                            }
                        },
                        enabled = canGoBack || !isHomeTab
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (canGoBack || !isHomeTab) Color.White else Color.Gray.copy(alpha = 0.4f)
                        )
                    }

                    // Forward Button
                    IconButton(
                        onClick = {
                            webViewRef?.let { if (it.canGoForward()) it.goForward() }
                        },
                        enabled = canGoForward
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward",
                            tint = if (canGoForward) Color.White else Color.Gray.copy(alpha = 0.4f)
                        )
                    }

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

    // Fullscreen HTML5 / YouTube video player overlay
    if (customVideoView != null) {
        Dialog(
            onDismissRequest = {
                customVideoCallback?.onCustomViewHidden()
                customVideoView = null
                customVideoCallback = null
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            AndroidView(
                factory = {
                    val view = customVideoView!!
                    (view.parent as? ViewGroup)?.removeView(view)
                    view
                },
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
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
                                                text = "${adBlockListsStatus.activeListCount}/${AdBlockListManager.ALL_LISTS.size} lists active • ${adBlockListsStatus.totalRuleCount} rules",
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

                                    // Reading Color Profiles (Original, OLED Dark, Warm Amber, E-Ink)
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text("🎨 Reading Color Profile", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text("Eye-comfort tuning for manga & webtoons", fontSize = 11.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            ReadingColorProfile.entries.forEach { profile ->
                                                val isSelected = profile == activeColorProfile
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(if (isSelected) Color(0xFF2563EB) else Color(0xFF282B3B))
                                                        .clickable {
                                                            activeColorProfile = profile
                                                            isNightInvertMode = (profile == ReadingColorProfile.OLED_INVERT)
                                                            AppPreferences.setReadingColorProfile(context, profile)
                                                            webViewRef?.setColorProfile(profile)
                                                        }
                                                        .padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(profile.iconEmoji, fontSize = 14.sp)
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = profile.displayName.substringBefore(" "),
                                                            fontSize = 10.sp,
                                                            color = if (isSelected) Color.White else Color.LightGray,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Seamless Webtoon Mode & Lazy Load Booster
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text("📜 Seamless Webtoon Stitcher", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            Text("Stitches image slices seamlessly & accelerates decoding", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Switch(
                                            checked = isWebtoonSeamlessMode,
                                            onCheckedChange = {
                                                isWebtoonSeamlessMode = it
                                                AppPreferences.setWebtoonSeamlessMode(context, it)
                                                webViewRef?.isWebtoonSeamlessMode = it
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
                                                onToggleKeepScreenOn(it || isVideoPlaying)
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
                                    navigateToUrl(bookmark.url, bookmark.name)
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
                                            navigateToUrl(item.url, item.title)
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
        var tabSearchQuery by remember { mutableStateOf("") }
        val regularTabs = tabs.filter { !it.isIncognito }
        val incognitoTabs = tabs.filter { it.isIncognito }
        val displayedTabs = if (tabFilter == "private") incognitoTabs else regularTabs
        val filteredTabs = if (tabSearchQuery.isBlank()) {
            displayedTabs
        } else {
            displayedTabs.filter {
                it.title.contains(tabSearchQuery, ignoreCase = true) ||
                it.url.contains(tabSearchQuery, ignoreCase = true)
            }
        }

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

                        // Search/Filter Tabs Input Bar when more than 4 tabs are open
                        if (tabs.size > 4) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E2232),
                                border = BorderStroke(1.dp, Color(0xFF2E354B))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    BasicTextField(
                                        value = tabSearchQuery,
                                        onValueChange = { tabSearchQuery = it },
                                        textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        cursorBrush = SolidColor(Color(0xFF64B5F6)),
                                        decorationBox = { innerTextField ->
                                            if (tabSearchQuery.isEmpty()) {
                                                Text("Search open tabs...", color = Color.Gray, fontSize = 12.sp)
                                            }
                                            innerTextField()
                                        }
                                    )
                                    if (tabSearchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { tabSearchQuery = "" },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear search",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

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
                                items(filteredTabs, key = { it.id }) { tab ->
                                    val isActive = tab.id == activeTabId
                                    val isHibernated = hibernatedTabIds.contains(tab.id)
                                    val domain = runCatching { Uri.parse(tab.url).host }.getOrNull().orEmpty()
                                    val isComicSite = domain.contains("comix") || domain.contains("manga")
                                    var cardSwipeOffset by remember { mutableFloatStateOf(0f) }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .offset { IntOffset(cardSwipeOffset.roundToInt(), 0) }
                                            .pointerInput(tab.id) {
                                                detectHorizontalDragGestures(
                                                    onDragEnd = {
                                                        if (cardSwipeOffset < -160f || cardSwipeOffset > 160f) {
                                                            closeTab(tab.id)
                                                        }
                                                        cardSwipeOffset = 0f
                                                    },
                                                    onDragCancel = { cardSwipeOffset = 0f },
                                                    onHorizontalDrag = { change, dragAmount ->
                                                        change.consume()
                                                        cardSwipeOffset += dragAmount
                                                    }
                                                )
                                            }
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
                                            // Card Top: Icon, Domain, Hibernation Badge, Close Button
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

                                                // Hibernation badge if tab memory was freed
                                                if (isHibernated) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color(0xFF065F46))
                                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "⚡ Saved",
                                                            color = Color(0xFFA7F3D0),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(4.dp))
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

                                            // Card Bottom: Reading Progress Bar & URL preview
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                if (tab.scrollProgress > 0f) {
                                                    val progressPct = (tab.scrollProgress * 100).toInt().coerceIn(1, 100)
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        LinearProgressIndicator(
                                                            progress = { tab.scrollProgress },
                                                            modifier = Modifier.weight(1f).height(3.dp),
                                                            color = if (tab.isIncognito) Color(0xFFA855F7) else Color(0xFF38BDF8),
                                                            trackColor = Color(0xFF22293C)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "$progressPct%",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF94A3B8)
                                                        )
                                                    }
                                                }
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
