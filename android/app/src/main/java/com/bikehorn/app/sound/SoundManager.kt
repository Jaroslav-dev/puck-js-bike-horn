package com.bikehorn.app.sound

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.SoundPool
import android.net.Uri
import com.bikehorn.app.data.BUNDLED_SOUNDS
import com.bikehorn.app.data.BundledSound
import com.bikehorn.app.data.CustomSound
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SoundManager(private val context: Context) {

    private val soundPool: SoundPool
    private val loadedSounds = mutableMapOf<Int, Int>() // ourSoundId -> soundPoolId

    // Sound durations in milliseconds, populated as sounds are loaded.
    // Exposed as a StateFlow so the UI can reactively show durations once they're known.
    private val _durations = MutableStateFlow<Map<Int, Long>>(emptyMap())
    val durations: StateFlow<Map<Int, Long>> = _durations

    // Keeps AssetFileDescriptors open while SoundPool loads them asynchronously.
    // SoundPool.load() is async — closing the AFD too early lets the OS reuse the same
    // file descriptor number for the next open(), causing SoundPool to read the wrong file.
    private val pendingAfds = mutableMapOf<Int, AssetFileDescriptor>() // soundPoolId -> afd

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(attrs)
            .build()

        // Close each pending AFD once SoundPool has finished decoding that sound
        soundPool.setOnLoadCompleteListener { _, sampleId, _ ->
            pendingAfds.remove(sampleId)?.close()
        }

        // Preload all bundled sounds and read their durations
        for (sound in BUNDLED_SOUNDS) {
            val poolId = soundPool.load(context, sound.resId, 1)
            loadedSounds[sound.id] = poolId
            // Open the raw resource as a file descriptor for MediaMetadataRetriever
            context.resources.openRawResourceFd(sound.resId)?.use { fd ->
                extractDuration(sound.id) {
                    setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                }
            }
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

    /**
     * Loads a single user-picked sound file into SoundPool.
     * The URI must already have a persistent read permission granted by the system.
     * Returns true on success; false if the file cannot be opened.
     */
    fun loadCustomSound(id: Int, uri: Uri): Boolean {
        if (loadedSounds.containsKey(id)) return true  // already loaded
        return try {
            val afd = context.contentResolver.openAssetFileDescriptor(uri, "r") ?: return false
            val poolId = soundPool.load(afd, 1)
            // Keep afd open — the OnLoadCompleteListener will close it once decoding finishes.
            // Closing here would free the file descriptor number for reuse, causing SoundPool
            // to decode the wrong file if another sound is loaded immediately after.
            pendingAfds[poolId] = afd
            loadedSounds[id] = poolId
            extractDuration(id) { setDataSource(context, uri) }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Loads all custom sounds from the persisted list, skipping any that are already loaded.
     * Called at startup after settings are read from DataStore.
     */
    fun loadCustomSounds(sounds: List<CustomSound>) {
        sounds.forEach { sound ->
            loadCustomSound(sound.id, Uri.parse(sound.uri))
        }
    }

    /**
     * Runs [configure] on a MediaMetadataRetriever to set its data source, extracts the
     * duration, stores it, then releases the retriever. Silently ignores any errors.
     */
    private fun extractDuration(id: Int, configure: MediaMetadataRetriever.() -> Unit) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.configure()
            val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
            retriever.release()
            if (ms != null) _durations.value = _durations.value + (id to ms)
        } catch (_: Exception) { }
    }

    fun release() {
        // Close any AFDs that haven't been closed yet (sounds still decoding on shutdown)
        pendingAfds.values.forEach { it.close() }
        pendingAfds.clear()
        soundPool.release()
    }
}
