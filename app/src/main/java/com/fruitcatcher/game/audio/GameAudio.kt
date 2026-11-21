package com.fruitcatcher.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import com.fruitcatcher.game.R

internal enum class Cue(val resource: Int) {
    TAP(R.raw.button_tap), START(R.raw.start_game), FRUIT(R.raw.catch_fruit), GOLD(R.raw.catch_gold),
    BOMB(R.raw.bomb_hit), HURT(R.raw.heart_loss), RECORD(R.raw.record_score), OVER(R.raw.game_over)
}

internal class GameAudio(private val context: Context) {
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
    private val manager = context.getSystemService(AudioManager::class.java)
    private val pool = SoundPool.Builder().setMaxStreams(5).setAudioAttributes(attributes).build()
    private val samples = IntArray(Cue.entries.size) { pool.load(context, Cue.entries[it].resource, 1) }
    private var player: MediaPlayer? = null
    private var track = 0
    private var trackAudible = true
    private var closed = false
    private var foreground = false
    private var canPlay = false
    private var requestActive = false
    private var music = true
    private var effects = true
    private val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes)
        .setAcceptsDelayedFocusGain(true)
        .setWillPauseWhenDucked(true)
        .setOnAudioFocusChangeListener { event ->
            if (!closed) {
                canPlay = event == AudioManager.AUDIOFOCUS_GAIN
                if (!canPlay) stopEffects()
                if (event == AudioManager.AUDIOFOCUS_LOSS) requestActive = false
                refresh()
            }
        }.build()

    private val streams = IntArray(8)
    private var nextStream = 0

    fun foreground(active: Boolean) {
        foreground = active
        updateFocus()
        refresh()
    }

    fun settings(musicEnabled: Boolean, effectsEnabled: Boolean) {
        music = musicEnabled
        effects = effectsEnabled
        if (!effects) stopEffects()
        updateFocus()
        refresh()
    }

    fun track(resource: Int, audible: Boolean = true) {
        if (track != resource) {
            player?.release()
            player = null
            track = resource
        }
        trackAudible = audible
        refresh()
    }

    fun play(cue: Cue) {
        if (foreground && effects && canPlay) {
            val gain = if (cue == Cue.HURT) 0.35f else 0.7f
            streams[nextStream] = pool.play(samples[cue.ordinal], gain, gain, 1, 0, 1f)
            nextStream = (nextStream + 1) % streams.size
        }
    }

    private fun updateFocus() {
        if (foreground && (music || effects)) {
            if (!requestActive) {
                val result = manager.requestAudioFocus(request)
                canPlay = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                requestActive = result != AudioManager.AUDIOFOCUS_REQUEST_FAILED
            }
        } else {
            if (requestActive) manager.abandonAudioFocusRequest(request)
            requestActive = false
            canPlay = false
            stopEffects()
        }
    }

    private fun stopEffects() {
        for (index in streams.indices) {
            if (streams[index] != 0) pool.stop(streams[index])
            streams[index] = 0
        }
    }

    private fun refresh() {
        if (foreground && canPlay && music && trackAudible && track != 0) {
            val current = player ?: MediaPlayer.create(context, track, attributes, AudioManager.AUDIO_SESSION_ID_GENERATE)
                ?.apply {
                    isLooping = true
                    setVolume(0.55f, 0.55f)
                    player = this
                }
            current?.start()
        } else {
            player?.takeIf { it.isPlaying }?.pause()
        }
    }

    fun release() {
        closed = true
        if (requestActive) manager.abandonAudioFocusRequest(request)
        player?.release()
        player = null
        pool.release()
    }
}
