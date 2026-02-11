package com.bikehorn.app.crash

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CrashAlertState(
    val active: Boolean = false,
    val secondsRemaining: Int = 0,
    val magnitude: Float = 0f,
    val alarmPlaying: Boolean = false,
)

class CrashAlertManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onPlayAlarm: () -> Int,
    private val onStopAlarm: (Int) -> Unit,
) {
    companion object {
        private const val TAG = "CrashAlertManager"
    }

    private val _state = MutableStateFlow(CrashAlertState())
    val state: StateFlow<CrashAlertState> = _state

    private var countdownJob: Job? = null
    private var alarmStreamId = 0

    fun triggerCrashAlert(magnitude: Float, countdownSeconds: Int, emergencyContact: String) {
        if (_state.value.active) return

        _state.value = CrashAlertState(
            active = true,
            secondsRemaining = countdownSeconds,
            magnitude = magnitude,
        )

        countdownJob = scope.launch {
            for (i in countdownSeconds downTo 1) {
                _state.value = _state.value.copy(secondsRemaining = i)
                delay(1000)
            }
            // Countdown expired - trigger alarm
            _state.value = _state.value.copy(secondsRemaining = 0, alarmPlaying = true)
            alarmStreamId = onPlayAlarm()
            sendEmergencySms(emergencyContact)
        }
    }

    fun cancel() {
        countdownJob?.cancel()
        countdownJob = null
        if (alarmStreamId != 0) {
            onStopAlarm(alarmStreamId)
            alarmStreamId = 0
        }
        _state.value = CrashAlertState()
    }

    @SuppressLint("MissingPermission")
    private fun sendEmergencySms(contact: String) {
        if (contact.isBlank()) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "SMS permission not granted")
            return
        }

        val location = getLastKnownLocation()
        val locationText = if (location != null) {
            "Location: https://maps.google.com/?q=${location.latitude},${location.longitude}"
        } else {
            "Location unavailable"
        }

        val message = "CRASH DETECTED! Bike horn app triggered an emergency alert. $locationText"

        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(contact, null, message, null, null)
            Log.i(TAG, "Emergency SMS sent to $contact")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(): Location? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return null

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    }
}
