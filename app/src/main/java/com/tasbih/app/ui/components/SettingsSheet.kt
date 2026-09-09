package com.tasbih.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tasbih.app.data.model.AppSettings
import com.tasbih.app.data.model.VibrationLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
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

            // Vibratsiya darajasi
            Text(
                text = "Vibratsiya kuchi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    VibrationLevel.OFF to "O'chiq",
                    VibrationLevel.LIGHT to "Yengil",
                    VibrationLevel.MEDIUM to "O'rta",
                    VibrationLevel.STRONG to "Kuchli"
                ).forEach { (level, title) ->
                    FilterChip(
                        selected = settings.vibrationLevel == level,
                        onClick = {
                            onSettingsChanged(settings.copy(vibrationLevel = level))
                        },
                        label = { Text(title) }
                    )
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
