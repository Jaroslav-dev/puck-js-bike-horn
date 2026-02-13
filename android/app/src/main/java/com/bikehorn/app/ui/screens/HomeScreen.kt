package com.bikehorn.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bikehorn.app.ble.ConnectionState
import com.bikehorn.app.ble.ScannedDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    connectionState: ConnectionState,
    deviceName: String?,
    lastEvent: String?,
    scannedDevices: List<ScannedDevice>,
    bluetoothSpeakerName: String?,
    onScan: () -> Unit,
    onConnect: (ScannedDevice) -> Unit,
    onDisconnect: () -> Unit,
    onNavigateToSounds: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bike Horn") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Connection status card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (connectionState) {
                        ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                        ConnectionState.SCANNING, ConnectionState.CONNECTING ->
                            MaterialTheme.colorScheme.secondaryContainer
                        ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = when (connectionState) {
                            ConnectionState.CONNECTED -> Icons.Default.BluetoothConnected
                            ConnectionState.SCANNING -> Icons.Default.BluetoothSearching
                            else -> Icons.Default.Bluetooth
                        },
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (connectionState) {
                                ConnectionState.CONNECTED -> "Connected"
                                ConnectionState.SCANNING -> "Scanning..."
                                ConnectionState.CONNECTING -> "Connecting..."
                                ConnectionState.DISCONNECTED -> "Disconnected"
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (deviceName != null) {
                            Text(
                                text = deviceName,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    if (connectionState == ConnectionState.SCANNING ||
                        connectionState == ConnectionState.CONNECTING
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Audio output indicator — shows whether sound routes to BT speaker or phone
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (bluetoothSpeakerName != null)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = if (bluetoothSpeakerName != null)
                            Icons.Default.Speaker
                        else
                            Icons.Default.SpeakerPhone,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Column {
                        Text(
                            text = if (bluetoothSpeakerName != null)
                                "Audio: $bluetoothSpeakerName"
                            else
                                "Audio: Phone speaker",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        // Warn user if no BT speaker — horn will be quiet
                        if (bluetoothSpeakerName == null) {
                            Text(
                                text = "Connect a Bluetooth speaker for louder horn",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Action buttons
            when (connectionState) {
                ConnectionState.DISCONNECTED -> {
                    Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
                        Text("Scan for Puck.js")
                    }
                }
                ConnectionState.CONNECTED -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Button(
                            onClick = onNavigateToSounds,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Sound Setup")
                        }
                        Button(
                            onClick = onDisconnect,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Disconnect")
                        }
                    }
                }
                else -> {} // Scanning/Connecting - show devices below
            }

            // Last event
            if (lastEvent != null) {
                Spacer(Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Last Event", style = MaterialTheme.typography.labelMedium)
                        Text(lastEvent, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            // Scanned devices list
            if (connectionState == ConnectionState.SCANNING && scannedDevices.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Found Devices", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(scannedDevices) { device ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onConnect(device) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(device.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    device.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
