# Kuro Reader 📖⚡

A high-performance Android browser app engineered specifically for reading online comics, manga, manhwa, and webtoons. Designed for distraction-free reading with a built-in aggressive multi-list ad blocker, full immersive/transparent edge-to-edge system bars, multi-tab and private browsing, and dynamic display mode optimization supporting high refresh rate screens (**60Hz, 90Hz, 120Hz, 144Hz, and 165Hz+**).

---

## ✨ Key Features

- **🚀 High Refresh Rate Display Optimization (up to 165Hz)**:
  - Hardware-level detection of supported refresh rates using Android's `Display.supportedModes`.
  - Automatically locks window attributes (`preferredDisplayModeId` & `preferredRefreshRate`) to the highest hardware refresh rate (120Hz, 144Hz, 165Hz) for ultra-fluid, tear-free scrolling.
  - In-app refresh rate selector: switch between **Auto (Max)**, **60Hz** (battery saver), **90Hz**, **120Hz**, **144Hz**, and **165Hz**.

- **📑 Multi-Tab Browsing & Tab Manager**:
  - Full multi-tab engine supporting up to 20 concurrent tabs with automatic LRU trimming.
  - Interactive **Tab Switcher Sheet**: preview open tabs, active status badges, URLs, and switch between tabs with a single tap.
  - Individual tab close buttons (`✕`) and one-tap **Close All Tabs** action.
  - Dynamic top bar **Tab Counter Badge** with instant status indication (`99+` support).
  - Open links in new background or foreground tabs via long-press menus.

- **🕶️ Private & Incognito Browsing Mode**:
  - Dedicated private tabs with complete session isolation.
  - **Zero trace**: disables cache (`LOAD_NO_CACHE`), disables form data saving (`saveFormData = false`), clears web history/cookies on navigation, and never writes visited pages to reading history.
  - Distinctive incognito UI with deep purple styling, private badge icons (`🕶️`), and an informative incognito privacy dashboard.
  - Filter tabs in the switcher by **All**, **Regular**, or **Private**, with a dedicated **Close All Private Tabs** action.

- **🏠 Curated Home Dashboard & Multi-Engine Search**:
  - Interactive homepage (`about:home`) displayed on new tabs and home navigations.
  - **Multi-Engine Search Selector**: quickly toggle between **Google** 🔍, **DuckDuckGo** 🦆, **Brave** 🦁, **Bing** 🌐, and **Ecosia** 🌱 with direct search bar integration.
  - **Curated Manga & Manhwa Directory**: instant 1-tap launchpad to top reading hubs:
    - **Asura Scans** (Top action, regression & solo leveling manhwa)
    - **Demonic Scans** (Martial arts & fast action releases)
    - **Flame Comics** (Trending Korean fantasy webtoons)
    - **Comix** (Clean, high-speed reader with full-width webtoon view)
    - **MangaDex** (Non-profit community hub with scanlation teams)
    - **MangaKatana** (Vast manga & manhwa archives)
    - **Webtoons** (Official global webcomics platform)
    - **MangaFreak** (Daily chapter releases)
    - **ComicK** (Modern manga tracking platform)
    - **MangaPark** (Multi-source high-res aggregator)
  - Quick-access bookmark grid and recent reading history carousel right on the home dashboard.

- **👆 Intuitive Horizontal Swipe Gestures**:
  - **Swipe Right** (left-to-right): navigates back in browsing history, or smoothly returns to the Home dashboard if at the root page.
  - **Swipe Left** (right-to-left): navigates forward in browsing history.
  - Velocity and angle-calibrated gesture detection ensuring continuous vertical manga/webtoon scrolling is never accidentally interrupted.

- **🛡️ Built-in Multi-List AdBlock & Security Engine (15 Filter Lists across 4 Categories)**:
  - **Default (4/4)**:
    - `EasyList` (Primary ad-blocking list: banners, popups, video ads)
    - `EasyPrivacy` (Tracker, analytics, and beacon blocking)
    - `Peter Lowe – Ads, trackers, and more` (Hosts-format ad/spyware servers)
    - `uBlock filters – Ads, trackers, and more` (uBlock Origin Base ad & popunder rules)
  - **Privacy (1/2)**:
    - `AdGuard/uBO – URL Tracking Protection` (Strips URL tracking parameters and telemetry)
    - `Block Outsider Intrusion into LAN` (Prevents external web requests into localhost and LAN)
  - **Malware protection, security (2/2)**:
    - `uBlock filters – Badware risks` (Badware, scareware, and mobile clickjacking protection)
    - `Malicious URL Blocklist` (Actively updated list of malware and exploit domains)
  - **Annoyances (6/7)**:
    - `EasyList – AI Widgets` (Blocks AI summaries and floating assist widgets)
    - `EasyList/uBO – Cookie Notices` (Removes GDPR/CCPA cookie consent dialogs)
    - `EasyList – Notifications` (Blocks push notification prompts and subscription modals)
    - `EasyList – Other Annoyances` (Page irritations, surveys, self-promos, and visual nags)
    - `EasyList/uBO – Overlay Notices` (Newsletter overlays, anti-adblock modals, and sign-up paywalls)
    - `EasyList – Social Widgets` (Social sharing buttons, follower counts, and comment widgets)
    - `EasyList – Chat Widgets` (Floating customer service chat bubbles and support popups)
  - **Hardened Anti-Redirect & Anti-Popup Protection**:
    - Full `shouldOverrideUrlLoading` interception blocking top-level ad navigations and malicious external schemes (`intent:`, `market:`, `vnd.youtube:`).
    - Early script injection (`ANTI_POPUP_JS`) at `onPageStarted` neutralizing `window.open` and sweeping invisible clickjack overlays.
    - First-party comic site safeguards ensuring legitimate manga chapters and image CDNs are never blocked.
  - **In-App Filter Lists Manager & Concurrent Updater**:
    - Per-list toggle switches with persistent storage in `SharedPreferences`.
    - Concurrent background downloading with GZIP compression and fallback mirrors.
    - Sub-microsecond \(O(1)\) domain suffix matching and dynamic cosmetic CSS filtering.

- **🎨 Kuro Cosmic HyperScroll Adaptive Icon**:
  - Bespoke modern adaptive icon featuring dynamic manga panel art, cyber-cyan and electric-magenta energy vortex trails, and a 165Hz golden speed-lightning crest.
  - Includes dedicated Android 13+ Material You monochrome theming and multi-density high-res mipmaps.

- **📱 Smart Scroll Fullscreen & HUD**:
  - **Basic Browser Mode by Default**: Launches with standard browser controls (status bar, top URL/search bar, and bottom navigation bar).
  - **Dynamic Scroll-Down Fullscreen**: Seamlessly enters immersive fullscreen mode when scrolling down through manga chapters; restores controls when scrolling up or returning to top.
  - **Tap-to-Toggle HUD**: Single-tap anywhere to manually toggle between fullscreen reading mode and standard browser view.

- **📚 Manga Sites & Dynamic Bookmark Management**:
  - Custom bookmarks manager with 1-tap addition, editing, and deletion.
  - Instant synchronization between the Home dashboard and the reader controls drawer.
  - Seamless navigation to any saved comic chapter or reading portal.

- **📜 Full Browsing History**:
  - Automatically records visited pages and manga chapters with page titles, URLs, and timestamps (automatically suppressed in Incognito tabs).
  - Quick access via the Home dashboard or the reader controls bar.
  - Instant keyword search filtering across browsing records with individual or bulk deletion.

- **🔄 One-Tap AdBlock Filter Refresh**:
  - Readily accessible in two places:
    1. **Top Bar Shield Badge (`🛡️`)**: Tap the shield icon to open the AdBlock dialog and tap **"🔄 Refresh & Update Filter Lists Now"**.
    2. **Reader Settings (`⚙️`)**: Scroll to the AdBlock section and tap **"Update Lists Now"**.
  - Downloads and parses latest filter rules concurrently across all 15 filter sources.

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
│  - Top Bar (Auto-hiding, URL bar, AdBlock badge, Tabs) │
│  - HomeScreenContent (Search engines, Manhwa directory)│
│  - Tab Switcher Sheet (Multi-tab, Incognito isolation) │
│  - Bottom Control Bar (Reader mode, Refresh rate, Nav) │
│  - Quick Bookmarks Drawer & Settings Bottom Sheet      │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│              ComicWebView (AndroidView)                │
│  - Hardware Accelerated Layer (LAYER_TYPE_HARDWARE)    │
│  - Touch & Horizontal Swipe Gesture Navigation         │
│  - Incognito Privacy Enforcement (No-cache, no-cookies)│
│  - Custom WebChromeClient (Block popups & redirects)   │
│  - Custom WebViewClient with AdBlockEngine             │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│         AdBlockEngine & Core Data Managers             │
│  - Domain & Subdomain Filter Set (15 filter sources)   │
│  - TabManager (Multi-tab state, LRU persistence)       │
│  - SearchEngine & ManhwaDirectory catalogs             │
│  - BookmarkManager & HistoryManager                    │
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
| **Tab Switcher** | Tab counter badge in bottom bar | Open tab switcher sheet, switch active tabs, close tabs, or clear all tabs |
| **Back / Forward Navigation** | Back / Forward buttons in bottom bar (or swipe gestures) | Move backward and forward through web history or return to Home |
| **Refresh Page** | Dedicated Refresh button in upper bar | Reload active web page or refresh home content |
| **Incognito Mode** | New Private Tab in tab sheet | Browse privately with cache/cookies disabled, purple theme, and no history tracking |
| **Horizontal Swipe Navigation** | Swipe Left / Right on screen | Swipe right to navigate back (or return to Home); swipe left to navigate forward |
| **Home Dashboard** | Return to `about:home` or New Tab | Access multi-engine search, curated manhwa directory, bookmarks, and recent history |
| **Search Engines** | Home search bar engine chips | Switch search providers between Google, DuckDuckGo, Brave, Bing, and Ecosia |
| **Manage Sites & Bookmarks** | "+ Sites" button in bottom bar | View, add custom sites, bookmark current page, or delete saved bookmarks |
| **Page Navigation** | Volume Up / Down | Scrolls up or down one page length for one-handed reading |
| **Refresh Rate** | Settings Sheet / ⚡ Badge | Choose between Auto (Max), 60Hz, 90Hz, 120Hz, 144Hz, or 165Hz |
| **AdBlock Shield** | Shield icon in top bar | View blocked ads count, toggle AdBlock, or whitelist current domain |
| **OLED Invert** | Moon icon in bottom bar | Inverts white comic backgrounds for dark-room reading |
| **Screen Awake** | Settings Sheet | Keep screen continuously on while reading |

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.