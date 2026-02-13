package com.bikehorn.app

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.bikehorn.app.ble.BleConnectionService
import com.bikehorn.app.navigation.NavGraph
import com.bikehorn.app.ui.screens.CrashAlertScreen
import com.bikehorn.app.ui.theme.BikeHornTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as BleConnectionService.LocalBinder).service
            service.bleManager?.let { viewModel.attachBleManager(it) }
            serviceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
        }
    }

    private fun startBleService() {
        if (serviceBound) return
        val serviceIntent = Intent(this, BleConnectionService::class.java)
        startForegroundService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BikeHornTheme {
                val permissions = buildList {
                    add(Manifest.permission.BLUETOOTH_SCAN)
                    add(Manifest.permission.BLUETOOTH_CONNECT)
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val permissionState = rememberMultiplePermissionsState(permissions)

                if (!permissionState.allPermissionsGranted) {
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        permissionState.launchMultiplePermissionRequest()
                    }
                }

                // Start BLE service only after permissions are granted
                if (permissionState.allPermissionsGranted) {
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        startBleService()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()

                    Box(modifier = Modifier.fillMaxSize()) {
                        NavGraph(
                            navController = navController,
                            viewModel = viewModel,
                        )

                        // Crash alert overlay
                        val crashState by viewModel.crashAlertState.collectAsState()
                        val settings by viewModel.settings.collectAsState()

                        if (crashState.active) {
                            CrashAlertScreen(
                                state = crashState,
                                emergencyContact = settings.emergencyContact,
                                onCancel = viewModel::cancelCrashAlert,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
        super.onDestroy()
    }
}
