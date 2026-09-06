# Kuro Reader — Developer Guide & APK Build Instructions

This guide explains how to build, install, test, and develop **Kuro Reader** (`com.example.comicreader`).

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
├── MainActivity.kt               # Entry activity, window insets, volume key scrolling
├── adblock/
│   ├── AdBlockEngine.kt          # Host matching, cosmetic CSS filtering, anti-popup JS
│   └── AdBlockListManager.kt     # Multi-list downloading, GZIP extraction, 16 filter lists
├── data/
│   ├── BookmarkManager.kt        # Persistent bookmarks (Comix, MangaFreak, custom sites)
│   ├── HistoryManager.kt         # Browsing & reading history storage
│   └── SessionManager.kt         # Last visited page persistence & startup session restoration
├── display/
│   └── RefreshRateManager.kt     # 60/90/120/144/165Hz display mode detection & locking
├── theme/
│   ├── Color.kt / Theme.kt       # Jetpack Compose OLED dark mode styling
└── ui/
    ├── ComicWebView.kt           # Hardware-accelerated WebView with gesture detection
    └── ReaderScreen.kt           # Main UI: URL bar, Brave menu, Home speed-dial, bottom bar
```

---

## 💡 5. Important Architecture Decisions

- **Restore Last Visited Page**: Managed by `SessionManager`. Every time a valid HTTP/HTTPS page is navigated to, its URL is automatically persisted. On app startup/restart, if "Restore Last Visited Page" is enabled (enabled by default), Kuro Reader automatically re-opens the exact comic chapter/page where the user left off. Can be toggled on/off in the Kuro Menu (⋮).
- **HUD Tap Zones**: To prevent taps on website elements (like search buttons, logos, tabs, or chapter pagination) from accidentally toggling the reader HUD, `ComicWebView.onTouchEvent` strictly limits tap-to-toggle gestures to the **center reading zone** (`30%..70%` vertical, `20%..80%` horizontal).
- **Non-Obscuring Column Layout**: The main browser UI uses a `Column` layout rather than an overlapping `Box`. When the URL bar is visible, the web content begins cleanly below the bar so site headers are never covered. When scrolling down to read, the bars collapse (`shrinkVertically()`) and the WebView expands to full-screen edge-to-edge.
- **Safe Insets**: Dialog menus (Brave menu, Home speed dial, History, AdBlock) use `.windowInsetsPadding(WindowInsets.safeDrawing)` with custom border styling to guarantee options never clip off the screen on devices with hole-punch cameras or curved corners.
