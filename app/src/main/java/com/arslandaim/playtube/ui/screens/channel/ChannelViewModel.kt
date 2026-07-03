/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arslandaim.playtube.domain.model.ChannelDetails
import com.arslandaim.playtube.domain.usecase.GetChannelDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val getChannelDetailsUseCase: GetChannelDetailsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChannelUiState>(ChannelUiState.Loading)
    val uiState: StateFlow<ChannelUiState> = _uiState.asStateFlow()

    fun loadChannel(channelUrl: String) {
        viewModelScope.launch {
            _uiState.value = ChannelUiState.Loading
            getChannelDetailsUseCase(channelUrl)
                .onSuccess { details ->
                    _uiState.value = ChannelUiState.Success(details)
                }
                .onFailure { exception ->
                    _uiState.value = ChannelUiState.Error(exception.message ?: "Unknown error")
                }
        }
    }
}

sealed interface ChannelUiState {
    object Loading : ChannelUiState
    data class Success(val details: ChannelDetails) : ChannelUiState
    data class Error(val message: String) : ChannelUiState
}
