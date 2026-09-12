package com.tasbih.app.data.repository

import com.tasbih.app.data.model.AppSettings
import com.tasbih.app.data.model.DhikrItem
import kotlinx.coroutines.flow.Flow

/**
 * Tasbih loyihasining asosiy ma'lumotlar ombori interfeysi (Repository Pattern).
 * UI va ViewModel qatlamlari ma'lumotlar saqlash manbasiga (DataStore yoki Room) bog'lanib qolmaydi.
 */
interface TasbihRepository {
    val dhikrListFlow: Flow<List<DhikrItem>>
    val selectedDhikrIdFlow: Flow<String>
    val settingsFlow: Flow<AppSettings>

    suspend fun selectDhikr(id: String)
    suspend fun incrementDhikr(id: String)
    suspend fun decrementDhikr(id: String)
    suspend fun resetDhikr(id: String)
    suspend fun updateDhikrTarget(id: String, targetCount: Int)
    suspend fun addCustomDhikr(name: String, arabicText: String, targetCount: Int)
    suspend fun deleteCustomDhikr(id: String)
    suspend fun updateSettings(settings: AppSettings)
    suspend fun saveDhikrCounts(id: String, currentCount: Int, totalCount: Long, lastTap: Long, activeTimeMillis: Long)
    suspend fun updateDhikrCalibration(id: String, isCalibrated: Boolean, normalIntervalMs: Long, learningPeriodStartMillis: Long = 0L)
}
