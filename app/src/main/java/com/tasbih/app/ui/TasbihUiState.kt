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
    val sessionActiveTimeMillis: Long = 0L,
    val isTimingPaused: Boolean = true
) {
    val progress: Float
        get() {
            val dhikr = currentDhikr ?: return 0f
            if (dhikr.targetCount <= 0) return 0f
            return (dhikr.currentCount.toFloat() / dhikr.targetCount.toFloat()).coerceIn(0f, 1f)
        }

    val formattedSessionTime: String
        get() {
            val totalSeconds = (sessionActiveTimeMillis / 1000).coerceAtLeast(0)
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
}
