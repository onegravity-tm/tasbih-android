package com.tasbih.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.tasbih.app.R
import com.tasbih.app.data.model.AppSettings
import com.tasbih.app.data.model.VibrationLevel

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
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Sozlamalar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Vibratsiya bo'limi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_vibration_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
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
                Spacer(modifier = Modifier.height(12.dp))

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
                    modifier = Modifier.fillMaxWidth()
                )

                // 5 ta Magnetic Station indikator nuqtalari
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { station ->
                        val isCurrent = station == activeIntensity
                        Box(
                            modifier = Modifier
                                .size(if (isCurrent) 10.dp else 6.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(
                                    if (isCurrent) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    }
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                FilledTonalButton(
                    onClick = { onTestVibration(activeIntensity) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(text = stringResource(R.string.settings_vibration_test_button))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Ovoz effekti
            SettingToggleItem(
                title = "Tovush signali",
                subtitle = "Har bir bosishda mayin chertish tovushi",
                checked = settings.isSoundEnabled,
                onCheckedChange = { onSettingsChanged(settings.copy(isSoundEnabled = it)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Ovoz tugmalari orqali sanash
            SettingToggleItem(
                title = "Ovoz tugmalari orqali sanash",
                subtitle = "Faqat ilova ekranda ochiq bo'lganida ishlaydi",
                checked = settings.isVolumeButtonsEnabled,
                onCheckedChange = { onSettingsChanged(settings.copy(isVolumeButtonsEnabled = it)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Ekranni doim yoqiq tutish
            SettingToggleItem(
                title = "Ekranni yoqiq tutish",
                subtitle = "Zikr paytida telefon ekrani o'chib qolmaydi",
                checked = settings.isKeepScreenOn,
                onCheckedChange = { onSettingsChanged(settings.copy(isKeepScreenOn = it)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Butun ekranni bosish rejimi
            SettingToggleItem(
                title = "To'liq ekran bosish rejimi",
                subtitle = "Ekranning istalgan joyiga bosganda hisob oshadi",
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
                fontWeight = FontWeight.Medium
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
