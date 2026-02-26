package com.bikehorn.app

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
    private val bluetoothAdapter: BluetoothAdapter? =
        application.getSystemService(BluetoothManager::class.java)?.adapter

    private var bleManager: BleManager? = null

    // Reactive state that updates when Bluetooth is toggled on/off
    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled

    // Listens for system Bluetooth state changes (on/off)
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                _isBluetoothEnabled.value = (state == BluetoothAdapter.STATE_ON)
            }
        }
    }

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
        // Register receiver to track Bluetooth on/off state changes
        application.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
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

    // Mirror BleManager state into ViewModel-owned flows so Compose always collects
    // from a stable reference (avoids stale fallback flows when bleManager is null initially)
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName

    private val _scannedDevices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val scannedDevices: StateFlow<List<ScannedDevice>> = _scannedDevices

    val crashAlertManager = CrashAlertManager(
        context = application,
        scope = viewModelScope,
        onPlayAlarm = { soundManager.playAlarm(4) }, // alarm sound id
        onStopAlarm = { soundManager.stopStream(it) },
    )

    val crashAlertState: StateFlow<CrashAlertState> = crashAlertManager.state

    fun attachBleManager(manager: BleManager) {
        bleManager = manager
        // Forward BleManager state flows into ViewModel-owned flows so the UI updates
        viewModelScope.launch {
            manager.connectionState.collect { _connectionState.value = it }
        }
        viewModelScope.launch {
            manager.connectedDeviceName.collect { _connectedDeviceName.value = it }
        }
        viewModelScope.launch {
            manager.scannedDevices.collect { _scannedDevices.value = it }
        }
        viewModelScope.launch {
            manager.events.collect { event ->
                handleEvent(event)
            }
        }
    }

    private fun handleEvent(event: PuckEvent) {
        when (event) {
            is PuckEvent.SinglePress -> {
                _lastEvent.value = "1 Tap"
                playForPattern(ButtonPattern.SINGLE_PRESS)
            }
            is PuckEvent.DoublePress -> {
                _lastEvent.value = "2 Taps"
                playForPattern(ButtonPattern.DOUBLE_PRESS)
            }
            is PuckEvent.TriplePress -> {
                _lastEvent.value = "3 Taps"
                playForPattern(ButtonPattern.TRIPLE_PRESS)
            }
            is PuckEvent.LongPress -> {
                _lastEvent.value = "Long Press"
                playForPattern(ButtonPattern.LONG_PRESS)
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
            // Play the user-assigned sound on motion events detected by the accelerometer
            is PuckEvent.Acceleration -> {
                _lastEvent.value = "Acceleration"
                soundManager.play(settings.value.accelerationSoundId)
            }
            is PuckEvent.Braking -> {
                _lastEvent.value = "Braking"
                soundManager.play(settings.value.brakingSoundId)
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

    fun setAccelerationSound(soundId: Int) {
        viewModelScope.launch {
            prefsRepo.setAccelerationSound(soundId)
        }
    }

    fun setBrakingSound(soundId: Int) {
        viewModelScope.launch {
            prefsRepo.setBrakingSound(soundId)
        }
    }

    fun cancelCrashAlert() {
        crashAlertManager.cancel()
    }

    override fun onCleared() {
        // Unregister BT state and audio listeners, release sound resources
        getApplication<Application>().unregisterReceiver(bluetoothStateReceiver)
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        soundManager.release()
        super.onCleared()
    }
}
