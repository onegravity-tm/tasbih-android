package com.tasbih.app.data.timing

import java.util.ArrayDeque

/**
 * Har bir zikr uchun alohida ishlovchi mustaqil Timing menejeri.
 *
 * Qat'iy tasdiqlangan Adaptive 1.5x / 2.0x qoidalari:
 * 1. Interval <= 1.5 * T_avg:
 *    -> Faol vaqtga to'liq qo'shiladi
 *    -> Asosiy ritm (rolling average) hisobiga kiritiladi.
 *
 * 2. 1.5 * T_avg < Interval <= 2.0 * T_avg:
 *    -> Nafas olish / tabiiy oraliq sifatida faol vaqtga to'liq qo'shiladi
 *    -> Lekin asosiy ritm (rolling average)ni buzmasligi uchun unga qo'shilmaydi.
 *
 * 3. Interval > 2.0 * T_avg:
 *    -> PAUSE deb hisoblanadi
 *    -> Ushbu interval faol vaqtga 0 ms qo'shadi
 *    -> Yangi faol interval keyingi TAPdan boshlanadi.
 *
 * Calibration:
 * - Dastlabki 5 ta tap (warmup) xavfsiz keng chegara (4500ms) bilan qabul qilinadi.
 * - 5 ta interval to'plangach, zikr kalibratsiyalangan hisoblanadi va uning dastlabki T_avg qiymati saqlanadi.
 */
class DhikrTimingManager(
    val dhikrId: String,
    initialIsCalibrated: Boolean = false,
    initialNormalIntervalMs: Long = 0L,
    private val maxRecentIntervals: Int = 8,
    private val defaultWarmupThresholdMs: Long = 4500L,
    private val minThresholdMs: Long = 2000L,
    private val maxThresholdMs: Long = 8000L
) {
    private val recentIntervals = ArrayDeque<Long>()
    private var lastTapTimestamp: Long = 0L
    private var isTimingActive: Boolean = false

    var isCalibrated: Boolean = initialIsCalibrated
        private set

    var normalIntervalMs: Long = initialNormalIntervalMs
        private set

    init {
        if (initialNormalIntervalMs > 0L) {
            recentIntervals.addLast(initialNormalIntervalMs)
        }
    }

    /**
     * Yangi TAP kelganda chaqiriladi.
     * @param now Hozirgi millisekund vaqti (System.currentTimeMillis())
     * @return Pair<ActiveDeltaMillis, NewlyCalibrated: Boolean>
     */
    fun onDhikrTap(now: Long = System.currentTimeMillis()): Pair<Long, Boolean> {
        if (!isTimingActive || lastTapTimestamp <= 0L) {
            // Sessiyadagi birinchi tap: faol vaqt hali 0 ms
            lastTapTimestamp = now
            isTimingActive = true
            return Pair(0L, false)
        }

        val interval = now - lastTapTimestamp
        lastTapTimestamp = now

        if (interval <= 0L) {
            return Pair(0L, false)
        }

        var newlyCalibrated = false

        // Dastlabki kalibratsiya davri (hali 5 ta interval yig'ilmagan)
        if (!isCalibrated) {
            if (interval <= defaultWarmupThresholdMs) {
                recentIntervals.addLast(interval)
                if (recentIntervals.size >= 5) {
                    isCalibrated = true
                    normalIntervalMs = recentIntervals.average().toLong()
                    newlyCalibrated = true
                }
                return Pair(interval, newlyCalibrated)
            } else {
                // Warmup paytida ham uzoq kutish pauza deb olinadi
                return Pair(0L, false)
            }
        }

        // Kalibratsiyadan o'tgan zikr uchun Adaptive 1.5x / 2.0x qoidalari:
        val currentAvg = calculateCurrentAvg()
        val limit15x = (currentAvg * 1.5).toLong()
        val pauseLimit20x = (currentAvg * 2.0).toLong().coerceIn(minThresholdMs, maxThresholdMs)

        return when {
            // 1. Interval <= 1.5 * T_avg -> faol vaqtga qo'shiladi va rolling average'ga kiritiladi
            interval <= limit15x -> {
                recordInterval(interval)
                Pair(interval, false)
            }
            // 2. 1.5 * T_avg < interval <= 2.0 * T_avg -> faol vaqtga qo'shiladi, lekin ritm buzilmasligi uchun rolling average'ga kiritilmaydi
            interval <= pauseLimit20x -> {
                Pair(interval, false)
            }
            // 3. Interval > 2.0 * T_avg -> PAUSE: 0 ms qo'shiladi
            else -> {
                Pair(0L, false)
            }
        }
    }

    fun pauseTiming() {
        isTimingActive = false
        lastTapTimestamp = 0L
    }

    fun reset() {
        recentIntervals.clear()
        lastTapTimestamp = 0L
        isTimingActive = false
        isCalibrated = false
        normalIntervalMs = 0L
    }

    fun isPaused(now: Long = System.currentTimeMillis()): Boolean {
        if (!isTimingActive || lastTapTimestamp <= 0L) return true
        val delta = now - lastTapTimestamp
        val currentAvg = calculateCurrentAvg()
        val pauseLimit = (currentAvg * 2.0).toLong().coerceIn(minThresholdMs, maxThresholdMs)
        return delta > pauseLimit
    }

    private fun calculateCurrentAvg(): Long {
        if (recentIntervals.isEmpty()) {
            return if (normalIntervalMs > 0L) normalIntervalMs else 1500L
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
