package com.tasbih.app.data.timing

import java.util.ArrayDeque

enum class TapClassification {
    ACTIVE_NORMAL,      // <= 1.5 * T_avg: faol, average'ga qo'shiladi
    ACTIVE_BORDERLINE,  // 1.5x .. 2.0x: faol, average'ga qo'shilmaydi
    PAUSE               // > 2.0x: pauza deb topildi
}

/**
 * Har bir zikr uchun alohida ishlovchi mustaqil Timing menejeri.
 *
 * 15 KUNLIK ADAPTIVE RHYTHM LEARNING:
 * - 15 kunlik davr faqat va faqat foydalanuvchining odatiy ritmini (T_avg) o'rganish uchun ishlatiladi.
 * - 15 kun tugaganda Jami Timer (totalActiveTimeMillis), totalCount, currentCount aslo o'zgarmaydi.
 * - Faqat eski ritm unutilib, yangi 15 kunlik davr uchun qaytadan o'rganila boshlaydi.
 *
 * DYNAMIC ADAPTIVE 1.5x / 2.0x THRESHOLD:
 * - T_avg foydalanuvchining real TAP oralig'idan o'rganiladi (1.3s bo'lsa 1.3s, 10s bo'lsa 10s).
 * - Pause threshold:
 *   1. Interval <= 1.5 * T_avg: faol vaqt + rolling average
 *   2. 1.5 * T_avg < Interval <= 2.0 * T_avg: faol vaqtga qo'shiladi, lekin rolling average'ni buzmaydi
 *   3. Interval > 2.0 * T_avg: PAUSE deb hisoblanadi (0 ms qo'shiladi)
 * - Minimum pauza chegarasi: 2000ms. Yuqori chegara T_avg ga mutanosib ravishda cheksiz moslashadi.
 */
class DhikrTimingManager(
    val dhikrId: String,
    initialIsCalibrated: Boolean = false,
    initialNormalIntervalMs: Long = 0L,
    initialLearningPeriodStartMillis: Long = 0L,
    private val maxRecentIntervals: Int = 8,
    private val defaultWarmupFallbackMs: Long = 15000L, // Boshlang'ich keng warmup fallback (hatto 10s ritmlarni ham qabul qiladi)
    private val minThresholdMs: Long = 2000L
) {
    companion object {
        const val FIFTEEN_DAYS_MILLIS = 15L * 24L * 60L * 60L * 1000L
    }

    private val recentIntervals = ArrayDeque<Long>()

    var isCalibrated: Boolean = initialIsCalibrated
        private set

    var normalIntervalMs: Long = initialNormalIntervalMs
        private set

    var learningPeriodStartMillis: Long = initialLearningPeriodStartMillis
        private set

    init {
        if (initialNormalIntervalMs > 0L) {
            recentIntervals.addLast(initialNormalIntervalMs)
        }
    }

    /**
     * 15 kunlik o'rganish siklini tekshiradi.
     * Agar 15 kun o'tgan bo'lsa, ritm xotirasi yangi davr uchun yangilanadi.
     * (Jami timer va hisoblarga tegmaydi!)
     * @return Boolean: yangi davr boshlangan bo'lsa true
     */
    fun checkAndRefresh15DayPeriod(now: Long = System.currentTimeMillis()): Boolean {
        if (learningPeriodStartMillis <= 0L) {
            learningPeriodStartMillis = now
            return true
        }

        if (now - learningPeriodStartMillis >= FIFTEEN_DAYS_MILLIS) {
            // 15 KUNLIK SIKL TUGADI:
            // Eski ritmni unutib, yangi ritmni o'rganishni boshlaymiz:
            learningPeriodStartMillis = now
            recentIntervals.clear()
            isCalibrated = false
            normalIntervalMs = 0L
            return true
        }
        return false
    }

    /**
     * Tap oralig'idagi intervalni klassifikatsiya qiladi va ritmni yangilaydi.
     * @param interval Oxirgi va yangi tap orasidagi millisekund farqi
     * @param now Hozirgi devays vaqti
     * @return Pair<TapClassification, StateChanged: Boolean>
     */
    fun classifyAndRecordTap(interval: Long, now: Long = System.currentTimeMillis()): Pair<TapClassification, Boolean> {
        val periodReset = checkAndRefresh15DayPeriod(now)

        if (interval <= 0L) {
            return Pair(TapClassification.ACTIVE_NORMAL, periodReset)
        }

        var stateChanged = periodReset

        // Dastlabki kalibratsiya davri (hali 5 ta interval yig'ilmagan)
        if (!isCalibrated) {
            if (interval <= defaultWarmupFallbackMs) {
                recentIntervals.addLast(interval)
                if (recentIntervals.size >= 5) {
                    isCalibrated = true
                    normalIntervalMs = recentIntervals.average().toLong()
                    stateChanged = true
                }
                return Pair(TapClassification.ACTIVE_NORMAL, stateChanged)
            } else {
                // Warmup paytida ham juda uzoq kutish pauza deb olinadi
                return Pair(TapClassification.PAUSE, stateChanged)
            }
        }

        // Kalibratsiyadan o'tgan zikr uchun Adaptive 1.5x / 2.0x qoidalari:
        val currentAvg = calculateCurrentAvg()
        val limit15x = (currentAvg * 1.5).toLong()
        val pauseLimit20x = getPauseLimit()

        return when {
            // 1. Interval <= 1.5 * T_avg -> faol vaqtga qo'shiladi va rolling average'ga kiritiladi
            interval <= limit15x -> {
                recordInterval(interval)
                Pair(TapClassification.ACTIVE_NORMAL, stateChanged)
            }
            // 2. 1.5 * T_avg < interval <= 2.0 * T_avg -> faol vaqtga qo'shiladi, lekin ritm buzilmasligi uchun rolling average'ga kiritilmaydi
            interval <= pauseLimit20x -> {
                Pair(TapClassification.ACTIVE_BORDERLINE, stateChanged)
            }
            // 3. Interval > 2.0 * T_avg -> PAUSE: vaqt qo'shilmaydi
            else -> {
                Pair(TapClassification.PAUSE, stateChanged)
            }
        }
    }

    fun getPauseLimit(): Long {
        val currentAvg = calculateCurrentAvg()
        if (currentAvg <= 0L || !isCalibrated) {
            // Hali kalibratsiyalanmagan paytda barcha tezliklar (1.3s, 2.3s, hatto 10s)
            // bemalol qabul qilinishi uchun keng warmup fallback
            return defaultWarmupFallbackMs
        }
        // Foydalanuvchining real ritmiga (T_avg) dinamik 2.0x ko'paytuvchi:
        return (currentAvg * 2.0).toLong().coerceAtLeast(minThresholdMs)
    }

    fun syncFromSavedState(
        savedIsCalibrated: Boolean,
        savedNormalIntervalMs: Long,
        savedPeriodStartMillis: Long
    ) {
        if (savedPeriodStartMillis > 0L && learningPeriodStartMillis <= 0L) {
            learningPeriodStartMillis = savedPeriodStartMillis
        }
        if (savedIsCalibrated && !isCalibrated) {
            isCalibrated = true
            normalIntervalMs = savedNormalIntervalMs
            if (savedNormalIntervalMs > 0L && recentIntervals.isEmpty()) {
                recentIntervals.addLast(savedNormalIntervalMs)
            }
        }
    }

    fun reset() {
        recentIntervals.clear()
        isCalibrated = false
        normalIntervalMs = 0L
        learningPeriodStartMillis = 0L
    }

    fun calculateCurrentAvg(): Long {
        if (recentIntervals.isEmpty()) {
            return if (normalIntervalMs > 0L) normalIntervalMs else 0L
        }
        return recentIntervals.average().toLong().coerceAtLeast(500L)
    }

    private fun recordInterval(interval: Long) {
        if (recentIntervals.size >= maxRecentIntervals) {
            recentIntervals.removeFirst()
        }
        recentIntervals.addLast(interval)
        normalIntervalMs = recentIntervals.average().toLong()
    }
}
