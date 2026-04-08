package com.bikehorn.app.data

import com.bikehorn.app.R

enum class ButtonPattern(val label: String) {
    SINGLE_PRESS("1 Tap"),
    DOUBLE_PRESS("2 Taps"),
    TRIPLE_PRESS("3 Taps"),
    LONG_PRESS("Long Press (2s)");

    companion object {
        fun fromEventType(type: String): ButtonPattern? = when (type) {
            "single_press" -> SINGLE_PRESS
            "double_press" -> DOUBLE_PRESS
            "triple_press" -> TRIPLE_PRESS
            "long_press" -> LONG_PRESS
            else -> null
        }
    }
}

data class BundledSound(
    val id: Int,
    val name: String,
    val resId: Int
)

/** A sound picked by the user from device storage. IDs start at 101. */
data class CustomSound(
    val id: Int,
    val name: String,
    val uri: String,  // persisted content:// URI string
)

/** Unified type used in dropdowns — covers bundled sounds, custom sounds, and media actions. */
data class SoundOption(val id: Int, val name: String)

val BUNDLED_SOUNDS = listOf(
    BundledSound(1, "Bicycle Bell", R.raw.bicycle_bell),
    BundledSound(2, "Tram Bell", R.raw.tram_bell),
    BundledSound(3, "Police Siren", R.raw.police),
    BundledSound(4, "UFO", R.raw.ufo),
    // 750Hz sine tone — this frequency penetrates noise-cancelling headphones,
    // making it audible to pedestrians even with ANC earbuds at full volume.
    BundledSound(5, "750Hz Pedestrian Bell", R.raw.bell_750hz),
)

// Reserved negative IDs for media player control — never clash with real sound IDs (which are ≥ 1)
const val MEDIA_ACTION_NEXT       = -1
const val MEDIA_ACTION_PREVIOUS   = -2
const val MEDIA_ACTION_PLAY_PAUSE = -3

/**
 * Media control actions that can be assigned to a button pattern in place of a sound.
 * Dispatched as standard Android media key events, so they work with Spotify, YouTube Music,
 * the stock player, or any app that registers a media session.
 */
val MEDIA_ACTIONS = listOf(
    SoundOption(MEDIA_ACTION_NEXT,       "Next Track"),
    SoundOption(MEDIA_ACTION_PREVIOUS,   "Previous Track"),
    SoundOption(MEDIA_ACTION_PLAY_PAUSE, "Play / Pause"),
)

data class SoundAssignment(
    val pattern: ButtonPattern,
    val soundId: Int
)

data class AppSettings(
    val assignments: Map<ButtonPattern, Int> = mapOf(
        ButtonPattern.SINGLE_PRESS to 1,
        ButtonPattern.DOUBLE_PRESS to 2,
        ButtonPattern.TRIPLE_PRESS to 3,
        ButtonPattern.LONG_PRESS to 4,
    ),
    val emergencyContact: String = "",
    val crashThreshold: Float = 3.0f,
    val countdownDuration: Int = 10,
    // Sounds played when the accelerometer detects forward acceleration or braking
    val accelerationSoundId: Int = 1,  // default: Bicycle Bell
    val brakingSoundId: Int = 2,       // default: Tram Bell
    // User-uploaded sounds from device storage
    val customSounds: List<CustomSound> = emptyList(),
)
