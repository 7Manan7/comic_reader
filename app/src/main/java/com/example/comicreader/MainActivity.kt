package com.example.comicreader

import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.comicreader.adblock.AdBlockEngine
import com.example.comicreader.adblock.AdBlockListManager
import com.example.comicreader.display.RefreshRateManager
import com.example.comicreader.theme.ComicReaderTheme
import com.example.comicreader.ui.ComicWebView
import com.example.comicreader.ui.ReaderScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var activeWebView: ComicWebView? = null
    private var isVolumeScrollEnabled: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure edge-to-edge rendering
        enableEdgeToEdge()

        // Initialize high refresh rate manager (optimizes for 60, 120, 144, 165 Hz displays)
        RefreshRateManager.init(this)

        // Initialize EasyList and EasyPrivacy filter lists in background
        lifecycleScope.launch {
            AdBlockListManager.init(applicationContext)
        }

        // Load persisted adblock whitelist
        AdBlockEngine.loadWhitelist(applicationContext)

        // Default to standard browser mode (status bar visible, fullscreen on scroll down)
        setImmersiveMode(false)

        setContent {
            ComicReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ReaderScreen(
                        onToggleFullscreen = { isFullscreen ->
                            setImmersiveMode(isFullscreen)
                        },
                        onToggleKeepScreenOn = { keepOn ->
                            setKeepScreenOn(keepOn)
                        },
                        onToggleVolumeScroll = { enabled ->
                            isVolumeScrollEnabled = enabled
                        },
                        onRegisterWebView = { webView ->
                            activeWebView = webView
                        }
                    )
                }
            }
        }
    }

    /**
     * Controls status bar visibility:
     * - Immersive Fullscreen: status bar is completely hidden for distraction-free reading.
     * - Non-immersive: status bar is transparent with content drawn edge-to-edge behind it.
     */
    private fun setImmersiveMode(fullscreen: Boolean) {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (fullscreen) {
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.statusBars())
            insetsController.isAppearanceLightStatusBars = false
        }
    }

    /**
     * Keeps screen awake while reading manga/comic chapters.
     */
    private fun setKeepScreenOn(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /**
     * Intercepts Volume Up and Down keys for one-handed comic page scrolling.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val action = event.action
        val keyCode = event.keyCode

        if (isVolumeScrollEnabled && activeWebView != null && action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    activeWebView?.scrollPageDown()
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    activeWebView?.scrollPageUp()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
