package com.tasbih.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tasbih.app.R
import com.tasbih.app.data.model.AppSettings
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    onTestVibration: (Int) -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var isDismissing by remember { mutableStateOf(false) }

    val dismissGracefully: () -> Unit = remember(coroutineScope, sheetState) {
        {
            if (!isDismissing) {
                isDismissing = true
                coroutineScope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        onDismiss()
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            dismissGracefully()
        },
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

                // Mahalliy state — faqat ushbu composable ichida boshqariladi.
                // settings.vibrationIntensity o'zgarganda reset bo'ladi.
                var sliderValue by remember(settings.vibrationIntensity) {
                    mutableFloatStateOf(settings.vibrationIntensity.toFloat())
                }
                val activeIntensity = sliderValue.roundToInt().coerceIn(1, 5)

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

                Spacer(modifier = Modifier.height(4.dp))

                // =========================================================================
                // VIBRATION SLIDER — Material3 Slider + Canvas dot overlay
                //
                // ROOT CAUSE FIX: Oldingi implementation'da ikki alohida .pointerInput()
                // bloki bor edi (detectTapGestures + detectHorizontalDragGestures).
                // Compose'da competitive gesture'lar bo'lganda detectTapGestures
                // ACTION_DOWN'ni consume qilib oladi va drag hech qachon ishlamaydi.
                //
                // YECHIM: Material3 Slider — gesture handling platforma darajasida
                // battle-tested. 5 ta "magnetic station" uchun dots Canvas orqali
                // alohida chiziladi, lekin Slider'ning o'z gesture handling'iga tegmaymiz.
                //
                // Dot X koordinatasi = Slider ichki track geometriyasi:
                //   trackPadding = 10.dp (thumb radius default)
                //   dotX_i = trackPadding + (i/4) * (sliderWidth - 2*trackPadding)
                // =========================================================================
                VibrationSlider(
                    value = sliderValue,
                    onValueChange = { newVal ->
                        sliderValue = newVal
                    },
                    onValueChangeFinished = {
                        val snapped = sliderValue.roundToInt().coerceIn(1, 5)
                        sliderValue = snapped.toFloat()
                        if (snapped != settings.vibrationIntensity) {
                            onSettingsChanged(settings.copy(vibrationIntensity = snapped))
                        }
                        onTestVibration(snapped)
                    },
                    primaryColor = MaterialTheme.colorScheme.primary,
                    onPrimaryColor = MaterialTheme.colorScheme.onPrimary,
                    outlineVariantColor = MaterialTheme.colorScheme.outlineVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

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

            SettingToggleItem(
                title = stringResource(R.string.settings_sound_title),
                subtitle = stringResource(R.string.settings_sound_subtitle),
                checked = settings.isSoundEnabled,
                onCheckedChange = { onSettingsChanged(settings.copy(isSoundEnabled = it)) }
            )

            Spacer(modifier = Modifier.height(14.dp))

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

            SettingToggleItem(
                title = stringResource(R.string.settings_keep_screen_on_title),
                subtitle = stringResource(R.string.settings_keep_screen_on_subtitle),
                checked = settings.isKeepScreenOn,
                onCheckedChange = { onSettingsChanged(settings.copy(isKeepScreenOn = it)) }
            )

            Spacer(modifier = Modifier.height(14.dp))

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
 * Vibration Slider — Material3 Slider asosida, 5 ta dot Canvas orqali overlay sifatida.
 *
 * Material3 Slider gesture handling'i platforma darajasida ishlaydi va
 * real qurilmalarda drag/tap 100% ishonchli.
 *
 * 5 ta "magnetic station" dots vizual maqsadda chiziladi;
 * snap logikasi onValueChangeFinished'da amalga oshiriladi.
 *
 * Slider ichki track geometriyasi (M3 default):
 *   thumbRadius = 10.dp
 *   trackStart  = thumbRadius
 *   trackEnd    = sliderWidth - thumbRadius
 *   dot_i X     = thumbRadius + (i/4) * (sliderWidth - 2*thumbRadius)
 */
@Composable
private fun VibrationSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    primaryColor: Color,
    onPrimaryColor: Color,
    outlineVariantColor: Color
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Material3 Slider — barcha gesture handling o'z ichida
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 1f..5f,
            steps = 0, // smooth, continuous drag
            modifier = Modifier.fillMaxWidth()
        )

        // 5 ta dot overlay — faqat vizual, gesture emas
        val thumbRadiusDp = 10.dp
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.Center)
        ) {
            val thumbRadiusPx = thumbRadiusDp.toPx()
            val trackLength = size.width - 2f * thumbRadiusPx
            if (trackLength <= 0f) return@Canvas
            val centerY = size.height / 2f
                val clamped = value.coerceIn(1f, 5f)
                val activeLevel = clamped.roundToInt().coerceIn(1, 5)

                for (i in 0..4) {
                    val level = i + 1
                    val dotX = thumbRadiusPx + (i / 4f) * trackLength
                    val isActive = level == activeLevel
                    val isPassed = level < activeLevel

                    val dotRadius = if (isActive) 4.dp.toPx() else 2.5.dp.toPx()
                    val dotColor = when {
                        isActive -> onPrimaryColor
                        isPassed -> primaryColor.copy(alpha = 0.7f)
                        else -> outlineVariantColor
                    }
                    drawCircle(
                        color = dotColor,
                        radius = dotRadius,
                        center = Offset(dotX, centerY)
                    )
                }
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
