package com.bikehorn.app

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
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
import com.bikehorn.app.ble.ConnectionState
import com.bikehorn.app.navigation.NavGraph
import com.bikehorn.app.ui.screens.CrashAlertScreen
import com.bikehorn.app.ui.theme.BikeHornTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var serviceBound = false
    // Null on devices/emulators with no NFC hardware — all NFC calls are guarded with ?.
    private var nfcAdapter: NfcAdapter? = null

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
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        // Handle NFC deep link when app was closed (cold launch via bikehorn://pair)
        handleNfcPairIntent(intent)

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

    override fun onResume() {
        super.onResume()
        // Enable NFC reader mode to suppress the system "New tag" popup while the app is
        // in the foreground. All tag reads are delivered to handleNfcTag() instead.
        nfcAdapter?.enableReaderMode(
            this,
            ::handleNfcTag,
            NfcAdapter.FLAG_READER_NFC_A,  // Puck.js uses NFC-A (nRF52 ISO 14443-A)
            null,
        )
    }

    override fun onPause() {
        super.onPause()
        // Release reader mode so normal NFC dispatch resumes when app is backgrounded.
        // The ACTION_VIEW intent filter handles cold-launch taps while we're in the background.
        nfcAdapter?.disableReaderMode(this)
    }

    /** Called by the OS when a new intent arrives while the activity is already on top (singleTop). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle NFC deep link when app was already open (bikehorn://pair arrives as new intent)
        handleNfcPairIntent(intent)
    }

    /**
     * Called by NFC reader mode (foreground) with the raw Tag from the Puck.js.
     * Runs on a background thread — UI/ViewModel calls are dispatched to the main thread.
     *
     * Behaviour:
     * - Already connected → silently ignore (suppresses the system popup with no action)
     * - Not connected     → extract the MAC from the NDEF URL and connect
     */
    private fun handleNfcTag(tag: Tag) {
        if (viewModel.connectionState.value == ConnectionState.CONNECTED) return
        val message = Ndef.get(tag)?.cachedNdefMessage ?: return
        for (record in message.records) {
            val uri = record.toUri() ?: continue
            if (uri.scheme == "bikehorn" && uri.host == "pair") {
                val addr = uri.getQueryParameter("addr") ?: continue
                // connectByAddress accesses ViewModel state — must run on the main thread
                runOnUiThread { viewModel.connectByAddress(addr) }
                return
            }
        }
    }

    /**
     * Extracts the BLE MAC address from a bikehorn://pair?addr=XX:XX:XX:XX:XX:XX deep link
     * and hands it to the ViewModel to initiate a direct connection (no BLE scan needed).
     * Called from both onCreate (cold launch) and onNewIntent (foreground tap).
     */
    private fun handleNfcPairIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "bikehorn" && data.host == "pair") {
            val addr = data.getQueryParameter("addr") ?: return
            viewModel.connectByAddress(addr)
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
