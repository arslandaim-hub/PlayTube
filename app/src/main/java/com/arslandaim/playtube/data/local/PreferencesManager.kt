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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
open class PreferencesManager @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.dataStore

    open val isHistoryEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[HISTORY_ENABLED] ?: true
        }

    open val isSearchHistoryPaused: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[SEARCH_HISTORY_PAUSED] ?: false
        }

    open val isPipEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PIP_ENABLED] ?: false
        }

    open val isBackgroundPlayEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[BACKGROUND_PLAY_ENABLED] ?: false
        }

    open val isSubtitlesEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[SUBTITLES_ENABLED] ?: false
        }

    open val isOnboardingCompleted: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[ONBOARDING_COMPLETED] ?: false
        }

    open val isSearchGridView: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[SEARCH_GRID_VIEW] ?: false
        }

    open val isAutoUpdateEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[AUTO_UPDATE_ENABLED] ?: false
        }

    open val isRecommendationsPaused: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[RECOMMENDATIONS_PAUSED] ?: false
        }

    open val isAutoplayEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[AUTOPLAY_ENABLED] ?: true
        }

    open val isIncognitoMode: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[INCOGNITO_MODE] ?: false
        }

    open val preferredSubtitleLanguage: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PREFERRED_SUBTITLE_LANGUAGE]
        }

    open val preferredQuality: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PREFERRED_QUALITY] ?: "Auto"
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

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setSearchGridView(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SEARCH_GRID_VIEW] = enabled
        }
    }

    suspend fun setAutoUpdateEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_UPDATE_ENABLED] = enabled
        }
    }

    suspend fun setRecommendationsPaused(paused: Boolean) {
        dataStore.edit { preferences ->
            preferences[RECOMMENDATIONS_PAUSED] = paused
        }
    }

    suspend fun setAutoplayEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTOPLAY_ENABLED] = enabled
        }
    }

    suspend fun setIncognitoMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[INCOGNITO_MODE] = enabled
        }
    }

    suspend fun setPreferredSubtitleLanguage(language: String?) {
        dataStore.edit { preferences ->
            if (language == null) preferences.remove(PREFERRED_SUBTITLE_LANGUAGE)
            else preferences[PREFERRED_SUBTITLE_LANGUAGE] = language
        }
    }

    suspend fun setPreferredQuality(quality: String) {
        dataStore.edit { preferences ->
            preferences[PREFERRED_QUALITY] = quality
        }
    }

    companion object {
        val HISTORY_ENABLED = booleanPreferencesKey("history_enabled")
        val SEARCH_HISTORY_PAUSED = booleanPreferencesKey("search_history_paused")
        val PIP_ENABLED = booleanPreferencesKey("pip_enabled")
        val BACKGROUND_PLAY_ENABLED = booleanPreferencesKey("background_play_enabled")
        val SUBTITLES_ENABLED = booleanPreferencesKey("subtitles_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SEARCH_GRID_VIEW = booleanPreferencesKey("search_grid_view")
        val AUTO_UPDATE_ENABLED = booleanPreferencesKey("auto_update_enabled")
        val RECOMMENDATIONS_PAUSED = booleanPreferencesKey("recommendations_paused")
        val AUTOPLAY_ENABLED = booleanPreferencesKey("autoplay_enabled")
        val INCOGNITO_MODE = booleanPreferencesKey("incognito_mode")
        val PREFERRED_SUBTITLE_LANGUAGE = stringPreferencesKey("preferred_subtitle_language")
        val PREFERRED_QUALITY = stringPreferencesKey("preferred_quality")
    }
}
