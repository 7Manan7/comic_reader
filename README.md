# Kuro Reader 📖⚡

A high-performance Android browser engineered specifically for reading online comics, manga, manhwa, and webtoons. Designed for distraction-free reading with a built-in aggressive multi-list ad blocker, bounded LRU memory hibernation pool, seamless webtoon image stitching, 165Hz hands-free auto-scrolling ("Cruise Mode"), full immersive edge-to-edge system bars, multi-tab private browsing, and hardware-accelerated display mode optimization supporting high refresh rate screens (**60Hz, 90Hz, 120Hz, 144Hz, and 165Hz+**).

---

> 📖 **Documentation Links**:
> - [**Technical Specifications & Feature Details**](TECHNICAL_DETAILS.md) — Comprehensive technical reference, architecture diagrams, AdBlock engine specifications, memory pool internals, and full feature directory.
> - [**Developer Guide & APK Build Instructions**](DEV_README.md) — Step-by-step instructions for building, installing, debugging, and testing Kuro Reader.

---

## ✨ Key Features

- **🚀 High Refresh Rate Display Optimization (up to 165Hz)**:
  - Hardware-level detection of supported refresh rates using Android's `Display.supportedModes`.
  - Automatically locks window attributes (`preferredDisplayModeId` & `preferredRefreshRate`) to the highest hardware refresh rate (120Hz, 144Hz, 165Hz) for ultra-fluid, tear-free scrolling.
  - In-app refresh rate selector: switch between **Auto (Max)**, **60Hz** (battery saver), **90Hz**, **120Hz**, **144Hz**, and **165Hz**.

- **🧹 Bounded LRU WebView Hibernation Pool (Memory Safety)**:
  - Caps live in-memory `ComicWebView` instances to `MAX_ACTIVE_WEBVIEWS = 5` using an access-ordered LRU policy.
  - Automatically evicts background tabs when opening new ones, capturing exact scroll ratio and Chromium state bundle, stopping loads, and destroying native WebViews to prevent native heap exhaustion and OOM crashes.
  - Transparently restores exact reading scroll position when switching back to hibernated tabs.
  - Displays a subtle `"⚡ Saved"` badge on hibernated tab cards.

- **⚡ Hands-Free 165Hz Auto-Scroll ("Webtoon Cruise Mode")**:
  - Hands-free reading at variable velocities (`0.5x` – `5.0x`) locked to the display's native refresh rate using `window.requestAnimationFrame`.
  - Delta-time normalization ensures consistent pixel velocity across 60Hz, 120Hz, 144Hz, and 165Hz panels.
  - Automatically pauses on touch (`ACTION_DOWN`) and resumes on release (`ACTION_UP`), or stops upon reaching the bottom of the chapter.

- **🖼️ Seamless Webtoon Image Stitcher & Lazy-Load Booster**:
  - Automatically eliminates 1–3px horizontal seam gaps between sliced chapter images via injected zero-margin, zero-padding CSS rules.
  - Converts `data-src`, `data-lazy-src`, and `loading="lazy"` attributes into `loading="eager"` and forces pre-decoding two viewports ahead so fast scrolling never hits blank placeholders.
  - Controlled via `webtoon_seamless_mode` in settings.

- **🧭 Smart Chapter Detector & Floating Quick-Action Pill**:
  - Real-time DOM scanner extracts chapter labels (e.g., `Ch. 142`) and detects `"Previous Chapter"` / `"Next Chapter"` links.
  - Sleek glassmorphic floating pill provides 1-tap **Prev Ch** / **Next Ch** navigation, Cruise Mode auto-scroll toggle with speed slider, and a chapter scroll percentage badge (tap to jump top or bottom).
  - Highlights Next Chapter with an accent pulse when reading progress exceeds 92%.

- **📏 Real-Time Reading Progress Scrubber**:
  - Ultra-thin (`2.dp`) neon gradient progress bar along the top screen edge indicating real-time chapter reading completion (`0%` to `100%`).

- **👆 Ergonomic Bottom-Bar Gestures**:
  - Horizontal swipe left/right across the bottom navigation bar smoothly transitions to the previous or next open tab with haptic feedback.

- **🎨 Advanced Eye-Comfort Color Profiles**:
  - 4 specialized reading color profiles:
    1. **Original**: Standard true-color rendering.
    2. **OLED Invert**: High-contrast dark invert with automatic comic artwork and media re-inversion.
    3. **Warm Amber**: Night-time blue-light filter (`sepia` & warm saturation) that softens harsh white speech bubbles.
    4. **E-Ink Mono**: High-contrast grayscale manga mode tailored for classic black-and-white manga.

- **🛡️ Built-in Multi-List AdBlock & Security Engine (15 Filter Lists)**:
  - **Thread-Safe Atomic Rule Swapping**: Filter rules are grouped into an immutable `CompiledAdBlockRules` snapshot stored in an `AtomicReference` for lock-free Chromium background I/O reads.
  - **LRU Decision Cache**: 2048-entry in-memory cache resolves repeated image and asset requests in \(O(1)\) without string splitting.
  - **15 Filter Lists**: Comprehensive coverage across Default, Privacy, Malware/Security, and Annoyances categories.
  - **Anti-Popup & Anti-Redirect Safeguards**: Neutralizes rogue `window.open` calls, sweeps transparent clickjack overlays (`z-index >= 999`), and intercepts malicious external intent schemes.
  - **First-Party Safeguards**: Ensures legitimate comic chapters, CDNs, and reading portals are never blocked.

- **📑 Multi-Tab Browsing & Enhanced Tab Switcher**:
  - Supports up to 50 concurrent tabs with search/filter bar, live chapter reading progress indicators, and memory-saved status badges.
  - Filter tabs by **All**, **Regular**, or **Private**, with one-tap **Close All Tabs** and **Close All Private Tabs**.

- **🕶️ Private & Incognito Browsing Mode**:
  - Complete session isolation: disables cache (`LOAD_NO_CACHE`), disables form data saving, clears history on navigation, and never writes visited pages to reading history.
  - Automatic cold-start purge clears all private tabs when restarting the app.
  - Distinctive deep purple incognito theme.

- **🏠 Upgraded Home Dashboard (`about:home`)**:
  - **Shield & Performance Telemetry Strip**: Displays live blocked ads count, locked display refresh rate, and active RAM pool tab count (`X/5`).
  - **"Continue Reading" Carousel**: Enhanced cards showing detected chapter badges, source domain pills, relative timestamps, and linear progress bars (`72% read`).
  - **Multi-Engine Search**: Instant provider switching between Google, DuckDuckGo, Brave, Bing, and Ecosia.
  - **Curated Manga Hubs**: 1-tap launchpads to Asura Scans, Demonic Scans, Flame Comics, Comix, MangaDex, MangaKatana, Webtoons, MangaFreak, ComicK, and MangaPark.

- **🔒 Delimiter-Safe Persistence (`SafeDelimiterCodec`)**:
  - Sanitization helper prevents corrupted records or column shifting when titles or URLs contain `;;` or newline characters.

- **📱 Smart Scroll Fullscreen & Touch Controls**:
  - Automatically collapses bars when scrolling down to read; restores controls when scrolling up or returning to top.
  - Hardware Volume Button scrolling for effortless one-handed reading.
  - Direct native touch passthrough ensures embedded manga readers operate without gesture hijacking.

---

## 🏗️ Architecture

```
┌────────────────────────────────────────────────────────┐
│                   MainActivity                         │
│  - Edge-to-Edge & Immersive Window Insets Controller   │
│  - Display Mode & High Refresh Rate Manager (165Hz)    │
│  - Screen-Always-On & Volume Key Navigation Handler    │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│                 Compose UI Layer                       │
│  - Top Bar (Auto-hiding, URL bar, AdBlock badge, Tabs) │
│  - Top Edge Neon Reading Progress Scrubber (2dp)       │
│  - Floating Quick-Action Reader Pill (Prev/Next/Cruise)│
│  - HomeScreenContent (Search, Telemetry, Manhwa hubs)  │
│  - Tab Switcher Sheet (Search, Memory Saved, Incognito)│
│  - Bottom Control Bar (Swipe-to-switch tabs + haptics) │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│              WebViewPoolManager (Max 5)                │
│  - Access-ordered LRU eviction & HibernatedTabState    │
│  - Scroll ratio capture & Bundle state restoration     │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│              ComicWebView (AndroidView)                │
│  - Hardware-Accelerated Rendering Layer                │
│  - ReaderOptimizer (Seamless Stitching, Lazy Booster)  │
│  - 165Hz Cruise Mode (rAF delta-time normalized loop)  │
│  - Smart Chapter Detector (DOM scanner & WebBridge)    │
│  - Reading Color Profiles (OLED, Warm Amber, E-Ink)    │
│  - Incognito Privacy Enforcement (No-cache, no-cookies)│
│  - Custom WebChromeClient (Block popups & redirects)   │
│  - Custom WebViewClient with AdBlockEngine             │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│         AdBlockEngine & Core Data Managers             │
│  - AtomicReference<CompiledAdBlockRules> (Lock-free)   │
│  - 2048-entry LRU Decision Cache                       │
│  - SafeDelimiterCodec (;; and newline safe persistence)│
│  - TabManager, BookmarkManager, HistoryManager         │
└────────────────────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

- **Language**: Kotlin 2.3+
- **UI Framework**: Jetpack Compose (Material 3)
- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 16 (API 36)
- **Architecture**: Modern Android Architecture with Compose StateFlow and Coroutines
- **Build System**: Gradle 9 with Version Catalog (`libs.versions.toml`)

---

## 🚀 Getting Started

### Prerequisites

- Java Development Kit (JDK 17+)
- Android SDK (API 34+)

### Build & Run

1. **Clone the repository**:
   ```bash
   git clone https://github.com/7Manan7/comic_reader.git
   cd comic_reader
   ```

2. **Run Unit Tests**:
   ```powershell
   .\gradlew.bat testDebugUnitTest
   ```

3. **Build Debug APK**:
   ```powershell
   .\gradlew.bat assembleDebug
   ```
   The compiled APK will be generated at:
   `app/build/outputs/apk/debug/app-debug.apk`

4. **Install on Device or Emulator**:
   ```powershell
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

---

## ⚙️ Configuration & Reader Controls

| Feature | Control | Description |
|---|---|---|
| **Toggle Browser / Fullscreen** | Scroll Down / Up or Center Tap | Automatically switches to fullscreen when scrolling down; restores browser when scrolling up or center-tapping |
| **Cruise Mode (Auto-Scroll)** | `▶ Cruise` button in Floating Pill | Hands-free auto-scrolling with inline speed slider (`0.5x` – `5.0x`); pauses on touch and resumes on release |
| **Previous / Next Chapter** | `⏮ Prev Ch` / `Next Ch ⏭` in Pill | 1-tap navigation to previous/next chapter detected dynamically from webpage DOM |
| **Jump to Top / Bottom** | Tap Chapter Badge in Floating Pill | Instantly scrolls to top (if >50% read) or to bottom (if <=50% read) |
| **Reading Progress Bar** | Top edge neon progress bar | Displays real-time chapter reading percentage (`0%` to `100%`) |
| **Switch Tabs via Gesture** | Horizontal swipe on bottom bar | Swipe left/right on bottom navigation bar to switch between adjacent tabs with haptic feedback |
| **Full-Width URL Bar** | Top navigation bar | Search engines or direct URL input with vertically centered, unclipped text and clear button |
| **Tab Switcher** | Tab counter badge in bottom bar | Open tab switcher sheet with tab search, scroll progress bars, memory saved badges, and close actions |
| **Manga Touch & Page Turning** | Native gestures on web page | Tap or swipe on website readers to flip pages; zoom or pan wide comic panels without interference |
| **Eye-Comfort Color Profiles** | Color profile selector in Settings / Moon icon | Switch between Original, OLED Invert, Warm Amber (blue-light filter), and E-Ink Mono (grayscale) |
| **Webtoon Seamless Mode** | Settings toggle switch | Eliminates 1–3px seam gaps between manhwa image slices and enables eager 2-viewport ahead pre-decoding |
| **Refresh Rate** | Settings Sheet / ⚡ Badge | Choose between Auto (Max), 60Hz, 90Hz, 120Hz, 144Hz, or 165Hz |
| **Page Navigation** | Hardware Volume Up / Down | Scrolls up or down by 75% of viewport height for one-handed reading |
| **Incognito Mode** | New Private Tab in tab sheet | Browse privately with cache/cookies disabled, purple theme, and no history tracking |
| **AdBlock Shield** | Shield icon in top bar | View blocked ads count, toggle AdBlock, or whitelist current domain |
| **Screen Awake** | Settings Sheet | Keep screen continuously on while reading long chapters |

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.