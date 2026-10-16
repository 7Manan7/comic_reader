# Kuro Reader — Developer Guide & APK Build Instructions

This guide explains how to build, install, test, and develop **Kuro Reader** (`com.example.comicreader`).

---

> 📘 For an exhaustive breakdown of all architecture components, data flow diagrams, and feature specifications, see [**TECHNICAL_DETAILS.md**](TECHNICAL_DETAILS.md).

---

## 📦 1. How to Generate the APK

### Prerequisites
- **JDK 17** (configured via Gradle toolchain)
- **Android SDK** with compileSdk 36
- Windows PowerShell / Command Prompt or bash

### Quick Build (Debug APK)
To build a ready-to-install debug APK:

```powershell
.\gradlew.bat assembleDebug
```
*(On Linux/macOS: `./gradlew assembleDebug`)*

Once the build finishes with **`BUILD SUCCESSFUL`**, the APK is located at:
```text
app\build\outputs\apk\debug\app-debug.apk
```

> **Why Debug APK for testing?**  
> Debug APKs are automatically signed with Android's default debug keystore, meaning **you can immediately install and run it on any Android device** without manual keystore configuration or certificate warnings.

### Clean & Fresh Rebuild
If you ever encounter cached build artifacts or stale resources:

```powershell
.\gradlew.bat clean assembleDebug
```

### Production / Release APK
To build an optimized release binary:

```powershell
.\gradlew.bat assembleRelease
```
Output location:
```text
app\build\outputs\apk\release\app-release-unsigned.apk
```
*(Note: To publish or install a release APK on end-user devices, it must be signed with your private release keystore using `apksigner` or a configured `signingConfigs` block in `app/build.gradle.kts`)*.

---

## 📲 2. How to Install the APK on a Device

### Method A: Direct Install via ADB (Fastest)
1. Enable **Developer Options** and **USB Debugging** on your Android phone.
2. Connect the phone to your PC via USB cable.
3. Verify connection:
   ```powershell
   adb devices
   ```
4. Install or update the app:
   ```powershell
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```
   *(The `-r` flag reinstalls/updates the app while preserving app data, history, and bookmarks).*

5. Alternatively, build and install in a single step:
   ```powershell
   .\gradlew.bat installDebug
   ```

### Method B: Manual Transfer / Sideload
1. Copy `app\build\outputs\apk\debug\app-debug.apk` to your phone via:
   - USB File Transfer (MTP)
   - Google Drive / OneDrive
   - Local chat (Telegram / WhatsApp / Discord)
2. On your phone, tap the `.apk` file to install.
3. If prompted, toggle **"Allow from this source"** for your file manager.
4. Open **Kuro Reader** from your app drawer.

---

## 🛠️ 3. Debugging & Logs

### Viewing App Logs in Real Time
Filter logs by Kuro Reader's key components:

```powershell
adb logcat -s ComicWebView:D AdBlockEngine:D AdBlockListManager:D RefreshRateManager:D
```

To view all exceptions and crashes:
```powershell
adb logcat *:E
```

---

## 📂 4. Project Architecture Overview

```text
app/src/main/java/com/example/comicreader/
├── MainActivity.kt               # Entry activity, window insets, volume key scrolling, immersive mode
├── adblock/
│   ├── AdBlockEngine.kt          # Host matching, cosmetic CSS filtering, anti-popup JS, whitelist
│   ├── AdBlockListManager.kt     # Multi-list downloading, GZIP extraction, 15 filter lists
│   └── EasyListParser.kt         # Efficient parser for ABP/EasyList rules and domain hosts
├── data/
│   ├── AppPreferences.kt         # Centralized SharedPreferences storage for settings
│   ├── BookmarkManager.kt        # Persistent bookmarks (custom sites & directory stars)
│   ├── HistoryManager.kt         # Browsing & reading history storage
│   ├── ManhwaDirectory.kt        # Curated catalog of top manhwa/manga/webtoon reading portals
│   ├── SearchEngine.kt           # Search engine definitions (Google, DuckDuckGo, Brave, Bing, Ecosia)
│   ├── SessionManager.kt         # Last visited page persistence & startup session restoration
│   ├── Tab.kt                    # Tab data model (id, title, url, lastAccessed, isIncognito)
│   └── TabManager.kt             # In-memory cached multi-tab engine with async disk persistence
├── display/
│   └── RefreshRateManager.kt     # 60/90/120/144/165Hz display mode detection & locking
├── theme/
│   ├── Color.kt / Theme.kt       # Jetpack Compose OLED dark & incognito purple styling
│   └── Type.kt                   # Typography definitions
├── ui/
│   ├── ComicWebView.kt           # Hardware-accelerated WebView with input bridge & touch controls
│   ├── ReaderScreen.kt           # Main UI: URL bar, tab manager, Brave menu, bottom nav bar
│   └── home/
│       └── HomeScreenContent.kt  # Native home dashboard: multi-engine search, manhwa directory, history
└── youtube/
    └── YouTubeOptimizer.kt       # YouTube web app styling, CSS optimization, and fullscreen media
```

---

## 💡 5. Important Architecture Decisions

- **URL Bar Architecture (No Text Clipping)**: The top navigation URL bar is constructed using a custom `Surface` with `Row(verticalAlignment = Alignment.CenterVertically)` and a `BasicTextField`. Unlike standard `OutlinedTextField` (which enforces a 56dp minimum height and 16dp vertical padding that clips descenders like `g`, `p`, `y`, `_` in compact bars), this custom implementation guarantees pixel-perfect vertical centering, ample clearance, and zero clipping across all screen densities and font scaling levels.
- **WebView Lifecycle & Recomposition Safety**: WebViews are persistent across tab switches via `tabWebViews`. Inside `AndroidView`, the `update` block is strictly limited to declarative visual styling (`textZoom`, `invertMode`) and **never calls `loadUrl()`**. Intentional navigations (URL bar input, home dashboard cards, bookmarks, history) invoke a centralized `navigateToUrl()` function. This prevents accidental reloads back to the initial URL when Compose recomposes during scroll events, HUD auto-hiding, or progress updates.
- **In-Memory Tab Management (`TabManager`)**: Tab state is cached synchronously in memory (`cachedTabs`, `cachedActiveTabId`) and asynchronously persisted to `SharedPreferences`. This eliminates disk read latency and prevents race conditions between rapid lifecycle events (e.g., `onUrlChanged` followed by `onTitleReceived`).
- **Touch Handling & Manga Reader Compatibility**: `ComicWebView.onTouchEvent` allows horizontal touches and swipes to pass directly to web pages. This ensures webtoon horizontal panning, zoom gestures, and website-embedded manga readers (e.g. tap/swipe to turn pages) function natively without interference. Browser history navigation is cleanly handled via Android's native edge-swipe back gesture and dedicated bottom bar controls.
- **HUD Tap Zones**: To prevent taps on interactive website elements (search bars, logos, navigation buttons) from inadvertently toggling the reader HUD, tap-to-toggle is strictly restricted to the **center reading zone** (`30%..70%` vertical, `20%..80%` horizontal).
- **Restore Last Visited Page**: Managed by `SessionManager`. Every valid HTTP/HTTPS URL visited is persisted. On app launch, if "Restore Last Visited Page" is enabled, Kuro Reader automatically re-opens the exact chapter where the user left off.
- **Non-Obscuring Column Layout**: The browser UI uses a `Column` layout rather than an overlapping `Box`. When the URL bar is visible, web content begins cleanly below the bar so site headers are never covered. When scrolling down to read, the bars collapse (`shrinkVertically()`) and the WebView expands to full-screen edge-to-edge.
- **Safe Insets**: Dialog menus use `.windowInsetsPadding(WindowInsets.safeDrawing)` with custom border styling to guarantee options never clip off the screen on devices with hole-punch cameras or curved corners.
