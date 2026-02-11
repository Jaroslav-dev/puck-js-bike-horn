package com.bikehorn.app.ble

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.bikehorn.app.BikeHornApp
import com.bikehorn.app.MainActivity

class BleConnectionService : Service() {

    inner class LocalBinder : Binder() {
        val service: BleConnectionService get() = this@BleConnectionService
    }

    private val binder = LocalBinder()
    var bleManager: BleManager? = null
        private set

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        bleManager = BleManager(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        bleManager?.destroy()
        bleManager = null
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, BikeHornApp.BLE_CHANNEL_ID)
            .setContentTitle("Bike Horn")
            .setContentText("BLE connection active")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
