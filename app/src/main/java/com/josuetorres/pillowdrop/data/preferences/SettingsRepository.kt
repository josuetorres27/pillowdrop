package com.josuetorres.pillowdrop.data.preferences

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
import com.josuetorres.pillowdrop.domain.model.ActiveTimerState
import com.josuetorres.pillowdrop.domain.model.ShutdownSettings
import com.josuetorres.pillowdrop.domain.model.TimerMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.pillowDropDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pillowdrop_preferences"
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val CONTACT_KEYWORD = stringPreferencesKey("contact_keyword")
        val MATCH_ANY_CALL = booleanPreferencesKey("match_any_whatsapp_call")
        val TIMER_MODE = stringPreferencesKey("timer_mode")
        val DEFAULT_DURATION_MIN = intPreferencesKey("default_duration_minutes")
        val TARGET_HOUR = intPreferencesKey("target_hour")
        val TARGET_MINUTE = intPreferencesKey("target_minute")
        val HANGUP_WHATSAPP_ENABLED = booleanPreferencesKey("hangup_whatsapp_enabled")
        val AUTO_START_ON_CALL = booleanPreferencesKey("auto_start_on_whitelisted_call")
        val NIGHT_WINDOW_ENABLED = booleanPreferencesKey("night_window_enabled")
        val NIGHT_START_HOUR = intPreferencesKey("night_start_hour")
        val NIGHT_START_MINUTE = intPreferencesKey("night_start_minute")
        val NIGHT_END_HOUR = intPreferencesKey("night_end_hour")
        val NIGHT_END_MINUTE = intPreferencesKey("night_end_minute")
        val ALL_DAY_TEST_MODE = booleanPreferencesKey("all_day_test_mode")
        val BATTERY_GUARD_ENABLED = booleanPreferencesKey("battery_guard_enabled")
        val BATTERY_THRESHOLD = intPreferencesKey("battery_threshold_percent")
        val WARNING_VIBRATION_ENABLED = booleanPreferencesKey("warning_vibration_enabled")
        val NUKE_APPS_ENABLED = booleanPreferencesKey("nuke_apps_enabled")
        val NUKE_ALL_NON_PROTECTED = booleanPreferencesKey("nuke_all_non_protected")
        val SELECTED_PACKAGES = stringSetPreferencesKey("selected_packages_to_close")

        val TIMER_ACTIVE = booleanPreferencesKey("timer_is_active")
        val TIMER_TRIGGER_MS = longPreferencesKey("timer_trigger_epoch_ms")
        val TIMER_WARNING_MS = longPreferencesKey("timer_warning_epoch_ms")
        val TIMER_STARTED_MS = longPreferencesKey("timer_started_epoch_ms")
        val TIMER_AUTO_FROM_CALL = booleanPreferencesKey("timer_auto_from_call")
        val TIMER_LABEL = stringPreferencesKey("timer_label")
    }

    val settingsFlow: Flow<ShutdownSettings> = context.pillowDropDataStore.data.map { prefs ->
        val modeRaw = prefs[Keys.TIMER_MODE]
        val mode = runCatching {
            if (modeRaw != null) TimerMode.valueOf(modeRaw) else TimerMode.DURATION
        }.getOrDefault(TimerMode.DURATION)

        ShutdownSettings(
            contactKeyword = prefs[Keys.CONTACT_KEYWORD] ?: "muñequita",
            matchAnyWhatsAppCall = prefs[Keys.MATCH_ANY_CALL] ?: false,
            timerMode = mode,
            defaultDurationMinutes = prefs[Keys.DEFAULT_DURATION_MIN] ?: 45,
            targetHour = prefs[Keys.TARGET_HOUR] ?: 3,
            targetMinute = prefs[Keys.TARGET_MINUTE] ?: 0,
            hangUpWhatsAppEnabled = prefs[Keys.HANGUP_WHATSAPP_ENABLED] ?: true,
            autoStartOnWhitelistedCall = prefs[Keys.AUTO_START_ON_CALL] ?: true,
            nightWindowEnabled = prefs[Keys.NIGHT_WINDOW_ENABLED] ?: true,
            nightStartHour = prefs[Keys.NIGHT_START_HOUR] ?: 22,
            nightStartMinute = prefs[Keys.NIGHT_START_MINUTE] ?: 0,
            nightEndHour = prefs[Keys.NIGHT_END_HOUR] ?: 7,
            nightEndMinute = prefs[Keys.NIGHT_END_MINUTE] ?: 0,
            allDayTestMode = prefs[Keys.ALL_DAY_TEST_MODE] ?: false,
            batteryGuardEnabled = prefs[Keys.BATTERY_GUARD_ENABLED] ?: true,
            batteryThresholdPercent = prefs[Keys.BATTERY_THRESHOLD] ?: 15,
            warningVibrationEnabled = prefs[Keys.WARNING_VIBRATION_ENABLED] ?: true,
            nukeAppsEnabled = prefs[Keys.NUKE_APPS_ENABLED] ?: false,
            nukeAllNonProtected = prefs[Keys.NUKE_ALL_NON_PROTECTED] ?: false,
            selectedPackagesToClose = prefs[Keys.SELECTED_PACKAGES] ?: emptySet()
        )
    }

    val activeTimerFlow: Flow<ActiveTimerState> = context.pillowDropDataStore.data.map { prefs ->
        ActiveTimerState(
            isActive = prefs[Keys.TIMER_ACTIVE] ?: false,
            triggerEpochMillis = prefs[Keys.TIMER_TRIGGER_MS] ?: 0L,
            warningEpochMillis = prefs[Keys.TIMER_WARNING_MS] ?: 0L,
            startedEpochMillis = prefs[Keys.TIMER_STARTED_MS] ?: 0L,
            autoStartedFromCall = prefs[Keys.TIMER_AUTO_FROM_CALL] ?: false,
            label = prefs[Keys.TIMER_LABEL] ?: ""
        )
    }

    suspend fun getSettingsSnapshot(): ShutdownSettings = settingsFlow.first()

    suspend fun getActiveTimerSnapshot(): ActiveTimerState = activeTimerFlow.first()

    suspend fun updateSettings(transform: (ShutdownSettings) -> ShutdownSettings) {
        context.pillowDropDataStore.edit { prefs ->
            val current = settingsFlow.first()
            val updated = transform(current)
            prefs[Keys.CONTACT_KEYWORD] = updated.contactKeyword
            prefs[Keys.MATCH_ANY_CALL] = updated.matchAnyWhatsAppCall
            prefs[Keys.TIMER_MODE] = updated.timerMode.name
            prefs[Keys.DEFAULT_DURATION_MIN] = updated.defaultDurationMinutes
            prefs[Keys.TARGET_HOUR] = updated.targetHour
            prefs[Keys.TARGET_MINUTE] = updated.targetMinute
            prefs[Keys.HANGUP_WHATSAPP_ENABLED] = updated.hangUpWhatsAppEnabled
            prefs[Keys.AUTO_START_ON_CALL] = updated.autoStartOnWhitelistedCall
            prefs[Keys.NIGHT_WINDOW_ENABLED] = updated.nightWindowEnabled
            prefs[Keys.NIGHT_START_HOUR] = updated.nightStartHour
            prefs[Keys.NIGHT_START_MINUTE] = updated.nightStartMinute
            prefs[Keys.NIGHT_END_HOUR] = updated.nightEndHour
            prefs[Keys.NIGHT_END_MINUTE] = updated.nightEndMinute
            prefs[Keys.ALL_DAY_TEST_MODE] = updated.allDayTestMode
            prefs[Keys.BATTERY_GUARD_ENABLED] = updated.batteryGuardEnabled
            prefs[Keys.BATTERY_THRESHOLD] = updated.batteryThresholdPercent
            prefs[Keys.WARNING_VIBRATION_ENABLED] = updated.warningVibrationEnabled
            prefs[Keys.NUKE_APPS_ENABLED] = updated.nukeAppsEnabled
            prefs[Keys.NUKE_ALL_NON_PROTECTED] = updated.nukeAllNonProtected
            prefs[Keys.SELECTED_PACKAGES] = updated.selectedPackagesToClose
        }
    }

    suspend fun setActiveTimer(state: ActiveTimerState) {
        context.pillowDropDataStore.edit { prefs ->
            prefs[Keys.TIMER_ACTIVE] = state.isActive
            prefs[Keys.TIMER_TRIGGER_MS] = state.triggerEpochMillis
            prefs[Keys.TIMER_WARNING_MS] = state.warningEpochMillis
            prefs[Keys.TIMER_STARTED_MS] = state.startedEpochMillis
            prefs[Keys.TIMER_AUTO_FROM_CALL] = state.autoStartedFromCall
            prefs[Keys.TIMER_LABEL] = state.label
        }
    }

    suspend fun clearActiveTimer() {
        setActiveTimer(ActiveTimerState())
    }
}
