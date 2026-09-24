package com.tasbih.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tasbih.app.R
import com.tasbih.app.data.model.AppSettings
import kotlin.math.abs
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

                // Slider value state: faqat slider ichida boshqariladi.
                // remember(settings.vibrationIntensity) orqali settings o'zgarganda reset bo'ladi.
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

                Spacer(modifier = Modifier.height(8.dp))

                VibrationSlider(
                    value = sliderValue,
                    onValueChange = { newVal ->
                        // Drag paytida real-time yangilanish
                        sliderValue = newVal
                    },
                    onValueChangeFinished = { finalVal ->
                        // Barmoq olinganda: eng yaqin levelga snap + saqlash
                        val snapped = finalVal.roundToInt().coerceIn(1, 5)
                        sliderValue = snapped.toFloat()
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
 * YAGONA pointerInput bloki bilan ishlaydigan custom Vibration Slider.
 *
 * ROOT CAUSE FIX (oldingi versiyada drag ishlamasligining sababi):
 * - Oldingi versiyada detectTapGestures va detectHorizontalDragGestures
 *   IKKI ALOHIDA .pointerInput() blokida edi.
 * - Compose'da bir composable'ga ikki pointerInput qo'yilsa, ular competitive
 *   ishlaydi va detectTapGestures ACTION_DOWN'ni avval consume qiladi,
 *   shuning uchun detectHorizontalDragGestures hech qachon drag eventlarini olmaydi.
 *
 * YECHIM:
 * - Yagona pointerInput + awaitEachGesture + awaitFirstDown + horizontalDrag
 * - Barcha gesture handling bitta gesture detector ichida.
 * - Tap va Drag bir xil gesture session ichida ajralib chiqadi.
 *
 * PERFORMANCE FIX:
 * - BoxWithConstraints olib tashlandi (qimmat layout pass).
 * - Slider kengligi onSizeChanged bilan o'lchanadi.
 * - onValueChange faqat real qiymat o'zganda chaqiriladi.
 *
 * Geometriya:
 * - R = thumbRadius = 12.dp
 * - L = width - 2*R  (foydali track uzunligi)
 * - Dot i (i=0..4): X_i = R + (i/4) * L
 * - Thumb: X_thumb = R + ((value-1)/4) * L
 * => Level 1..5 da thumb markazi = dot markazi (aynan)
 */
@Composable
private fun VibrationSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit,
    primaryColor: Color,
    onPrimaryColor: Color,
    trackInactiveColor: Color,
    outlineVariantColor: Color
) {
    // Slider kengligi pixel'da — onSizeChanged bilan lazily o'lchanadi
    var sliderWidthPx by remember { mutableFloatStateOf(0f) }
    val thumbRadiusDp = 12.dp

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .onSizeChangedCompat { size ->
                sliderWidthPx = size.width.toFloat()
            }
            .pointerInput(Unit) {
                // Bir-biriga to'sqinlik qilmaydigan yagona gesture handler.
                // awaitEachGesture: har bir touch sessiyasini boshidan oxirigacha tutadi.
                // awaitFirstDown: barmoq tegishini kutadi.
                // Keyin harakat masofasiga qarab Tap yoki Drag deb belgilaydi.
                awaitEachGesture {
                    val thumbRadiusPx = thumbRadiusDp.toPx()
                    val trackLength = sliderWidthPx - 2f * thumbRadiusPx
                    if (trackLength <= 0f) return@awaitEachGesture

                    // Barmoq tegishini kutamiz (Initial DOWN event)
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()

                    // Barmoq teggan joydan qiymatni hisoblash
                    fun xToValue(x: Float): Float {
                        val fraction = ((x - thumbRadiusPx) / trackLength).coerceIn(0f, 1f)
                        return (1f + fraction * 4f).coerceIn(1f, 5f)
                    }

                    // DOWN holatida darhol thumb'ni barmoq ostiga ko'chiramiz
                    val downValue = xToValue(down.position.x)
                    onValueChange(downValue)

                    var isDragging = false
                    var lastValue = downValue

                    // Pointer eventslarini kuzatish (UP yoki CANCEL gacha)
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Main)
                        val change = event.changes.firstOrNull() ?: break

                        if (change.pressed) {
                            // Barmoq harakatda: drag
                            change.consume()
                            val newValue = xToValue(change.position.x)
                            if (newValue != lastValue) {
                                isDragging = true
                                lastValue = newValue
                                onValueChange(newValue)
                            }
                        } else {
                            // Barmoq olingan: release
                            change.consume()
                            // Snap to nearest level
                            val snapped = lastValue.roundToInt().coerceIn(1, 5).toFloat()
                            if (snapped != lastValue) {
                                onValueChange(snapped)
                            }
                            onValueChangeFinished(snapped)
                            break
                        }
                    }
                }
            }
    ) {
        val thumbRadiusPx = thumbRadiusDp.toPx()
        // sliderWidthPx birinchi recomposition'dan keyin to'ldiriladi,
        // ammo Canvas size ham mavjud — ikkalasi bir xil bo'lishi kerak.
        val w = if (sliderWidthPx > 0f) sliderWidthPx else size.width
        val trackLength = w - 2f * thumbRadiusPx
        val centerY = size.height / 2f
        val trackStroke = 6.dp.toPx()

        val clamped = value.coerceIn(1f, 5f)
        val fraction = (clamped - 1f) / 4f
        val thumbCenterX = thumbRadiusPx + fraction * trackLength
        val activeLevel = clamped.roundToInt().coerceIn(1, 5)

        // --- 1. Inaktiv track (butun uzunlik) ---
        drawLine(
            color = trackInactiveColor,
            start = Offset(thumbRadiusPx, centerY),
            end = Offset(thumbRadiusPx + trackLength, centerY),
            strokeWidth = trackStroke,
            cap = StrokeCap.Round
        )

        // --- 2. Aktiv track (boshidan thumb gacha) ---
        if (thumbCenterX > thumbRadiusPx) {
            drawLine(
                color = primaryColor,
                start = Offset(thumbRadiusPx, centerY),
                end = Offset(thumbCenterX, centerY),
                strokeWidth = trackStroke,
                cap = StrokeCap.Round
            )
        }

        // --- 3. 5 ta station nuqtalari ---
        for (i in 0..4) {
            val level = i + 1
            // Dot X koordinatasi = thumb formula bilan identik → 100% mos
            val dotX = thumbRadiusPx + (i / 4f) * trackLength
            val isActive = level == activeLevel
            val isPassed = level < activeLevel

            val dotRadius = if (isActive) 4.5.dp.toPx() else 3.dp.toPx()
            val dotColor = when {
                isActive -> onPrimaryColor
                isPassed -> primaryColor.copy(alpha = 0.85f)
                else -> outlineVariantColor
            }
            drawCircle(color = dotColor, radius = dotRadius, center = Offset(dotX, centerY))
        }

        // --- 4. Thumb ---
        // Soya
        drawCircle(
            color = Color.Black.copy(alpha = 0.20f),
            radius = thumbRadiusPx + 1.5.dp.toPx(),
            center = Offset(thumbCenterX, centerY + 1.5.dp.toPx())
        )
        // Asosiy doira
        drawCircle(
            color = primaryColor,
            radius = thumbRadiusPx,
            center = Offset(thumbCenterX, centerY)
        )
        // Ichki oq yadro
        drawCircle(
            color = onPrimaryColor,
            radius = 4.dp.toPx(),
            center = Offset(thumbCenterX, centerY)
        )
    }
}

/**
 * Canvas composable'ning o'lchamini olish uchun yordamchi extension.
 * onSizeChanged Layout modifier orqali ishlaydi.
 */
private fun Modifier.onSizeChangedCompat(onSizeChanged: (androidx.compose.ui.unit.IntSize) -> Unit): Modifier =
    this.then(
        androidx.compose.ui.layout.onSizeChanged { size -> onSizeChanged(size) }
    )

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
