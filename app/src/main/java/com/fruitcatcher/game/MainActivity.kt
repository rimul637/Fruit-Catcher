package com.fruitcatcher.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.fruitcatcher.game.audio.Cue
import com.fruitcatcher.game.audio.GameAudio
import com.fruitcatcher.game.data.GamePreferences
import com.fruitcatcher.game.data.Preferences
import com.fruitcatcher.game.game.Drop
import com.fruitcatcher.game.game.GameEngine
import com.fruitcatcher.game.game.SceneArt
import com.fruitcatcher.game.ui.FruitCatcher
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class Screen { SPLASH, MENU, BEST, SETTINGS, PLAY, PAUSE, GAME_OVER }

class MainActivity : ComponentActivity() {
    private lateinit var audio: GameAudio
    private lateinit var preferences: GamePreferences
    private var foreground by mutableStateOf(false)
    private var art by mutableStateOf<SceneArt?>(null)
    internal var screen by mutableStateOf(Screen.SPLASH)
        private set
    internal var settingsFrom by mutableStateOf(Screen.MENU)
        private set
    internal var currentScore by mutableStateOf(0L)
        private set
    internal var best by mutableStateOf(0L)
        private set
    internal var hearts by mutableStateOf(3)
        private set
    internal var newRecord by mutableStateOf(false)
        private set
    internal var options by mutableStateOf(Preferences())
        private set
    private var bestAtStart = 0L
    internal val engine = GameEngine(onCatch = ::collected)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        preferences = GamePreferences(this)
        audio = GameAudio(this)
        setContent { FruitCatcher(this, art, foreground) }
        lifecycleScope.launch {
            preferences.values.collect { saved ->
                options = saved
                best = maxOf(saved.best, best)
                audio.settings(saved.music, saved.effects)
            }
        }
        lifecycleScope.launch {
            art = withContext(Dispatchers.Default) { SceneArt(applicationContext) }
            delay(920)
            open(Screen.MENU)
        }
    }

    override fun onResume() {
        super.onResume()
        foreground = true
        audio.foreground(true)
    }

    override fun onPause() {
        foreground = false
        audio.foreground(false)
        if (screen == Screen.PLAY) open(Screen.PAUSE)
        super.onPause()
    }

    override fun onDestroy() {
        audio.release()
        super.onDestroy()
    }

    internal fun start() {
        engine.restart()
        bestAtStart = best
        currentScore = 0
        hearts = 3
        newRecord = false
        audio.play(Cue.START)
        open(Screen.PLAY)
    }

    internal fun navigate(destination: Screen) {
        audio.play(Cue.TAP)
        if (destination == Screen.SETTINGS) settingsFrom = screen
        if (destination == Screen.PLAY) engine.resume()
        open(destination)
    }

    internal fun back() {
        when (screen) {
            Screen.SPLASH, Screen.MENU -> finish()
            Screen.BEST, Screen.GAME_OVER -> navigate(Screen.MENU)
            Screen.SETTINGS -> navigate(settingsFrom)
            Screen.PLAY -> navigate(Screen.PAUSE)
            Screen.PAUSE -> navigate(Screen.PLAY)
        }
    }

    internal fun toggleMusic() = changePreference(true, !options.music)

    internal fun toggleEffects() = changePreference(false, !options.effects)

    private fun changePreference(isMusic: Boolean, enabled: Boolean) {
        audio.play(Cue.TAP)
        options = if (isMusic) options.copy(music = enabled) else options.copy(effects = enabled)
        audio.settings(options.music, options.effects)
        lifecycleScope.launch {
            try {
                if (isMusic) preferences.setMusic(enabled) else preferences.setEffects(enabled)
            } catch (_: IOException) {
                android.widget.Toast.makeText(this@MainActivity, "Couldn’t save audio settings.",
                    android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun collected(type: Drop) {
        currentScore = engine.score
        hearts = engine.hearts
        when (type) {
            Drop.BOMB -> {
                audio.play(Cue.BOMB)
                audio.play(Cue.HURT)
            }
            Drop.GOLDEN -> audio.play(Cue.GOLD)
            else -> audio.play(Cue.FRUIT)
        }
        if (currentScore > best) {
            val score = currentScore
            best = score
            lifecycleScope.launch {
                try {
                    preferences.saveRecord(score)
                } catch (_: IOException) {
                    // Try again at game over if storage was temporarily unavailable.
                }
            }
        }
        if (hearts == 0) {
            newRecord = currentScore > bestAtStart
            audio.play(if (newRecord) Cue.RECORD else Cue.OVER)
            if (newRecord) {
                val finalScore = currentScore
                lifecycleScope.launch {
                    try {
                        preferences.saveRecord(finalScore)
                    } catch (_: IOException) {
                        android.widget.Toast.makeText(this@MainActivity, "Couldn’t save the best score.",
                            android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            open(Screen.GAME_OVER)
        }
    }

    private fun open(destination: Screen) {
        if (destination != Screen.PLAY) engine.pause()
        screen = destination
        val fromPause = destination == Screen.PAUSE ||
            (destination == Screen.SETTINGS && settingsFrom == Screen.PAUSE)
        val track = when {
            destination == Screen.SPLASH || destination == Screen.GAME_OVER -> 0
            destination == Screen.PLAY || fromPause -> R.raw.music_game
            else -> R.raw.music_menu
        }
        audio.track(track, audible = !fromPause)
    }
}
