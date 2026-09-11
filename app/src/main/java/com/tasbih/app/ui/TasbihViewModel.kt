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
import com.tasbih.app.data.timing.DhikrTimingManager
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

    // Sheet va Dialoglar holati (isDhikr, isSettings, isAdd, isEditTarget, isCalibrationDialog)
    private val _dialogState = MutableStateFlow(
        DialogState(
            isDhikrSheetOpen = false,
            isSettingsSheetOpen = false,
            isAddDhikrDialogOpen = false,
            isEditTargetDialogOpen = false,
            isCalibrationDialogOpen = false
        )
    )

    // PERFORMANCE FIX: In-Memory tezkor hisob xotirasi (0ms UI latency uchun)
    // Map<DhikrId, Triple<currentCount, totalCount, activeTimeMillis>>
    private val _inMemoryCounts = MutableStateFlow<Map<String, Triple<Int, Long, Long>>>(emptyMap())

    // Har bir zikr uchun ALOHIDA mustaqil TimingManager
    private val timingManagers = mutableMapOf<String, DhikrTimingManager>()
    private val _isTimingPaused = MutableStateFlow(true)
    private var pauseWatcherJob: Job? = null
    private var tapCounterSinceLastFlush = 0

    // Diskka yozishni (DataStore I/O) asinxron kechiktirib (debounced) bajarish
    private var debounceSaveJob: Job? = null
    private var pendingSaveDhikrId: String? = null
    private var pendingSaveCount: Int = 0
    private var pendingSaveTotal: Long = 0L
    private var pendingSaveActiveTime: Long = 0L

    val uiState: StateFlow<TasbihUiState> = combine(
        repository.dhikrListFlow,
        repository.selectedDhikrIdFlow,
        repository.settingsFlow,
        _inMemoryCounts,
        _dialogState,
        _isTimingPaused
    ) { args: Array<Any> ->
        @Suppress("UNCHECKED_CAST")
        val repoDhikrs = args[0] as List<DhikrItem>
        val selectedId = args[1] as String
        val settings = args[2] as AppSettings
        @Suppress("UNCHECKED_CAST")
        val memoryCounts = args[3] as Map<String, Triple<Int, Long, Long>>
        val dialogs = args[4] as DialogState
        val isPaused = args[5] as Boolean

        // Repozitoriya ma'lumotlarini in-memory eng yangi hisob bilan birlashtiramiz
        val mergedDhikrs = repoDhikrs.map { item ->
            val mem = memoryCounts[item.id]
            if (mem != null) {
                item.copy(
                    currentCount = mem.first,
                    totalCount = mem.second,
                    totalActiveTimeMillis = mem.third
                )
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
            isEditTargetDialogOpen = dialogs.isEditTargetDialogOpen,
            isCalibrationDialogOpen = dialogs.isCalibrationDialogOpen,
            isTimingPaused = isPaused
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

    private fun getOrCreateTimingManager(dhikr: DhikrItem): DhikrTimingManager {
        return timingManagers.getOrPut(dhikr.id) {
            DhikrTimingManager(
                dhikrId = dhikr.id,
                initialIsCalibrated = dhikr.isCalibrated,
                initialNormalIntervalMs = dhikr.normalIntervalMs
            )
        }
    }

    /**
     * Counter +1 (Ekranni bosganda yoki Volume Up orqali)
     */
    fun onCounterClick() {
        val current = uiState.value.currentDhikr ?: return

        // 1. Agar birinchi marta bo'lsa va kalibratsiyalanmagan bo'lsa, ogohlantirish dialogini ko'rsatish
        if (!current.isCalibrated && current.totalCount == 0L && !_dialogState.value.isCalibrationDialogOpen) {
            _dialogState.value = _dialogState.value.copy(isCalibrationDialogOpen = true)
        }

        val newCount = current.currentCount + 1
        val newTotal = current.totalCount + 1

        // 2. Ushbu zikrga tegishli alohida timing menejer orqali hisoblash
        val manager = getOrCreateTimingManager(current)
        val (deltaMs, newlyCalibrated) = manager.onDhikrTap()
        val newActiveTime = current.totalActiveTimeMillis + deltaMs
        _isTimingPaused.value = false

        if (newlyCalibrated) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.updateDhikrCalibration(current.id, true, manager.normalIntervalMs)
            }
        }

        // 3. In-Memory holatni darhol (0ms kechikishsiz) yangilaymiz
        updateMemoryCount(current.id, newCount, newTotal, newActiveTime)

        // 4. Taktil va ovozli aloqa
        val isTargetReached = current.targetCount > 0 && newCount == current.targetCount
        triggerHapticAndSound(uiState.value.settings, isTargetReached)

        // 5. Maqsad yakunlanganda zudlik bilan diskka commit qilish
        if (isTargetReached) {
            flushPendingSave()
        } else {
            // Xavfsiz periodic/debounced persistence
            tapCounterSinceLastFlush++
            if (tapCounterSinceLastFlush >= 15) {
                flushPendingSave()
                tapCounterSinceLastFlush = 0
            } else {
                scheduleDebouncedSave(current.id, newCount, newTotal, newActiveTime)
            }
        }

        // 6. Pauza monitoringi
        startPauseWatcher(current.id)
    }

    fun dismissCalibrationDialog() {
        _dialogState.value = _dialogState.value.copy(isCalibrationDialogOpen = false)
    }

    /**
     * Counter -1 (Volume Down orqali orqaga qaytarish, minimum 0)
     */
    fun onCounterDecrement() {
        val current = uiState.value.currentDhikr ?: return
        if (current.currentCount <= 0) return // 0 dan pastga tushmaydi

        val newCount = current.currentCount - 1
        val currentTotal = current.totalCount
        val currentActive = current.totalActiveTimeMillis

        updateMemoryCount(current.id, newCount, currentTotal, currentActive)
        triggerHaptic(VibrationLevel.LIGHT)
        scheduleDebouncedSave(current.id, newCount, currentTotal, currentActive)
    }

    /**
     * Nolga tushirish (Reset)
     */
    fun onResetClick() {
        val current = uiState.value.currentDhikr ?: return
        flushPendingSave()

        timingManagers[current.id]?.reset()
        _isTimingPaused.value = true
        pauseWatcherJob?.cancel()

        updateMemoryCount(current.id, 0, current.totalCount, 0L)
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
        // Oldingi zikrning barcha faol vaqt va sanog'ini darhol xavfsiz commit qilish
        flushPendingSave()
        timingManagers.values.forEach { it.pauseTiming() }
        _isTimingPaused.value = true
        pauseWatcherJob?.cancel()

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
        timingManagers.remove(id)
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

    fun onAppBackgrounded() {
        timingManagers.values.forEach { it.pauseTiming() }
        _isTimingPaused.value = true
        pauseWatcherJob?.cancel()
        flushPendingSave()
    }

    private fun startPauseWatcher(dhikrId: String) {
        pauseWatcherJob?.cancel()
        pauseWatcherJob = viewModelScope.launch {
            while (!_isTimingPaused.value) {
                delay(1000)
                val manager = timingManagers[dhikrId]
                if (manager != null && manager.isPaused()) {
                    _isTimingPaused.value = true
                    flushPendingSave()
                    break
                }
            }
        }
    }

    private fun updateMemoryCount(id: String, count: Int, total: Long, activeTime: Long) {
        val currentMap = _inMemoryCounts.value.toMutableMap()
        currentMap[id] = Triple(count, total, activeTime)
        _inMemoryCounts.value = currentMap
    }

    private fun scheduleDebouncedSave(id: String, count: Int, total: Long, activeTime: Long) {
        pendingSaveDhikrId = id
        pendingSaveCount = count
        pendingSaveTotal = total
        pendingSaveActiveTime = activeTime

        debounceSaveJob?.cancel()
        debounceSaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(400) // 400ms bosishlar oralig'ini kutish (fonda silliq saqlash)
            repository.saveDhikrCounts(id, count, total, System.currentTimeMillis(), activeTime)
            pendingSaveDhikrId = null
        }
    }

    private fun flushPendingSave() {
        val id = pendingSaveDhikrId ?: return
        debounceSaveJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveDhikrCounts(id, pendingSaveCount, pendingSaveTotal, System.currentTimeMillis(), pendingSaveActiveTime)
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
        val isEditTargetDialogOpen: Boolean,
        val isCalibrationDialogOpen: Boolean
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
