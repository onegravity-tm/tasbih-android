package com.tasbih.app

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasbih.app.ui.TasbihUiState
import com.tasbih.app.ui.TasbihViewModel
import com.tasbih.app.ui.components.AddDhikrDialog
import com.tasbih.app.ui.components.DhikrSelectionSheet
import com.tasbih.app.ui.components.SettingsSheet
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
                    onOpenDhikrSheet = { viewModel.setDhikrSheetOpen(true) },
                    onOpenSettingsSheet = { viewModel.setSettingsSheetOpen(true) }
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
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val isVolumeControlEnabled = viewModel.uiState.value.settings.isVolumeButtonsEnabled
            if (isVolumeControlEnabled &&
                (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
            ) {
                viewModel.onCounterClick()
                return true // Tizim ovozini o'zgartirmaslik uchun eventni o'zlashtiramiz
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
    onOpenDhikrSheet: () -> Unit,
    onOpenSettingsSheet: () -> Unit
) {
    val currentDhikr = uiState.currentDhikr

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tasbih",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onOpenDhikrSheet) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Zikrlar ro'yxati"
                        )
                    }
                    IconButton(onClick = onOpenSettingsSheet) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Sozlamalar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Yuqori ma'lumot qismi (Zikr nomi va arabcha yozuvi)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 8.dp)
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
                                text = currentDhikr?.name ?: "Zikr tanlanmagan",
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

                    Spacer(modifier = Modifier.height(12.dp))

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
                                text = "Maqsadga erishildi! 🎉",
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Markaziy doira va progress indikatori
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

                    // Bosiluvchi hisoblagich doirasi
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(240.dp)
                            .shadow(16.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
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
                            Text(
                                text = "${currentDhikr?.currentCount ?: 0}",
                                fontSize = 72.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )

                            Text(
                                text = if (currentDhikr != null && currentDhikr.targetCount > 0) {
                                    "Maqsad: ${currentDhikr.targetCount}"
                                } else {
                                    "Erkin zikr"
                                },
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                text = "Jami: ${currentDhikr?.totalCount ?: 0}",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Pastki qism: Nolga tushirish tugmasi
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = onResetClick,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Nolga tushirish",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Nolga tushirish",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
