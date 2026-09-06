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
    var showBraveMenu by remember { mutableStateOf(false) }
    var showHomeSheet by remember { mutableStateOf(false) }

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

                    // Refresh Button
                    IconButton(onClick = { webViewRef?.reload() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload",
                            tint = Color.White
                        )
                    }

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

        // Web Content View & Loading Progress Indicator
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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
                                }
                            } else {
                                // User scrolled up or reached top: restore basic browser UI
                                if (!isHudVisible || isImmersiveFullscreen) {
                                    isHudVisible = true
                                    isImmersiveFullscreen = false
                                }
                            }
                        }

                        onSingleTap = {
                            // Toggle UI HUD and fullscreen mode on tap
                            val showBrowser = !isHudVisible
                            isHudVisible = showBrowser
                            isImmersiveFullscreen = !showBrowser
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
                        .align(Alignment.TopCenter),
                    color = Color(0xFF64B5F6),
                    trackColor = Color.Transparent
                )
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
                    .background(Color(0xE61E1E24))
                    .then(if (!isImmersiveFullscreen) Modifier.navigationBarsPadding() else Modifier)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                // Clean Bottom Navigation Bar (No carousel, no settings clutter)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home Button (Opens Quick Sites & Bookmarks Speed-Dial)
                    IconButton(onClick = { showHomeSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home & Sites",
                            tint = Color.White
                        )
                    }

                    // Back Button
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

                    // Forward Button
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

                    // Browsing History Button
                    IconButton(onClick = {
                        historyList = HistoryManager.getHistory(context)
                        showHistorySheet = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = Color.White
                        )
                    }

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

    // Home & Quick Manga Sites Speed-Dial (Opened via Home button)
    if (showHomeSheet) {
        Dialog(
            onDismissRequest = { showHomeSheet = false },
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
                        onClick = { showHomeSheet = false }
                    )
                    // Invisible border on up, down, left and right:
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.78f)
                        // Visible border for the menu:
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
                            Column {
                                Text(
                                    text = "🏠 Quick Manga Sites",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Tap any comic site to start reading",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            IconButton(
                                onClick = { showHomeSheet = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                            }
                        }

                        HorizontalDivider(color = Color(0xFF2B2E42), thickness = 1.dp)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            bookmarks.forEach { bookmark ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2230)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFF2B2E42))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                inputUrl = bookmark.url
                                                webViewRef?.loadUrl(bookmark.url)
                                                showHomeSheet = false
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp),
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
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = bookmark.url,
                                                    color = Color(0xFF90CAF9),
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Open →",
                                            color = Color(0xFF64B5F6),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedButton(
                                onClick = {
                                    showHomeSheet = false
                                    showBookmarksSheet = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64B5F6)),
                                border = BorderStroke(1.dp, Color(0xFF3F445A))
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Manage / Add Sites",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Manage / Add Custom Sites", fontSize = 13.sp)
                            }
                        }
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
                                    // Back
                                    IconButton(
                                        onClick = {
                                            webViewRef?.let { if (it.canGoBack()) it.goBack() }
                                            showBraveMenu = false
                                        },
                                        enabled = canGoBack
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = if (canGoBack) Color.White else Color.DarkGray
                                        )
                                    }

                                    // Forward
                                    IconButton(
                                        onClick = {
                                            webViewRef?.let { if (it.canGoForward()) it.goForward() }
                                            showBraveMenu = false
                                        },
                                        enabled = canGoForward
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "Forward",
                                            tint = if (canGoForward) Color.White else Color.DarkGray
                                        )
                                    }

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
                                        onClick = { isNightInvertMode = !isNightInvertMode }
                                    ) {
                                        Text(
                                            text = if (isNightInvertMode) "🌙" else "☀️",
                                            fontSize = 18.sp
                                        )
                                    }
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
                                            Text("🔄 Update 16 Filter Lists Now", fontSize = 12.sp, color = Color.White)
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
                                            text = if (showFilterListDetails) "Hide Filter Lists ▲" else "View / Toggle 16 Filter Lists ▼",
                                            color = Color(0xFF64B5F6),
                                            fontSize = 12.sp
                                        )
                                    }

                                    if (showFilterListDetails) {
                                        FilterListCategory.entries.forEach { category ->
                                            val categoryLists = AdBlockListManager.ALL_LISTS.filter { it.category == category }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = category.title,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF90CAF9),
                                                fontSize = 12.sp
                                            )
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
                                            onCheckedChange = { isNightInvertMode = it }
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
                                                    .clickable { if (webTextZoom > 75) webTextZoom -= 25 }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text("-25%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (webTextZoom == 100) Color(0xFF1E88E5) else Color(0xFF282B3B))
                                                    .clickable { webTextZoom = 100 }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text("100%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFF282B3B))
                                                    .clickable { if (webTextZoom < 250) webTextZoom += 25 }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text("+25%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }

                            // 5. Quick Navigation Links (Bookmarks, History)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        showBraveMenu = false
                                        showBookmarksSheet = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64B5F6)),
                                    border = BorderStroke(1.dp, Color(0xFF3F445A))
                                ) {
                                    Icon(Icons.Default.Bookmark, contentDescription = "Bookmarks", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sites", fontSize = 12.sp)
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
                                    border = BorderStroke(1.dp, Color(0xFF3F445A))
                                ) {
                                    Icon(Icons.Default.History, contentDescription = "History", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("History", fontSize = 12.sp)
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
