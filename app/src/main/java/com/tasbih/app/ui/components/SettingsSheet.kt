package com.tasbih.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
                .verticalScroll(rememberScrollState())
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

                Spacer(modifier = Modifier.height(8.dp))

                // =========================================================================
                // 100% DETERMINISTIK YAGONA KOORDINATA TIZIMLI CUSTOM VIBRATION SLIDER
                // Track, 5 ta dot va Thumb aynan bitta Canvas ichida bitta formula asosida chiziladi:
                // Level 1, 2, 3, 4, 5 da Thumb markazi aynan 1, 2, 3, 4, 5-dot markaziga teng!
                // =========================================================================
                CustomVibrationSlider(
                    value = sliderRawValue,
                    onValueChange = { newVal ->
                        sliderRawValue = newVal
                    },
                    onValueChangeFinished = { finalValue ->
                        val snapped = finalValue.roundToInt().coerceIn(1, 5)
                        sliderRawValue = snapped.toFloat()
                        if (snapped != settings.vibrationIntensity) {
                            onSettingsChanged(settings.copy(vibrationIntensity = snapped))
                        }
                        onTestVibration(snapped)
                    },
                    primaryColor = MaterialTheme.colorScheme.primary,
                    onPrimaryColor = MaterialTheme.colorScheme.onPrimary,
                    trackInactiveColor = MaterialTheme.colorScheme.surfaceVariant,
                    outlineVariantColor = MaterialTheme.colorScheme.outlineVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

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

/**
 * 100% Yagona koordinatali, xatolik ehtimoli 0 bo'lgan maxsus Vibration Slider.
 * 
 * Geometriya:
 * - Umumiy kenglik: W
 * - Thumb radiusi: R = 12.dp
 * - Foydali harakat o'qi (Track Length): L = W - 2*R
 * - 5 ta nuqtaning X koordinatasi: X_i = R + (i / 4.0) * L  (i = 0, 1, 2, 3, 4)
 * - Thumb X koordinatasi: X_thumb = R + ((value - 1.0) / 4.0) * L
 * 
 * Natijada: Level 1..5 bo'lganda thumb markazi aynan X_i nuqtaning qoq ustiga 100.0% tushadi.
 */
@Composable
private fun CustomVibrationSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit,
    primaryColor: Color,
    onPrimaryColor: Color,
    trackInactiveColor: Color,
    outlineVariantColor: Color
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        val thumbRadiusDp = 12.dp
        val activeLevel = value.roundToInt().coerceIn(1, 5)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .pointerInput(totalWidthPx) {
                    val thumbRadiusPx = thumbRadiusDp.toPx()
                    val trackLengthPx = totalWidthPx - 2 * thumbRadiusPx

                    detectTapGestures { offset ->
                        if (trackLengthPx > 0) {
                            val fraction = ((offset.x - thumbRadiusPx) / trackLengthPx).coerceIn(0f, 1f)
                            val snappedLevel = (1 + fraction * 4f).roundToInt().coerceIn(1, 5)
                            onValueChange(snappedLevel.toFloat())
                            onValueChangeFinished(snappedLevel.toFloat())
                        }
                    }
                }
                .pointerInput(totalWidthPx) {
                    val thumbRadiusPx = thumbRadiusDp.toPx()
                    val trackLengthPx = totalWidthPx - 2 * thumbRadiusPx

                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            if (trackLengthPx > 0) {
                                val currentFraction = (value - 1f) / 4f
                                val currentPx = thumbRadiusPx + currentFraction * trackLengthPx
                                val newPx = (currentPx + dragAmount).coerceIn(thumbRadiusPx, thumbRadiusPx + trackLengthPx)
                                val newFraction = (newPx - thumbRadiusPx) / trackLengthPx
                                val newValue = 1f + newFraction * 4f
                                onValueChange(newValue.coerceIn(1f, 5f))
                            }
                        },
                        onDragEnd = {
                            val snapped = value.roundToInt().coerceIn(1, 5).toFloat()
                            onValueChange(snapped)
                            onValueChangeFinished(snapped)
                        },
                        onDragCancel = {
                            val snapped = value.roundToInt().coerceIn(1, 5).toFloat()
                            onValueChange(snapped)
                            onValueChangeFinished(snapped)
                        }
                    )
                }
        ) {
            val thumbRadiusPx = thumbRadiusDp.toPx()
            val trackLengthPx = size.width - 2 * thumbRadiusPx
            val centerY = size.height / 2
            val trackStrokePx = 6.dp.toPx()

            val clampedValue = value.coerceIn(1f, 5f)
            val currentFraction = (clampedValue - 1f) / 4f
            val thumbCenterX = thumbRadiusPx + currentFraction * trackLengthPx

            // 1. Inaktiv fon treki (butun o'q bo'ylab)
            drawLine(
                color = trackInactiveColor,
                start = Offset(thumbRadiusPx, centerY),
                end = Offset(thumbRadiusPx + trackLengthPx, centerY),
                strokeWidth = trackStrokePx,
                cap = StrokeCap.Round
            )

            // 2. Aktiv rangli trek (boshidan thumb markazigacha)
            if (thumbCenterX > thumbRadiusPx) {
                drawLine(
                    color = primaryColor,
                    start = Offset(thumbRadiusPx, centerY),
                    end = Offset(thumbCenterX, centerY),
                    strokeWidth = trackStrokePx,
                    cap = StrokeCap.Round
                )
            }

            // 3. 5 ta Magnetic Station nuqtalari
            for (i in 0..4) {
                val stationLevel = i + 1
                val dotCenterX = thumbRadiusPx + (i / 4f) * trackLengthPx
                val isSelected = stationLevel == activeLevel

                // Dot o'lchami va rangi
                val dotRadius = if (isSelected) 4.5.dp.toPx() else 3.dp.toPx()
                val dotColor = if (stationLevel <= activeLevel) {
                    if (isSelected) onPrimaryColor else primaryColor.copy(alpha = 0.8f)
                } else {
                    outlineVariantColor
                }

                drawCircle(
                    color = dotColor,
                    radius = dotRadius,
                    center = Offset(dotCenterX, centerY)
                )
            }

            // 4. Thumb (Sokin, nafis va tactile tugma)
            // Soya
            drawCircle(
                color = Color.Black.copy(alpha = 0.18f),
                radius = thumbRadiusPx + 1.dp.toPx(),
                center = Offset(thumbCenterX, centerY + 1.5.dp.toPx())
            )
            // Asosiy thumb doirasi
            drawCircle(
                color = primaryColor,
                radius = thumbRadiusPx,
                center = Offset(thumbCenterX, centerY)
            )
            // Thumb ichki oq yadrosi
            drawCircle(
                color = onPrimaryColor,
                radius = 4.dp.toPx(),
                center = Offset(thumbCenterX, centerY)
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
