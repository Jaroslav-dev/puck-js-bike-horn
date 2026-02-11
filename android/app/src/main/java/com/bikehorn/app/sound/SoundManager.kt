package com.bikehorn.app.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import com.bikehorn.app.data.BUNDLED_SOUNDS
import com.bikehorn.app.data.BundledSound

class SoundManager(private val context: Context) {

    private val soundPool: SoundPool
    private val loadedSounds = mutableMapOf<Int, Int>() // bundledSoundId -> soundPoolId

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(attrs)
            .build()

        // Preload all bundled sounds
        for (sound in BUNDLED_SOUNDS) {
            val poolId = soundPool.load(context, sound.resId, 1)
            loadedSounds[sound.id] = poolId
        }
    }

    fun play(soundId: Int) {
        val poolId = loadedSounds[soundId] ?: return
        // Play at max volume, normal rate
        soundPool.play(poolId, 1.0f, 1.0f, 1, 0, 1.0f)
    }

    fun playAlarm(soundId: Int): Int {
        val poolId = loadedSounds[soundId] ?: return 0
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
            0
        )
        // Loop until stopped
        return soundPool.play(poolId, 1.0f, 1.0f, 1, -1, 1.0f)
    }

    fun stopStream(streamId: Int) {
        if (streamId != 0) {
            soundPool.stop(streamId)
        }
    }

    fun getSoundCatalog(): List<BundledSound> = BUNDLED_SOUNDS

    fun release() {
        soundPool.release()
    }
}
