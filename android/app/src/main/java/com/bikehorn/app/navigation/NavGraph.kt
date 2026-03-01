package com.bikehorn.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.bikehorn.app.MainViewModel
import com.bikehorn.app.ui.screens.HomeScreen
import com.bikehorn.app.ui.screens.SettingsScreen
import com.bikehorn.app.ui.screens.SoundAssignmentScreen

object Routes {
    const val HOME = "home"
    const val SOUNDS = "sounds"
    const val SETTINGS = "settings"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: MainViewModel,
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val deviceName by viewModel.connectedDeviceName.collectAsState()
    val lastEvent by viewModel.lastEvent.collectAsState()
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val settings by viewModel.settings.collectAsState()
    // Combined bundled + custom sound list for dropdowns
    val soundCatalog by viewModel.soundCatalog.collectAsState()
    // Duration of each sound in ms (populated once SoundPool finishes loading each sound)
    val soundDurations by viewModel.soundDurations.collectAsState()
    // Name of connected BT speaker, or null if audio routes to phone speaker
    val bluetoothSpeakerName by viewModel.bluetoothSpeakerName.collectAsState()
    // Reactive Bluetooth on/off state
    val isBluetoothEnabled by viewModel.isBluetoothEnabled.collectAsState()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                connectionState = connectionState,
                deviceName = deviceName,
                lastEvent = lastEvent,
                scannedDevices = scannedDevices,
                bluetoothSpeakerName = bluetoothSpeakerName,
                isBluetoothEnabled = isBluetoothEnabled,
                onScan = viewModel::startScan,
                onConnect = { viewModel.connect(it.device) },
                onDisconnect = viewModel::disconnect,
                onNavigateToSounds = { navController.navigate(Routes.SOUNDS) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.SOUNDS) {
            SoundAssignmentScreen(
                assignments = settings.assignments,
                soundCatalog = soundCatalog,
                soundDurations = soundDurations,
                customSounds = settings.customSounds,
                onAssign = viewModel::assignSound,
                onPreview = viewModel::previewSound,
                onAddCustomSound = viewModel::addCustomSound,
                onRemoveCustomSound = viewModel::removeCustomSound,
                onRenameCustomSound = viewModel::renameCustomSound,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                settings = settings,
                soundCatalog = soundCatalog,
                onEmergencyContactChange = viewModel::setEmergencyContact,
                onCrashThresholdChange = viewModel::setCrashThreshold,
                onCountdownDurationChange = viewModel::setCountdownDuration,
                onAccelerationSoundChange = viewModel::setAccelerationSound,
                onBrakingSoundChange = viewModel::setBrakingSound,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
