package com.tasbih.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.tasbih.app.R
import com.tasbih.app.data.model.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    onTestVibration: (Int) -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // 1. VIBRATSIYA BO'LIMI
            // ==========================================
            Text(
                text = stringResource(R.string.settings_section_vibration),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_vibration_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_vibration_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.isVibrationEnabled,
                    onCheckedChange = { onSettingsChanged(settings.copy(isVibrationEnabled = it)) }
                )
            }

            if (settings.isVibrationEnabled) {
                Spacer(modifier = Modifier.height(14.dp))

                var sliderRawValue by remember(settings.vibrationIntensity) {
                    mutableFloatStateOf(settings.vibrationIntensity.toFloat())
                }
                val activeIntensity = sliderRawValue.roundToInt().coerceIn(1, 5)

                val levelText = when (activeIntensity) {
                    1 -> stringResource(R.string.settings_vibration_level_1)
                    2 -> stringResource(R.string.settings_vibration_level_2)
                    3 -> stringResource(R.string.settings_vibration_level_3)
                    4 -> stringResource(R.string.settings_vibration_level_4)
                    else -> stringResource(R.string.settings_vibration_level_5)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_vibration_level_1),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$activeIntensity — $levelText",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.settings_vibration_level_5),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Erkin harakatlanuvchi (continuous) Slider
                Slider(
                    value = sliderRawValue,
                    onValueChange = { newVal ->
                        sliderRawValue = newVal
                    },
                    onValueChangeFinished = {
                        // Magnetic snap: barmoq qo'yib yuborilganda eng yaqin stansiyaga snap bo'ladi
                        val snapped = sliderRawValue.roundToInt().coerceIn(1, 5)
                        sliderRawValue = snapped.toFloat()
                        if (snapped != settings.vibrationIntensity) {
                            onSettingsChanged(settings.copy(vibrationIntensity = snapped))
                        }
                        onTestVibration(snapped)
                    },
                    valueRange = 1f..5f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // 5 ta Magnetic Station nuqtalarining 100% mukammal alignmenti:
                // Material3 Slider thumb radiusi 10.dp (20.dp diametr).
                // Shuning uchun stansiyalar 10.dp va (width - 10.dp) oralig'ida aniq chiziladi.
                val primaryColor = MaterialTheme.colorScheme.primary
                val outlineColor = MaterialTheme.colorScheme.outlineVariant
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val thumbRadiusPx = 10.dp.toPx()
                                val availableWidth = size.width - 2 * thumbRadiusPx
                                if (availableWidth > 0) {
                                    val fraction = ((offset.x - thumbRadiusPx) / availableWidth).coerceIn(0f, 1f)
                                    val tappedLevel = (1 + fraction * 4f).roundToInt().coerceIn(1, 5)
                                    sliderRawValue = tappedLevel.toFloat()
                                    if (tappedLevel != settings.vibrationIntensity) {
                                        onSettingsChanged(settings.copy(vibrationIntensity = tappedLevel))
                                    }
                                    onTestVibration(tappedLevel)
                                }
                            }
                        }
                ) {
                    val thumbRadiusPx = 10.dp.toPx()
                    val availableWidth = size.width - 2 * thumbRadiusPx
                    val centerY = size.height / 2

                    for (i in 0..4) {
                        val stationLevel = i + 1
                        val cx = thumbRadiusPx + availableWidth * (i / 4f)
                        val isCurrent = stationLevel == activeIntensity

                        val circleRadius = if (isCurrent) 5.dp.toPx() else 3.dp.toPx()
                        val circleColor = if (isCurrent) primaryColor else outlineColor

                        drawCircle(
                            color = circleColor,
                            radius = circleRadius,
                            center = Offset(cx, centerY)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                FilledTonalButton(
                    onClick = { onTestVibration(activeIntensity) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = stringResource(R.string.settings_vibration_test_button),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // 2. OVOZ VA BOSHQARUV BO'LIMI
            // ==========================================
            Text(
                text = stringResource(R.string.settings_section_sound_control),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Ovoz effekti
            SettingToggleItem(
                title = stringResource(R.string.settings_sound_title),
                subtitle = stringResource(R.string.settings_sound_subtitle),
                checked = settings.isSoundEnabled,
                onCheckedChange = { onSettingsChanged(settings.copy(isSoundEnabled = it)) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Ovoz tugmalari orqali sanash
            SettingToggleItem(
                title = stringResource(R.string.settings_volume_buttons_title),
                subtitle = stringResource(R.string.settings_volume_buttons_subtitle),
                checked = settings.isVolumeButtonsEnabled,
                onCheckedChange = { onSettingsChanged(settings.copy(isVolumeButtonsEnabled = it)) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // 3. EKRAN BO'LIMI
            // ==========================================
            Text(
                text = stringResource(R.string.settings_section_display),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Ekranni doim yoqiq tutish
            SettingToggleItem(
                title = stringResource(R.string.settings_keep_screen_on_title),
                subtitle = stringResource(R.string.settings_keep_screen_on_subtitle),
                checked = settings.isKeepScreenOn,
                onCheckedChange = { onSettingsChanged(settings.copy(isKeepScreenOn = it)) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Butun ekranni bosish rejimi
            SettingToggleItem(
                title = stringResource(R.string.settings_fullscreen_tap_title),
                subtitle = stringResource(R.string.settings_fullscreen_tap_subtitle),
                checked = settings.isFullScreenTapEnabled,
                onCheckedChange = { onSettingsChanged(settings.copy(isFullScreenTapEnabled = it)) }
            )
        }
    }
}

@Composable
private fun SettingToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
