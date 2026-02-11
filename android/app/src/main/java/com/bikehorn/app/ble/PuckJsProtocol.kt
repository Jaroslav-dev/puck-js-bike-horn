package com.bikehorn.app.ble

import org.json.JSONObject

sealed class PuckEvent {
    data object ShortPress : PuckEvent()
    data object LongPress : PuckEvent()
    data object VeryLongPress : PuckEvent()
    data object RepeatedPress : PuckEvent()
    data class Crash(val magnitude: Float) : PuckEvent()
}

object PuckJsProtocol {

    fun parse(message: String): PuckEvent? {
        return try {
            val json = JSONObject(message.trim())
            when (json.optString("t")) {
                "short_press" -> PuckEvent.ShortPress
                "long_press" -> PuckEvent.LongPress
                "very_long_press" -> PuckEvent.VeryLongPress
                "repeated_press" -> PuckEvent.RepeatedPress
                "crash" -> PuckEvent.Crash(json.optDouble("d", 0.0).toFloat())
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}
