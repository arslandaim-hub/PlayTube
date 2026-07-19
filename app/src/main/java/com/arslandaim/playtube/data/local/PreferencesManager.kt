/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.dataStore

    val isHistoryEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[HISTORY_ENABLED] ?: true
    }

    val isSearchHistoryPaused: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SEARCH_HISTORY_PAUSED] ?: false
    }

    val isPipEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PIP_ENABLED] ?: false
    }

    val isBackgroundPlayEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[BACKGROUND_PLAY_ENABLED] ?: false
    }

    val isSubtitlesEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SUBTITLES_ENABLED] ?: false
    }

    suspend fun setHistoryEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[HISTORY_ENABLED] = enabled
        }
    }

    suspend fun setSearchHistoryPaused(paused: Boolean) {
        dataStore.edit { preferences ->
            preferences[SEARCH_HISTORY_PAUSED] = paused
        }
    }

    suspend fun setPipEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PIP_ENABLED] = enabled
        }
    }

    suspend fun setBackgroundPlayEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[BACKGROUND_PLAY_ENABLED] = enabled
        }
    }

    suspend fun setSubtitlesEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SUBTITLES_ENABLED] = enabled
        }
    }

    companion object {
        val HISTORY_ENABLED = booleanPreferencesKey("history_enabled")
        val SEARCH_HISTORY_PAUSED = booleanPreferencesKey("search_history_paused")
        val PIP_ENABLED = booleanPreferencesKey("pip_enabled")
        val BACKGROUND_PLAY_ENABLED = booleanPreferencesKey("background_play_enabled")
        val SUBTITLES_ENABLED = booleanPreferencesKey("subtitles_enabled")
    }
}
