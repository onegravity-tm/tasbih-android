package com.tasbih.app.data.model

/**
 * Zikr ma'lumotlar modeli.
 * 
 * Kelajakdagi Timing, History va Room migratsiyasi uchun to'liq moslashtirilgan.
 * [totalActiveTimeMillis] va [lastActiveTimestamp] kelajakdagi Timing funksiyasida
 * taplar orasidagi interval va pause vaqtlarini tahlil qilish uchun hozirdanoq ajratilgan.
 */
data class DhikrItem(
    val id: String,
    val name: String,
    val arabicText: String = "",
    val targetCount: Int = 33, // 0 = cheksiz (erkin zikr)
    val currentCount: Int = 0,
    val totalCount: Long = 0L,
    val isCustom: Boolean = false,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val totalActiveTimeMillis: Long = 0L,
    val lastActiveTimestamp: Long = 0L,
    val isCalibrated: Boolean = false,
    val normalIntervalMs: Long = 0L
)
