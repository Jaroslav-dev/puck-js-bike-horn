package com.bikehorn.app

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bikehorn.app.ble.BleManager
import com.bikehorn.app.ble.ConnectionState
import com.bikehorn.app.ble.PuckEvent
import com.bikehorn.app.ble.ScannedDevice
import com.bikehorn.app.crash.CrashAlertManager
import com.bikehorn.app.crash.CrashAlertState
import com.bikehorn.app.data.AppSettings
import com.bikehorn.app.data.ButtonPattern
import com.bikehorn.app.data.PreferencesRepo
import com.bikehorn.app.sound.SoundManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = PreferencesRepo(application)
    val soundManager = SoundManager(application)
    private val audioManager = application.getSystemService(AudioManager::class.java)

    private var bleManager: BleManager? = null

    // Tracks whether a Bluetooth A2DP speaker is connected for audio output
    private val _bluetoothSpeakerName = MutableStateFlow<String?>(null)
    val bluetoothSpeakerName: StateFlow<String?> = _bluetoothSpeakerName

    // Callback to detect Bluetooth audio devices connecting/disconnecting
    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            updateBluetoothSpeakerState()
        }
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            updateBluetoothSpeakerState()
        }
    }

    init {
        // Check initial state and listen for changes
        updateBluetoothSpeakerState()
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, Handler(Looper.getMainLooper()))
    }

    /** Finds the first connected Bluetooth audio output device (A2DP speaker) */
    private fun updateBluetoothSpeakerState() {
        val btSpeaker = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
        _bluetoothSpeakerName.value = btSpeaker?.productName?.toString()
    }

    val settings: StateFlow<AppSettings> = prefsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _lastEvent = MutableStateFlow<String?>(null)
    val lastEvent: StateFlow<String?> = _lastEvent

    val connectionState: StateFlow<ConnectionState>
        get() = bleManager?.connectionState
            ?: MutableStateFlow(ConnectionState.DISCONNECTED)

    val connectedDeviceName: StateFlow<String?>
        get() = bleManager?.connectedDeviceName
            ?: MutableStateFlow(null)

    val scannedDevices: StateFlow<List<ScannedDevice>>
        get() = bleManager?.scannedDevices
            ?: MutableStateFlow(emptyList())

    val crashAlertManager = CrashAlertManager(
        context = application,
        scope = viewModelScope,
        onPlayAlarm = { soundManager.playAlarm(4) }, // alarm sound id
        onStopAlarm = { soundManager.stopStream(it) },
    )

    val crashAlertState: StateFlow<CrashAlertState> = crashAlertManager.state

    fun attachBleManager(manager: BleManager) {
        bleManager = manager
        viewModelScope.launch {
            manager.events.collect { event ->
                handleEvent(event)
            }
        }
    }

    private fun handleEvent(event: PuckEvent) {
        when (event) {
            is PuckEvent.ShortPress -> {
                _lastEvent.value = "Short Press"
                playForPattern(ButtonPattern.SHORT_PRESS)
            }
            is PuckEvent.LongPress -> {
                _lastEvent.value = "Long Press"
                playForPattern(ButtonPattern.LONG_PRESS)
            }
            is PuckEvent.VeryLongPress -> {
                _lastEvent.value = "Very Long Press"
                playForPattern(ButtonPattern.VERY_LONG_PRESS)
            }
            is PuckEvent.RepeatedPress -> {
                _lastEvent.value = "Repeated Press"
                playForPattern(ButtonPattern.REPEATED_PRESS)
            }
            is PuckEvent.Crash -> {
                _lastEvent.value = "Crash (${event.magnitude}g)"
                val s = settings.value
                crashAlertManager.triggerCrashAlert(
                    magnitude = event.magnitude,
                    countdownSeconds = s.countdownDuration,
                    emergencyContact = s.emergencyContact,
                )
            }
        }
    }

    private fun playForPattern(pattern: ButtonPattern) {
        val soundId = settings.value.assignments[pattern] ?: return
        soundManager.play(soundId)
    }

    fun startScan() {
        bleManager?.startScan()
    }

    fun connect(device: BluetoothDevice) {
        bleManager?.connect(device)
    }

    fun disconnect() {
        bleManager?.disconnect()
    }

    fun assignSound(pattern: ButtonPattern, soundId: Int) {
        viewModelScope.launch {
            prefsRepo.setSoundAssignment(pattern, soundId)
        }
    }

    fun previewSound(soundId: Int) {
        soundManager.play(soundId)
    }

    fun setEmergencyContact(contact: String) {
        viewModelScope.launch {
            prefsRepo.setEmergencyContact(contact)
        }
    }

    fun setCrashThreshold(threshold: Float) {
        viewModelScope.launch {
            prefsRepo.setCrashThreshold(threshold)
        }
    }

    fun setCountdownDuration(seconds: Int) {
        viewModelScope.launch {
            prefsRepo.setCountdownDuration(seconds)
        }
    }

    fun cancelCrashAlert() {
        crashAlertManager.cancel()
    }

    override fun onCleared() {
        // Unregister BT audio listener and release sound resources
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        soundManager.release()
        super.onCleared()
    }
}
