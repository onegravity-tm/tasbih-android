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
import com.tasbih.app.data.model.DhikrItem
import com.tasbih.app.data.model.VibrationLevel
import com.tasbih.app.data.repository.TasbihDataStoreRepository
import com.tasbih.app.data.repository.TasbihRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    // Sheet va Dialoglar holati (isDhikr, isSettings, isAdd, isEditTarget)
    private val _dialogState = MutableStateFlow(
        DialogState(
            isDhikrSheetOpen = false,
            isSettingsSheetOpen = false,
            isAddDhikrDialogOpen = false,
            isEditTargetDialogOpen = false
        )
    )

    // PERFORMANCE FIX: In-Memory tezkor hisob xotirasi (0ms UI latency uchun)
    // Map<DhikrId, Pair<currentCount, totalCount>>
    private val _inMemoryCounts = MutableStateFlow<Map<String, Pair<Int, Long>>>(emptyMap())

    // Diskka yozishni (DataStore I/O) asinxron kechiktirib (debounced) bajarish
    private var debounceSaveJob: Job? = null
    private var pendingSaveDhikrId: String? = null
    private var pendingSaveCount: Int = 0
    private var pendingSaveTotal: Long = 0L

    val uiState: StateFlow<TasbihUiState> = combine(
        repository.dhikrListFlow,
        repository.selectedDhikrIdFlow,
        repository.settingsFlow,
        _inMemoryCounts,
        _dialogState
    ) { repoDhikrs, selectedId, settings, memoryCounts, dialogs ->
        // Repozitoriya ma'lumotlarini in-memory eng yangi hisob bilan birlashtiramiz
        val mergedDhikrs = repoDhikrs.map { item ->
            val mem = memoryCounts[item.id]
            if (mem != null) {
                item.copy(currentCount = mem.first, totalCount = mem.second)
            } else {
                item
            }
        }

        val current = mergedDhikrs.find { it.id == selectedId } ?: mergedDhikrs.firstOrNull()
        val isTargetReached = current != null && current.targetCount > 0 && current.currentCount >= current.targetCount

        TasbihUiState(
            dhikrList = mergedDhikrs,
            currentDhikr = current,
            settings = settings,
            isTargetReached = isTargetReached,
            isDhikrSheetOpen = dialogs.isDhikrSheetOpen,
            isSettingsSheetOpen = dialogs.isSettingsSheetOpen,
            isAddDhikrDialogOpen = dialogs.isAddDhikrDialogOpen,
            isEditTargetDialogOpen = dialogs.isEditTargetDialogOpen
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TasbihUiState()
    )

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 50)
        } catch (_: Exception) {
            // Audio tizimi band bo'lsa xato bermaydi
        }
    }

    /**
     * Counter +1 (Ekranni bosganda yoki Volume Up orqali)
     */
    fun onCounterClick() {
        val current = uiState.value.currentDhikr ?: return
        val newCount = current.currentCount + 1
        val newTotal = current.totalCount + 1

        // 1. In-Memory holatni darhol (0ms kechikishsiz) yangilaymiz
        updateMemoryCount(current.id, newCount, newTotal)

        // 2. Taktil va ovozli aloqa (UI threadni to'xtatmaslik uchun)
        val isTargetReached = current.targetCount > 0 && newCount == current.targetCount
        triggerHapticAndSound(uiState.value.settings, isTargetReached)

        // 3. Diskka (DataStore'ga) asinxron debounced yozish
        scheduleDebouncedSave(current.id, newCount, newTotal)
    }

    /**
     * Counter -1 (Volume Down orqali orqaga qaytarish, minimum 0)
     */
    fun onCounterDecrement() {
        val current = uiState.value.currentDhikr ?: return
        if (current.currentCount <= 0) return // 0 dan pastga tushmaydi

        val newCount = current.currentCount - 1
        // Adashganda orqaga qaytarish totalCount'ni kamaytirmaydi, currentCount'ni to'g'irlaydi
        val currentTotal = current.totalCount

        updateMemoryCount(current.id, newCount, currentTotal)
        triggerHaptic(VibrationLevel.LIGHT)
        scheduleDebouncedSave(current.id, newCount, currentTotal)
    }

    /**
     * Nolga tushirish (Reset)
     */
    fun onResetClick() {
        val current = uiState.value.currentDhikr ?: return
        flushPendingSave()

        updateMemoryCount(current.id, 0, current.totalCount)
        triggerHaptic(VibrationLevel.LIGHT)

        viewModelScope.launch(Dispatchers.IO) {
            repository.resetDhikr(current.id)
        }
    }

    /**
     * Zikrning maqsadini (Target) tahrirlash (masalan, 33 -> 99 yoki 100)
     */
    fun updateCurrentDhikrTarget(newTarget: Int) {
        val current = uiState.value.currentDhikr ?: return
        setEditTargetDialogOpen(false)

        viewModelScope.launch(Dispatchers.IO) {
            repository.updateDhikrTarget(current.id, newTarget)
        }
    }

    fun selectDhikr(id: String) {
        flushPendingSave()
        viewModelScope.launch {
            repository.selectDhikr(id)
            setDhikrSheetOpen(false)
        }
    }

    fun addCustomDhikr(name: String, arabicText: String, targetCount: Int) {
        flushPendingSave()
        viewModelScope.launch {
            repository.addCustomDhikr(name, arabicText, targetCount)
            setAddDhikrDialogOpen(false)
            setDhikrSheetOpen(false)
        }
    }

    fun deleteCustomDhikr(id: String) {
        flushPendingSave()
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
        _dialogState.value = _dialogState.value.copy(isDhikrSheetOpen = isOpen)
    }

    fun setSettingsSheetOpen(isOpen: Boolean) {
        _dialogState.value = _dialogState.value.copy(isSettingsSheetOpen = isOpen)
    }

    fun setAddDhikrDialogOpen(isOpen: Boolean) {
        _dialogState.value = _dialogState.value.copy(isAddDhikrDialogOpen = isOpen)
    }

    fun setEditTargetDialogOpen(isOpen: Boolean) {
        _dialogState.value = _dialogState.value.copy(isEditTargetDialogOpen = isOpen)
    }

    private fun updateMemoryCount(id: String, count: Int, total: Long) {
        val currentMap = _inMemoryCounts.value.toMutableMap()
        currentMap[id] = Pair(count, total)
        _inMemoryCounts.value = currentMap
    }

    private fun scheduleDebouncedSave(id: String, count: Int, total: Long) {
        pendingSaveDhikrId = id
        pendingSaveCount = count
        pendingSaveTotal = total

        debounceSaveJob?.cancel()
        debounceSaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(400) // 400ms bosishlar oralig'ini kutish (fonda silliq saqlash)
            repository.saveDhikrCounts(id, count, total, System.currentTimeMillis())
            pendingSaveDhikrId = null
        }
    }

    private fun flushPendingSave() {
        val id = pendingSaveDhikrId ?: return
        debounceSaveJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveDhikrCounts(id, pendingSaveCount, pendingSaveTotal, System.currentTimeMillis())
            pendingSaveDhikrId = null
        }
    }

    private fun triggerHapticAndSound(settings: AppSettings, isTargetReached: Boolean) {
        if (isTargetReached) {
            triggerHapticDuration(120)
            if (settings.isSoundEnabled) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 100)
            }
        } else {
            triggerHaptic(settings.vibrationLevel)
            if (settings.isSoundEnabled) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 30)
            }
        }
    }

    private fun triggerHaptic(level: VibrationLevel) {
        val durationMs = when (level) {
            VibrationLevel.OFF -> return
            VibrationLevel.LIGHT -> 20L
            VibrationLevel.MEDIUM -> 40L
            VibrationLevel.STRONG -> 70L
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
        flushPendingSave()
        toneGenerator?.release()
        toneGenerator = null
    }

    private data class DialogState(
        val isDhikrSheetOpen: Boolean,
        val isSettingsSheetOpen: Boolean,
        val isAddDhikrDialogOpen: Boolean,
        val isEditTargetDialogOpen: Boolean
    )

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
