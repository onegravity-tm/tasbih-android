package com.tasbih.app.data.timing

import java.util.ArrayDeque

/**
 * Zikr aytishning haqiqiy (sof) faol vaqtini hisoblash va
 * TAPlar orasidagi interval tahlili orqali avtomatik pauzani aniqlash klassi.
 *
 * Ishlash mantig'i:
 * 1. Birinchi TAP kelganda session faollashadi, lekin o'tgan vaqt 0 deb olinadi.
 * 2. Har bir keyingi TAP kelganda:
 *    - delta = now - lastTapTime
 *    - Agar delta <= pauseThreshold bo'lsa: ushbu delta faol vaqtga qo'shiladi va interval statistikasiga yoziladi.
 *    - Agar delta > pauseThreshold bo'lsa: foydalanuvchi tanaffus qilgan deb hisoblanadi (PAUSE).
 *      Ushbu ortiqcha kutish vaqti faol vaqtga QO'SHILMAYDI! Yangi faol oraliq shu TAPdan qayta davom etadi.
 * 3. Dinamik Threshold (Rolling Average):
 *    - Dastlabki bir nechta tap (warmup) uchun standart xavfsiz threshold (4500ms) ishlatiladi.
 *    - Yetarlicha taplar (oxirgi 5-10 ta normal oraliq) yig'ilgach, o'rtacha sur'at (rolling average) hisoblanadi.
 *    - Dinamik threshold = max(2500ms, min(10000ms, (rollingAvg * 2.0).toLong()))
 *      Bu orqali juda tez zikr aytayotgan odamda ham, sokin aytayotgan odamda ham adolatli pauza aniqlanadi.
 */
class DhikrTimingManager(
    private val maxRecentIntervals: Int = 8,
    private val defaultThresholdMs: Long = 4500L,
    private val minThresholdMs: Long = 2500L,
    private val maxThresholdMs: Long = 10000L
) {
    private val recentIntervals = ArrayDeque<Long>()
    private var lastTapTimestamp: Long = 0L
    private var isTimingActive: Boolean = false

    /**
     * Yangi TAP kelganda chaqiriladi.
     * @param now Hozirgi millisekund vaqti (System.currentTimeMillis())
     * @return Ushbu tap natijasida faol vaqtga qo'shilishi kerak bo'lgan millisekundlar (activeDeltaMs)
     */
    fun onDhikrTap(now: Long = System.currentTimeMillis()): Long {
        if (!isTimingActive || lastTapTimestamp <= 0L) {
            // Birinchi tap: timing boshlandi
            lastTapTimestamp = now
            isTimingActive = true
            return 0L
        }

        val delta = now - lastTapTimestamp
        lastTapTimestamp = now

        if (delta <= 0L) {
            return 0L
        }

        val currentThreshold = calculateDynamicThreshold()

        return if (delta <= currentThreshold) {
            // Normal sur'atdagi tap: oraliqni tarixdagiga qo'shamiz va faol vaqtga qo'shamiz
            recordInterval(delta)
            delta
        } else {
            // Uzoq tanaffusdan keyingi birinchi tap: pauza deb hisoblanadi.
            // Bu tanaffus vaqti faol vaqtga qo'shilmaydi (0 ms)!
            // Yangi oraliq shu tapdan boshlab davom etadi.
            0L
        }
    }

    /**
     * Ilova fonga ketganda yoki foydalanuvchi to'xtaganda chaqiriladi.
     */
    fun pauseTiming() {
        isTimingActive = false
        lastTapTimestamp = 0L
    }

    /**
     * Zikr o'zgarganda yoki reset qilinganda tozalash.
     */
    fun reset() {
        recentIntervals.clear()
        lastTapTimestamp = 0L
        isTimingActive = false
    }

    /**
     * Hozirgi holatda pauzami yoki yo'qligini tekshirish.
     * Agar oxirgi tapdan beri thresholddan ko'p vaqt o'tgan bo'lsa, pauza hisoblanadi.
     */
    fun isPaused(now: Long = System.currentTimeMillis()): Boolean {
        if (!isTimingActive || lastTapTimestamp <= 0L) return true
        val delta = now - lastTapTimestamp
        return delta > calculateDynamicThreshold()
    }

    /**
     * Hozirgi dinamik pauza chegarasi (threshold) millisekundlarda.
     */
    fun calculateDynamicThreshold(): Long {
        if (recentIntervals.size < 3) {
            return defaultThresholdMs
        }
        val avg = recentIntervals.average()
        // 2x ko'paytuvchi orqali pauza aniqlanadi
        val dynamic = (avg * 2.0).toLong()
        return dynamic.coerceIn(minThresholdMs, maxThresholdMs)
    }

    private fun recordInterval(interval: Long) {
        if (recentIntervals.size >= maxRecentIntervals) {
            recentIntervals.removeFirst()
        }
        recentIntervals.addLast(interval)
    }
}
