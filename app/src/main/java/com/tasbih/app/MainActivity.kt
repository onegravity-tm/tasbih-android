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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
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

    // Counter TAP scale animatsiyasi: 1.0f -> 0.97f -> 1.0f (~80ms spring-like, 0ms click latency)
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
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ==========================================================
                // 1. YUQORI QISM: Nafis va Ixcham Zikr Kartasi + Target Badge
                // ==========================================================
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onOpenDhikrSheet)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentDhikr?.name ?: stringResource(R.string.no_dhikr_selected),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = stringResource(R.string.cd_dhikr_list),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (!currentDhikr?.arabicText.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentDhikr?.arabicText ?: "",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 22.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

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
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // ==========================================================
                // 2. MARKAZIY QISM: Haqiqiy Tasbih Medallion (Dominant & Sokin)
                // - Bir-biriga kiritilgan nafis progress rim va sokin tactile sirt
                // - Qalin uzilgan halqa o'rniga yaxlit ko'rinish
                // ==========================================================
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(260.dp)
                ) {
                    // Yaxlit Progress Rim (Agar target > 0 bo'lsa)
                    if (currentDhikr != null && currentDhikr.targetCount > 0) {
                        CircularProgressIndicator(
                            progress = { uiState.progress },
                            modifier = Modifier.size(256.dp),
                            strokeWidth = 5.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            color = MaterialTheme.colorScheme.primary,
                            strokeCap = StrokeCap.Round
                        )
                    } else {
                        // Erkin zikr holatida sokin, nozik tashqi halqa
                        Box(
                            modifier = Modifier
                                .size(256.dp)
                                .border(
                                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    CircleShape
                                )
                        )
                    }

                    // Asosiy bosiluvchi Tasbih sirti (Scale animatsiyasi + yumshoq depth)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(242.dp)
                            .graphicsLayer {
                                scaleX = counterScale
                                scaleY = counterScale
                            }
                            .shadow(12.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                            .clip(CircleShape)
                            .border(
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                CircleShape
                            )
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.90f)
                                    )
                                )
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onCounterClick
                            )
                    ) {
                        // Tasbih medaloni ichki konsentrik dekorativ halqasi (Craftsmanship)
                        Box(
                            modifier = Modifier
                                .size(224.dp)
                                .border(
                                    BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                                    CircleShape
                                )
                        )

                        // Raqamlar va Target ko'rsatkichi
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${currentDhikr?.currentCount ?: 0}",
                                fontSize = 68.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            // Maqsad ko'rsatkichi (Nafis ichki pill)
                            Surface(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = if (currentDhikr != null && currentDhikr.targetCount > 0) {
                                        "/ ${currentDhikr.targetCount}"
                                    } else {
                                        "/ ∞"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.90f),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // ==========================================================
                // 3. MA'LUMOTLAR BO'LIMI: 3 Ustunli Aniq Ierarxiya
                // Primary: Jami hisob | Secondary: Faol vaqt | Tertiary: Ritm
                // ==========================================================
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. PRIMARY: Jami hisob
                        Column(
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "JAMI HISOB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${currentDhikr?.totalCount ?: 0L}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Ajratuvchi chiziq
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(30.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        )

                        // 2. SECONDARY: Faol vaqt
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (!uiState.isTimingPaused) Color(0xFF4CAF50)
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                        )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.isTimingPaused && (currentDhikr?.totalActiveTimeMillis ?: 0L) > 0L) "VAQT (PAUZA)" else "FAOL VAQT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.4.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uiState.formattedTotalTime,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Ajratuvchi chiziq
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(30.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        )

                        // 3. TERTIARY: Ritm
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1.1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "RITM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                IconButton(
                                    onClick = onRelearnRhythmClick,
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.timing_relearn_rhythm_button),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (currentDhikr?.isCalibrated == true) uiState.formattedRhythm else "O‘rganilmoqda",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // ==========================================================
                // 4. PASTKI BOSHQARUV: MAQSAD va Nolga Tushirish
                // - Matnlar sinmaydi (Single-line guaranteed)
                // - Counter TAP zonasidan to'liq xavfsiz uzoqlikda
                // ==========================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // MAQSAD tugmasi (Secondary action)
                    OutlinedButton(
                        onClick = onEditTargetClick,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.cd_edit_target),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (currentDhikr != null && currentDhikr.targetCount > 0) {
                                stringResource(R.string.target_label, currentDhikr.targetCount)
                            } else {
                                stringResource(R.string.free_dhikr_label)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Nolga tushirish tugmasi (Subtle Danger - Single line guaranteed!)
                    OutlinedButton(
                        onClick = onResetClick,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, resetBorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = resetTextColor
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.cd_reset),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.reset_button_label),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
        }
    }
}
