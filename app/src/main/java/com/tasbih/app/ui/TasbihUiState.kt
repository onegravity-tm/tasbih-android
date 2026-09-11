package com.tasbih.app.ui

import com.tasbih.app.data.model.AppSettings
import com.tasbih.app.data.model.DhikrItem

/**
 * Tasbih ekranining to'liq UI holati (State).
 */
data class TasbihUiState(
    val dhikrList: List<DhikrItem> = emptyList(),
    val currentDhikr: DhikrItem? = null,
    val settings: AppSettings = AppSettings(),
    val isTargetReached: Boolean = false,
    val isDhikrSheetOpen: Boolean = false,
    val isSettingsSheetOpen: Boolean = false,
    val isAddDhikrDialogOpen: Boolean = false,
    val isEditTargetDialogOpen: Boolean = false,
    val isCalibrationDialogOpen: Boolean = false,
    val isTimingPaused: Boolean = true
) {
    val progress: Float
        get() {
            val dhikr = currentDhikr ?: return 0f
            if (dhikr.targetCount <= 0) return 0f
            return (dhikr.currentCount.toFloat() / dhikr.targetCount.toFloat()).coerceIn(0f, 1f)
        }

    /**
     * Variant A: Floor / Truncation (kasr sekund tashlanadi).
     * 43.8s -> 43s
     * 59.9s -> 59s
     * 60.0s -> 1m 0s
     * 1m 18.9s -> 1m 18s
     */
    val formattedTotalTime: String
        get() {
            val totalMs = currentDhikr?.totalActiveTimeMillis ?: 0L
            val totalSeconds = (totalMs / 1000).coerceAtLeast(0L)
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60

            return when {
                hours > 0 -> "${hours}s ${minutes}m ${seconds}s"
                minutes > 0 -> "${minutes}m ${seconds}s"
                else -> "${seconds}s"
            }
        }
}
