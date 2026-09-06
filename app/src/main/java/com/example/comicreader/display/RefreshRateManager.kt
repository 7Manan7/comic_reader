package com.example.comicreader.display

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/**
 * Manages display refresh rates and optimizes window rendering for
 * high refresh rate screens (60Hz, 90Hz, 120Hz, 144Hz, 165Hz+).
 */
object RefreshRateManager {
    private const val TAG = "RefreshRateManager"

    data class RefreshRateOption(
        val label: String,
        val targetHz: Float // -1f for Auto/Max
    )

    private val _currentRefreshRate = MutableStateFlow(60f)
    val currentRefreshRate: StateFlow<Float> = _currentRefreshRate.asStateFlow()

    private val _availableModes = MutableStateFlow<List<Float>>(emptyList())
    val availableModes: StateFlow<List<Float>> = _availableModes.asStateFlow()

    private val _selectedModeLabel = MutableStateFlow("Auto (Max)")
    val selectedModeLabel: StateFlow<String> = _selectedModeLabel.asStateFlow()

    /**
     * Initializes and queries the display hardware to detect supported refresh rates.
     */
    fun init(activity: Activity) {
        val display = getDisplay(activity) ?: return
        val rates = getSupportedRefreshRates(display)
        _availableModes.value = rates

        val activeRate = display.refreshRate
        _currentRefreshRate.value = activeRate
        Log.i(TAG, "Active refresh rate: $activeRate Hz. Supported rates: $rates")

        // Default to the maximum available refresh rate (e.g., 120Hz, 144Hz, 165Hz)
        setMaxRefreshRate(activity)
    }

    /**
     * Query all unique supported refresh rates from the display.
     */
    fun getSupportedRefreshRates(display: Display): List<Float> {
        val modes = display.supportedModes ?: return listOf(display.refreshRate)
        return modes.map { (it.refreshRate * 10).roundToInt() / 10f }
            .distinct()
            .sorted()
    }

    /**
     * Finds and locks the window to the highest available refresh rate (e.g. 165Hz, 144Hz, 120Hz).
     */
    fun setMaxRefreshRate(activity: Activity) {
        val display = getDisplay(activity) ?: return
        val modes = display.supportedModes ?: return

        val maxMode = modes.maxByOrNull { it.refreshRate }
        if (maxMode != null) {
            applyDisplayMode(activity, maxMode)
            _selectedModeLabel.value = "Auto (Max: ${maxMode.refreshRate.roundToInt()}Hz)"
            Log.i(TAG, "Locked to Max display mode: ${maxMode.refreshRate} Hz (modeId: ${maxMode.modeId})")
        }
    }

    /**
     * Sets the display to a specific target refresh rate (e.g. 60f, 120f, 144f, 165f) or Auto (-1f).
     */
    fun setRefreshRate(activity: Activity, targetHz: Float) {
        if (targetHz <= 0f) {
            setMaxRefreshRate(activity)
            return
        }

        val display = getDisplay(activity) ?: return
        val modes = display.supportedModes ?: return

        // Find the mode closest to targetHz
        val bestMode = modes.minByOrNull { kotlin.math.abs(it.refreshRate - targetHz) }
        if (bestMode != null) {
            applyDisplayMode(activity, bestMode)
            _selectedModeLabel.value = "${targetHz.roundToInt()} Hz"
            Log.i(TAG, "Applied target refresh rate: ${bestMode.refreshRate} Hz")
        }
    }

    private fun applyDisplayMode(activity: Activity, mode: Display.Mode) {
        val window = activity.window
        val layoutParams = window.attributes

        layoutParams.preferredDisplayModeId = mode.modeId
        layoutParams.preferredRefreshRate = mode.refreshRate
        window.attributes = layoutParams

        _currentRefreshRate.value = mode.refreshRate
    }

    @Suppress("DEPRECATION")
    private fun getDisplay(activity: Activity): Display? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display
        } else {
            activity.windowManager.defaultDisplay
        }
    }
}
