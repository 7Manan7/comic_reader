# Kuro Reader — Developer Guide & APK Build Instructions

This guide explains how to build, install, test, and develop **Kuro Reader** (`com.example.comicreader`).

---

> 📘 For an exhaustive breakdown of all architecture components, data flow diagrams, memory pool management, and complete feature specifications, see [**TECHNICAL_DETAILS.md**](TECHNICAL_DETAILS.md).

---

## 📦 1. How to Generate the APK

### Prerequisites
- **JDK 17** (configured via Gradle toolchain)
- **Android SDK** with `compileSdk 36`
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

### Run Unit Tests & Build Verification
To run all unit tests and verify the debug build in one step:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

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

## 🧪 3. Running Unit Tests

The test suite covers memory safety, delimiter encoding, chapter extraction, ad-blocking logic, and tab management:

```powershell
.\gradlew.bat testDebugUnitTest
```

### Test Suite Directory (`app/src/test/java/com/example/comicreader/`)
- `data/SafeDelimiterCodecTest.kt`: Tests escaping/unescaping of `;;` (`&#59;&#59;`), newlines, and corrupted record resistance.
- `ui/ReaderOptimizerTest.kt`: Validates chapter extraction regexes (`"Solo Leveling Chapter 145"` -> `"Ch. 145"`), seamless webtoon CSS, and color profile CSS.
- `data/TabManagerTest.kt`: Validates tab creation, LRU trimming, incognito tab isolation, and scroll progress tracking.
- `data/HistoryManagerTest.kt`: Tests history ordering, deduplication, and chapter metadata persistence.
- `adblock/AdBlockEngineTest.kt`: Tests lock-free snapshot rule lookups, domain suffix matching, exception precedence, and dangerous URL redirect schemes.
- `data/BookmarkManagerTest.kt` & `data/SearchEngineTest.kt`: Validates bookmark deduplication and search provider query resolution.

---

## 🛠️ 4. Debugging & Logs

### Viewing App Logs in Real Time
Filter logs by Kuro Reader's key components:

```powershell
adb logcat -s ComicWebView:D WebViewPoolManager:D AdBlockEngine:D AdBlockListManager:D RefreshRateManager:D ReaderOptimizer:D
```

To view all exceptions and crashes:
```powershell
adb logcat *:E
```

---

## 📂 5. Project Architecture Overview

```text
app/src/main/java/com/example/comicreader/
├── MainActivity.kt               # Entry activity, window insets, volume key scrolling, immersive mode
├── adblock/
│   ├── AdBlockEngine.kt          # Atomic snapshot rule swapping, LRU decision cache, cosmetic CSS, anti-popup JS
│   ├── AdBlockListManager.kt     # Multi-list downloading, GZIP extraction, 15 filter lists
│   └── EasyListParser.kt         # Efficient parser for ABP/EasyList rules and domain hosts
├── data/
│   ├── AppPreferences.kt         # Centralized SharedPreferences storage for settings
│   ├── BookmarkManager.kt        # Persistent bookmarks (custom sites & directory stars)
│   ├── HistoryManager.kt         # Browsing & reading history storage (scroll progress & chapter tracking)
│   ├── ManhwaDirectory.kt        # Curated catalog of top manhwa/manga/webtoon reading portals
│   ├── SafeDelimiterCodec.kt     # Delimiter-safe encoder/decoder for persistence (;; and newline safe)
│   ├── SearchEngine.kt           # Search engine definitions (Google, DuckDuckGo, Brave, Bing, Ecosia)
│   ├── SessionManager.kt         # Last visited page persistence & startup session restoration
│   ├── Tab.kt                    # Tab data model (id, title, url, lastAccessed, isIncognito, scrollProgress, detectedChapter)
│   └── TabManager.kt             # In-memory cached multi-tab engine with async disk persistence
├── display/
│   └── RefreshRateManager.kt     # 60/90/120/144/165Hz display mode detection & locking
├── theme/
│   ├── Color.kt / Theme.kt       # Jetpack Compose OLED dark & incognito purple styling
│   └── Type.kt                   # Typography definitions
├── ui/
│   ├── ComicWebView.kt           # Hardware-accelerated WebView with WebInputBridge, Cruise mode, & touch controls
│   ├── ReaderOptimizer.kt        # Webtoon image stitcher, lazy booster, 165Hz auto-scroll, chapter detector, color profiles
│   ├── ReaderScreen.kt           # Main UI: URL bar, 2dp progress bar, floating reader pill, bottom bar tab swipe
│   ├── WebViewPoolManager.kt     # Bounded LRU pool (max 5 active), hibernation state & scroll ratio capture
│   └── home/
│       └── HomeScreenContent.kt  # Native home dashboard: telemetry strip, continue reading cards, search, manhwa directory
└── youtube/
    └── YouTubeOptimizer.kt       # YouTube web app styling, CSS optimization, and fullscreen media
```

---

## 💡 6. Important Architecture Decisions

- **Bounded LRU WebView Hibernation Pool (`WebViewPoolManager`)**: Holding dozens of active Chromium WebViews in memory causes native heap exhaustion and Out-Of-Memory (OOM) crashes on image-heavy manhwa chapters. `WebViewPoolManager` strictly limits active in-memory WebViews to `MAX_ACTIVE_WEBVIEWS = 5`. Background tabs beyond 5 are hibernated: their scroll progress ratio and Chromium navigation bundle are saved, and the native WebView is stopped, detached, and destroyed. When a hibernated tab is reopened, it is lazily reconstructed and its exact reading position is restored once `onPageFinished` fires.
- **Thread-Safe Atomic Rule Swapping (`AdBlockEngine`)**: `WebViewClient.shouldInterceptRequest` executes concurrently on Chromium background I/O threads while filter list updates occur in coroutines. Rule sets are grouped into an immutable `CompiledAdBlockRules` snapshot stored in an `AtomicReference`. Reads (`isAd()`, `matchesDomainSuffix()`) access the local snapshot with zero lock contention. A 2048-entry in-memory LRU decision cache caches `(host, firstPartyHost)` evaluations for \(O(1)\) lookups on repetitive CDN image queries.
- **Hands-Free Auto-Scroll with Delta-Time Normalization**: Webtoon reading requires constant downward scrolling. "Cruise Mode" implements a scroll loop inside JavaScript via `window.requestAnimationFrame`. By normalizing against elapsed frame time (`dt / 16.667ms`), the scroll velocity remains completely smooth and uniform across 60Hz, 120Hz, 144Hz, and 165Hz displays. It automatically pauses on touch and resumes on release.
- **Smart DOM Chapter Detection & Floating Quick-Action Pill**: Scans webpage DOM elements for previous/next chapter links and chapter numbers, reporting back to Kotlin via `@JavascriptInterface fun onChapterNavigationDetected(...)`. This drives a floating glassmorphic pill in Compose providing 1-tap chapter transitions, auto-scroll controls, and top/bottom jump actions without navigating messy site footers.
- **Delimiter-Safe Persistence (`SafeDelimiterCodec`)**: Uses a lightweight replacement (`&#59;&#59;` and space for newlines) on serialization and decoding on deserialization, completely preventing corrupted records or column shifts when comic titles or URLs contain semicolons or line breaks.
- **URL Bar Architecture (No Text Clipping)**: The top navigation URL bar is constructed using a custom `Surface` with `Row(verticalAlignment = Alignment.CenterVertically)` and a `BasicTextField`. This avoids standard `OutlinedTextField` 56dp minimum height padding and guarantees zero clipping for descenders like `g`, `p`, `y`, `_` across all font scaling levels.
- **WebView Lifecycle & Recomposition Safety**: Inside `AndroidView`, the `update` block is strictly limited to declarative visual styling (`textZoom`, `setColorProfile`) and **never calls `loadUrl()`**. Intentional navigations invoke a centralized `navigateToUrl()` function, preventing reloads during Compose recompositions or scroll events.
- **HUD Tap Zones**: Center-zone tap-to-toggle is strictly restricted to the **center reading zone** (`30%..70%` vertical, `20%..80%` horizontal) so taps on interactive webpage headers, links, and navigation bars operate normally.
