package com.bikehorn.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bikehorn.app.data.AppSettings
import com.bikehorn.app.data.SoundOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    soundCatalog: List<SoundOption>,       // bundled + custom sounds for dropdowns
    onEmergencyContactChange: (String) -> Unit,
    onCrashThresholdChange: (Float) -> Unit,
    onCountdownDurationChange: (Int) -> Unit,
    onAccelerationSoundChange: (Int) -> Unit,
    onBrakingSoundChange: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Emergency Contact
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Emergency Contact",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = settings.emergencyContact,
                        onValueChange = onEmergencyContactChange,
                        label = { Text("Phone number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Text(
                        "SMS will be sent to this number on crash detection",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Crash Sensitivity
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Crash Detection Sensitivity",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Threshold: ${"%.1f".format(settings.crashThreshold)}g")
                    }
                    Slider(
                        value = settings.crashThreshold,
                        onValueChange = onCrashThresholdChange,
                        valueRange = 1.5f..8.0f,
                        steps = 12,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("More sensitive", style = MaterialTheme.typography.bodySmall)
                        Text("Less sensitive", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Countdown Duration
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Alert Countdown",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Duration: ${settings.countdownDuration} seconds")
                    Slider(
                        value = settings.countdownDuration.toFloat(),
                        onValueChange = { onCountdownDurationChange(it.toInt()) },
                        valueRange = 5f..30f,
                        steps = 24,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("5s", style = MaterialTheme.typography.bodySmall)
                        Text("30s", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Motion Sounds
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Motion Sounds",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Plays when the accelerometer detects pedaling or braking",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))

                    // Acceleration sound dropdown
                    Text("Acceleration", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    val accelSound = soundCatalog.find { it.id == settings.accelerationSoundId }
                    var accelExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = accelExpanded,
                        onExpandedChange = { accelExpanded = it },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TextField(
                            value = accelSound?.name ?: "Select sound",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(accelExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = accelExpanded,
                            onDismissRequest = { accelExpanded = false },
                        ) {
                            soundCatalog.forEach { sound ->
                                DropdownMenuItem(
                                    text = { Text(sound.name) },
                                    onClick = {
                                        onAccelerationSoundChange(sound.id)
                                        accelExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Braking sound dropdown
                    Text("Braking", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    val brakeSound = soundCatalog.find { it.id == settings.brakingSoundId }
                    var brakeExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = brakeExpanded,
                        onExpandedChange = { brakeExpanded = it },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TextField(
                            value = brakeSound?.name ?: "Select sound",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(brakeExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = brakeExpanded,
                            onDismissRequest = { brakeExpanded = false },
                        ) {
                            soundCatalog.forEach { sound ->
                                DropdownMenuItem(
                                    text = { Text(sound.name) },
                                    onClick = {
                                        onBrakingSoundChange(sound.id)
                                        brakeExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // About
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("About", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Bike Horn v1.0\n\n" +
                            "Smart horn & crash detection for cyclists.\n" +
                            "Connect your Puck.js to use as a wireless button.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
