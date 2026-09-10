package com.tasbih.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tasbih.app.data.model.AppSettings
import com.tasbih.app.data.model.DhikrItem
import com.tasbih.app.data.model.VibrationLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tasbih_preferences")

class TasbihDataStoreRepository(private val context: Context) : TasbihRepository {

    private object PrefKeys {
        val SELECTED_DHIKR_ID = stringPreferencesKey("selected_dhikr_id")
        val VIBRATION_LEVEL = stringPreferencesKey("vibration_level")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VOLUME_BUTTONS_ENABLED = booleanPreferencesKey("volume_buttons_enabled")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val FULLSCREEN_TAP_ENABLED = booleanPreferencesKey("fullscreen_tap_enabled")
        val CUSTOM_DHIKR_IDS = stringSetPreferencesKey("custom_dhikr_ids")

        fun countKey(id: String) = intPreferencesKey("count_$id")
        fun totalKey(id: String) = longPreferencesKey("total_$id")
        fun nameKey(id: String) = stringPreferencesKey("name_$id")
        fun arabicKey(id: String) = stringPreferencesKey("arabic_$id")
        fun targetKey(id: String) = intPreferencesKey("target_$id")
        fun activeTimeKey(id: String) = longPreferencesKey("active_time_$id")
        fun lastTapKey(id: String) = longPreferencesKey("last_tap_$id")
    }

    private val defaultDhikrs = listOf(
        DhikrItem(
            id = "subhanallah",
            name = "Subhanalloh",
            arabicText = "سُبْحَانَ ٱللَّٰهِ",
            targetCount = 33,
            orderIndex = 0
        ),
        DhikrItem(
            id = "alhamdulillah",
            name = "Alhamdulillah",
            arabicText = "ٱلْحَمْدُ لِلَّٰهِ",
            targetCount = 33,
            orderIndex = 1
        ),
        DhikrItem(
            id = "allahu_akbar",
            name = "Allohu Akbar",
            arabicText = "ٱللَّٰهُ أَكْبَرُ",
            targetCount = 33,
            orderIndex = 2
        ),
        DhikrItem(
            id = "astaghfirullah",
            name = "Astag'firulloh",
            arabicText = "أَسْتَغْفِرُ ٱللَّٰهَ",
            targetCount = 100,
            orderIndex = 3
        ),
        DhikrItem(
            id = "la_ilaha_illallah",
            name = "La ilaha illalloh",
            arabicText = "لَا إِلَٰهَ إِلَّا ٱللَّٰهُ",
            targetCount = 100,
            orderIndex = 4
        ),
        DhikrItem(
            id = "salawat",
            name = "Allohumma solli 'ala Muhammad",
            arabicText = "اللَّهُمَّ صَلِّ عَلَىٰ مُحَمَّدٍ",
            targetCount = 100,
            orderIndex = 5
        ),
        DhikrItem(
            id = "free_dhikr",
            name = "Erkin zikr",
            arabicText = "",
            targetCount = 0,
            orderIndex = 6
        )
    )

    override val selectedDhikrIdFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PrefKeys.SELECTED_DHIKR_ID] ?: defaultDhikrs.first().id
    }

    override val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        val vibString = preferences[PrefKeys.VIBRATION_LEVEL] ?: VibrationLevel.MEDIUM.name
        val vibLevel = try {
            VibrationLevel.valueOf(vibString)
        } catch (_: Exception) {
            VibrationLevel.MEDIUM
        }

        AppSettings(
            vibrationLevel = vibLevel,
            isSoundEnabled = preferences[PrefKeys.SOUND_ENABLED] ?: false,
            isVolumeButtonsEnabled = preferences[PrefKeys.VOLUME_BUTTONS_ENABLED] ?: true,
            isKeepScreenOn = preferences[PrefKeys.KEEP_SCREEN_ON] ?: false,
            isFullScreenTapEnabled = preferences[PrefKeys.FULLSCREEN_TAP_ENABLED] ?: false
        )
    }

    override val dhikrListFlow: Flow<List<DhikrItem>> = context.dataStore.data.map { preferences ->
        val customIds = preferences[PrefKeys.CUSTOM_DHIKR_IDS] ?: emptySet()

        val builtInList = defaultDhikrs.map { item ->
            val count = preferences[PrefKeys.countKey(item.id)] ?: 0
            val total = preferences[PrefKeys.totalKey(item.id)] ?: 0L
            val target = preferences[PrefKeys.targetKey(item.id)] ?: item.targetCount
            val activeTime = preferences[PrefKeys.activeTimeKey(item.id)] ?: 0L
            val lastTap = preferences[PrefKeys.lastTapKey(item.id)] ?: 0L
            item.copy(
                targetCount = target,
                currentCount = count,
                totalCount = total,
                totalActiveTimeMillis = activeTime,
                lastActiveTimestamp = lastTap
            )
        }

        val customList = customIds.mapNotNull { id ->
            val name = preferences[PrefKeys.nameKey(id)] ?: return@mapNotNull null
            val arabic = preferences[PrefKeys.arabicKey(id)] ?: ""
            val target = preferences[PrefKeys.targetKey(id)] ?: 33
            val count = preferences[PrefKeys.countKey(id)] ?: 0
            val total = preferences[PrefKeys.totalKey(id)] ?: 0L
            val activeTime = preferences[PrefKeys.activeTimeKey(id)] ?: 0L
            val lastTap = preferences[PrefKeys.lastTapKey(id)] ?: 0L

            DhikrItem(
                id = id,
                name = name,
                arabicText = arabic,
                targetCount = target,
                currentCount = count,
                totalCount = total,
                isCustom = true,
                orderIndex = 100,
                totalActiveTimeMillis = activeTime,
                lastActiveTimestamp = lastTap
            )
        }

        builtInList + customList
    }

    override suspend fun selectDhikr(id: String) {
        context.dataStore.edit { preferences ->
            preferences[PrefKeys.SELECTED_DHIKR_ID] = id
        }
    }

    override suspend fun incrementDhikr(id: String) {
        val now = System.currentTimeMillis()
        context.dataStore.edit { preferences ->
            val countKey = PrefKeys.countKey(id)
            val totalKey = PrefKeys.totalKey(id)
            val lastTapKey = PrefKeys.lastTapKey(id)

            val currentCount = preferences[countKey] ?: 0
            val currentTotal = preferences[totalKey] ?: 0L

            preferences[countKey] = currentCount + 1
            preferences[totalKey] = currentTotal + 1
            preferences[lastTapKey] = now
        }
    }

    override suspend fun decrementDhikr(id: String) {
        context.dataStore.edit { preferences ->
            val countKey = PrefKeys.countKey(id)
            val currentCount = preferences[countKey] ?: 0
            if (currentCount > 0) {
                preferences[countKey] = currentCount - 1
            }
        }
    }

    override suspend fun saveDhikrCounts(id: String, currentCount: Int, totalCount: Long, lastTap: Long, activeTimeMillis: Long) {
        context.dataStore.edit { preferences ->
            preferences[PrefKeys.countKey(id)] = currentCount
            preferences[PrefKeys.totalKey(id)] = totalCount
            preferences[PrefKeys.lastTapKey(id)] = lastTap
            preferences[PrefKeys.activeTimeKey(id)] = activeTimeMillis
        }
    }

    override suspend fun resetDhikr(id: String) {
        context.dataStore.edit { preferences ->
            preferences[PrefKeys.countKey(id)] = 0
            preferences[PrefKeys.lastTapKey(id)] = 0L
            preferences[PrefKeys.activeTimeKey(id)] = 0L
        }
    }

    override suspend fun updateDhikrTarget(id: String, targetCount: Int) {
        context.dataStore.edit { preferences ->
            preferences[PrefKeys.targetKey(id)] = targetCount
        }
    }

    override suspend fun addCustomDhikr(name: String, arabicText: String, targetCount: Int) {
        val id = "custom_" + UUID.randomUUID().toString().take(8)
        context.dataStore.edit { preferences ->
            val currentCustomIds = preferences[PrefKeys.CUSTOM_DHIKR_IDS] ?: emptySet()
            preferences[PrefKeys.CUSTOM_DHIKR_IDS] = currentCustomIds + id
            preferences[PrefKeys.nameKey(id)] = name.trim()
            preferences[PrefKeys.arabicKey(id)] = arabicText.trim()
            preferences[PrefKeys.targetKey(id)] = targetCount
            preferences[PrefKeys.countKey(id)] = 0
            preferences[PrefKeys.totalKey(id)] = 0L
            preferences[PrefKeys.SELECTED_DHIKR_ID] = id
        }
    }

    override suspend fun deleteCustomDhikr(id: String) {
        context.dataStore.edit { preferences ->
            val currentCustomIds = preferences[PrefKeys.CUSTOM_DHIKR_IDS] ?: emptySet()
            preferences[PrefKeys.CUSTOM_DHIKR_IDS] = currentCustomIds - id
            preferences.remove(PrefKeys.nameKey(id))
            preferences.remove(PrefKeys.arabicKey(id))
            preferences.remove(PrefKeys.targetKey(id))
            preferences.remove(PrefKeys.countKey(id))
            preferences.remove(PrefKeys.totalKey(id))
            preferences.remove(PrefKeys.activeTimeKey(id))
            preferences.remove(PrefKeys.lastTapKey(id))

            if (preferences[PrefKeys.SELECTED_DHIKR_ID] == id) {
                preferences[PrefKeys.SELECTED_DHIKR_ID] = defaultDhikrs.first().id
            }
        }
    }

    override suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[PrefKeys.VIBRATION_LEVEL] = settings.vibrationLevel.name
            preferences[PrefKeys.SOUND_ENABLED] = settings.isSoundEnabled
            preferences[PrefKeys.VOLUME_BUTTONS_ENABLED] = settings.isVolumeButtonsEnabled
            preferences[PrefKeys.KEEP_SCREEN_ON] = settings.isKeepScreenOn
            preferences[PrefKeys.FULLSCREEN_TAP_ENABLED] = settings.isFullScreenTapEnabled
        }
    }
}
