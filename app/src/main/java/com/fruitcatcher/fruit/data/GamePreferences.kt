package com.fruitcatcher.fruit.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.fruitCatcherStore by preferencesDataStore("orchard")
private val bestScore = longPreferencesKey("best_score")
private val music = booleanPreferencesKey("music")
private val effects = booleanPreferencesKey("sound_effects")

internal data class Preferences(val best: Long = 0, val music: Boolean = true, val effects: Boolean = true)

internal class GamePreferences(context: Context) {
    private val store = context.applicationContext.fruitCatcherStore

    val values = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { Preferences(it[bestScore] ?: 0, it[music] ?: true, it[effects] ?: true) }

    suspend fun saveRecord(score: Long) {
        store.edit { mutable ->
            if (score > (mutable[bestScore] ?: 0L)) mutable[bestScore] = score
        }
    }

    suspend fun setMusic(enabled: Boolean) {
        store.edit { it[music] = enabled }
    }

    suspend fun setEffects(enabled: Boolean) {
        store.edit { it[effects] = enabled }
    }
}
