package com.tasbih.app.data.model

enum class VibrationLevel {
    OFF,
    LIGHT,
    MEDIUM,
    STRONG
}

data class AppSettings(
    val isVibrationEnabled: Boolean = true,
    val vibrationIntensity: Int = 3, // 1 dan 5 gacha (1: Juda yengil, 3: O'rta, 5: Maksimal)
    val vibrationLevel: VibrationLevel = VibrationLevel.MEDIUM,
    val isSoundEnabled: Boolean = false,
    val isVolumeButtonsEnabled: Boolean = true,
    val isKeepScreenOn: Boolean = false,
    val isFullScreenTapEnabled: Boolean = false
)
