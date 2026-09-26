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
import com.tasbih.app.data.model.AppSettings
import com.tasbih.app.data.model.DhikrItem
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {

    private val viewModel: TasbihViewModel by viewModels {
        TasbihViewModel.provideFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TasbihTheme {
                // =========================================================================
                // PERFORMANCE FIX: 100ms Ticker Recomposition Isolation
                // uiState ichidagi maydonlar distinctUntilChanged() bilan ajratilgan.
                // 100ms ticker faqat formattedTotalTimeFlow'ga ta'sir qiladi,
                // asosiy ekran, medallion va sheetlar ticker sababli keraksiz qayta
                // compose BO'LMAYDI!
                // =========================================================================

                val currentDhikr by remember(viewModel) {
                    viewModel.uiState.map { it.currentDhikr }.distinctUntilChanged()
                }.collectAsStateWithLifecycle(initialValue = null)

                val settings by remember(viewModel) {
                    viewModel.uiState.map { it.settings }.distinctUntilChanged()
                }.collectAsStateWithLifecycle(initialValue = AppSettings())

                val isTargetReached by remember(viewModel) {
                    viewModel.uiState.map { it.isTargetReached }.distinctUntilChanged()
                }.collectAsStateWithLifecycle(initialValue = false)

                val isTimingPaused by remember(viewModel) {
                    viewModel.uiState.map { it.isTimingPaused }.distinctUntilChanged()
                }.collectAsStateWithLifecycle(initialValue = true)

                val progress by remember(viewModel) {
                    viewModel.uiState.map { it.progress }.distinctUntilChanged()
                }.collectAsStateWithLifecycle(initialValue = 0f)

                val formattedRhythm by remember(viewModel) {
                    viewModel.uiState.map { it.formattedRhythm }.distinctUntilChanged()
                }.collectAsStateWithLifecycle(initialValue = "")

                val dhikrList by remember(viewModel) {
                    viewModel.uiState.map { it.dhikrList }.distinctUntilChanged()
                }.collectAsStateWithLifecycle(initialValue = emptyList())

                val isDhikrSheetOpenState by remember(viewModel) {
                    viewModel.uiState.map { it.isDhikrSheetOpen }.distinctUntilChanged()
                }.collectAsStateWithLifecycle(initialValue = false)

                val isSettingsSheetOpenState by remember(viewModel) {
                    viewModel.uiState.map { it.isSettingsSheetOpen }.distinctUntilChanged()
                }.collectAsStateWithLifecycle(initialValue = false)

                val isAddDhikrDialogOpen by remember(viewModel) {
                    viewModel.uiState.map { it.isAddDhikrDialogOpen }.distinctUntilChanged()
                }.collectAsStateWithLifecycle(initialValue = false)

                val isEditTargetDialogOpen by remember(viewModel) {
                    viewModel.uiState.map { it.isEditTargetDialogOpen }.distinctUntilChanged()
                }.collectAsStateWithLifecycle(initialValue = false)

                // Sekundiga ko'pi bilan 1 marta o'zgaruvchi vaqt oqimi
                val formattedTotalTimeFlow = remember(viewModel) {
                    viewModel.uiState.map { it.formattedTotalTime }.distinctUntilChanged()
                }


                // Ekranni doim yoqiq tutish sozlamasi
                LaunchedEffect(settings.isKeepScreenOn) {
                    if (settings.isKeepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                TasbihApp(
                    currentDhikr = currentDhikr,
                    progress = progress,
                    isTargetReached = isTargetReached,
                    isFullScreenTapEnabled = settings.isFullScreenTapEnabled,
                    isTimingPaused = isTimingPaused,
                    formattedRhythm = formattedRhythm,
                    formattedTotalTimeFlow = formattedTotalTimeFlow,
                    onCounterClick = viewModel::onCounterClick,
                    onResetClick = viewModel::onResetClick,
                    onRelearnRhythmClick = viewModel::onRelearnRhythmClick,
                    onOpenDhikrSheet = { viewModel.setDhikrSheetOpen(true) },
                    onOpenSettingsSheet = { viewModel.setSettingsSheetOpen(true) },
                    onEditTargetClick = { viewModel.setEditTargetDialogOpen(true) }
                )

                // Zikrlar ro'yxati sheet
                if (isDhikrSheetOpenState) {
                    DhikrSelectionSheet(
                        dhikrList = dhikrList,
                        selectedId = currentDhikr?.id,
                        onSelect = viewModel::selectDhikr,
                        onAddNewClick = { viewModel.setAddDhikrDialogOpen(true) },
                        onDeleteCustom = viewModel::deleteCustomDhikr,
                        onDismiss = {
                            viewModel.setDhikrSheetOpen(false)
                        }
                    )
                }

                // Sozlamalar sheet
                if (isSettingsSheetOpenState) {
                    SettingsSheet(
                        settings = settings,
                        onSettingsChanged = viewModel::updateSettings,
                        onTestVibration = viewModel::testVibration,
                        onDismiss = {
                            viewModel.setSettingsSheetOpen(false)
                        }
                    )
                }

                // Yangi zikr qo'shish dialogi
                if (isAddDhikrDialogOpen) {
                    AddDhikrDialog(
                        onDismiss = { viewModel.setAddDhikrDialogOpen(false) },
                        onConfirm = viewModel::addCustomDhikr
                    )
                }

                // Maqsadni tahrirlash dialogi
                if (isEditTargetDialogOpen && currentDhikr != null) {
                    EditTargetDialog(
                        dhikrName = currentDhikr!!.name,
                        currentTarget = currentDhikr!!.targetCount,
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
                        return true
                    }
                    KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        viewModel.onCounterDecrement()
                        return true
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
    currentDhikr: DhikrItem?,
    progress: Float,
    isTargetReached: Boolean,
    isFullScreenTapEnabled: Boolean,
    isTimingPaused: Boolean,
    formattedRhythm: String,
    formattedTotalTimeFlow: Flow<String>,
    onCounterClick: () -> Unit,
    onResetClick: () -> Unit,
    onRelearnRhythmClick: () -> Unit,
    onOpenDhikrSheet: () -> Unit,
    onOpenSettingsSheet: () -> Unit,
    onEditTargetClick: () -> Unit
) {
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

    // =========================================================================
    // ALLOCATION OPTIMIZATION: Og'ir chizma obyektlari remember qilinadi
    // =========================================================================
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant
    val medallionBrush = remember(primaryColor) {
        Brush.radialGradient(
            colors = listOf(
                primaryColor,
                primaryColor.copy(alpha = 0.90f)
            )
        )
    }
    val dhikrCardBorder = remember(outlineVariantColor) {
        BorderStroke(1.dp, outlineVariantColor.copy(alpha = 0.4f))
    }
    val statsCardBorder = remember(outlineVariantColor) {
        BorderStroke(1.dp, outlineVariantColor.copy(alpha = 0.3f))
    }
    val resetBtnBorder = remember(resetBorderColor) {
        BorderStroke(1.dp, resetBorderColor)
    }
    val outerRimBorder = remember(primaryColor) {
        BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.2f))
    }
    val medallionBorder = remember(primaryColor) {
        BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.25f))
    }
    val innerRingBorder = remember {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    }

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
                    if (isFullScreenTapEnabled) {
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
                        border = dhikrCardBorder,
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
                        visible = isTargetReached,
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
                            progress = { progress },
                            modifier = Modifier.size(256.dp),
                            strokeWidth = 5.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            color = primaryColor,
                            strokeCap = StrokeCap.Round
                        )
                    } else {
                        // Erkin zikr holatida sokin, nozik tashqi halqa
                        Box(
                            modifier = Modifier
                                .size(256.dp)
                                .border(outerRimBorder, CircleShape)
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
                            .shadow(12.dp, CircleShape, spotColor = primaryColor.copy(alpha = 0.35f))
                            .clip(CircleShape)
                            .border(medallionBorder, CircleShape)
                            .background(brush = medallionBrush)
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
                                .border(innerRingBorder, CircleShape)
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
                    border = statsCardBorder,
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

                        // 2. SECONDARY: Faol vaqt (Faqat ActiveTimeText mustaqil yangilanadi)
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
                                            if (!isTimingPaused) Color(0xFF4CAF50)
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                        )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isTimingPaused && (currentDhikr?.totalActiveTimeMillis ?: 0L) > 0L) "VAQT (PAUZA)" else "FAOL VAQT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.4.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            ActiveTimeText(formattedTotalTimeFlow)
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
                                        tint = primaryColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (currentDhikr?.isCalibrated == true) formattedRhythm else "O‘rganilmoqda",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryColor
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
                        border = resetBtnBorder,
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

/**
 * RECOMPOSITION ISOLATION: Faqat vaqt o'zgarganda qayta chiziluvchi eng quyi composable.
 * Tickerning vaqt yangilanishi butun TasbihApp yoki Scaffolding'ni qayta compose qilmaydi.
 */
@Composable
private fun ActiveTimeText(
    formattedTotalTimeFlow: Flow<String>,
    modifier: Modifier = Modifier
) {
    val formattedTime by formattedTotalTimeFlow.collectAsStateWithLifecycle(initialValue = "0s")
    Text(
        text = formattedTime,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}
