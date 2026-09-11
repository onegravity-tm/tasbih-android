package com.tasbih.app.ui

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.SystemClock
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
import com.tasbih.app.data.timing.TapClassification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
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
    // Map<DhikrId, Triple<currentCount, totalCount, activeTimeMillis>>
    private val _inMemoryCounts = MutableStateFlow<Map<String, Triple<Int, Long, Long>>>(emptyMap())

    // Zikr tanlanganda 0ms ichida UI va Timing uzilishini ta'minlovchi tezkor override
    private val _selectedDhikrIdOverride = MutableStateFlow<String?>(null)

    // Har bir zikr uchun ALOHIDA mustaqil TimingManager
    private val timingManagers = mutableMapOf<String, DhikrTimingManager>()
    private val _isTimingPaused = MutableStateFlow(true)
    private val _displayTimeMillis = MutableStateFlow(0L)

    // Active session timing parametrlari (monotonic clock asosida)
    private var activeSessionDhikrId: String? = null
    private var activeSessionStartUptime: Long = 0L
    private var lastTapUptime: Long = 0L
    private var sessionAccumulatedMs: Long = 0L
    private var tickerJob: Job? = null
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
        _selectedDhikrIdOverride,
        repository.settingsFlow,
        _inMemoryCounts,
        _dialogState,
        _isTimingPaused,
        _displayTimeMillis
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val repoDhikrs = args[0] as List<DhikrItem>
        val repoSelectedId = args[1] as String
        val overrideId = args[2] as? String
        val settings = args[3] as AppSettings
        @Suppress("UNCHECKED_CAST")
        val memoryCounts = args[4] as Map<String, Triple<Int, Long, Long>>
        val dialogs = args[5] as DialogState
        val isPaused = args[6] as Boolean
        val displayTime = args[7] as Long

        val selectedId = overrideId ?: repoSelectedId

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
            isTimingPaused = isPaused,
            displayTimeMillis = displayTime
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
        startTicker()
    }

    private fun getOrCreateTimingManager(dhikr: DhikrItem): DhikrTimingManager {
        val existing = timingManagers[dhikr.id]
        if (existing != null) {
            if (dhikr.isCalibrated && !existing.isCalibrated) {
                existing.syncFromSavedState(dhikr.isCalibrated, dhikr.normalIntervalMs)
            }
            return existing
        }
        val newManager = DhikrTimingManager(
            dhikrId = dhikr.id,
            initialIsCalibrated = dhikr.isCalibrated,
            initialNormalIntervalMs = dhikr.normalIntervalMs
        )
        timingManagers[dhikr.id] = newManager
        return newManager
    }

    /**
     * Real-time Ticker Coroutine:
     * - Har 100ms da ishlaydi.
     * - ACTIVE paytida vaqt silliq va bir maromda oshib boradi.
     * - PAUSE paytida timer oxirgi tasdiqlangan vaqtda muzlab (freeze bo'lib) turadi.
     */
    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(100)
                val current = uiState.value.currentDhikr
                if (current == null) continue

                val currentId = current.id
                val committedBase = current.totalActiveTimeMillis

                if (!_isTimingPaused.value && activeSessionDhikrId == currentId && lastTapUptime > 0L) {
                    val now = SystemClock.elapsedRealtime()
                    val timeSinceLastTap = now - lastTapUptime
                    val manager = getOrCreateTimingManager(current)
                    val pauseLimit = manager.getPauseLimit()

                    if (timeSinceLastTap > pauseLimit) {
                        // PAUZA aniqlandi: oxirgi TAP gacha bo'lgan vaqtda timer freeze bo'ladi
                        commitPause(currentId)
                    } else {
                        // ACTIVE holat: vaqt silliq oshadi
                        val activeElapsed = sessionAccumulatedMs + (now - activeSessionStartUptime)
                        _displayTimeMillis.value = committedBase + activeElapsed
                    }
                } else {
                    // PAUSE holat: timer committed vaqtda to'xtab turadi
                    _displayTimeMillis.value = committedBase
                }
            }
        }
    }

    /**
     * Pauza yuz berganda faol sessiyani oxirgi tasdiqlangan TAP vaqtida to'xtatish (freeze).
     */
    private fun commitPause(dhikrId: String) {
        _isTimingPaused.value = true
        if (activeSessionStartUptime > 0L && lastTapUptime >= activeSessionStartUptime) {
            val sessionDuration = lastTapUptime - activeSessionStartUptime
            val current = uiState.value.currentDhikr
            if (current != null && current.id == dhikrId) {
                val newActiveTime = current.totalActiveTimeMillis + sessionAccumulatedMs + sessionDuration
                updateMemoryCount(dhikrId, current.currentCount, current.totalCount, newActiveTime)
                _displayTimeMillis.value = newActiveTime
                scheduleDebouncedSave(dhikrId, current.currentCount, current.totalCount, newActiveTime)
            }
        }
        val manager = timingManagers[dhikrId]
        if (manager != null && manager.isCalibrated) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.updateDhikrCalibration(dhikrId, true, manager.normalIntervalMs)
            }
        }
        activeSessionStartUptime = 0L
        lastTapUptime = 0L
        sessionAccumulatedMs = 0L
        activeSessionDhikrId = null
        flushPendingSave()
    }

    /**
     * Faol sessiya vaqtini saqlash uchun bazaga commit qilish.
     */
    private fun commitSessionTime(dhikrId: String) {
        if (activeSessionStartUptime > 0L && lastTapUptime >= activeSessionStartUptime) {
            val sessionDuration = lastTapUptime - activeSessionStartUptime
            val current = uiState.value.currentDhikr
            if (current != null && current.id == dhikrId) {
                val newActiveTime = current.totalActiveTimeMillis + sessionAccumulatedMs + sessionDuration
                updateMemoryCount(dhikrId, current.currentCount, current.totalCount, newActiveTime)
                activeSessionStartUptime = lastTapUptime
                sessionAccumulatedMs = 0L
            }
        }
        val manager = timingManagers[dhikrId]
        if (manager != null && manager.isCalibrated) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.updateDhikrCalibration(dhikrId, true, manager.normalIntervalMs)
            }
        }
    }

    /**
     * Counter +1 (Ekranni bosganda yoki Volume Up orqali)
     */
    fun onCounterClick() {
        val current = uiState.value.currentDhikr ?: return
        val now = SystemClock.elapsedRealtime()
        val manager = getOrCreateTimingManager(current)

        val newCount = current.currentCount + 1
        val newTotal = current.totalCount + 1

        if (_isTimingPaused.value || activeSessionDhikrId != current.id || lastTapUptime <= 0L) {
            // RESUME yoki BIRINCHI TAP:
            activeSessionDhikrId = current.id
            activeSessionStartUptime = now
            lastTapUptime = now
            sessionAccumulatedMs = 0L
            _isTimingPaused.value = false
        } else {
            // Davom etayotgan sessiya: intervalni klassifikatsiya qilish
            val interval = now - lastTapUptime
            val (classification, newlyCalibrated) = manager.classifyAndRecordTap(interval)

            when (classification) {
                TapClassification.ACTIVE_NORMAL, TapClassification.ACTIVE_BORDERLINE -> {
                    lastTapUptime = now
                }
                TapClassification.PAUSE -> {
                    // Kutilmagan pauza: oldingi sessiyani freeze qilamiz va yangisini boshlaymiz
                    commitPause(current.id)
                    activeSessionDhikrId = current.id
                    activeSessionStartUptime = now
                    lastTapUptime = now
                    sessionAccumulatedMs = 0L
                    _isTimingPaused.value = false
                }
            }

            if (newlyCalibrated) {
                viewModelScope.launch(Dispatchers.IO) {
                    repository.updateDhikrCalibration(current.id, true, manager.normalIntervalMs)
                }
            }
        }

        // In-Memory holatni darhol yangilaymiz (0ms UI latency)
        updateMemoryCount(current.id, newCount, newTotal, current.totalActiveTimeMillis)

        // Taktil va ovozli aloqa
        val isTargetReached = current.targetCount > 0 && newCount == current.targetCount
        triggerHapticAndSound(uiState.value.settings, isTargetReached)

        // Maqsad yakunlanganda zudlik bilan diskka commit qilish
        if (isTargetReached) {
            commitSessionTime(current.id)
            flushPendingSave()
        } else {
            // Xavfsiz periodic/debounced persistence
            tapCounterSinceLastFlush++
            if (tapCounterSinceLastFlush >= 15) {
                commitSessionTime(current.id)
                flushPendingSave()
                tapCounterSinceLastFlush = 0
            } else {
                scheduleDebouncedSave(current.id, newCount, newTotal, current.totalActiveTimeMillis)
            }
        }
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
        activeSessionDhikrId = null
        activeSessionStartUptime = 0L
        lastTapUptime = 0L
        sessionAccumulatedMs = 0L

        updateMemoryCount(current.id, 0, current.totalCount, 0L)
        _displayTimeMillis.value = 0L
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
        val oldId = activeSessionDhikrId
        if (oldId != null) {
            commitPause(oldId)
        }
        flushPendingSave()

        // 0ms ichida UI va timing state yangi zikrga o'tadi
        _selectedDhikrIdOverride.value = id
        _isTimingPaused.value = true
        activeSessionDhikrId = null
        activeSessionStartUptime = 0L
        lastTapUptime = 0L
        sessionAccumulatedMs = 0L

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
        val currentId = activeSessionDhikrId
        if (currentId != null) {
            commitPause(currentId)
        }
        _isTimingPaused.value = true
        activeSessionDhikrId = null
        activeSessionStartUptime = 0L
        lastTapUptime = 0L
        sessionAccumulatedMs = 0L
        flushPendingSave()
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
        tickerJob?.cancel()
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

