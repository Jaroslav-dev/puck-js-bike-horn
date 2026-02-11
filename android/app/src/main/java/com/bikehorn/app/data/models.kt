package com.bikehorn.app.data

import com.bikehorn.app.R

enum class ButtonPattern(val label: String) {
    SHORT_PRESS("Short Press"),
    LONG_PRESS("Long Press"),
    VERY_LONG_PRESS("Very Long Press"),
    REPEATED_PRESS("Repeated Press");

    companion object {
        fun fromEventType(type: String): ButtonPattern? = when (type) {
            "short_press" -> SHORT_PRESS
            "long_press" -> LONG_PRESS
            "very_long_press" -> VERY_LONG_PRESS
            "repeated_press" -> REPEATED_PRESS
            else -> null
        }
    }
}

data class BundledSound(
    val id: Int,
    val name: String,
    val resId: Int
)

val BUNDLED_SOUNDS = listOf(
    BundledSound(1, "Bell Ding", R.raw.bell_ding),
    BundledSound(2, "Horn Short", R.raw.horn_short),
    BundledSound(3, "Horn Loud", R.raw.horn_loud),
    BundledSound(4, "Alarm", R.raw.alarm),
    BundledSound(5, "Bicycle Bell", R.raw.bicycle_bell),
    BundledSound(6, "Air Horn", R.raw.air_horn),
)

data class SoundAssignment(
    val pattern: ButtonPattern,
    val soundId: Int
)

data class AppSettings(
    val assignments: Map<ButtonPattern, Int> = mapOf(
        ButtonPattern.SHORT_PRESS to 1,
        ButtonPattern.LONG_PRESS to 2,
        ButtonPattern.VERY_LONG_PRESS to 3,
        ButtonPattern.REPEATED_PRESS to 6,
    ),
    val emergencyContact: String = "",
    val crashThreshold: Float = 3.0f,
    val countdownDuration: Int = 10,
)
