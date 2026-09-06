# Comic Reader 📖⚡

A high-performance Android browser app engineered specifically for reading online comics, manga, manhwa, and webtoons. Designed for distraction-free reading with a built-in aggressive ad blocker, full immersive/transparent edge-to-edge system bars, and dynamic display mode optimization supporting high refresh rate screens (**60Hz, 90Hz, 120Hz, 144Hz, and 165Hz+**).

---

## ✨ Key Features

- **🚀 High Refresh Rate Display Optimization (up to 165Hz)**:
  - Hardware-level detection of supported refresh rates using Android's `Display.supportedModes`.
  - Automatically locks window attributes (`preferredDisplayModeId` & `preferredRefreshRate`) to the highest hardware refresh rate (120Hz, 144Hz, 165Hz) for ultra-fluid, tear-free scrolling.
  - In-app refresh rate selector: switch between **Auto (Max)**, **60Hz** (battery saver), **90Hz**, **120Hz**, **144Hz**, and **165Hz**.

- **🛡️ Built-in Multi-List AdBlock & Security Engine (16 Filter Lists across 3 Categories)**:
  - **uBlock Origin Built-in Filters (uAssets)**:
    - `filters.txt` (uBlock Base ad & popunder rules)
    - `badware.txt` (Badware, scareware, and mobile clickjacking protection)
    - `privacy.txt` (Privacy and telemetry blocking)
    - `quick-fixes.txt` (Rapid circumvention response rules)
    - `unbreak.txt` (Site fixes for broken comic readers)
  - **Standard Default Third-Party Lists**:
    - `EasyList` (Primary ad-blocking list: banners, popups, video ads)
    - `EasyPrivacy` (Tracker, analytics, and beacon blocking)
    - `Peter Lowe’s Ad and Tracking server list` (Hosts-format ad/spyware servers)
    - `URLhaus Malicious URLs` (Malware & exploit protection)
  - **Optional Common Lists (uBO & AdGuard)**:
    - `uBlock filters – Annoyances` (Overlays, popups, and floating spam)
    - `EasyList – Cookie Notices` (Fanboy's Cookie Monster list)
    - `Fanboy’s Annoyance List` (Popups, newsletters, in-page notifications)
    - `Fanboy’s Social Blocking List` (Social media trackers and widgets)
    - `AdGuard Base Filter` (Advanced ad blocking optimized for Chromium)
    - `AdGuard Tracking Protection` (Comprehensive telemetry filters)
    - `AdGuard Mobile Ads` (Mobile-specific redirect and banner blocking)
  - **Hardened Anti-Redirect & Anti-Popup Protection**:
    - Full `shouldOverrideUrlLoading` interception blocking top-level ad navigations and malicious external schemes (`intent:`, `market:`, `vnd.youtube:`).
    - Early script injection (`ANTI_POPUP_JS`) at `onPageStarted` neutralizing `window.open` and sweeping invisible clickjack overlays.
    - First-party comic site safeguards ensuring legitimate manga chapters and image CDNs are never blocked.
  - **In-App Filter Lists Manager & Concurrent Updater**:
    - Per-list toggle switches with persistent storage in `SharedPreferences`.
    - Concurrent background downloading with GZIP compression and fallback mirrors.
    - Sub-microsecond \(O(1)\) domain suffix matching and dynamic cosmetic CSS filtering.

- **🎨 Custom Comic & Manga App Icon**:
  - Bespoke comic-themed adaptive icon featuring an open manga volume with panel art, cosmic radial backdrop, and high refresh rate 165Hz lightning emblem.

- **📱 Basic Browser UI with Smart Scroll Fullscreen**:
  - **Basic Browser Mode by Default**: Launches with standard browser appearance (visible status bar, top URL/search bar, and bottom navigation bar).
  - **Dynamic Scroll-Down Fullscreen**: Seamlessly enters immersive fullscreen mode when scrolling down through manga chapters; restores the basic browser controls when scrolling up or returning to top.
  - **Tap-to-Toggle HUD**: Single-tap anywhere to manually toggle between fullscreen reading mode and standard browser view.

- **📚 Manga Sites & Dynamic Bookmark Management**:
  - Pre-configured with popular manga hubs: **Comix** (`https://comix.to/`), **MangaFreak** (`https://ww3.mangafreak.me/`), and **MangaKatana** (`https://mangakatana.com/`).
  - **Add Custom Manga Sites**: Add your own favorite manga, manhwa, and comic reading websites with persistent local storage.
  - **Single-Tap Bookmark**: Instantly bookmark the currently viewed comic chapter or site.
  - **Manage & Delete Bookmarks**: Open the "+ Sites" sheet to manage, navigate to, or remove saved sites.

- **📜 Full Browsing History**:
  - Automatically records visited pages and manga chapters with page titles, URLs, and timestamps.
  - Quick access via the **History** chip in the bookmarks bar or the **History** button on the bottom controls.
  - Built-in instant search filtering across history records.
  - Click any entry to reopen, delete individual entries, or clear all history with single-tap.

- **🔄 One-Tap AdBlock Filter Refresh**:
  - Readily accessible in two places:
    1. **Top Bar Shield Badge (`🛡️`)**: Tap the shield icon to open the AdBlock dialog and tap **"🔄 Refresh & Update Filter Lists Now"**.
    2. **Reader Settings (`⚙️`)**: Scroll to the AdBlock section and tap **"Update Lists Now"**.
  - Downloads and parses latest filter rules concurrently across all 16 filter sources.

- **📖 Comic Reading UX**:
  - **Hardware Volume Button Navigation**: Scroll pages up/down using the device's physical volume rocker for easy one-handed reading.
  - **Night / OLED Invert Filter**: Inverts web pages to dark backgrounds for comfortable reading in low-light environments.
  - **Keep Screen Awake**: Prevents screen timeouts while reading chapters.

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
│  - Top Bar (Auto-hiding, URL bar, AdBlock badge)       │
│  - Bottom Control Bar (Reader mode, Refresh rate, Nav) │
│  - Quick Bookmarks Drawer & Settings Bottom Sheet      │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│              ComicWebView (AndroidView)                │
│  - Hardware Accelerated Layer (LAYER_TYPE_HARDWARE)    │
│  - DOM / WebGL / Smooth Fling Enabled                  │
│  - Custom WebChromeClient (Block popups & redirects)   │
│  - Custom WebViewClient with AdBlockEngine             │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│                   AdBlockEngine                        │
│  - Domain & Subdomain Filter Set                       │
│  - Resource regex filters (scripts, banners, popups)   │
│  - Cosmetic CSS Element Inserter (removes ad spaces)   │
│  - Domain Whitelist & Live Block Counter               │
└────────────────────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

- **Language**: Kotlin 2.1+
- **UI Framework**: Jetpack Compose (Material 3)
- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 15 / 16 (API 36)
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
   .\gradlew.bat test
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
| **Toggle Browser / Fullscreen** | Scroll Down / Up or Tap | Automatically switches to fullscreen when scrolling down; restores browser when scrolling up or tapping |
| **Manage Sites & Bookmarks** | "+ Sites" button in bottom bar | View, add custom sites, bookmark current page, or delete saved bookmarks |
| **Page Navigation** | Volume Up / Down | Scrolls up or down one page length for one-handed reading |
| **Refresh Rate** | Settings Sheet / ⚡ Badge | Choose between Auto (Max), 60Hz, 90Hz, 120Hz, 144Hz, or 165Hz |
| **AdBlock Shield** | Shield icon in top bar | View blocked ads count, toggle AdBlock, or whitelist current domain |
| **OLED Invert** | Moon icon in bottom bar | Inverts white comic backgrounds for dark-room reading |
| **Screen Awake** | Settings Sheet | Keep screen continuously on while reading |

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.