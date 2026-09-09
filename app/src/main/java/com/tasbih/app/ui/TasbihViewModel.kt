package com.tasbih.app.ui

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tasbih.app.data.model.AppSettings
import com.tasbih.app.data.model.VibrationLevel
import com.tasbih.app.data.repository.TasbihDataStoreRepository
import com.tasbih.app.data.repository.TasbihRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TasbihViewModel(
    application: Application,
    private val repository: TasbihRepository = TasbihDataStoreRepository(application.applicationContext)
) : AndroidViewModel(application) {

    private val _sheetState = MutableStateFlow(
        Triple(
            false, // isDhikrSheetOpen
            false, // isSettingsSheetOpen
            false  // isAddDhikrDialogOpen
        )
    )

    val uiState: StateFlow<TasbihUiState> = combine(
        repository.dhikrListFlow,
        repository.selectedDhikrIdFlow,
        repository.settingsFlow,
        _sheetState
    ) { dhikrs, selectedId, settings, sheetState ->
        val current = dhikrs.find { it.id == selectedId } ?: dhikrs.firstOrNull()
        val isTargetReached = current != null && current.targetCount > 0 && current.currentCount >= current.targetCount

        TasbihUiState(
            dhikrList = dhikrs,
            currentDhikr = current,
            settings = settings,
            isTargetReached = isTargetReached,
            isDhikrSheetOpen = sheetState.first,
            isSettingsSheetOpen = sheetState.second,
            isAddDhikrDialogOpen = sheetState.third
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TasbihUiState()
    )

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 60)
        } catch (_: Exception) {
            // Agar audio tizimi band bo'lsa xato bermaydi
        }
    }

    fun onCounterClick() {
        val state = uiState.value
        val current = state.currentDhikr ?: return

        triggerHapticAndSound(state.settings, isTargetReached = (current.targetCount > 0 && current.currentCount + 1 == current.targetCount))

        viewModelScope.launch {
            repository.incrementDhikr(current.id)
        }
    }

    fun onResetClick() {
        val current = uiState.value.currentDhikr ?: return
        triggerHaptic(VibrationLevel.LIGHT)
        viewModelScope.launch {
            repository.resetDhikr(current.id)
        }
    }

    fun selectDhikr(id: String) {
        viewModelScope.launch {
            repository.selectDhikr(id)
            setDhikrSheetOpen(false)
        }
    }

    fun addCustomDhikr(name: String, arabicText: String, targetCount: Int) {
        viewModelScope.launch {
            repository.addCustomDhikr(name, arabicText, targetCount)
            setAddDhikrDialogOpen(false)
            setDhikrSheetOpen(false)
        }
    }

    fun deleteCustomDhikr(id: String) {
        viewModelScope.launch {
            repository.deleteCustomDhikr(id)
        }
    }

    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch {
            repository.updateSettings(settings)
        }
    }

    fun setDhikrSheetOpen(isOpen: Boolean) {
        _sheetState.value = _sheetState.value.copy(first = isOpen)
    }

    fun setSettingsSheetOpen(isOpen: Boolean) {
        _sheetState.value = _sheetState.value.copy(second = isOpen)
    }

    fun setAddDhikrDialogOpen(isOpen: Boolean) {
        _sheetState.value = _sheetState.value.copy(third = isOpen)
    }

    private fun triggerHapticAndSound(settings: AppSettings, isTargetReached: Boolean) {
        if (isTargetReached) {
            // Maqsadga yetganda maxsus uzoqroq vibratsiya
            triggerHapticDuration(100)
            if (settings.isSoundEnabled) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
            }
        } else {
            triggerHaptic(settings.vibrationLevel)
            if (settings.isSoundEnabled) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
            }
        }
    }

    private fun triggerHaptic(level: VibrationLevel) {
        val durationMs = when (level) {
            VibrationLevel.OFF -> return
            VibrationLevel.LIGHT -> 25L
            VibrationLevel.MEDIUM -> 45L
            VibrationLevel.STRONG -> 75L
        }
        triggerHapticDuration(durationMs)
    }

    private fun triggerHapticDuration(durationMs: Long) {
        try {
            val context = getApplication<Application>().applicationContext
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {
            // Xatolik bermaydi
        }
    }

    override fun onCleared() {
        super.onCleared()
        toneGenerator?.release()
        toneGenerator = null
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TasbihViewModel(application) as T
                }
            }
    }
}
