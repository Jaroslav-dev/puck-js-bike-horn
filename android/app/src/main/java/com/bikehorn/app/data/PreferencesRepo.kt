package com.bikehorn.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bikehorn_prefs")

class PreferencesRepo(private val context: Context) {

    private object Keys {
        val EMERGENCY_CONTACT = stringPreferencesKey("emergency_contact")
        val CRASH_THRESHOLD = floatPreferencesKey("crash_threshold")
        val COUNTDOWN_DURATION = intPreferencesKey("countdown_duration")
        // Motion detection sound assignments
        val ACCELERATION_SOUND = intPreferencesKey("acceleration_sound")
        val BRAKING_SOUND = intPreferencesKey("braking_sound")
        fun assignmentKey(pattern: ButtonPattern) =
            intPreferencesKey("assign_${pattern.name}")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val assignments = ButtonPattern.entries.associateWith { pattern ->
            prefs[Keys.assignmentKey(pattern)]
                ?: AppSettings().assignments[pattern]
                ?: 1
        }
        AppSettings(
            assignments = assignments,
            emergencyContact = prefs[Keys.EMERGENCY_CONTACT] ?: "",
            crashThreshold = prefs[Keys.CRASH_THRESHOLD] ?: 3.0f,
            countdownDuration = prefs[Keys.COUNTDOWN_DURATION] ?: 10,
            accelerationSoundId = prefs[Keys.ACCELERATION_SOUND] ?: 1,
            brakingSoundId = prefs[Keys.BRAKING_SOUND] ?: 2,
        )
    }

    suspend fun setSoundAssignment(pattern: ButtonPattern, soundId: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.assignmentKey(pattern)] = soundId
        }
    }

    suspend fun setEmergencyContact(number: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EMERGENCY_CONTACT] = number
        }
    }

    suspend fun setCrashThreshold(threshold: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CRASH_THRESHOLD] = threshold
        }
    }

    suspend fun setCountdownDuration(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.COUNTDOWN_DURATION] = seconds
        }
    }

    suspend fun setAccelerationSound(soundId: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCELERATION_SOUND] = soundId
        }
    }

    suspend fun setBrakingSound(soundId: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BRAKING_SOUND] = soundId
        }
    }
}
