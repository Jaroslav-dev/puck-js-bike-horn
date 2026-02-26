package com.bikehorn.app.ble

import org.json.JSONObject

sealed class PuckEvent {
    data object SinglePress : PuckEvent()
    data object DoublePress : PuckEvent()
    data object TriplePress : PuckEvent()
    data object LongPress : PuckEvent()
    data class Crash(val magnitude: Float) : PuckEvent()
    // Motion events from accelerometer delta detection (Puck.js v2 only)
    data object Acceleration : PuckEvent()
    data object Braking : PuckEvent()
}

object PuckJsProtocol {

    fun parse(message: String): PuckEvent? {
        return try {
            val json = JSONObject(message.trim())
            when (json.optString("t")) {
                "single_press" -> PuckEvent.SinglePress
                "double_press" -> PuckEvent.DoublePress
                "triple_press" -> PuckEvent.TriplePress
                "long_press" -> PuckEvent.LongPress
                "crash" -> PuckEvent.Crash(json.optDouble("d", 0.0).toFloat())
                "acceleration" -> PuckEvent.Acceleration
                "braking" -> PuckEvent.Braking
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}
