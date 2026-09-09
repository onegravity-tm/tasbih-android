package com.tasbih.app.data.model

enum class VibrationLevel {
    OFF,
    LIGHT,
    MEDIUM,
    STRONG
}

data class AppSettings(
    val vibrationLevel: VibrationLevel = VibrationLevel.MEDIUM,
    val isSoundEnabled: Boolean = false,
    val isVolumeButtonsEnabled: Boolean = true,
    val isKeepScreenOn: Boolean = false,
    val isFullScreenTapEnabled: Boolean = false
)
