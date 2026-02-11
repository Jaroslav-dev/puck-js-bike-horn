package com.bikehorn.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class BikeHornApp : Application() {

    companion object {
        const val BLE_CHANNEL_ID = "ble_connection"
        const val CRASH_CHANNEL_ID = "crash_alert"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val bleChannel = NotificationChannel(
            BLE_CHANNEL_ID,
            "BLE Connection",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Maintains Bluetooth connection to Puck.js"
        }

        val crashChannel = NotificationChannel(
            CRASH_CHANNEL_ID,
            "Crash Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Emergency crash detection alerts"
        }

        manager.createNotificationChannel(bleChannel)
        manager.createNotificationChannel(crashChannel)
    }
}
