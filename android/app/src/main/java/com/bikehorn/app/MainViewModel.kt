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
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bikehorn.app.ble.BleManager
import com.bikehorn.app.ble.ConnectionState
import com.bikehorn.app.ble.PuckEvent
import com.bikehorn.app.ble.ScannedDevice
import com.bikehorn.app.crash.CrashAlertManager
import com.bikehorn.app.crash.CrashAlertState
import com.bikehorn.app.data.AppSettings
import com.bikehorn.app.data.BUNDLED_SOUNDS
import com.bikehorn.app.data.ButtonPattern
import com.bikehorn.app.data.CustomSound
import com.bikehorn.app.data.PreferencesRepo
import com.bikehorn.app.data.SoundOption
import com.bikehorn.app.sound.SoundManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = PreferencesRepo(application)
    val soundManager = SoundManager(application)
    private val audioManager = application.getSystemService(AudioManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? =
        application.getSystemService(BluetoothManager::class.java)?.adapter

    private var bleManager: BleManager? = null

    // Holds a MAC address from an NFC intent that arrived before the BLE service was bound.
    // Consumed once in attachBleManager() so the connection is initiated immediately after binding.
    private var pendingConnectAddress: String? = null

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

    // Battery level of the connected BT speaker (null = no speaker, API < 33, or speaker doesn't report it)
    private val _speakerBatteryLevel = MutableStateFlow<Int?>(null)
    val speakerBatteryLevel: StateFlow<Int?> = _speakerBatteryLevel

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
        // Load any previously saved custom sounds into SoundPool once settings are available.
        // loadCustomSounds() is idempotent — it skips IDs that are already loaded.
        viewModelScope.launch {
            prefsRepo.settings.collect { s ->
                soundManager.loadCustomSounds(s.customSounds)
            }
        }
    }

    /** Finds the first connected Bluetooth audio output device (A2DP speaker) and reads its battery */
    private fun updateBluetoothSpeakerState() {
        val btSpeaker = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
        _bluetoothSpeakerName.value = btSpeaker?.productName?.toString()
        // Battery level via AVRCP if available; null clears when speaker disconnects
        _speakerBatteryLevel.value = if (btSpeaker != null) readSpeakerBattery(btSpeaker) else null
    }

    /**
     * Reads battery level from the A2DP device via BluetoothDevice.getBatteryLevel().
     * That method is a hidden API not in the public SDK, so we call it via reflection.
     * Returns null if the device blocks hidden API access or the speaker doesn't report battery.
     */
    private fun readSpeakerBattery(device: AudioDeviceInfo): Int? {
        return try {
            val addr = device.address.takeIf { it.isNotEmpty() } ?: return null
            val btDevice = bluetoothAdapter?.getRemoteDevice(addr) ?: return null
            // Reflection: getBatteryLevel() is a hidden API; returns -1 if unsupported by speaker
            val method = btDevice.javaClass.getMethod("getBatteryLevel")
            val level = method.invoke(btDevice) as? Int ?: return null
            if (level >= 0) level else null
        } catch (_: Exception) {
            null
        }
    }

    val settings: StateFlow<AppSettings> = prefsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    // Sound durations in ms, populated as sounds finish loading into SoundPool
    val soundDurations: StateFlow<Map<Int, Long>> = soundManager.durations

    // Combined catalog of bundled + user-uploaded sounds for dropdown menus
    val soundCatalog: StateFlow<List<SoundOption>> = prefsRepo.settings
        .map { s ->
            BUNDLED_SOUNDS.map { SoundOption(it.id, it.name) } +
            s.customSounds.map { SoundOption(it.id, it.name) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, BUNDLED_SOUNDS.map { SoundOption(it.id, it.name) })

    private val _lastEvent = MutableStateFlow<String?>(null)
    val lastEvent: StateFlow<String?> = _lastEvent

    // Battery percentage reported by the Puck.js (null = not yet received / disconnected)
    private val _batteryLevel = MutableStateFlow<Int?>(null)
    val batteryLevel: StateFlow<Int?> = _batteryLevel

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

    /**
     * Called from MainActivity when an NFC deep link arrives (bikehorn://pair?addr=XX:XX:XX:XX:XX:XX).
     * If the BLE service is already bound, connects immediately; otherwise stores the address
     * as pending so attachBleManager() can act on it once the service is ready.
     */
    fun connectByAddress(address: String) {
        // Normalise: strip any trailing address-type suffix (e.g. " public") and uppercase
        val cleanAddr = address.substringBefore(" ").uppercase().trim()
        val device = bluetoothAdapter?.getRemoteDevice(cleanAddr) ?: return
        if (bleManager != null) {
            bleManager?.connect(device)
        } else {
            // Service not bound yet — store and consume once it binds
            pendingConnectAddress = cleanAddr
        }
    }

    fun attachBleManager(manager: BleManager) {
        bleManager = manager
        // Forward BleManager state flows into ViewModel-owned flows so the UI updates
        viewModelScope.launch {
            manager.connectionState.collect { state ->
                _connectionState.value = state
                // Clear stale battery reading when the Puck disconnects
                if (state == ConnectionState.DISCONNECTED) _batteryLevel.value = null
            }
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
        // If an NFC intent arrived before the service was bound, connect now
        pendingConnectAddress?.let { addr ->
            pendingConnectAddress = null
            bluetoothAdapter?.getRemoteDevice(addr)?.let { manager.connect(it) }
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
            is PuckEvent.BatteryLevel -> {
                // Only store valid values (firmware sends -1 if reading failed)
                if (event.percent in 0..100) _batteryLevel.value = event.percent
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

    /**
     * Adds a user-picked audio file as a custom sound.
     * Takes a persistent URI permission so the file can be re-opened after app restarts.
     */
    fun addCustomSound(uri: Uri) {
        val app = getApplication<Application>()
        // Persist read permission so the URI survives app restarts.
        // OpenDocument URIs always support this; wrapped in try-catch as a safety net.
        try {
            app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // URI doesn't support persistable permissions — the sound will work this session
            // but won't survive an app restart. This shouldn't happen with OpenDocument.
        }
        // Extract a human-readable name from the URI (strips the file extension)
        val name = resolveDisplayName(uri)
        // Assign the next available custom sound ID (101, 102, …)
        val id = (settings.value.customSounds.maxOfOrNull { it.id } ?: 100) + 1
        val sound = CustomSound(id, name, uri.toString())
        soundManager.loadCustomSound(id, uri)
        viewModelScope.launch {
            prefsRepo.saveCustomSounds(settings.value.customSounds + sound)
        }
    }

    /** Removes a custom sound by ID. The SoundPool entry remains until the next app start. */
    fun removeCustomSound(id: Int) {
        viewModelScope.launch {
            prefsRepo.saveCustomSounds(settings.value.customSounds.filter { it.id != id })
        }
    }

    /** Updates the display name of a custom sound without changing its URI or ID. */
    fun renameCustomSound(id: Int, newName: String) {
        viewModelScope.launch {
            val updated = settings.value.customSounds.map { sound ->
                if (sound.id == id) sound.copy(name = newName.trim()) else sound
            }
            prefsRepo.saveCustomSounds(updated)
        }
    }

    /** Queries ContentResolver for the file's display name, falling back to the URI path. */
    private fun resolveDisplayName(uri: Uri): String {
        val cursor = getApplication<Application>().contentResolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
        )
        return cursor?.use {
            if (it.moveToFirst()) {
                it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    .substringBeforeLast(".")  // drop extension
            } else null
        } ?: uri.lastPathSegment ?: "Custom Sound"
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
