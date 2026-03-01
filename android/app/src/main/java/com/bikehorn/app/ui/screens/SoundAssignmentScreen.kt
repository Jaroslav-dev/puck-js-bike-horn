package com.bikehorn.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bikehorn.app.data.ButtonPattern
import com.bikehorn.app.data.CustomSound
import com.bikehorn.app.data.MEDIA_ACTIONS
import com.bikehorn.app.data.SoundOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundAssignmentScreen(
    assignments: Map<ButtonPattern, Int>,
    soundCatalog: List<SoundOption>,       // bundled + custom sounds combined
    soundDurations: Map<Int, Long>,        // soundId -> duration in ms
    customSounds: List<CustomSound>,        // custom-only list, for management UI
    onAssign: (ButtonPattern, Int) -> Unit,
    onPreview: (Int) -> Unit,
    onAddCustomSound: (Uri) -> Unit,
    onRemoveCustomSound: (Int) -> Unit,
    onRenameCustomSound: (Int, String) -> Unit,
    onBack: () -> Unit,
) {
    // Formats a duration in ms to a compact string: "0.8s", "3.2s", "1:05"
    fun formatDuration(ms: Long): String = when {
        ms < 60_000 -> "%.1fs".format(ms / 1000.0)
        else -> "%d:%02d".format(ms / 60_000, (ms % 60_000) / 1000)
    }

    // Tracks which custom sound is currently being renamed (null = dialog hidden)
    var renamingSound by remember { mutableStateOf<CustomSound?>(null) }
    var renameText by remember { mutableStateOf("") }

    // Rename dialog — shown when the user taps the edit icon on a custom sound
    renamingSound?.let { sound ->
        AlertDialog(
            onDismissRequest = { renamingSound = null },
            title = { Text("Rename Sound") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            onRenameCustomSound(sound.id, renameText)
                        }
                        renamingSound = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renamingSound = null }) { Text("Cancel") }
            },
        )
    }

    // OpenDocument (SAF) instead of GetContent — SAF URIs always support
    // takePersistableUriPermission, which is required for reading the file after app restarts.
    // GetContent URIs from Downloads/browsers do not support persistable permissions and crash.
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onAddCustomSound(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sound Assignments") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // One card per button pattern
            items(ButtonPattern.entries) { pattern ->
                val currentSoundId = assignments[pattern] ?: 1
                val currentSound = soundCatalog.find { it.id == currentSoundId }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = pattern.label,
                            style = MaterialTheme.typography.titleMedium,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            var expanded by remember { mutableStateOf(false) }

                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it },
                                modifier = Modifier.weight(1f),
                            ) {
                                TextField(
                                    value = currentSound?.name ?: "Select sound",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                                    },
                                    modifier = Modifier.menuAnchor(),
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                ) {
                                    // Show all sounds (bundled + custom) in the dropdown
                                    soundCatalog.forEach { sound ->
                                        DropdownMenuItem(
                                            text = { Text(sound.name) },
                                            trailingIcon = {
                                                val ms = soundDurations[sound.id]
                                                if (ms != null) {
                                                    Text(
                                                        text = formatDuration(ms),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                            },
                                            onClick = {
                                                onAssign(pattern, sound.id)
                                                expanded = false
                                            },
                                        )
                                    }
                                }
                            }

                            // Media action IDs are negative — nothing to preview
                            if (currentSoundId >= 0) {
                                IconButton(onClick = { onPreview(currentSoundId) }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Preview")
                                }
                            }
                        }
                    }
                }
            }

            // Custom sounds management card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Custom Sounds", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Add your own audio files from device storage",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))

                        if (customSounds.isEmpty()) {
                            Text(
                                "No custom sounds added yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                        } else {
                            // Row for each custom sound: name, preview, rename, delete
                            customSounds.forEach { sound ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = sound.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        val ms = soundDurations[sound.id]
                                        if (ms != null) {
                                            Text(
                                                text = formatDuration(ms),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    IconButton(onClick = { onPreview(sound.id) }) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Preview ${sound.name}")
                                    }
                                    IconButton(onClick = {
                                        renamingSound = sound
                                        renameText = sound.name
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Rename ${sound.name}")
                                    }
                                    IconButton(onClick = { onRemoveCustomSound(sound.id) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove ${sound.name}",
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        // File picker button — launches the system audio file browser
                        Button(
                            onClick = { filePicker.launch(arrayOf("audio/*")) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.padding(4.dp))
                            Text("Add Custom Sound")
                        }
                    }
                }
            }

            // Music playback control info card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Music Playback Control",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "You can control your music player directly from the Puck.js button. " +
                            "Select one of the media actions from any button's dropdown above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        // List the available media actions so the user knows what to look for
                        MEDIA_ACTIONS.forEach { action ->
                            Text(
                                text = "• ${action.name}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Horn sounds automatically pause music while playing, then resume it " +
                            "when the sound finishes — no extra setup needed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
