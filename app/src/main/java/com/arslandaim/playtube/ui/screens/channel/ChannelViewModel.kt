/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arslandaim.playtube.data.local.SubscriptionEntity
import com.arslandaim.playtube.domain.model.ChannelDetails
import com.arslandaim.playtube.domain.usecase.GetChannelDetailsUseCase
import com.arslandaim.playtube.domain.usecase.IsSubscribedUseCase
import com.arslandaim.playtube.domain.usecase.ToggleSubscriptionUseCase
import com.arslandaim.playtube.utils.VideoUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val getChannelDetailsUseCase: GetChannelDetailsUseCase,
    private val isSubscribedUseCase: IsSubscribedUseCase,
    private val toggleSubscriptionUseCase: ToggleSubscriptionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChannelUiState>(ChannelUiState.Loading)
    val uiState: StateFlow<ChannelUiState> = _uiState.asStateFlow()

    private val _isSubscribed = MutableStateFlow<Boolean?>(null)
    val isSubscribed: StateFlow<Boolean?> = _isSubscribed.asStateFlow()

    private var currentDetails: ChannelDetails? = null

    private var subscriptionJob: Job? = null

    fun loadChannel(channelUrl: String) {
        viewModelScope.launch {
            _uiState.value = ChannelUiState.Loading
            _isSubscribed.value = null // Reset for new channel
            
            // 1. Try immediate ID extraction from URL
            val immediateId = VideoUtils.extractChannelId(channelUrl)
            if (immediateId != null) {
                observeSubscription(immediateId)
            }
            
            getChannelDetailsUseCase(channelUrl)
                .onSuccess { details ->
                    currentDetails = details
                    _uiState.value = ChannelUiState.Success(details)
                    
                    // 2. Observe using the REAL canonical channel ID (handles cases where URL was a name/@handle)
                    if (details.id != immediateId) {
                        observeSubscription(details.id)
                    }
                }
                .onFailure { exception ->
                    _uiState.value = ChannelUiState.Error(exception.message ?: "Unknown error")
                }
        }
    }

    private fun observeSubscription(channelId: String) {
        subscriptionJob?.cancel()
        subscriptionJob = viewModelScope.launch {
            isSubscribedUseCase(channelId).collectLatest {
                _isSubscribed.value = it
            }
        }
    }

    fun toggleSubscription() {
        val details = currentDetails ?: return
        viewModelScope.launch {
            toggleSubscriptionUseCase(
                SubscriptionEntity(
                    channelId = details.id,
                    name = details.name,
                    thumbnailUrl = details.avatarUrl,
                    subscriberCount = details.subscriberCount
                )
            )
        }
    }
}

sealed interface ChannelUiState {
    object Loading : ChannelUiState
    data class Success(val details: ChannelDetails) : ChannelUiState
    data class Error(val message: String) : ChannelUiState
}
