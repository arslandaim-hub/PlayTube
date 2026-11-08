/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arslandaim.playtube.data.local.PreferencesManager
import com.arslandaim.playtube.domain.repository.DownloadRepository
import com.arslandaim.playtube.domain.repository.LibraryRepository
import com.arslandaim.playtube.utils.LocaleUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChannelStat(
    val channelName: String,
    val count: Int,
    val percentage: Float
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val downloadRepository: DownloadRepository,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    val isHistoryEnabled: StateFlow<Boolean> = preferencesManager.isHistoryEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isSearchHistoryPaused: StateFlow<Boolean> = preferencesManager.isSearchHistoryPaused
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isPipEnabled: StateFlow<Boolean> = preferencesManager.isPipEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isBackgroundPlayEnabled: StateFlow<Boolean> = preferencesManager.isBackgroundPlayEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isAutoUpdateEnabled: StateFlow<Boolean> = preferencesManager.isAutoUpdateEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isDynamicColorEnabled: StateFlow<Boolean> = preferencesManager.isDynamicColorEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isBlockAiContentEnabled: StateFlow<Boolean> = preferencesManager.isBlockAiContentEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val appThemeColor: StateFlow<String> = preferencesManager.appThemeColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "RED")

    val appThemeMode: StateFlow<String> = preferencesManager.appThemeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val isPlayerGesturesEnabled: StateFlow<Boolean> = preferencesManager.isPlayerGesturesEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isRecommendationsPaused: StateFlow<Boolean> = preferencesManager.isRecommendationsPaused
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val userInterests: StateFlow<List<com.arslandaim.playtube.data.local.UserInterestEntity>> = libraryRepository.getInterestsFlow()
        .map { list -> list.sortedByDescending { it.weight } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalWatchTimeFormatted: StateFlow<String> = libraryRepository.getHistory()
        .map { history ->
            val totalMs = history.sumOf { if (it.progressMs > 0) it.progressMs else 0L }
            val totalSec = totalMs / 1000
            val hours = totalSec / 3600
            val mins = (totalSec % 3600) / 60
            if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0m")

    val totalVideosWatched: StateFlow<Int> = libraryRepository.getHistory()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val topWatchedChannel: StateFlow<String?> = libraryRepository.getHistory()
        .map { history ->
            history.groupingBy { it.uploaderName }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val topChannelsList: StateFlow<List<ChannelStat>> = libraryRepository.getHistory()
        .map { history ->
            val total = history.size
            if (total == 0) emptyList()
            else {
                history.groupingBy { it.uploaderName }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .take(5)
                    .map { (channel, count) ->
                        ChannelStat(
                            channelName = channel,
                            count = count,
                            percentage = count.toFloat() / total
                        )
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subtitleFontSize: StateFlow<Float> = preferencesManager.subtitleFontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 16f)

    val subtitleBackgroundOpacity: StateFlow<Float> = preferencesManager.subtitleBackgroundOpacity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.65f)

    val appLanguage: StateFlow<String?> = preferencesManager.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val availableLocales = LocaleUtils.getAvailableLocales(context)

    fun setHistoryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setHistoryEnabled(enabled)
        }
    }

    fun setSearchHistoryPaused(paused: Boolean) {
        viewModelScope.launch {
            preferencesManager.setSearchHistoryPaused(paused)
        }
    }

    fun setPipEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setPipEnabled(enabled)
        }
    }

    fun setBackgroundPlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setBackgroundPlayEnabled(enabled)
        }
    }

    fun setAutoUpdateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAutoUpdateEnabled(enabled)
        }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDynamicColorEnabled(enabled)
        }
    }

    fun setBlockAiContentEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setBlockAiContentEnabled(enabled)
        }
    }

    fun setAppThemeColor(colorName: String) {
        viewModelScope.launch {
            preferencesManager.setAppThemeColor(colorName)
        }
    }

    fun setAppThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesManager.setAppThemeMode(mode)
        }
    }

    fun setPlayerGesturesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setPlayerGesturesEnabled(enabled)
        }
    }

    fun setRecommendationsPaused(paused: Boolean) {
        viewModelScope.launch {
            preferencesManager.setRecommendationsPaused(paused)
        }
    }

    fun setSubtitleFontSize(size: Float) {
        viewModelScope.launch {
            preferencesManager.setSubtitleFontSize(size)
        }
    }

    fun setSubtitleBackgroundOpacity(opacity: Float) {
        viewModelScope.launch {
            preferencesManager.setSubtitleBackgroundOpacity(opacity)
        }
    }

    fun setAppLanguage(tag: String?) {
        viewModelScope.launch {
            preferencesManager.setAppLanguage(tag)
            val appLocale: LocaleListCompat = if (tag == null) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(tag)
            }
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    fun clearLearnedInterests() {
        viewModelScope.launch {
            libraryRepository.clearAllInterests()
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            downloadRepository.clearAllDownloads()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            libraryRepository.clearHistory()
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            libraryRepository.clearSearchHistory()
        }
    }
}
