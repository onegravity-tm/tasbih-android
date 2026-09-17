package com.tasbih.app

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import com.tasbih.app.ui.TasbihUiState
import com.tasbih.app.ui.TasbihViewModel
import com.tasbih.app.ui.components.AddDhikrDialog
import com.tasbih.app.ui.components.DhikrSelectionSheet
import com.tasbih.app.ui.components.EditTargetDialog
import com.tasbih.app.ui.components.SettingsSheet
import com.tasbih.app.ui.theme.SubtleDangerOutlineDark
import com.tasbih.app.ui.theme.SubtleDangerOutlineLight
import com.tasbih.app.ui.theme.SubtleDangerTextDark
import com.tasbih.app.ui.theme.SubtleDangerTextLight
import com.tasbih.app.ui.theme.TasbihTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TasbihViewModel by viewModels {
        TasbihViewModel.provideFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TasbihTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                // Ekranni doim yoqiq tutish sozlamasi
                LaunchedEffect(uiState.settings.isKeepScreenOn) {
                    if (uiState.settings.isKeepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                TasbihApp(
                    uiState = uiState,
                    onCounterClick = viewModel::onCounterClick,
                    onResetClick = viewModel::onResetClick,
                    onRelearnRhythmClick = viewModel::onRelearnRhythmClick,
                    onOpenDhikrSheet = { viewModel.setDhikrSheetOpen(true) },
                    onOpenSettingsSheet = { viewModel.setSettingsSheetOpen(true) },
                    onEditTargetClick = { viewModel.setEditTargetDialogOpen(true) }
                )

                // Zikrlar ro'yxati sheet
                if (uiState.isDhikrSheetOpen) {
                    DhikrSelectionSheet(
                        dhikrList = uiState.dhikrList,
                        selectedId = uiState.currentDhikr?.id,
                        onSelect = viewModel::selectDhikr,
                        onAddNewClick = { viewModel.setAddDhikrDialogOpen(true) },
                        onDeleteCustom = viewModel::deleteCustomDhikr,
                        onDismiss = { viewModel.setDhikrSheetOpen(false) }
                    )
                }

                // Sozlamalar sheet
                if (uiState.isSettingsSheetOpen) {
                    SettingsSheet(
                        settings = uiState.settings,
                        onSettingsChanged = viewModel::updateSettings,
                        onTestVibration = viewModel::testVibration,
                        onDismiss = { viewModel.setSettingsSheetOpen(false) }
                    )
                }

                // Yangi zikr qo'shish dialogi
                if (uiState.isAddDhikrDialogOpen) {
                    AddDhikrDialog(
                        onDismiss = { viewModel.setAddDhikrDialogOpen(false) },
                        onConfirm = viewModel::addCustomDhikr
                    )
                }

                // Maqsadni tahrirlash dialogi
                if (uiState.isEditTargetDialogOpen && uiState.currentDhikr != null) {
                    EditTargetDialog(
                        dhikrName = uiState.currentDhikr!!.name,
                        currentTarget = uiState.currentDhikr!!.targetCount,
                        onDismiss = { viewModel.setEditTargetDialogOpen(false) },
                        onConfirm = viewModel::updateCurrentDhikrTarget
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.onAppBackgrounded()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val isVolumeControlEnabled = viewModel.uiState.value.settings.isVolumeButtonsEnabled
            if (isVolumeControlEnabled) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP -> {
                        viewModel.onCounterClick()
                        return true // Tizim ovozini o'zgartirmaslik uchun eventni o'zlashtiramiz
                    }
                    KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        viewModel.onCounterDecrement()
                        return true // Tizim ovozini o'zgartirmaslik uchun eventni o'zlashtiramiz
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihApp(
    uiState: TasbihUiState,
    onCounterClick: () -> Unit,
    onResetClick: () -> Unit,
    onRelearnRhythmClick: () -> Unit,
    onOpenDhikrSheet: () -> Unit,
    onOpenSettingsSheet: () -> Unit,
    onEditTargetClick: () -> Unit
) {
    val currentDhikr = uiState.currentDhikr
    val isDark = isSystemInDarkTheme()
    val resetBorderColor = if (isDark) SubtleDangerOutlineDark else SubtleDangerOutlineLight
    val resetTextColor = if (isDark) SubtleDangerTextDark else SubtleDangerTextLight

    // Counter TAP scale animatsiyasi: 1.0f -> 0.97f -> 1.0f (~80ms spring-like)
    var tapAnimTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(currentDhikr?.currentCount, currentDhikr?.totalCount) {
        if (currentDhikr != null && currentDhikr.totalCount > 0L) {
            tapAnimTrigger = true
            delay(40)
            tapAnimTrigger = false
        }
    }
    val counterScale by animateFloatAsState(
        targetValue = if (tapAnimTrigger) 0.97f else 1.0f,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "counterScale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onOpenDhikrSheet) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = stringResource(R.string.cd_dhikr_list)
                        )
                    }
                    IconButton(onClick = onOpenSettingsSheet) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .then(
                    if (uiState.settings.isFullScreenTapEnabled) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onCounterClick
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ==========================================
                // 1. YUQORI QISM: Zikr kartasi va Target Badge
                // ==========================================
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(onClick = onOpenDhikrSheet)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = currentDhikr?.name ?: stringResource(R.string.no_dhikr_selected),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )

                            if (!currentDhikr?.arabicText.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = currentDhikr?.arabicText ?: "",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 24.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    AnimatedVisibility(
                        visible = uiState.isTargetReached,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.target_reached_badge),
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // ==========================================
                // 2. MARKAZIY QISM: Counter Doirasi (currentCount / target)
                // "Jami" counter ichidan chiqarilgan, maksimal toza va sokin
                // ==========================================
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(280.dp)
                ) {
                    // Aylana progress indikator (agar targetCount > 0 bo'lsa)
                    if (currentDhikr != null && currentDhikr.targetCount > 0) {
                        CircularProgressIndicator(
                            progress = { uiState.progress },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 10.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            color = MaterialTheme.colorScheme.primary,
                            strokeCap = StrokeCap.Round
                        )
                    }

                    // Bosiluvchi hisoblagich doirasi (subtle scale animatsiyasi bilan)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(240.dp)
                            .graphicsLayer {
                                scaleX = counterScale
                                scaleY = counterScale
                            }
                            .shadow(16.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
                                    )
                                )
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onCounterClick
                            )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Asosiy sanoq: katta va aniq
                            Text(
                                text = "${currentDhikr?.currentCount ?: 0}",
                                fontSize = 72.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )

                            // Maqsad ko'rsatkichi: "currentCount / target" formatida
                            if (currentDhikr != null && currentDhikr.targetCount > 0) {
                                Text(
                                    text = "/ ${currentDhikr.targetCount}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            } else {
                                Text(
                                    text = "/ ∞",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.65f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // 3. MA'LUMOTLAR BO'LIMI: Jami sanoq, Vaqt va Ritm (bitta uyg'un kartada)
                // ==========================================
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Yuqori qator: Jami sanoq va Jami vaqt (status nuqtasi bilan)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.total_count_label, currentDhikr?.totalCount ?: 0L),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (!uiState.isTimingPaused) {
                                                Color(0xFF4CAF50) // Faol (yashil)
                                            } else {
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.6f) // Pauza (kulrang)
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${stringResource(R.string.timing_total_time_label)} ${uiState.formattedTotalTime}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (uiState.isTimingPaused && (currentDhikr?.totalActiveTimeMillis ?: 0L) > 0L) {
                                    Text(
                                        text = " ${stringResource(R.string.timing_paused_label)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // Quyi qator: Ritm va "Ritmni qayta o'rganish" tugmasi
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.timing_rhythm_label),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = uiState.formattedRhythm,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Surface(
                                onClick = onRelearnRhythmClick,
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.timing_relearn_rhythm_button),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.timing_relearn_rhythm_button),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 4. PASTKI BOSHQARUV: MAQSAD va Nolga tushirish (Secondary Actions)
                // Counter TAP zonasidan to'liq xavfsiz uzoqlikda
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // MAQSAD tugmasi (Secondary Action, oson topiluvchi, tasodifiy bosilmaydigan)
                    OutlinedButton(
                        onClick = onEditTargetClick,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.cd_edit_target),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentDhikr != null && currentDhikr.targetCount > 0) {
                                stringResource(R.string.target_label, currentDhikr.targetCount)
                            } else {
                                stringResource(R.string.free_dhikr_label)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Nolga tushirish tugmasi (Subtle Danger OutlinedButton)
                    OutlinedButton(
                        onClick = onResetClick,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, resetBorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = resetTextColor
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.cd_reset),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.reset_button_label),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
