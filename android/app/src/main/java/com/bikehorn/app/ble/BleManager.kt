package com.bikehorn.app.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class ConnectionState {
    DISCONNECTED, SCANNING, CONNECTING, CONNECTED
}

data class ScannedDevice(
    val name: String,
    val address: String,
    val device: BluetoothDevice
)

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    companion object {
        private const val TAG = "BleManager"

        // Nordic UART Service UUIDs
        val NUS_SERVICE_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        val NUS_TX_UUID: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
        val CCC_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        private const val MAX_RECONNECT_DELAY_MS = 30_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private var gatt: BluetoothGatt? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0
    private var lastConnectedAddress: String? = null
    private var messageBuffer = StringBuilder()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName

    private val _scannedDevices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val scannedDevices: StateFlow<List<ScannedDevice>> = _scannedDevices

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val messages: SharedFlow<String> = _messages

    private val _events = MutableSharedFlow<PuckEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<PuckEvent> = _events

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: return
            Log.d(TAG, "Scan found device: '$name' (${device.address})")
            // Only show devices with "Puck" in the name (e.g. "Puck.js ABCD")
            if (!name.contains("Puck", ignoreCase = true)) return
            val existing = _scannedDevices.value
            if (existing.none { it.address == device.address }) {
                Log.i(TAG, "Found Puck.js device: $name (${device.address})")
                _scannedDevices.value = existing + ScannedDevice(name, device.address, device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed: errorCode=$errorCode")
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Connected to ${gatt.device.name}")
                    _connectionState.value = ConnectionState.CONNECTED
                    _connectedDeviceName.value = gatt.device.name
                    lastConnectedAddress = gatt.device.address
                    reconnectAttempt = 0
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "Disconnected")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    _connectedDeviceName.value = null
                    gatt.close()
                    this@BleManager.gatt = null
                    scheduleReconnect()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return

            val service = gatt.getService(NUS_SERVICE_UUID) ?: run {
                Log.e(TAG, "NUS service not found")
                return
            }
            val txChar = service.getCharacteristic(NUS_TX_UUID) ?: run {
                Log.e(TAG, "TX characteristic not found")
                return
            }

            gatt.setCharacteristicNotification(txChar, true)
            val descriptor = txChar.getDescriptor(CCC_DESCRIPTOR_UUID)
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == NUS_TX_UUID) {
                val chunk = characteristic.getStringValue(0)
                processChunk(chunk)
            }
        }
    }

    private fun processChunk(chunk: String) {
        messageBuffer.append(chunk)
        // Messages are newline-delimited
        while (true) {
            val newlineIdx = messageBuffer.indexOf('\n')
            if (newlineIdx == -1) break
            val line = messageBuffer.substring(0, newlineIdx).trim()
            messageBuffer.delete(0, newlineIdx + 1)
            if (line.isNotEmpty()) {
                _messages.tryEmit(line)
                PuckJsProtocol.parse(line)?.let { event ->
                    _events.tryEmit(event)
                }
            }
        }
    }

    fun startScan() {
        // Prevent starting a scan if one is already running (avoids SCAN_FAILED_ALREADY_STARTED)
        if (_connectionState.value == ConnectionState.SCANNING) return

        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: run {
            Log.e(TAG, "BLE scanner not available — is Bluetooth enabled?")
            return
        }

        _scannedDevices.value = emptyList()
        _connectionState.value = ConnectionState.SCANNING
        Log.i(TAG, "Starting BLE scan (no UUID filter, matching Puck.js by name)")

        // Scan without UUID filter — Puck.js doesn't always advertise NUS UUID in scan data.
        // Devices are filtered by name in the scan callback instead.
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, scanCallback)

        // Stop scanning after 15 seconds
        scope.launch {
            delay(15_000)
            stopScan()
        }
    }

    fun stopScan() {
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        if (_connectionState.value == ConnectionState.SCANNING) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    fun connect(device: BluetoothDevice) {
        stopScan()
        cancelReconnect()
        _connectionState.value = ConnectionState.CONNECTING
        gatt = device.connectGatt(context, false, gattCallback)
    }

    fun connect(address: String) {
        val device = bluetoothAdapter?.getRemoteDevice(address) ?: return
        connect(device)
    }

    fun disconnect() {
        cancelReconnect()
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        _connectionState.value = ConnectionState.DISCONNECTED
        _connectedDeviceName.value = null
        lastConnectedAddress = null
    }

    private fun scheduleReconnect() {
        val address = lastConnectedAddress ?: return
        reconnectJob = scope.launch {
            val delayMs = minOf(
                1000L * (1L shl reconnectAttempt.coerceAtMost(4)),
                MAX_RECONNECT_DELAY_MS
            )
            Log.i(TAG, "Reconnecting in ${delayMs}ms (attempt $reconnectAttempt)")
            delay(delayMs)
            reconnectAttempt++
            _connectionState.value = ConnectionState.CONNECTING
            val device = bluetoothAdapter?.getRemoteDevice(address) ?: return@launch
            gatt = device.connectGatt(context, false, gattCallback)
        }
    }

    private fun cancelReconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        reconnectAttempt = 0
    }

    fun destroy() {
        cancelReconnect()
        stopScan()
        gatt?.disconnect()
        gatt?.close()
    }
}
